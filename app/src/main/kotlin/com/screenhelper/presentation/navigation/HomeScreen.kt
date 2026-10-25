package com.screenhelper.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenhelper.model.*
import com.screenhelper.presentation.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatTime(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

@Composable
fun HomeScreen(profile: CharacterProfile, relation: RelationshipState, memos: List<Memo>, running: Boolean, demo: Boolean,
               togglePet: () -> Unit, chat: () -> Unit, memo: () -> Unit, relationship: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=16.dp), verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) { Eyebrow("QIBAN  /  YOUR LITTLE COMPANION"); Text("给日常，添一点陪伴",style=MaterialTheme.typography.headlineSmall) }
        }
        Box(Modifier.fillMaxWidth().height(310.dp).background(Brush.linearGradient(listOf(Color(0xFFE5EEDD),Color(0xFFF0EEDD))),RoundedCornerShape(30.dp))) {
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Tag(if(demo) "离线演示 · 随时陪伴" else "云端连接 · 由你定义",Color.White.copy(alpha=.65f))
                Text("${profile.name}在这里",fontSize=26.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text("${profile.subtitle}\n今天，也一起慢慢来吧。",fontSize=13.sp,lineHeight=22.sp,color=Muted)
            }
            Portrait(profile,modifier=Modifier.size(210.dp,230.dp).align(Alignment.BottomEnd).offset(x=6.dp,y=8.dp))
            Text("把微小的时刻，\n收进星光里。",fontSize=11.sp,lineHeight=20.sp,color=Green.copy(alpha=.7f),modifier=Modifier.align(Alignment.BottomStart).padding(24.dp))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(1f).clickable(onClick=relationship)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) { Glyph("heart",Modifier.size(17.dp),Green); Text("我们的关系",fontSize=12.sp,color=Muted) }
                Text(relation.stage,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Text("${relation.points} 点星光 · ${relation.activeSeconds / 60} 分钟陪伴",fontSize=10.sp,color=Muted)
            }
            Panel(Modifier.weight(1f).clickable(onClick=memo)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) { Glyph("memo",Modifier.size(17.dp),Green); Text("今日小计划",fontSize=12.sp,color=Muted) }
                Text("${memos.count { !it.completed }} 件待完成",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Text("交给我，替你记着",fontSize=10.sp,color=Muted)
            }
        }
        Surface(color=WarmWhite,shape=RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(38.dp).background(Mint,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center) { Glyph("screen",Modifier.size(20.dp),Green) }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(if(running) "桌面陪伴已开启" else "让陪伴来到桌面",fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                    Text("轻触互动 · 连点三下打开备忘",fontSize=10.sp,color=Muted)
                }
                Switch(running,{togglePet()},modifier=Modifier.height(32.dp))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("下一件小事",style=MaterialTheme.typography.titleMedium)
            TextButton(onClick=memo) { Text("查看全部  →",fontSize=12.sp) }
        }
        val next=memos.firstOrNull { !it.completed }
        Panel(Modifier.fillMaxWidth().clickable(onClick=memo),color=Color(0xFFF0EEE4)) {
            Text(next?.title ?: "给自己留一个小小的计划",fontWeight=FontWeight.Medium)
            Text(next?.let { formatTime(it.dueAt) } ?: "喝杯水、休息片刻，或开始一件喜欢的事。",fontSize=12.sp,color=Muted)
        }
        Button(onClick=chat,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp)) { Text("和${profile.name}聊一会儿"); Spacer(Modifier.width(10.dp)); Glyph("arrow",Modifier.size(18.dp),Color.White) }
        Text("每一次相遇，都值得被好好记住。",fontSize=11.sp,color=Muted,modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=8.dp))
    }
}

@Preview(showBackground=true,widthDp=393,heightDp=850)
@Composable
private fun HomePreview() { ScreenHelperTheme { HomeScreen(CharacterProfile(),RelationshipState("default",12,180),emptyList(),false,true,{},{},{},{}) } }
