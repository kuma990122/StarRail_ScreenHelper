package com.screenhelper.data.remote.provider

import com.screenhelper.data.remote.dto.ReplyDecoder
import com.screenhelper.data.remote.dto.SseDecoder
import com.screenhelper.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CompatibleApiProvider(private val client: OkHttpClient) {
    fun stream(endpoint: EndpointConfig, input: ModelRequest, maxTokens: Int = 512): Flow<AiEvent> = callbackFlow {
        require(endpoint.key.isNotBlank()) { "请先在设置中填写 API Key，或开启演示模式" }
        require(endpoint.model.isNotBlank()) { "请填写模型名称" }
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", input.system))
        input.turns.forEach { turn ->
            val content: Any = if (turn.imagePath != null) {
                val file = File(turn.imagePath)
                require(file.length() <= 6 * 1024 * 1024) { "图片过大，请重新选择较小的图片" }
                val mime = when (file.extension.lowercase()) {
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }
                val encoded = file.readBytes().toByteString().base64()
                JSONArray().put(JSONObject().put("type", "text").put("text", turn.text))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:$mime;base64,$encoded")))
            } else turn.text
            messages.put(JSONObject().put("role", turn.role).put("content", content))
        }
        val json = JSONObject().put("model", endpoint.model).put("messages", messages).put("stream", true)
            .put("max_tokens", maxTokens.coerceIn(16, 1024)).put("temperature", 0.7)
        val request = Request.Builder().url(url(endpoint.baseUrl, "chat/completions"))
            .header("Authorization", "Bearer ${endpoint.key}").header("Accept", "text/event-stream")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (!call.isCanceled()) close(IOException("网络请求失败，请检查连接或 API 地址", e)) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!response.isSuccessful) throw IOException(errorMessage(response.code))
                        val source = requireNotNull(response.body).source()
                        val sse = SseDecoder()
                        val decoder = ReplyDecoder()
                        var stopped = false
                        var finished = false
                        fun consume(event: String?) {
                            if (event == null || stopped) return
                            if (event.trim() == "[DONE]") { stopped = true; return }
                            val payload = JSONObject(event)
                            if (payload.has("error")) throw IOException("模型接口返回错误，请检查模型与输入能力")
                            val choice = payload.optJSONArray("choices")?.optJSONObject(0)
                            if (choice != null && !choice.isNull("finish_reason")) finished = true
                            val delta = choice?.optJSONObject("delta")?.optString("content", "").orEmpty()
                            val visible = decoder.append(delta)
                            if (visible.isNotEmpty()) trySendBlocking(AiEvent.Text(visible)).getOrThrow()
                        }
                        while (!source.exhausted() && !call.isCanceled() && !stopped) {
                            consume(sse.line(source.readUtf8Line() ?: break))
                        }
                        consume(sse.flush())
                        check(stopped || finished) { "模型连接提前结束，已保留收到的内容" }
                        val (tail, emotion) = decoder.finish()
                        if (tail.isNotEmpty()) trySendBlocking(AiEvent.Text(tail)).getOrThrow()
                        trySendBlocking(AiEvent.Feeling(emotion)).getOrThrow()
                        close()
                    } catch (e: Exception) { if (!call.isCanceled()) close(e) }
                }
            }
        })
        awaitClose { call.cancel() }
    }

    suspend fun transcribe(endpoint: EndpointConfig, audio: File): String {
        require(endpoint.key.isNotBlank() && endpoint.model.isNotBlank()) { "请配置语音识别 API，或使用系统语音输入" }
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("model", endpoint.model)
            .addFormDataPart("file", audio.name, RequestBody.create("audio/mp4".toMediaType(), audio)).build()
        val request = Request.Builder().url(url(endpoint.baseUrl, "audio/transcriptions"))
            .header("Authorization", "Bearer ${endpoint.key}").post(body).build()
        return await(client.newCall(request)).use { response ->
            if (!response.isSuccessful) throw IOException(errorMessage(response.code))
            JSONObject(requireNotNull(response.body).string()).optString("text").also { require(it.isNotBlank()) { "没有识别到语音内容" } }
        }
    }

    private suspend fun await(call: Call): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        })
    }
    companion object {
        fun url(base: String, suffix: String): String {
            val clean = base.trim().trimEnd('/')
            require(clean.startsWith("https://") || clean.startsWith("http://localhost:") || clean.startsWith("http://127.0.0.1:") || clean.startsWith("http://10.0.2.2:")) { "API 地址需要使用 HTTPS" }
            require(clean.isNotEmpty()) { "请填写 API 地址" }
            return if (clean.endsWith("/$suffix")) clean else "$clean/$suffix"
        }
        private fun errorMessage(code: Int) = when (code) {
            401, 403 -> "API 鉴权失败，请检查 Key 与访问权限（$code）"
            402 -> "API 余额不足（402）"
            404 -> "API 地址或模型不存在（404）"
            429 -> "请求受限，请稍后手动重试（429）"
            else -> "模型请求未成功（HTTP $code）"
        }
    }
}
