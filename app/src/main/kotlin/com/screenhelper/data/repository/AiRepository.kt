package com.screenhelper.data.repository

import com.screenhelper.data.local.preferences.SettingsRepository
import com.screenhelper.data.remote.provider.CompatibleApiProvider
import com.screenhelper.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File

class AiRepository(private val settings: SettingsRepository, private val provider: CompatibleApiProvider) {
    fun stream(input: ModelRequest, maxTokens: Int = 512): Flow<AiEvent> = flow {
        val config = settings.current()
        if (config.demoMode) {
            val text = input.turns.lastOrNull()?.text.orEmpty()
            val emotion = when {
                text.contains("生气") -> Emotion.ANGRY
                text.contains("难过") || text.contains("悲伤") -> Emotion.SAD
                text.contains("开心") || text.contains("谢谢") -> Emotion.HAPPY
                else -> Emotion.NEUTRAL
            }
            val reply = when {
                input.turns.lastOrNull()?.hasImages == true -> "收到画面啦♪ 演示模式只验证流程，配置视觉模型后，我就能陪你看看屏幕上的小故事。"
                text.startsWith("记住") -> "好，我会把这件小事收进我们的共同记忆。下次聊天，也会带上它。"
                emotion == Emotion.SAD -> "听起来，今天有一点难熬。先慢慢呼吸一下吧，我在这里。想和我说说发生了什么吗？"
                emotion == Emotion.ANGRY -> "嗯，我听见你的不开心了。我们先把事情放慢一点，一起理清楚，好吗？"
                emotion == Emotion.HAPPY -> "看到你开心，我的星灯也跟着亮起来了。把这份小小的快乐，好好收藏吧。"
                text.contains("摸") || text.contains("触摸") -> "被你发现啦。今天的星灯也在认真营业，陪你一起把日子过得柔软一点。"
                else -> "我在呢。刚刚替你留住了一点星光。今天有什么想分享的小事，或者想一起完成的计划吗？"
            }
            for (chunk in reply.chunked(3)) { emit(AiEvent.Text(chunk)); delay(24) }
            emit(AiEvent.Feeling(emotion))
        } else {
            val hasImage = input.turns.any { it.hasImages }
            val endpoint = if (hasImage && config.vision.model.isNotBlank()) config.vision else config.text
            require(!hasImage || config.vision.model.isNotBlank() || config.textSupportsImages) { "请在设置中配置视觉模型，或确认当前文本模型支持图片" }
            emitAll(provider.stream(endpoint, input, maxTokens))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun transcribe(audio: File): String = withContext(Dispatchers.IO) {
        val config = settings.current()
        require(!config.demoMode) { "演示模式不上传录音；可用系统语音输入，或配置语音 API 后关闭演示模式" }
        provider.transcribe(config.speech, audio)
    }
}
