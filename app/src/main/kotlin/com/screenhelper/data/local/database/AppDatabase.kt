package com.screenhelper.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.screenhelper.data.local.database.dao.AppDao
import com.screenhelper.data.local.database.entity.*

@Database(entities = [CharacterEntity::class, MessageEntity::class, RelationshipEntity::class, InteractionEntity::class, MemoEntity::class, MemoryEntity::class, DailyMemoryEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_memories (characterId TEXT NOT NULL, day TEXT NOT NULL, summary TEXT NOT NULL, sourceHash TEXT NOT NULL, messageCount INTEGER NOT NULL, archivePath TEXT NOT NULL, generatedAt INTEGER NOT NULL, timezone TEXT NOT NULL, PRIMARY KEY(characterId, day))")
            }
        }
    }
}
