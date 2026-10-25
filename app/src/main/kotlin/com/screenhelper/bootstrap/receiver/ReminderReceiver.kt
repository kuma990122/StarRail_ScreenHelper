package com.screenhelper.bootstrap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenhelper.bootstrap.appContainer
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("memo_id") ?: return
        val version = intent.getIntExtra("version", -1)
        val pending = goAsync()
        val app = context.appContainer
        app.scope.launch {
            try { app.ready.await(); app.memo.due(id, version) }
            finally { pending.finish() }
        }
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) return
        val pending = goAsync()
        val app = context.appContainer
        app.scope.launch {
            try { app.ready.await(); app.memo.restore() }
            finally { pending.finish() }
        }
    }
}
