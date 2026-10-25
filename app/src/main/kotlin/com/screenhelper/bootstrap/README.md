# 应用装配与事件

ScreenHelperApp 创建唯一 AppContainer，手工装配数据库、配置、网络及业务服务。ready 保证初始角色和提醒恢复完成后再执行用户操作。SupervisorJob 避免一个任务失败取消所有功能。

ReminderReceiver 使用 goAsync 等待本地到期处理；RestoreReceiver 处理重启、升级、时间与精确提醒权限变化。接收器不启动收费模型请求。
