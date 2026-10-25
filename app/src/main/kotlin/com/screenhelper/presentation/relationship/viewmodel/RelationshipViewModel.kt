package com.screenhelper.presentation.relationship.viewmodel

import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import com.screenhelper.model.*
import com.screenhelper.presentation.ScreenViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class RelationshipViewModel(app: AppContainer) : ScreenViewModel(app) {
    val relation = app.settings.selectedCharacter.flatMapLatest { app.local.relationship(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RelationshipState("default"))
    val memories = app.settings.selectedCharacter.flatMapLatest { app.local.memories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<MemoryFact>())
    fun deleteMemory(id: String) = action("已移除这条共同记忆") { app.local.deleteMemory(id) }
}
