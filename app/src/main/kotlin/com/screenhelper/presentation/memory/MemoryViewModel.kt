package com.screenhelper.presentation.memory

import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import com.screenhelper.presentation.ScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MemoryViewModel(app: AppContainer) : ScreenViewModel(app) {
    val memories = app.settings.selectedCharacter.flatMapLatest { app.local.dailyMemories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val generating = MutableStateFlow(false)
    fun generate() = action {
        if (!generating.compareAndSet(false, true)) return@action
        try {
            val result = withContext(Dispatchers.IO) { app.dailyMemory.generate(app.settings.selectedCharacter.first(), LocalDate.now()) }
            app.uiMessages.emit(if (result == null) "今天还没有可整理的对话" else "今日回忆已保存")
        } finally { generating.value = false }
    }
}
