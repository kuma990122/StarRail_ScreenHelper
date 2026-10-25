package com.screenhelper.business.memory

import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.MemoryFact
import java.util.UUID

class MemoryService(private val local: LocalRepository) {
    suspend fun rememberExplicit(characterId: String, text: String) {
        val fact = Regex("^记住[：:，,]?\\s*(.+)", RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.get(1)?.trim() ?: return
        if (fact.isBlank()) return
        if (local.contextMemories(characterId).any { it.text == fact.take(300) }) return
        local.saveMemory(MemoryFact(UUID.randomUUID().toString(), characterId, fact.take(300), System.currentTimeMillis()))
    }
}
