# 设备测试

通过 tools/android.ps1 -Task Instrument 运行，需要在线模拟器或 USB 调试设备。AppFlowTest 检查页面、离线聊天、待办、数据库原子去重、Keystore、图片缩放、GIF 多帧导入、真实 AlarmManager 与桌宠三连击。测试会修改应用内测试数据并临时授予测试权限，请在测试设备运行。

截图先保存应用外部私有 test-screenshots 目录，用 -Task Screenshots 拉取到 tmp/screenshots。真实服务商能力与硬件麦克风需额外配置，不能从演示模式测试推断已验证。
