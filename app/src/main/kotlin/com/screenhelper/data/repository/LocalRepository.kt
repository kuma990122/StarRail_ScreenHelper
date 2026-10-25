package com.screenhelper.data.repository

import androidx.room.withTransaction
import com.screenhelper.data.local.database.AppDatabase
import com.screenhelper.data.local.database.entity.*
import com.screenhelper.model.*
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class LocalRepository(private val database: AppDatabase) {
    private val dao = database.dao()
    val characters = dao.characters().map { values -> values.map { it.model() } }
    val memos = dao.memos().map { values -> values.map { it.model() } }
    suspend fun initialize() {
        if (dao.characterCount() == 0) saveCharacter(CharacterProfile())
        dao.recoverMessages()
    }
    suspend fun character(id: String) = dao.character(id)?.model() ?: CharacterProfile()
    suspend fun saveCharacter(value: CharacterProfile) = dao.saveCharacter(CharacterEntity(value.id, value.name, value.subtitle, value.personality, value.world, value.backstory, JSONObject(value.assets.mapKeys { it.key.name }).toString()))
    fun messages(id: String) = dao.messages(id).map { rows -> rows.map { it.model() } }
    suspend fun history(id: String) = dao.history(id).reversed().map { it.model() }
    suspend fun saveMessage(value: ChatMessage) = dao.saveMessage(MessageEntity(value.id, value.characterId, value.role, value.text, value.createdAt, value.imagePath, value.status, value.emotion.name))
    suspend fun clearMessages(id: String) = dao.clearMessages(id)
    fun relationship(id: String) = dao.relationshipFlow(id).map { it?.model() ?: RelationshipState(id) }
    suspend fun currentRelationship(id: String) = dao.relationship(id)?.model() ?: RelationshipState(id)
    suspend fun updateRelationship(eventId: String, now: Long, id: String, transform: (RelationshipState) -> RelationshipState): RelationshipState = database.withTransaction {
        val state = currentRelationship(id)
        if (dao.insertEvent(InteractionEntity(eventId, now)) == -1L) return@withTransaction state
        val next = transform(state)
        dao.saveRelationship(RelationshipEntity(next.characterId, next.points, next.activeSeconds, next.dailyPoints, next.day, next.lastAwardAt, next.lastInteractionAt))
        dao.pruneEvents(now - 7 * 86_400_000L)
        next
    }
    suspend fun memo(id: String) = dao.memo(id)?.model()
    suspend fun saveMemo(value: Memo) = dao.saveMemo(value.entity())
    suspend fun deleteMemo(id: String) = dao.deleteMemo(id)
    suspend fun pendingMemos() = dao.pendingMemos().map { it.model() }
    suspend fun claimReminder(id: String, version: Int, now: Long): Memo? = database.withTransaction {
        if (dao.claimReminder(id, version, now) != 1) null else dao.memo(id)?.model()
    }
    fun memories(id: String) = dao.memories(id).map { rows -> rows.map { it.model() } }
    suspend fun contextMemories(id: String) = dao.contextMemories(id).map { it.model() }
    suspend fun saveMemory(value: MemoryFact) = dao.saveMemory(MemoryEntity(value.id, value.characterId, value.text, value.createdAt))
    suspend fun deleteMemory(id: String) = dao.deleteMemory(id)

    private fun CharacterEntity.model(): CharacterProfile {
        val json = JSONObject(assetsJson)
        val assets = Emotion.entries.mapNotNull { e -> json.optString(e.name).takeIf { it.isNotBlank() }?.let { e to it } }.toMap()
        return CharacterProfile(id, name, subtitle, personality, world, backstory, assets)
    }
    private fun MessageEntity.model() = ChatMessage(id, characterId, role, text, createdAt, imagePath, status, Emotion.parse(emotion))
    private fun RelationshipEntity.model() = RelationshipState(characterId, points, activeSeconds, dailyPoints, day, lastAwardAt, lastInteractionAt)
    private fun MemoEntity.model() = Memo(id, title, dueAt, completed, version, deliveredVersion, timezone)
    private fun Memo.entity() = MemoEntity(id, title, dueAt, completed, version, deliveredVersion, timezone)
    private fun MemoryEntity.model() = MemoryFact(id, characterId, text, createdAt)
}
