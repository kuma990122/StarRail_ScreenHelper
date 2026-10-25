package com.screenhelper.presentation.relationship.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenhelper.presentation.relationship.viewmodel.RelationshipViewModel
import com.screenhelper.presentation.theme.*

@Composable
fun RelationshipScreen(vm: RelationshipViewModel, back: () -> Unit) {
    val state by vm.relation.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        TextButton(onClick=back) { Text("← 返回陪伴") }
        PageHeading("OUR LITTLE STORY","一点点，成为熟悉的人","关系由真实互动和有效陪伴共同积累。")
        Panel(Modifier.fillMaxWidth(),Mint) {
            Text(state.stage,style=MaterialTheme.typography.headlineSmall)
            Text("${state.points} 点星光 · ${state.activeSeconds / 60} 分钟有效陪伴")
            LinearProgressIndicator(progress={ (state.points/140f).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
            Text("今日已获得 ${state.dailyPoints} / 24 点星光",color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        Text("关系里程碑",style=MaterialTheme.typography.titleMedium)
        listOf("初次相遇" to "从第一句问候开始","渐渐熟悉" to "20 点星光 + 5 分钟有效陪伴","彼此信赖" to "60 点星光 + 30 分钟有效陪伴","心有默契" to "140 点星光 + 120 分钟有效陪伴").forEach { (title,detail) ->
            Panel(Modifier.fillMaxWidth()) { Text(title,fontWeight=FontWeight.SemiBold); Text(detail,color=Muted,style=MaterialTheme.typography.bodySmall) }
        }
        Text("共同记忆",style=MaterialTheme.typography.titleMedium)
        Text("聊天时说“记住：……”即可保存一条记忆。",color=Muted,style=MaterialTheme.typography.bodySmall)
        if(memories.isEmpty()) Panel(Modifier.fillMaxWidth()) { Text("我们的故事，正在慢慢开始。",color=Muted) }
        memories.forEach { fact -> Panel(Modifier.fillMaxWidth()) { Text(fact.text); TextButton(onClick={vm.deleteMemory(fact.id)}) { Text("移除这条记忆") } } }
    }
}
