package com.screenhelper.presentation.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.screenhelper.presentation.pet.PetHost
import com.screenhelper.presentation.pet.PetAnchor
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenhelper.business.pet.PetPolicy
import com.screenhelper.model.*
import com.screenhelper.presentation.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

fun formatTime(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

@Composable
private fun HomeCard(modifier: Modifier=Modifier, content: @Composable ColumnScope.()->Unit) {
    Surface(modifier,shape=RoundedCornerShape(22.dp),color=WarmWhite) {
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
    }
}

@Composable
fun HomeScreen(profile: CharacterProfile, relation: RelationshipState, memos: List<Memo>, running: Boolean, demo: Boolean,
               togglePet: () -> Unit, chat: () -> Unit, memo: () -> Unit, relationship: () -> Unit,
               latestMemory: String? = null, memories: () -> Unit = {}) {
    val hostView = LocalView.current
    val anchorOwner = remember { Any() }
    DisposableEffect(anchorOwner) { onDispose { PetHost.anchor(anchorOwner,null) } }
    val nextMemo = PetPolicy.nextMemo(memos)
    val seconds by produceState(PetPolicy.weekSeconds(ZonedDateTime.now())) {
        while(true) { delay(1000); value=PetPolicy.weekSeconds(ZonedDateTime.now()) }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heroHeight=(maxHeight*.32f).coerceIn(165.dp,230.dp)
        val night=MaterialTheme.colorScheme.background.luminance()<.2f
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=22.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(if(night) "月下花园 / QUIET HOURS" else "花间来信 / A LITTLE NOTE")
                    Text("逐火之旅",style=MaterialTheme.typography.titleLarge,color=Ink)
                }
                Switch(running, { togglePet() }, modifier=Modifier.testTag("home_pet_switch").semantics { contentDescription="桌面陪伴" })
            }
            Box(Modifier.fillMaxWidth().height(heroHeight).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.tertiaryContainer)),RoundedCornerShape(topStart=30.dp,topEnd=55.dp,bottomEnd=30.dp,bottomStart=30.dp)).clickable(onClick=chat)) {
                val decoration=Green.copy(alpha=.16f)
                Canvas(Modifier.matchParentSize()) {
                    drawRoundRect(decoration,Offset(10.dp.toPx(),10.dp.toPx()),Size(size.width-20.dp.toPx(),size.height-20.dp.toPx()),CornerRadius(25.dp.toPx()),style=Stroke(1.dp.toPx()))
                    repeat(4) { i -> drawCircle(decoration,(25+i*13).dp.toPx(),Offset(size.width*.80f,size.height*.6f),style=Stroke(1.dp.toPx())) }
                }
                Box(Modifier.fillMaxHeight(.92f).aspectRatio(1f).align(Alignment.BottomEnd).onGloballyPositioned { coordinates ->
                    val location = IntArray(2)
                    hostView.getLocationOnScreen(location)
                    val bounds = coordinates.boundsInWindow()
                    PetHost.anchor(anchorOwner,PetAnchor(location[0] + bounds.center.x, location[1] + bounds.center.y))
                })
                Column(Modifier.padding(20.dp).fillMaxWidth(.66f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("逐火之旅",style=MaterialTheme.typography.labelSmall,color=Green)
                    Text("第 ${String.format(java.util.Locale.US,"%,d",seconds)} 次永劫回归",fontSize=12.sp,color=Green)
                    Text("今天，也一起\n写下新的故事♪",fontSize=21.sp,lineHeight=29.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Text("和${profile.name}聊一会儿 →",fontSize=11.sp,color=Green,modifier=Modifier.padding(top=5.dp))
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                HomeCard(Modifier.weight(1f).clickable(onClick=relationship)) {
                    Text("♡ 我们的关系",fontSize=11.sp,color=Muted)
                    Text(relation.stage,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Text("${relation.points} 点星光 · ${relation.activeSeconds/60} 分钟",fontSize=10.sp,color=Muted,maxLines=1)
                }
                HomeCard(Modifier.weight(1f).clickable(onClick=memo)) {
                    Text("下一件待办",fontSize=11.sp,color=Muted)
                    Text(nextMemo?.title ?: "暂无待办",fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(nextMemo?.let { formatTime(it.dueAt) } ?: "交给我，替你记着",fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
            HomeCard(Modifier.fillMaxWidth().clickable(onClick=memories)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("珍贵回忆",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                    Text("翻开故事 →",fontSize=11.sp,color=Green)
                }
                Text(latestMemory ?: "我们说过的话，会在这里变成一段段回忆。",maxLines=2,overflow=TextOverflow.Ellipsis,fontSize=12.sp,color=Muted,lineHeight=19.sp)
            }
        }
    }
}
