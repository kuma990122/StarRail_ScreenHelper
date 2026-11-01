package com.screenhelper.data.local.database.dao

import androidx.room.*
import com.screenhelper.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM messages WHERE characterId=:id AND createdAt>=:start AND createdAt<:end AND status!='streaming' ORDER BY createdAt")
    suspend fun dayMessages(id: String, start: Long, end: Long): List<MessageEntity>
    @Query("SELECT characterId, createdAt FROM messages WHERE role='user' AND createdAt<:before ORDER BY createdAt")
    suspend fun memoryDates(before: Long): List<MemoryDate>
    @Query("SELECT * FROM daily_memories WHERE characterId=:id ORDER BY day DESC")
    fun dailyMemories(id: String): Flow<List<DailyMemoryEntity>>
    @Query("SELECT * FROM daily_memories WHERE characterId=:id AND day=:day")
    suspend fun dailyMemory(id: String, day: String): DailyMemoryEntity?
    @Upsert suspend fun saveDailyMemory(value: DailyMemoryEntity)
    @Query("SELECT * FROM characters ORDER BY name") fun characters(): Flow<List<CharacterEntity>>
    @Query("SELECT * FROM characters WHERE id=:id") suspend fun character(id: String): CharacterEntity?
    @Upsert suspend fun saveCharacter(value: CharacterEntity)
    @Query("SELECT COUNT(*) FROM characters") suspend fun characterCount(): Int

    @Query("SELECT * FROM (SELECT * FROM messages WHERE characterId=:characterId ORDER BY createdAt DESC LIMIT 100) ORDER BY createdAt ASC")
    fun messages(characterId: String): Flow<List<MessageEntity>>
    @Query("SELECT * FROM messages WHERE characterId=:characterId AND status='complete' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun history(characterId: String, limit: Int = 30): List<MessageEntity>
    @Upsert suspend fun saveMessage(value: MessageEntity)
    @Query("UPDATE messages SET status='interrupted' WHERE status='streaming'") suspend fun recoverMessages()
    @Query("DELETE FROM messages WHERE characterId=:id") suspend fun clearMessages(id: String)

    @Query("SELECT * FROM relationships WHERE characterId=:id") fun relationshipFlow(id: String): Flow<RelationshipEntity?>
    @Query("SELECT * FROM relationships WHERE characterId=:id") suspend fun relationship(id: String): RelationshipEntity?
    @Upsert suspend fun saveRelationship(value: RelationshipEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEvent(value: InteractionEntity): Long
    @Query("DELETE FROM interaction_events WHERE createdAt < :cutoff") suspend fun pruneEvents(cutoff: Long)

    @Query("SELECT * FROM memos ORDER BY completed ASC, dueAt ASC") fun memos(): Flow<List<MemoEntity>>
    @Query("SELECT * FROM memos WHERE id=:id") suspend fun memo(id: String): MemoEntity?
    @Query("SELECT * FROM memos WHERE completed=0 AND deliveredVersion < version") suspend fun pendingMemos(): List<MemoEntity>
    @Upsert suspend fun saveMemo(value: MemoEntity)
    @Query("DELETE FROM memos WHERE id=:id") suspend fun deleteMemo(id: String)
    @Query("UPDATE memos SET deliveredVersion=:version WHERE id=:id AND version=:version AND completed=0 AND deliveredVersion < :version AND dueAt <= :now")
    suspend fun claimReminder(id: String, version: Int, now: Long): Int

    @Query("SELECT * FROM memories WHERE characterId=:id ORDER BY createdAt DESC LIMIT 30") fun memories(id: String): Flow<List<MemoryEntity>>
    @Query("SELECT * FROM memories WHERE characterId=:id ORDER BY createdAt DESC LIMIT 8") suspend fun contextMemories(id: String): List<MemoryEntity>
    @Upsert suspend fun saveMemory(value: MemoryEntity)
    @Query("DELETE FROM memories WHERE id=:id") suspend fun deleteMemory(id: String)
}

data class MemoryDate(val characterId: String, val createdAt: Long)
