# Android 平台适配

ReminderScheduler 对接 AlarmManager，有精确授权时使用 setExactAndAllowWhileIdle，否则降级非精确提醒。录音使用 MediaRecorder AAC。

ScreenCaptureService 提供两种授权采集：单帧保存到私有目录后停止，长边最多 1280px；持续模式每 5 秒采样，长边最多 720px、JPEG 质量 75，最近三帧缓存在内存。Surface 最新待采样图像、编码位图和网络请求快照是固定额外缓冲，不是屏幕历史文件。

Surface 事件仅替换待采样图像，到采样时才编码。画面静止时复用不可变 JPEG，仍推进窗口。推理规则在 business/screen/ScreenInteractionService；同一会话最多一个请求，不排队。本服务是平台与业务的编排入口，通过 AppContainer 调用业务，属于三层依赖中的平台入口例外。

锁屏清缓存并取消推理，旧编码结果用代次校验丢弃；部分系统直接结束录屏。停止、撤销或 API 错误释放资源。通知包含停止入口。旋转后建议停止再开启以更新采集尺寸。

每次会话都需系统授权，不绕过 FLAG_SECURE。单帧由用户在聊天页发送；持续模式经应用说明及系统授权后自动提交三图。演示和 mock 测试不访问收费 API。
