# 数据库与迁移

`AppDatabase` 声明实体、DAO 和数据库版本；`entity/Entities.kt` 定义持久化字段，`dao/AppDao.kt` 集中提供读写查询。界面通过 repository/业务服务访问数据。

版本 1 → 2 仅新增 `daily_memories` 表，以角色 ID 与本地日期为主键，记录摘要、消息数量、源哈希、时区和压缩归档路径。不要通过卸载应用或破坏性迁移实现升级，否则会丢失用户配置与聊天记录。

导出的版本结构在 `app/schemas`。设备测试 `IterationFlowTest.versionOneDatabaseMigratesWithoutLosingCharacters` 从版本 1 结构创建数据库并插入旧角色，再打开版本 2 验证数据保留与新表可用；归档测试另外验证重复生成、追加消息后更新和原始聊天保留。
