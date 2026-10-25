package com.screenhelper.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

open class ScreenViewModel(protected val app: AppContainer) : ViewModel() {
    protected fun action(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                app.ready.await()
                block()
                success?.let { app.uiMessages.emit(it) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { app.uiMessages.emit(e.message?.take(180) ?: "操作未完成，请重试") }
        }
    }
}
