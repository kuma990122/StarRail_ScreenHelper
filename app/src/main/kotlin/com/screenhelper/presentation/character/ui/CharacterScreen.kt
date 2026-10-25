package com.screenhelper.presentation.character.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenhelper.model.*
import com.screenhelper.presentation.character.viewmodel.CharacterViewModel
import com.screenhelper.presentation.theme.*

@Composable
fun CharacterScreen(vm: CharacterViewModel) {
    val profile by vm.active.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    var editing by remember(profile) { mutableStateOf(profile) }
    var emotion by remember { mutableStateOf(Emotion.NEUTRAL) }
    var uploadTarget by remember { mutableStateOf<Pair<String, Emotion>?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val target = uploadTarget
        if (uri != null && target != null) vm.upload(target.second, uri, target.first)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading("CHARACTER STUDIO", "让想象有个模样", "每个角色都有自己的来处，也有不同的心情。")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("我的伙伴 · ${profiles.size}", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { vm.create() }, modifier = Modifier.testTag("create_character")) { Text("＋ 新建") }
        }
        profiles.forEach { item ->
            FilterChip(selected = item.id == profile.id, onClick = { vm.select(item.id) }, label = { Text(item.name) })
        }
        Panel(Modifier.fillMaxWidth(), Mint) {
            Portrait(profile, Modifier.fillMaxWidth().height(200.dp), emotion)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Emotion.entries.forEach { item ->
                FilterChip(selected = emotion == item, onClick = { emotion = item }, label = { Text(item.label, style = MaterialTheme.typography.labelSmall) })
            } }
            Row {
                Button(onClick = { uploadTarget = profile.id to emotion; picker.launch("image/*") }, modifier = Modifier.testTag("upload_portrait")) { Text("上传${emotion.label}立绘") }
                TextButton(onClick = { vm.resetImage(emotion) }) { Text("恢复默认") }
            }
            Text("支持 GIF / WebP 动态立绘，也支持 PNG / JPG；单个文件小于 15 MB。缺少对应心情时使用平静立绘。Android 8 上的 WebP 以静态首帧显示。", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Panel {
            FormField("名字", editing.name, { editing = editing.copy(name = it) }, Modifier.testTag("character_name"))
            FormField("一句话介绍", editing.subtitle, { editing = editing.copy(subtitle = it) })
            FormField("性格与说话习惯", editing.personality, { editing = editing.copy(personality = it) }, minLines = 3)
            FormField("世界背景", editing.world, { editing = editing.copy(world = it) }, minLines = 3)
            FormField("过往经历", editing.backstory, { editing = editing.copy(backstory = it) }, minLines = 3)
            Button(onClick = { vm.save(editing) }, modifier = Modifier.fillMaxWidth().testTag("save_character")) { Text("保存角色设定") }
        }
    }
}
