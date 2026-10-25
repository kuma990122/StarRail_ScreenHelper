# 开发命令

Windows PowerShell：powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Run。脚本从 .tooling 加载 JDK 和 SDK，不依赖全局 PATH。常用任务：Build、Run、Devices、Test、Lint、Instrument、Screenshots、Diagnostics。

Emulator 默认打开窗口；自动化添加 -Headless。多设备 Run 添加 -Device SERIAL。依赖与构建缓存全部位于工程内。新机器需安装 JDK17、SDK35 与对应模拟器镜像，SetupSdk 负责已有命令行工具下的 SDK 包安装，不是全新机器的一键引导器。

Diagnostics 保存真实的 adb meminfo、CPU、帧统计和冷启动时间到 tmp/metrics。它会启动/停止应用和桌宠，仅适合测试设备。
