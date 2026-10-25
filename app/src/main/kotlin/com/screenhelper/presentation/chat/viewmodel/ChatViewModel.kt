package com.screenhelper.presentation.chat.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import com.screenhelper.model.ChatMessage
import com.screenhelper.presentation.ScreenViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(app: AppContainer) : ScreenViewModel(app) {
    val messages = app.settings.selectedCharacter.flatMapLatest { app.local.messages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ChatMessage>())
    val runtime = app.conversation.state
    val draft = MutableStateFlow("")
    val attachment = MutableStateFlow<String?>(null)
    val transcribing = MutableStateFlow(false)
    fun attach(uri: Uri) = action { attachment.value = app.assets.importImage(uri) }
    fun captured(path: String) { attachment.value = path }
    fun send() = action {
        if (app.conversation.send(app.settings.selectedCharacter.first(), draft.value, attachment.value)) {
            draft.value = ""
            attachment.value = null
        }
    }
    fun cancel() = app.conversation.cancel()
    fun clearHistory() = action("当前角色的对话记录已清空") {
        require(!runtime.value.busy) { "请先停止生成" }
        app.local.clearMessages(app.settings.selectedCharacter.first())
    }
    fun transcribe(file: File) = action {
        transcribing.value = true
        try { draft.value = app.ai.transcribe(file) }
        finally { transcribing.value = false; file.delete() }
    }
}
