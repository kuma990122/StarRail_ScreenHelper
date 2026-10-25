# 栖伴 · ScreenHelper

安卓 AI 桌宠 OC 验证工程。Kotlin + Jetpack Compose，三层架构，页面采用 MVVM；用户自行配置模型 API，数据默认保存在本机，无需部署服务端。

## 如何运行

在 VS Code 的 PowerShell 终端执行：

```powershell
Set-Location 'E:\AI Projects\ScreenHelper'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Run
```

脚本使用工程内 `.tooling` 中的 JDK/Android SDK，先编译，再安装并打开应用；没有在线设备时自动启动模拟器。有多台设备时加 `-Device 设备编号`，可通过 `-Task Devices` 查看。自动测试曾启动无窗口模拟器时，先执行 `-Task StopEmulator` 再 Run 即可查看窗口。

APK：`app/build/outputs/apk/debug/app-debug.apk`。入口：`app/src/main/kotlin/com/screenhelper/presentation/MainActivity.kt`。根目录 `main.py` 不参与运行。首次打开默认离线演示，不调用收费 API。完整说明见 [运行指南](docs/running.md)。

## 已实现内容

- 多角色设定、性格、世界、经历，以及五种心情立绘导入和回退。
- 每种心情可单独上传动态 GIF / WebP 立绘（PNG / JPG 仍可用）；角色预览和桌宠悬浮窗都会播放动画。
- 文本流式回复、停止、图片附件、系统语音输入/独立转写 API、主动授权单帧截屏。
- 互动冷却、每日积分上限、有效陪伴时长、关系阶段和上下文风格。
- 以“记住：”开头保存明确记忆，在关系页面管理。
- 待办创建/编辑/完成/删除、系统定时提醒、桌宠气泡和通知回退。
- 原生透明悬浮窗口、拖动、轻触回应、连点三次打开备忘录。
- Room、DataStore、Keystore AES-GCM 凭证加密；关闭系统备份。

真实图片和语音交互取决于配置模型的能力。演示模式的图片回复仅验证流程，不做图像识别。系统语音识别依赖设备已安装服务。提醒、通知、录音、悬浮窗和屏幕分享需对应系统权限。

## 目录与阅读顺序

```text
app/src/main/kotlin/com/screenhelper/
  bootstrap/     Application 装配、广播入口
  model/         共享 Kotlin 数据模型
  presentation/  Compose UI、ViewModel、桌宠与通知
  business/      对话、角色、关系、记忆、备忘规则
  data/          仓库、本地存储、模型协议、平台适配
app/src/test/          纯规则和 MockWebServer 测试
app/src/androidTest/   设备和页面测试
app/schemas/           Room 数据库结构
tools/                 Windows 构建和验证脚本
docs/                  架构、运行和测试记录
tmp/                   日志、测试截图、资源测量
```

各主要模块都有 README。建议从 MainActivity → 功能 ViewModel → business 服务 → repository 阅读。数据流见 [架构说明](docs/architecture.md)。

## 验证命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Test
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Lint
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Instrument
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Screenshots
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/android.ps1 -Task Diagnostics
```

设备测试会创建测试数据并临时授予测试权限，请使用测试设备。接口测试默认仅访问本地 mock，不消耗真实额度。

## 开发环境

Android 8.0+，compile/target SDK35，JDK17，Gradle8.9，AGP8.7.3，Kotlin2.0.21。版本固定用于复现，不表示当前最新版。VS Code 可编辑并执行脚本；Android Studio 适合 Compose Preview、Logcat 和 Profiler。新机器需先配置工具链，详见 tools/README.md。

默认猫咪由 MascotArt 原创程序绘制，没有第三方图片素材。请勿把真实 API Key 写入源码、日志或截图。

As of 2026-10-19

