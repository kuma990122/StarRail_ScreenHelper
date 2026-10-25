package com.screenhelper.business.conversation

import com.screenhelper.business.memory.MemoryService
import com.screenhelper.business.relationship.RelationshipService
import com.screenhelper.data.repository.AiRepository
import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class ConversationService(
    private val scope: CoroutineScope,
    private val local: LocalRepository,
    private val ai: AiRepository,
    private val relationship: RelationshipService,
    private val memory: MemoryService,
    private val publish: (String, Emotion) -> Unit,
) {
    private val mutableState = MutableStateFlow(ChatRuntime())
    val state: StateFlow<ChatRuntime> = mutableState.asStateFlow()
    private var task: Job? = null

    @Synchronized
    fun send(characterId: String, text: String, imagePath: String? = null, maxTokens: Int = 512): Boolean {
        if (mutableState.value.busy || (text.isBlank() && imagePath == null)) return false
        if (text.length > 4000) { mutableState.value = ChatRuntime(error = "消息请控制在 4000 字以内"); return false }
        mutableState.value = ChatRuntime(busy = true, characterId = characterId)
        task = scope.launch {
            val now = System.currentTimeMillis()
            val userId = UUID.randomUUID().toString()
            val assistantId = UUID.randomUUID().toString()
            val reply = StringBuilder()
            var emotion = Emotion.NEUTRAL
            var status = "interrupted"
            var error: String? = null
            var savedAssistant = false
            try {
                val profile = local.character(characterId)
                val history = local.history(characterId)
                local.saveMessage(ChatMessage(userId, characterId, "user", text.trim(), now, imagePath))
                local.saveMessage(ChatMessage(assistantId, characterId, "assistant", "", now + 1, status = "streaming"))
                savedAssistant = true
                val relation = relationship.interact(characterId, "chat:$userId")
                memory.rememberExplicit(characterId, text)
                val request = ContextBuilder.build(profile, relation, local.contextMemories(characterId), history, text.ifBlank { "请看看这张图片。" }, imagePath)
                var lastUpdate = 0L
                ai.stream(request, maxTokens).collect { event ->
                    when (event) {
                        is AiEvent.Text -> {
                            reply.append(event.value)
                            val time = System.currentTimeMillis()
                            if (time - lastUpdate >= 45) {
                                mutableState.value = ChatRuntime(true, characterId, reply.toString())
                                lastUpdate = time
                            }
                        }
                        is AiEvent.Feeling -> emotion = event.emotion
                    }
                }
                check(reply.isNotBlank()) { "模型没有返回文本，请检查接口与模型配置" }
                status = "complete"
                publish(reply.toString(), emotion)
            } catch (_: CancellationException) {
                error = "已停止生成，已收到的内容会保留"
            } catch (e: Exception) {
                error = e.message?.take(180) ?: "请求失败，请检查配置后手动重试"
            } finally {
                withContext(NonCancellable) {
                    try {
                        if (savedAssistant) local.saveMessage(ChatMessage(assistantId, characterId, "assistant", reply.toString(), now + 1, status = status, emotion = emotion))
                    } finally { mutableState.value = ChatRuntime(characterId = characterId, error = error) }
                }
            }
        }
        return true
    }
    fun cancel() { task?.cancel() }
    fun clearError() { mutableState.update { it.copy(error = null) } }
}
