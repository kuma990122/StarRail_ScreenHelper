package com.screenhelper.presentation.character.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import com.screenhelper.model.*
import com.screenhelper.presentation.ScreenViewModel
import kotlinx.coroutines.flow.*
import java.util.UUID

class CharacterViewModel(app: AppContainer) : ScreenViewModel(app) {
    val profiles = app.local.characters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(CharacterProfile()))
    val selectedId = app.settings.selectedCharacter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default")
    val active = combine(profiles, selectedId) { list, id -> list.firstOrNull { it.id == id } ?: list.firstOrNull() ?: CharacterProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CharacterProfile())
    fun save(value: CharacterProfile) = action("角色设定已保存") { app.character.save(value) }
    fun select(id: String) = action { app.settings.selectCharacter(id) }
    fun create() = action("新角色已创建，可以开始编辑") {
        val profile = CharacterProfile(id = UUID.randomUUID().toString(), name = "新的伙伴")
        app.character.save(profile)
        app.settings.selectCharacter(profile.id)
    }
    fun upload(emotion: Emotion, uri: Uri, characterId: String? = null) = action("${emotion.label}立绘已更新") {
        val id = characterId ?: app.settings.selectedCharacter.first()
        val profile = app.local.character(id)
        val path = app.assets.importPortrait(uri)
        app.character.save(profile.copy(assets = profile.assets + (emotion to path)))
    }
    fun resetImage(emotion: Emotion) = action("已恢复默认立绘") {
        val profile = app.local.character(app.settings.selectedCharacter.first())
        app.character.save(profile.copy(assets = profile.assets - emotion))
    }
}
