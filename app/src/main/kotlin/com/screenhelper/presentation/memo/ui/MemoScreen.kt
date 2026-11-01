package com.screenhelper.presentation.memo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.screenhelper.model.Memo
import com.screenhelper.bootstrap.appContainer
import com.screenhelper.data.platform.media.PetSound
import com.screenhelper.presentation.memo.viewmodel.MemoViewModel
import com.screenhelper.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoScreen(vm: MemoViewModel, requestExact: () -> Unit) {
    val app = LocalContext.current.appContainer
    LaunchedEffect(Unit) { app.sounds.play(PetSound.TODO) }
    val memos by vm.memos.collectAsStateWithLifecycle()
    var showCompleted by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Memo?>(null) }
    val visible = memos.filter { it.completed == showCompleted }.sortedBy { it.dueAt }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeading("LITTLE PLANS", "把小事，交给我记着", "提醒时双击桌宠，也可以来到这里。") }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !showCompleted, onClick = { showCompleted = false }, label = { Text("待完成") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = showCompleted, onClick = { showCompleted = true }, label = { Text("已完成") })
            Spacer(Modifier.weight(1f))
            Button(onClick = { editing = null; editor = true }, modifier = Modifier.testTag("add_memo")) { Text("＋ 新待办") }
        } }
        item { if (!vm.isExactAvailable()) Panel(color = Mint) {
            Text("当前使用系统非精确提醒，可能延后。", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = requestExact) { Text("允许准时提醒") }
        } }
        if (visible.isEmpty()) item { Panel(Modifier.fillMaxWidth()) { Text("这里还很轻盈", style = MaterialTheme.typography.titleMedium); Text("写下一件想完成的小事吧。", color = Muted) } }
        items(visible, key = { it.id }) { memo -> Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(memo.completed, { vm.toggle(memo.id) })
                Column(Modifier.weight(1f)) {
                    Text(memo.title, style = MaterialTheme.typography.titleMedium)
                    Text(SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(memo.dueAt)), color = Muted)
                    if (memo.deliveredVersion == memo.version) Text("已提醒", color = Green, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row { TextButton(onClick = { editing = memo; editor = true }) { Text("编辑") }; TextButton(onClick = { vm.delete(memo.id) }) { Text("删除") } }
        } }
    }
    if (editor) {
        val context = LocalContext.current
        var title by remember(editing) { mutableStateOf(editing?.title.orEmpty()) }
        var dueAt by remember(editing) { mutableLongStateOf(editing?.dueAt ?: (System.currentTimeMillis() + 60_000)) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { editor = false }, title = { Text(if (editing == null) "一件小计划" else "调整提醒") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField("要完成什么？", title, { title = it }, Modifier.testTag("memo_title"))
            OutlinedButton(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = dueAt }
                DatePickerDialog(context, { _, year, month, day ->
                    cal.set(year, month, day)
                    TimePickerDialog(context, { _, hour, minute -> cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0); dueAt = cal.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }) { Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(dueAt))) }
            TextButton(onClick = { dueAt = System.currentTimeMillis() + 60_000 }) { Text("一分钟后提醒") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { TextButton(onClick = {
            if (title.isBlank() || title.trim().length > 120 || dueAt <= System.currentTimeMillis()) error = "请填写 1–120 字待办并选择未来时间"
            else { vm.save(title, dueAt, editing?.id); editor = false }
        }, modifier = Modifier.testTag("save_memo")) { Text("保存提醒") } }, dismissButton = { TextButton(onClick = { editor = false }) { Text("取消") } })
    }
}
