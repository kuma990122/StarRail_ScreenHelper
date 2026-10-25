package com.screenhelper.presentation.chat.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.data.platform.media.VoiceRecorder
import com.screenhelper.model.*
import com.screenhelper.presentation.chat.viewmodel.ChatViewModel
import com.screenhelper.presentation.theme.*
import java.io.File
import java.util.Locale

@Composable
fun ChatScreen(character: CharacterProfile, settings: AppSettings, vm: ChatViewModel, captureScreen: () -> Unit) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val runtime by vm.runtime.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val attachment by vm.attachment.collectAsStateWithLifecycle()
    val transcribing by vm.transcribing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    var recording by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var clearConfirmation by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { vm.attach(it) } }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { vm.draft.value = it }
    }
    fun stopRecording() { recording = false; val file = recorder.stop(); if (file != null) vm.transcribe(file) else inputError = "录音太短，请再试一次" }
    fun startRecording() {
        runCatching { recorder.start(context.appContainer.assets.newAudioFile()) { stopRecording() }; recording = true }.onFailure { inputError = "无法录音，请检查麦克风权限与设备" }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) startRecording() else inputError = "麦克风权限未开启" }
    fun voiceInput() {
        if (recording) { stopRecording(); return }
        if (settings.speech.model.isNotBlank() && !settings.demoMode) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            runCatching { speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag()).putExtra(RecognizerIntent.EXTRA_PROMPT, "想对${character.name}说些什么？")) }
                .onFailure { inputError = "设备没有系统语音识别服务，请在设置中配置语音转写 API" }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP && recording) { recorder.cancel(); recording = false } }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); recorder.cancel() }
    }
    LaunchedEffect(messages.size, runtime.streamingText.length) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex + 1) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Portrait(character, modifier = Modifier.size(54.dp))
            Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(character.name, style = MaterialTheme.typography.titleLarge); Text(if (settings.demoMode) "离线演示 · 在这里听你说" else "${settings.text.model} · 流式对话", style = MaterialTheme.typography.bodySmall, color = Muted) }
            TextButton(onClick = { clearConfirmation = true }) { Text("清空") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("把今天的小事，慢慢说给我听。", color = Muted, style = MaterialTheme.typography.bodySmall) }
            if (messages.isEmpty()) item { Panel(Modifier.fillMaxWidth(), Mint) { Text("嗨，我是${character.name}。", style = MaterialTheme.typography.titleLarge); Text("文字、图片，或者一句语音，都可以成为我们故事的开始。") } }
            items(messages, key = { it.id }) { message ->
                val user = message.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                    Column(Modifier.widthIn(max = 310.dp).background(if (user) Mint else WarmWhite, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (user) "你" else character.name, style = MaterialTheme.typography.labelSmall, color = Muted)
                        message.imagePath?.let { AsyncImage(File(it), "消息中的图片", Modifier.fillMaxWidth().heightIn(max = 190.dp)) }
                        val text = if (message.status == "streaming" && runtime.characterId == message.characterId && runtime.busy) runtime.streamingText else message.text
                        Text(text.ifEmpty { if (message.status == "streaming") "正在想……" else "生成已中断" }, Modifier.testTag(if (user) "user_message" else "assistant_message"))
                        if (message.status == "interrupted") Text("未完成 · 已保留收到的内容", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        (inputError ?: runtime.error)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 22.dp)) }
        Surface(color = WarmWhite, shadowElevation = 3.dp) { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            attachment?.let { path -> Row(verticalAlignment = Alignment.CenterVertically) { AsyncImage(File(path), "待发送图片", Modifier.size(55.dp)); Text("发送时上传到你配置的模型", Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.bodySmall); IconButton(onClick = { vm.attachment.value = null }) { Glyph("close") } } }
            if (recording || transcribing) Text(if (recording) "正在录音，点击麦克风结束（最多 60 秒）" else "正在转写语音……", color = Green, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(draft, { vm.draft.value = it; inputError = null }, placeholder = { Text("今天想聊些什么？") }, modifier = Modifier.fillMaxWidth().testTag("chat_input"), maxLines = 4, shape = RoundedCornerShape(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { picker.launch("image/*") }, enabled = !runtime.busy) { Glyph("image"); }
                IconButton(onClick = { voiceInput() }, enabled = !transcribing && !runtime.busy) { Glyph(if (recording) "close" else "mic") }
                IconButton(onClick = captureScreen, enabled = !runtime.busy) { Glyph("screen") }
                Spacer(Modifier.weight(1f))
                if (runtime.busy) TextButton(onClick = { vm.cancel() }) { Text("停止生成") }
                else Button(onClick = { vm.send() }, enabled = (draft.isNotBlank() || attachment != null) && !recording && !transcribing, modifier = Modifier.testTag("send_message")) { Text("发送") }
            }
        } }
    }
    if (clearConfirmation) AlertDialog(onDismissRequest = { clearConfirmation = false }, title = { Text("清空当前角色的对话？") }, text = { Text("已提取的共同记忆仍可在关系页面单独管理。") }, confirmButton = { TextButton(onClick = { vm.clearHistory(); clearConfirmation = false }) { Text("清空") } }, dismissButton = { TextButton(onClick = { clearConfirmation = false }) { Text("取消") } })
}
