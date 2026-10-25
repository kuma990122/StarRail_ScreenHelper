package com.screenhelper.data.platform.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    fun start(output: File, onLimit: () -> Unit) {
        cancel()
        file = output
        @Suppress("DEPRECATION")
        val next = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        recorder = next
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(64_000)
            next.setAudioSamplingRate(24_000)
            next.setOutputFile(output.path)
            next.setMaxDuration(60_000)
            next.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onLimit() }
            next.prepare(); next.start()
        } catch (e: Exception) { cancel(); throw e }
    }
    fun stop(): File? {
        val current = recorder ?: return null
        recorder = null
        return try { current.stop(); file.also { file = null } }
        catch (_: RuntimeException) { file?.delete(); file = null; null }
        finally { current.release() }
    }
    fun cancel() { recorder?.run { runCatching { stop() }; release() }; recorder = null; file?.delete(); file = null }
}
