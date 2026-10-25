package com.screenhelper.bootstrap

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.screenhelper.bootstrap.receiver.ReminderReceiver
import com.screenhelper.business.character.CharacterService
import com.screenhelper.business.conversation.ConversationService
import com.screenhelper.business.memory.MemoryService
import com.screenhelper.business.memo.MemoService
import com.screenhelper.business.relationship.RelationshipService
import com.screenhelper.data.local.database.AppDatabase
import com.screenhelper.data.local.files.AssetStore
import com.screenhelper.data.local.preferences.SettingsRepository
import com.screenhelper.data.platform.alarm.ReminderScheduler
import com.screenhelper.data.remote.provider.CompatibleApiProvider
import com.screenhelper.data.repository.*
import com.screenhelper.model.*
import com.screenhelper.presentation.notification.NotificationPresenter
import com.screenhelper.presentation.pet.PetService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ScreenHelperApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "screenhelper.db").build()
    val local = LocalRepository(database)
    val settings = SettingsRepository(context)
    val assets = AssetStore(context)
    val notifications = NotificationPresenter(context)
    val scheduler = ReminderScheduler(context, ReminderReceiver::class.java)
    val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
    val ai = AiRepository(settings, CompatibleApiProvider(http))
    val character = CharacterService(local)
    val relationship = RelationshipService(local)
    val memory = MemoryService(local)
    val petState = MutableStateFlow(PetState())
    val captureResult = MutableStateFlow<CaptureResult?>(null)
    val uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private var activityVisible = false
    private var petVisible = false
    private var engagementTicker: Job? = null
    val memo = MemoService(local, scheduler, { value ->
        if (PetService.isRunning) showBubble("到时间啦：${value.title}", Emotion.HAPPY) else notifications.remind(value)
    })
    val conversation = ConversationService(scope, local, ai, relationship, memory, ::showBubble)
    val ready = scope.async { local.initialize(); memo.restore() }

    init {
        scope.launch {
            ready.await()
            combine(local.characters, settings.selectedCharacter) { profiles, id -> profiles.firstOrNull { it.id == id } ?: profiles.firstOrNull() ?: CharacterProfile() }
                .collect { profile -> petState.update { it.copy(character = profile) } }
        }
    }
    fun showBubble(text: String, emotion: Emotion = Emotion.NEUTRAL) {
        petState.update { it.copy(bubble = text, emotion = emotion, bubbleId = System.nanoTime()) }
    }
    @Synchronized
    fun presence(activity: Boolean? = null, pet: Boolean? = null) {
        activity?.let { activityVisible = it }
        pet?.let { petVisible = it }
        if (activityVisible || petVisible) {
            if (engagementTicker?.isActive == true) return
            engagementTicker = scope.launch {
                ready.await()
                while (isActive) {
                    delay(30_000)
                    relationship.active(petState.value.character.id, "active:${java.util.UUID.randomUUID()}", 30)
                }
            }
        } else { engagementTicker?.cancel(); engagementTicker = null }
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as ScreenHelperApp).container
