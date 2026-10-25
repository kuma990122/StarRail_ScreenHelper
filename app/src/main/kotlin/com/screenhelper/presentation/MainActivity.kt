package com.screenhelper.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.screenhelper.bootstrap.*
import com.screenhelper.data.platform.media.ScreenCaptureService
import com.screenhelper.model.*
import com.screenhelper.presentation.character.ui.CharacterScreen
import com.screenhelper.presentation.character.viewmodel.CharacterViewModel
import com.screenhelper.presentation.chat.ui.ChatScreen
import com.screenhelper.presentation.chat.viewmodel.ChatViewModel
import com.screenhelper.presentation.memo.ui.MemoScreen
import com.screenhelper.presentation.memo.viewmodel.MemoViewModel
import com.screenhelper.presentation.navigation.HomeScreen
import com.screenhelper.presentation.pet.PetService
import com.screenhelper.presentation.relationship.ui.RelationshipScreen
import com.screenhelper.presentation.relationship.viewmodel.RelationshipViewModel
import com.screenhelper.presentation.settings.ui.SettingsScreen
import com.screenhelper.presentation.settings.viewmodel.SettingsViewModel
import com.screenhelper.presentation.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val navigationRequest = MutableStateFlow("home" to 0L)
    private val capture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ContextCompat.startForegroundService(this, Intent(this, ScreenCaptureService::class.java)
                .putExtra("result_code", result.resultCode).putExtra("result_data", result.data))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        navigationRequest.value = (intent.getStringExtra("tab") ?: "home") to System.nanoTime()
        setContent { ScreenHelperTheme { RootScreen(appContainer, navigationRequest.collectAsStateWithLifecycle().value) {
            capture.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
        } } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigationRequest.value = (intent.getStringExtra("tab") ?: "home") to System.nanoTime()
    }
    override fun onResume() {
        super.onResume()
        appContainer.presence(activity = true)
        appContainer.scope.launch { appContainer.ready.await(); appContainer.relationship.interact(appContainer.petState.value.character.id, "visit:${UUID.randomUUID()}", 1) }
    }
    override fun onPause() { appContainer.presence(activity = false); super.onPause() }
}

private inline fun <reified T : ViewModel> factory(crossinline create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}

@Composable
private fun RootScreen(app: AppContainer, request: Pair<String, Long>, captureScreen: () -> Unit) {
    val characterVM: CharacterViewModel = viewModel(factory = remember(app) { factory { CharacterViewModel(app) } })
    val chatVM: ChatViewModel = viewModel(factory = remember(app) { factory { ChatViewModel(app) } })
    val relationshipVM: RelationshipViewModel = viewModel(factory = remember(app) { factory { RelationshipViewModel(app) } })
    val memoVM: MemoViewModel = viewModel(factory = remember(app) { factory { MemoViewModel(app) } })
    val settingsVM: SettingsViewModel = viewModel(factory = remember(app) { factory { SettingsViewModel(app) } })
    val character by characterVM.active.collectAsStateWithLifecycle()
    val relation by relationshipVM.relation.collectAsStateWithLifecycle()
    val memos by memoVM.memos.collectAsStateWithLifecycle()
    val settings by settingsVM.settings.collectAsStateWithLifecycle()
    val running by PetService.running.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf("home") }
    val scope = rememberCoroutineScope()
    val notifyPermission = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun startPet() {
        ContextCompat.startForegroundService(context, Intent(context, PetService::class.java))
        if (Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val overlayPermission = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) startPet()
    }
    fun togglePet() {
        if (running) context.stopService(Intent(context, PetService::class.java))
        else if (Settings.canDrawOverlays(context)) startPet()
        else overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
    }
    LaunchedEffect(request) { tab = request.first }
    LaunchedEffect(app) { app.uiMessages.collect { snackbars.showSnackbar(it) } }
    LaunchedEffect(app) { app.captureResult.collect { result ->
        if (result != null) {
            result.path?.let { chatVM.captured(it); tab = "chat" }
            result.error?.let { snackbars.showSnackbar(it) }
            app.captureResult.value = null
        }
    } }
    Scaffold(containerColor = CanvasColor, snackbarHost = { SnackbarHost(snackbars) }, bottomBar = {
        Surface(color = WarmWhite, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                listOf(Triple("home","陪伴","home"),Triple("chat","对话","chat"),Triple("memo","备忘","memo"),Triple("character","角色","user"),Triple("settings","设置","settings")).forEach { (id,label,icon) ->
                    val selected = tab == id || (tab == "relationship" && id == "home")
                    Column(Modifier.weight(1f).testTag("nav_$id").clickable { tab = id }.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(52.dp,31.dp).background(if(selected) Mint else Color.Transparent,RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Glyph(icon,Modifier.size(21.dp),if(selected) Green else Muted) }
                        Text(label,fontSize = 10.sp,color = if(selected) Green else Muted,fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when(tab) {
                "chat" -> ChatScreen(character, settings, chatVM, captureScreen)
                "memo" -> MemoScreen(memoVM) { if (Build.VERSION.SDK_INT >= 31) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:${context.packageName}"))) }
                "character" -> CharacterScreen(characterVM)
                "settings" -> SettingsScreen(settingsVM, running, ::togglePet, {
                    if(Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }, { if(Build.VERSION.SDK_INT >= 31) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:${context.packageName}"))) })
                "relationship" -> RelationshipScreen(relationshipVM) { tab="home" }
                else -> HomeScreen(character, relation, memos, running, settings.demoMode, ::togglePet, { tab="chat" }, { tab="memo" }, { tab="relationship" })
            }
        }
    }
}
