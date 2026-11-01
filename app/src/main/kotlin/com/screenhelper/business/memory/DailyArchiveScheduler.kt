package com.screenhelper.business.memory

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import com.screenhelper.bootstrap.appContainer
import kotlinx.coroutines.launch
import java.time.*

/** Inexact local midnight task; startup also catches up after process death or powered-off days. */
object DailyArchiveScheduler {
    fun schedule(context: Context) {
        val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = PendingIntent.getBroadcast(context, 5200, Intent(context, DailyArchiveReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }
}

class DailyArchiveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        context.appContainer.scope.launch {
            try { context.appContainer.ready.await(); context.appContainer.dailyMemory.archiveCompletedDays() }
            finally { DailyArchiveScheduler.schedule(context); pending.finish() }
        }
    }
}
