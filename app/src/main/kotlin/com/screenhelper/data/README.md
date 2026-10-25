# 数据层

LocalRepository 封装 Room；SettingsRepository 封装 DataStore；AiRepository 选择演示或真实接口。业务服务通过这些入口访问数据，UI 无需了解 SQL 或 JSON。

没有自建服务器。HTTP 从安卓设备直接发送到用户配置的提供商。平台适配集中在 platform，后续更换供应商或提醒实现时无需重写页面。
