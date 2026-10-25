# 桌宠窗口

PetService 是用户主动启用的前台服务；WindowManager 创建原生透明 View，避免单独常驻 Compose 页面。PNG/JPG 使用缩小后的静态位图；GIF 使用 Android Movie 按 30fps 上限播放，动画 WebP 使用 Android 9+ 的 AnimatedImageDrawable。解码在后台线程进行，离开窗口时暂停动画。

拖动移动位置；轻触显示回应；420ms 内连续三次点击打开备忘录。单击延迟至多 440ms，用于区分三连击。自定义图片采样至最长边约 512px；气泡 8 秒后关闭。关闭服务会移除窗口、取消协程和回调、释放图片资源。

MascotArt 是本工程原创的程序绘制猫咪，可随情绪改变表情。没有使用第三方美术素材。
