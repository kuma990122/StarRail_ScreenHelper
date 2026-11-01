# 迷迷动画资源映射

本目录 WebP 为用户提供的美术资源，没有从互联网下载其他角色图片。资源的使用及分发范围沿用提供者授权；本工程不声明其美术版权。

| 工程文件 | 用户素材 |
| --- | --- |
| idle.webp | 基础待机提醒/动态WebP_512/正常待机1.webp |
| sleep.webp | 基础待机提醒/动态WebP_512/待机睡觉.webp |
| wake1/2.webp | 基础待机提醒/动态WebP_512/亮屏1/2.webp |
| reminder1/2.webp | 基础待机提醒/动态WebP_512/待办提醒1/2.webp |
| click01_01…click03_04.webp | 交互动画/01_01…03_04，共 12 张 |
| drag04_01…drag04_04.webp | 交互动画/04_01…04_04，共 4 张 |
| left/right/bottom.webp | 放置/左侧贴边、右侧贴边、下方贴边 |

来源根目录：`E:\AI Projects\美术资源\迷迷`。25 张动画约 62.63 MiB，使用原始用户素材，主要增加安装包和磁盘占用。`PetAssets` 首次启动将 WebP 安装到私有文件夹，后续跳过已存在文件。README 不参与动画安装。Android 9+ 原生 ImageDecoder 播放动态 WebP，解码在后台进行，窗口只持有当前动画。

每个动画附带同名 `.bounds`，记录全部帧的 alpha 可见范围并集（归一化 left,top,right,bottom）。由 `python tools/portrait_bounds.py` 生成，需要 Pillow。新增或替换动画后重新生成元数据，应用安装时会复制元数据。浮窗渲染时使用它消除透明外边距，不修改或重编码原动画。更新已有同名动画时也需要更新安装缓存版本。
