package com.screenhelper.presentation.memo.viewmodel

import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import com.screenhelper.model.Memo
import com.screenhelper.presentation.ScreenViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MemoViewModel(app: AppContainer) : ScreenViewModel(app) {
    val memos = app.local.memos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Memo>())
    fun save(title: String, dueAt: Long, id: String? = null) = action("提醒已安排") { app.memo.save(title, dueAt, id) }
    fun toggle(id: String) = action { app.memo.toggle(id) }
    fun delete(id: String) = action("待办已删除") { app.memo.delete(id) }
    fun isExactAvailable() = app.scheduler.isExactAvailable()
}
