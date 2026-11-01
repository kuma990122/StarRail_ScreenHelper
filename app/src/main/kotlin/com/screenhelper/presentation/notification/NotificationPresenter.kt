package com.screenhelper.presentation.notification

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.screenhelper.R
import com.screenhelper.model.Memo
import com.screenhelper.presentation.MainActivity

class NotificationPresenter(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        manager.createNotificationChannel(NotificationChannel(PET_CHANNEL, "桌宠陪伴", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "待办提醒", NotificationManager.IMPORTANCE_HIGH))
        manager.createNotificationChannel(NotificationChannel(CAPTURE_CHANNEL, "屏幕采集", NotificationManager.IMPORTANCE_LOW))
    }
    fun launchIntent(tab: String = "home"): PendingIntent = PendingIntent.getActivity(context, tab.hashCode(),
        Intent(context, MainActivity::class.java).putExtra("tab", tab).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun ongoing(title: String, message: String, capture: Boolean = false): Notification = NotificationCompat.Builder(context, if (capture) CAPTURE_CHANNEL else PET_CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher).setContentTitle(title).setContentText(message).setContentIntent(launchIntent())
        .setOngoing(true).setSilent(true).build()
    fun capture(continuous: Boolean): Notification {
        val stop = PendingIntent.getBroadcast(context, 1002, Intent(context, StopScreenReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CAPTURE_CHANNEL).setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(if (continuous) "迷迷正在看屏幕" else "正在获取一张屏幕画面")
            .setContentText(if (continuous) "每 5 秒采样，三帧窗口会发送到所配置的视觉模型" else "确认后再发送这张图片")
            .setContentIntent(launchIntent("chat")).addAction(0,"停止共享",stop).setOngoing(true).setSilent(true).build()
    }
    fun remind(memo: Memo) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        manager.notify(memo.id.hashCode(), NotificationCompat.Builder(context, REMINDER_CHANNEL).setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("迷迷提醒你 · 到时间啦").setContentText(memo.title).setStyle(NotificationCompat.BigTextStyle().bigText(memo.title))
            .setContentIntent(launchIntent("memo")).setAutoCancel(true).setSilent(true).setPriority(NotificationCompat.PRIORITY_HIGH).build())
    }
    companion object {
        const val PET_CHANNEL = "pet"
        const val REMINDER_CHANNEL = "reminders"
        const val CAPTURE_CHANNEL = "capture"
    }
}

class StopScreenReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(Intent(context, com.screenhelper.data.platform.media.ScreenCaptureService::class.java))
    }
}
