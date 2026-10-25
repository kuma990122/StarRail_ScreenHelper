package com.screenhelper.data.local.files

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AssetStore(private val context: Context) {
    private val imageDir get() = File(context.filesDir, "images").apply { mkdirs() }
    suspend fun importImage(uri: Uri): String = import(uri, preserveAnimation = false)
    suspend fun importPortrait(uri: Uri): String = import(uri, preserveAnimation = true)

    private suspend fun import(uri: Uri, preserveAnimation: Boolean): String = withContext(Dispatchers.IO) {
        val temporary = File(context.cacheDir, "import-${UUID.randomUUID()}")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取图片" }
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 15 * 1024 * 1024) { "请选择小于 15 MB 的图片" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val signature = ByteArray(12)
            val signatureLength = temporary.inputStream().use { it.read(signature) }
            val extension = when {
                signatureLength >= 6 && String(signature, 0, 6, Charsets.US_ASCII).let { it == "GIF87a" || it == "GIF89a" } -> "gif"
                signatureLength >= 12 && String(signature, 0, 4, Charsets.US_ASCII) == "RIFF" && String(signature, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
                signatureLength >= 8 && signature.take(8).map { it.toInt() and 0xff } == listOf(137, 80, 78, 71, 13, 10, 26, 10) -> "png"
                signatureLength >= 2 && signature[0] == 0xff.toByte() && signature[1] == 0xd8.toByte() -> "jpg"
                else -> throw IllegalArgumentException("请选择 GIF、WebP、PNG 或 JPG 图片")
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "图片格式无法识别" }
            require(maxOf(bounds.outWidth, bounds.outHeight) <= 4096) { "图片长边不能超过 4096 像素" }
            val output = if (preserveAnimation && (extension == "gif" || extension == "webp")) {
                File(imageDir, "${UUID.randomUUID()}.$extension").also { temporary.copyTo(it) }
            } else {
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1536) sample *= 2
                val bitmap = requireNotNull(BitmapFactory.decodeFile(temporary.path, BitmapFactory.Options().apply { inSampleSize = sample })) { "图片解码失败" }
                File(imageDir, "${UUID.randomUUID()}.png").also {
                    try { it.outputStream().use { outputStream -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream) } }
                    finally { bitmap.recycle() }
                }
            }
            output.path
        } finally { temporary.delete() }
    }
    fun newAudioFile() = File(context.cacheDir, "voice-${UUID.randomUUID()}.m4a")
}
