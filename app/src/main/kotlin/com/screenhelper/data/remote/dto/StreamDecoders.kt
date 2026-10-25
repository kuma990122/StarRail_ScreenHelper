package com.screenhelper.data.remote.dto

import com.screenhelper.model.Emotion

/** SSE framing is independent from network packet and line boundaries. */
class SseDecoder {
    private val data = mutableListOf<String>()
    fun line(value: String): String? {
        if (value.isEmpty()) return flush()
        if (value.startsWith("data:")) data += value.substring(5).removePrefix(" ")
        return null
    }
    fun flush(): String? = data.takeIf { it.isNotEmpty() }?.joinToString("\n").also { data.clear() }
}

/** Hide an emotion suffix even when its prefix is split across SSE events. */
class ReplyDecoder {
    private val raw = StringBuilder()
    private var emitted = 0
    fun append(value: String): String {
        raw.append(value)
        val text = raw.toString()
        val markerAt = text.indexOf(MARKER)
        val safeEnd = if (markerAt >= 0) markerAt else {
            val held = (1 until MARKER.length).lastOrNull { text.endsWith(MARKER.take(it)) } ?: 0
            text.length - held
        }
        val delta = text.substring(emitted, maxOf(emitted, safeEnd))
        emitted = maxOf(emitted, safeEnd)
        return delta
    }
    fun finish(): Pair<String, Emotion> {
        val text = raw.toString()
        val markerAt = text.indexOf(MARKER)
        val end = if (markerAt >= 0) markerAt else text.length
        val tail = text.substring(minOf(emitted, end), end)
        val emotion = Regex("\\[\\[emotion:([a-zA-Z]+)]]").find(text)?.groupValues?.get(1)
        emitted = end
        return tail to Emotion.parse(emotion)
    }
    private companion object { const val MARKER = "[[emotion:" }
}
