package com.screenhelper.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.screenhelper.data.local.credentials.CredentialVault
import com.screenhelper.model.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

private val Context.appPreferences by preferencesDataStore("screenhelper")

class SettingsRepository(context: Context, private val vault: CredentialVault = CredentialVault()) {
    private val store = context.applicationContext.appPreferences
    private val settingsKey = stringPreferencesKey("provider_settings")
    private val selectedKey = stringPreferencesKey("selected_character")
    private val soundEffectsKey = booleanPreferencesKey("sound_effects")
    val soundEffects = store.data.map { it[soundEffectsKey] ?: true }.distinctUntilChanged()
    suspend fun setSoundEffects(enabled: Boolean) { store.edit { it[soundEffectsKey] = enabled } }
    val selectedCharacter = store.data.map { it[selectedKey] ?: "default" }.distinctUntilChanged()
    val settings = store.data.map { preferences -> decode(preferences[settingsKey]) }.distinctUntilChanged()
    suspend fun current() = settings.first()
    suspend fun selectCharacter(id: String) { store.edit { it[selectedKey] = id } }
    suspend fun save(value: AppSettings) {
        fun endpoint(e: EndpointConfig) = JSONObject().put("baseUrl", e.baseUrl.trim().trimEnd('/')).put("model", e.model.trim()).put("key", vault.encrypt(e.key.trim()))
        val json = JSONObject().put("demo", value.demoMode).put("touchTalk", value.touchTalk)
            .put("textImages", value.textSupportsImages).put("text", endpoint(value.text))
            .put("vision", endpoint(value.vision)).put("speech", endpoint(value.speech))
        store.edit { it[settingsKey] = json.toString() }
    }
    private fun decode(raw: String?): AppSettings {
        if (raw == null) return AppSettings()
        val json = JSONObject(raw)
        fun endpoint(name: String): EndpointConfig {
            val e = json.optJSONObject(name) ?: return EndpointConfig()
            return EndpointConfig(e.optString("baseUrl"), e.optString("model"), vault.decrypt(e.optString("key")))
        }
        return AppSettings(json.optBoolean("demo", true), endpoint("text"), endpoint("vision"), endpoint("speech"), json.optBoolean("textImages"), json.optBoolean("touchTalk", true))
    }
}
