package com.screenhelper.presentation.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenhelper.presentation.theme.*

@Composable
fun MemoryScreen(vm: MemoryViewModel, back: () -> Unit) {
    val memories by vm.memories.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=back) { Text("← 陪伴") }
            TextButton(onClick=vm::generate,enabled=!generating,modifier=Modifier.testTag("generate_memory")) { Text(if(generating) "整理中…" else "生成今日回忆") }
        }
        Text("珍贵回忆",style=MaterialTheme.typography.headlineSmall)
        Text("把我们聊过的小事，收进故事里♪",color=Muted,modifier=Modifier.padding(vertical=8.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(vertical=14.dp)) {
            if(memories.isEmpty()) item { Panel(Modifier.fillMaxWidth()) { Text("还没有归档的回忆"); Text("聊一会儿后，点击生成今日回忆；过去的对话会在每日归档时自动整理。",color=Muted) } }
            items(memories,key={it.characterId+it.day}) { memory ->
                Panel(Modifier.fillMaxWidth()) {
                    Eyebrow(memory.day)
                    Text(memory.summary,modifier=Modifier.testTag("daily_summary"))
                    Text("${memory.messageCount} 条对话 · 已压缩归档",style=MaterialTheme.typography.bodySmall,color=Muted)
                }
            }
        }
    }
}
