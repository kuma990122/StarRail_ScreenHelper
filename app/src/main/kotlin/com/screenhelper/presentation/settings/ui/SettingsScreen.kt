package com.screenhelper.presentation.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenhelper.model.*
import com.screenhelper.presentation.settings.viewmodel.SettingsViewModel
import com.screenhelper.presentation.theme.*

@Composable
private fun EndpointEditor(title: String, endpoint: EndpointConfig, change: (EndpointConfig) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    FormField("API Base URL", endpoint.baseUrl, { change(endpoint.copy(baseUrl = it.trim())) })
    FormField("模型名称", endpoint.model, { change(endpoint.copy(model = it.trim())) })
    OutlinedTextField(endpoint.key, { change(endpoint.copy(key = it.trim())) }, label = { Text("API Key（仅加密保存在本机）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, petRunning: Boolean, togglePet: () -> Unit, requestNotifications: () -> Unit, requestExact: () -> Unit) {
    val saved by vm.settings.collectAsStateWithLifecycle()
    var editing by remember(saved) { mutableStateOf(saved) }
    var advanced by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeading("MAKE IT YOURS", "陪伴的偏好", "你的角色、你的模型、你的节奏。")
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("离线演示", style = MaterialTheme.typography.titleMedium); Text("使用本地示例回复，不产生 API 费用", color = Muted, style = MaterialTheme.typography.bodySmall) }; Switch(editing.demoMode, { editing = editing.copy(demoMode = it) }, Modifier.testTag("demo_mode")) }
            EndpointEditor("文本模型", editing.text) { editing = editing.copy(text = it) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(editing.textSupportsImages, { editing = editing.copy(textSupportsImages = it) }); Text("我确认这个模型支持图片输入", style = MaterialTheme.typography.bodySmall) }
            Text("图片需要视觉模型；语音可使用系统输入或独立转写接口。填写服务商文档中的准确模型名称。保存不会发起网络请求。", style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "收起额外模型配置" else "配置视觉 / 语音模型") }
            if (advanced) {
                EndpointEditor("视觉模型（可选）", editing.vision) { editing = editing.copy(vision = it) }
                EndpointEditor("语音转写模型（可选）", editing.speech) { editing = editing.copy(speech = it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("触摸桌宠时生成对话", Modifier.weight(1f)); Switch(editing.touchTalk, { editing = editing.copy(touchTalk = it) }) }
            Button(onClick = { vm.save(editing) }, Modifier.fillMaxWidth().testTag("save_settings")) { Text("保存设置") }
        }
        Panel {
            Text("系统与权限", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = togglePet, modifier = Modifier.fillMaxWidth()) { Text(if (petRunning) "收起桌宠" else "开启桌宠悬浮窗") }
            TextButton(onClick = requestNotifications) { Text("允许待办通知") }
            TextButton(onClick = requestExact) { Text("允许精确提醒") }
            Text("截屏每次需要你主动授权；所有聊天、立绘与备忘保存在应用私有目录。向 API 发送消息时会包含当前角色设定与有限的历史记忆。", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Text("栖伴 · ScreenHelper 0.1.0", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
