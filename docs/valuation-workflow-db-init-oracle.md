# Oracle 数据库初始化说明

当前项目已移除内置数据库自动迁移能力，仓库内也不再保留 Oracle 专用迁移入口。

现阶段若要落 Oracle 环境，需要单独维护 Oracle 版建表 SQL，并按目标库实际情况手工执行。当前保留的 Oracle 运行配置仅包含数据源和 `yss.mybatis.helperDialect=oracle`，用于应用连接与 MyBatis 方言选择，不再承担自动建表职责。
