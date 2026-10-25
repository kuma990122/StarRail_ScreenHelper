# Android 平台适配

ReminderScheduler 对接 AlarmManager，允许精确闹钟时用 setExactAndAllowWhileIdle，否则降级为系统非精确提醒。录音使用 MediaRecorder AAC；屏幕采集采用用户明确授权的 MediaProjection 前台服务，只保存一帧，最长边压缩到 1280px，然后停止采集。

受保护界面可能输出黑屏，不能绕过 FLAG_SECURE。每次新采集都需新的系统授权；图片先进入聊天附件，发送仍由用户确认。
