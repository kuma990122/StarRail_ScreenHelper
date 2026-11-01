# 展示层与 MVVM

MainActivity 管理五个导航入口：陪伴、对话、备忘、角色、设置；关系和珍贵回忆从首页进入。页面收集 ViewModel 的 StateFlow，操作委托给业务服务。共享主题在 theme，原生悬浮窗口在 pet。

UI 不直接调用 HTTP 或 DAO。ScreenViewModel 统一等待初始化并显示操作结果。对话使用应用作用域运行，切换页面不自动重复发送请求。可用 Android Studio 的 HomePreview 查看静态布局；完整权限与桌宠流程需运行 APK。

0.2 按本地时间统一切换日夜主题和系统栏文字对比度；首页计数独立每秒刷新。
