package com.screenhelper.data.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.screenhelper.model.Memo

class ReminderScheduler(private val context: Context, private val receiverClass: Class<out BroadcastReceiver>) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    fun isExactAvailable(): Boolean = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    private fun intent(id: String, version: Int) = Intent(context, receiverClass)
        .setData(Uri.parse("screenhelper://memo/$id")).putExtra("memo_id", id).putExtra("version", version)
    fun schedule(memo: Memo) {
        val pending = PendingIntent.getBroadcast(context, memo.id.hashCode(), intent(memo.id, memo.version), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val time = maxOf(memo.dueAt, System.currentTimeMillis() + 1000)
        if (isExactAvailable()) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending); return }
            catch (_: SecurityException) { /* Permission may change between checking and scheduling. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }
    fun cancel(id: String) {
        val pending = PendingIntent.getBroadcast(context, id.hashCode(), intent(id, 0), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        manager.cancel(pending)
        pending.cancel()
    }
}
