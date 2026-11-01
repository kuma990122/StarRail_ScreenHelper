package com.screenhelper.business.memory

import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.*
import java.util.zip.GZIPOutputStream

object DailySummaryPolicy {
    // Local extractive summary: broad user topics, no assistant speculation or long details.
    fun summarize(messages: List<ChatMessage>): String {
        val topics = messages.filter { it.role == "user" && !it.text.startsWith("（用户") }.map { m ->
            m.text.replace(Regex("^记住[：:，,]?\\s*"), "")
                .replace(Regex("https?://\\S+|[\\w.+-]+@[\\w.-]+|\\d{7,}"), "…")
                .split(Regex("[。！？!?\\n]"))[0].trim().take(36)
        }.filter { it.isNotBlank() }.distinct()
        if (topics.isEmpty()) return "今天一起看了图片，留下了一段小小的陪伴。"
        val chosen = if (topics.size <= 5) topics else (0..4).map { topics[it * (topics.size - 1) / 4] }
        return "今天你聊到了：${chosen.joinToString("；")}。".take(220)
    }
    fun hash(messages: List<ChatMessage>): String = MessageDigest.getInstance("SHA-256")
        .digest(messages.joinToString("\u0000") { "${it.id}:${it.role}:${it.text}:${it.status}:${it.imagePath}" }.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

class DailyMemoryService(private val local: LocalRepository, private val archiveDirectory: File) {
    private val mutex = Mutex()
    suspend fun generate(id: String, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DailyMemory? = mutex.withLock {
        val messages = local.dayMessages(id, day.atStartOfDay(zone).toInstant().toEpochMilli(), day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        if (messages.none { it.role == "user" }) return@withLock null
        val hash = DailySummaryPolicy.hash(messages)
        val previous = local.dailyMemory(id, day.toString())
        if (previous?.sourceHash == hash && File(previous.archivePath).isFile) return@withLock previous
        archiveDirectory.mkdirs()
        val safeId = MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
        val output = File(archiveDirectory, "$safeId-$day-$hash.json.gz")
        val temp = File(archiveDirectory, "${output.name}.tmp")
        try {
            val rows = JSONArray()
            messages.forEach { rows.put(JSONObject().put("id", it.id).put("role", it.role).put("text", it.text).put("createdAt", it.createdAt).put("status", it.status).put("imagePath", it.imagePath)) }
            GZIPOutputStream(temp.outputStream()).bufferedWriter(Charsets.UTF_8).use { it.write(JSONObject().put("day",day.toString()).put("timezone",zone.id).put("messages",rows).toString()) }
            check(temp.renameTo(output) || output.isFile) { "回忆归档保存失败" }
            val memory = DailyMemory(id,day.toString(),DailySummaryPolicy.summarize(messages),hash,messages.size,output.path,System.currentTimeMillis(),zone.id)
            local.saveDailyMemory(memory)
            if (previous?.archivePath != output.path) previous?.archivePath?.let { File(it).delete() }
            memory
        } finally { temp.delete() }
    }
    suspend fun archiveCompletedDays(zone: ZoneId = ZoneId.systemDefault()) {
        val today = LocalDate.now(zone)
        local.memoryDates(today.atStartOfDay(zone).toInstant().toEpochMilli())
            .map { it.characterId to Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }.distinct()
            .forEach { (id, day) -> generate(id, day, zone) }
    }
}
