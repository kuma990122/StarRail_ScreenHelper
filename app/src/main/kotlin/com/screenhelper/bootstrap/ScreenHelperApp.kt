package com.screenhelper.bootstrap

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.screenhelper.bootstrap.receiver.ReminderReceiver
import com.screenhelper.business.character.CharacterService
import com.screenhelper.business.conversation.ConversationService
import com.screenhelper.business.memory.MemoryService
import com.screenhelper.business.memory.DailyMemoryService
import com.screenhelper.business.memory.DailyArchiveScheduler
import com.screenhelper.business.conversation.ContextBuilder
import com.screenhelper.business.pet.PetAction
import com.screenhelper.data.local.files.PetAssets
import java.io.File
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
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "screenhelper.db").addMigrations(AppDatabase.MIGRATION_1_2).build()
    val local = LocalRepository(database)
    val settings = SettingsRepository(context)
    val sounds = com.screenhelper.data.platform.media.PetSoundPlayer(context, settings.soundEffects, scope)
    val assets = AssetStore(context)
    val petAssets = PetAssets(context)
    val dailyMemory = DailyMemoryService(local, File(context.filesDir, "daily-archives"))
    val screenWatch = MutableStateFlow(ScreenWatchState())
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
        sounds.play(com.screenhelper.data.platform.media.PetSound.TODO)
        if (PetService.isRunning) showReminder(value) else notifications.remind(value)
    })
    val conversation = ConversationService(scope, local, ai, relationship, memory, ::showBubble)
    val screenInteraction = com.screenhelper.business.screen.ScreenInteractionService(local,ai,petState)
    val ready = scope.async {
        local.initialize()
        petAssets.install()
        ContextBuilder.mimiSkill = context.assets.open("skills/mimi.md").bufferedReader().use { it.readText() }
        val default = local.character("default")
        if (default.name == "小栖" || (default.name == "迷迷" && default.assets.isEmpty())) {
            local.saveCharacter(default.copy(name = "迷迷", subtitle = "把相遇写进记忆，一起走过逐火之旅♪",
                personality = "温柔、俏皮、共情且坚韧，偶尔以 ♪ 结尾。", world = "翁法罗斯，逐火之旅的世界。", backstory = "昔涟幼小的忆灵形态，在陪伴中积累情感与记忆。",
                assets = if (default.assets.isEmpty()) mapOf(Emotion.NEUTRAL to petAssets.path(PetAction.IDLE)) else default.assets))
        }
        memo.restore()
        DailyArchiveScheduler.schedule(context)
    }

    init {
        scope.launch {
            ready.await()
            runCatching { dailyMemory.archiveCompletedDays() }
            var archivedDay = java.time.LocalDate.now()
            while (isActive) {
                delay(60_000)
                val today = java.time.LocalDate.now()
                if(today != archivedDay) {
                    val result = runCatching { dailyMemory.archiveCompletedDays() }
                    if(result.isSuccess) archivedDay=today
                }
            }
        }
        scope.launch {
            ready.await()
            combine(local.characters, settings.selectedCharacter) { profiles, id -> profiles.firstOrNull { it.id == id } ?: profiles.firstOrNull() ?: CharacterProfile() }
                .collect { profile -> petState.update { it.copy(character = profile) } }
        }
    }
    fun showBubble(text: String, emotion: Emotion = Emotion.NEUTRAL) {
        petState.update { it.copy(bubble = text, emotion = emotion, bubbleId = System.nanoTime(), reminderId = null) }
    }
    fun showReminder(memo: Memo) {
        petState.update { it.copy(bubble = "到时间啦：${memo.title}", emotion = Emotion.HAPPY, bubbleId = System.nanoTime(), reminderId = memo.id) }
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
