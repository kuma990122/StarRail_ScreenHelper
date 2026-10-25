package com.screenhelper.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val personality: String,
    val world: String,
    val backstory: String,
    val assetsJson: String,
)

@Entity(tableName = "messages", indices = [Index(value = ["characterId", "createdAt"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val role: String,
    val text: String,
    val createdAt: Long,
    val imagePath: String?,
    val status: String,
    val emotion: String,
)

@Entity(tableName = "relationships")
data class RelationshipEntity(
    @PrimaryKey val characterId: String,
    val points: Int,
    val activeSeconds: Long,
    val dailyPoints: Int,
    val day: String,
    val lastAwardAt: Long,
    val lastInteractionAt: Long,
)

@Entity(tableName = "interaction_events", indices = [Index("createdAt")])
data class InteractionEntity(@PrimaryKey val id: String, val createdAt: Long)

@Entity(tableName = "memos", indices = [Index("dueAt")])
data class MemoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueAt: Long,
    val completed: Boolean,
    val version: Int,
    val deliveredVersion: Int,
    val timezone: String,
)

@Entity(tableName = "memories", indices = [Index("characterId")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val text: String,
    val createdAt: Long,
)
