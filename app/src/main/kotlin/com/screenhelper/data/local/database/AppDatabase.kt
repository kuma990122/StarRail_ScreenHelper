package com.screenhelper.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.screenhelper.data.local.database.dao.AppDao
import com.screenhelper.data.local.database.entity.*

@Database(entities = [CharacterEntity::class, MessageEntity::class, RelationshipEntity::class, InteractionEntity::class, MemoEntity::class, MemoryEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
