# 在 Windows 上运行

工程入口是 Android 的 `MainActivity`，不是根目录的 `main.py`。Android 源码需要先构建成 APK，再安装到模拟器或真机。VS Code 可用来编辑和执行终端命令。

## 当前状态

JDK、Android SDK 与模拟器已安装在本工程 `.tooling`。应用和设备测试 APK 已构建成功；运行与测试日志保存在 `tmp`。首次运行默认离线演示，不消耗 API 额度。

## 一条命令构建、安装并打开

也可以在 VS Code 按 `Ctrl+Shift+B` 执行默认任务“ScreenHelper: 运行应用”。其他构建和测试入口在“终端 → 运行任务”。不需要先安装扩展。

在 VS Code 终端执行：

```powershell
Set-Location 'E:\AI Projects\ScreenHelper'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Run
```

`Run` 会先构建调试 APK；有一台在线设备时直接使用，没有时启动项目中的 ScreenHelper 模拟器，然后安装并打开应用。首次构建或启动模拟器可能需要几分钟。已有模拟器或 USB 调试真机可以直接使用。多台设备在线时用 `-Task Devices` 查看编号，然后添加 `-Device 编号`。

自动化测试可添加 `-Headless`，日常查看 UI 请省略此参数。若模拟器已在无窗口模式运行，先执行 `-Task StopEmulator`，再执行 `-Task Run`。

## 分步运行

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Emulator
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Run
```

APK 路径为 `app/build/outputs/apk/debug/app-debug.apk`。也可复制到安卓 8.0 或更新的手机安装，或在 Android Studio 打开项目，选择设备点击 Run。

## 运行与检查

首次使用可先用默认演示模式检查页面和本地交互，不消耗 API 额度。真实对话在设置页填写自己的 API 配置并关闭演示模式。桌宠需手动开启“显示在其他应用上层”权限；通知、录音和屏幕分享权限分别按功能申请。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Test
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Lint
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Instrument
```

`Test` 是本地单元测试；`Lint` 是静态分析；`Instrument` 需要已启动的模拟器或真机。Android Studio 的 Compose Preview、Logcat 和 Profiler 适合查看界面、错误和内存；VS Code 不必安装扩展也能通过终端构建运行本项目。
