# Kotlin 源码导览

工程使用单一 app 模块，按展示层、业务层、数据层组织，再按功能分包。这样可先验证全流程，不引入跨 Gradle 模块的配置成本。

- presentation：Compose 页面、ViewModel、桌宠窗口与通知。
- business：角色、对话、关系、记忆、待办规则。
- data：Room、DataStore、凭证、文件、HTTP 与安卓系统适配。
- model：跨层不可变数据模型。
- bootstrap：Application 装配与安卓事件入口。

阅读顺序：MainActivity → 对应 ViewModel → business 服务 → repository → 实际存储或 API 实现。
