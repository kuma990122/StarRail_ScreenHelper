# 业务层

每个服务封装一个功能的决策，ViewModel 不复制规则。当前为初期验证保留具体仓库依赖与手工装配，规模扩大时可在此边界引入接口和更多 Gradle 模块。

- conversation：组织上下文、发送任务、增量输出、取消与落库。
- character：角色字段校验及立绘回退。
- relationship：互动冷却、每日上限和有效陪伴。
- memory：只自动提取以“记住”开头的明确陈述。
- memo：待办版本、到期校验及提醒去重。

纯规则测试位于 src/test，真实数据库、Keystore 和页面交互测试位于 src/androidTest。
