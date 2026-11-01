# 花间来信与月下花园

参考源文件在 tmp/ui-concepts-preview/，tmp/preview-browser-profile2/ 是浏览器配置目录。将 HTML/CSS 的粉紫、珍珠白、深梅色、弧形卡片及留白转换为原生 Compose，保留安卓导航和权限流程。

ScreenHelperTheme 每分钟核对本地时间，06:00–18:59 日间，其余夜间；统一所有页面的 MaterialTheme 和系统栏文字对比度。hourOverride 只供截图测试。首页独立每秒更新永劫回归。

ThemeScreenshotTest 截取两种主题；OverlayFlowTest 以系统时区验证真实夜间导航及睡眠，结束恢复时区。报告区分独立主题组件和完整应用截图。
