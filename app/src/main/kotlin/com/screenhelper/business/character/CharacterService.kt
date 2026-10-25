package com.screenhelper.business.character

import com.screenhelper.data.repository.LocalRepository
import com.screenhelper.model.*

class CharacterService(private val local: LocalRepository) {
    suspend fun save(profile: CharacterProfile) {
        require(profile.name.isNotBlank()) { "给角色起一个名字吧" }
        require(profile.name.length <= 30) { "名字请控制在 30 字以内" }
        require(profile.personality.length + profile.world.length + profile.backstory.length <= 6000) { "角色设定合计请控制在 6000 字以内" }
        local.saveCharacter(profile.copy(name = profile.name.trim()))
    }
    fun portrait(profile: CharacterProfile, emotion: Emotion): String? = profile.assets[emotion] ?: profile.assets[Emotion.NEUTRAL]
}
