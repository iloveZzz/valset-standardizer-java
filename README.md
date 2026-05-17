# valset-standardizer

面向外部估值表标准化的新 Java 多模块工程，当前已经拆分为“文件解析”和“ODS 原始行分析”两段链路。目标技术栈：

- Spring Boot 3
- db-scheduler
- MyBatis
- Apache POI
- Apache Commons CSV

目录说明：

- `valset-standardizer-tools`：非 DDD 的通用工具库聚合模块
  - `valset-standardizer-extract`：Excel / CSV 文件解析与 `t_ods_valuation_filedata` 持久化
  - `valset-standardizer-parser`：基于 ODS 原始行数据的估值分析
- `valset-standardizer-knowledge`：标准科目、历史映射提示和评估样本加载
- `valset-standardizer-task`：估值内部流程的任务适配、阶段分发与状态查询
- `valset-standardizer-batch`：基于 db-scheduler 的任务调度与分发
- `valset-standardizer-transfer`：文件收发分拣调度与任务分发，当前基于 db-scheduler
- `valset-standardizer-workflow`：通用 ETL 平台适配层，下面包含
  - `valset-standardizer-taskflow-adapter`：统一工作流 DTO、状态、日志、控制接口与数据库运行态
  - `valset-standardizer-taskflow-springbatch`：批量任务 适配实现
  - `valset-standardizer-taskflow-dolohinscheduler`：DolphinScheduler 适配实现
  - `valset-standardizer-taskflow-xxljob`：XXL-JOB 适配实现
- `yss-valset-standardizer`：整合应用，承载启动类、应用服务、控制器、运行配置以及原 `core` / `infra` 代码

## 当前链路

1. 文件解析任务先把 Excel / CSV 按行解析并落到 ODS 表，底层保留内部实现以兼容历史任务。
2. `PARSE_WORKBOOK` 和 `MATCH_SUBJECT` 的文件输入以 `workbookPath` 为主，内部优先使用 `localTempPath` / `realStoragePath` 定位文件，不再把 `fileId` 作为文件读取前提。
3. `API` / `DB` 数据源继续走原有分析器。
4. 文件主数据现在统一由 `t_transfer_object` 承担，估值文件通过 `VALUATION_TABLE` 标签识别，`t_transfer_object_tag` 记录接入分类，`file_id` 仍作为任务关联键保留。
5. 任务执行状态和阶段日志现在以 批量任务 元数据为准，主要回放 `BATCH_JOB_EXECUTION`、`BATCH_JOB_EXECUTION_PARAMS`、`BATCH_STEP_EXECUTION` 与 `t_parse_queue` 的关联信息。
6. `WorkflowEngineAdapter` 仅用于估值内部流程的触发、重试和查询，不承担通用 ETL 平台编排。
7. 同一份文件默认会复用已成功完成的抽取任务、解析任务和匹配任务；如果需要重新执行，可在接口里传入 `forceRebuild=true`。上传返回里会带 `fileFingerprint`，便于排查是否命中同一份文件。
8. 通用 ETL 场景走 `valset-standardizer-workflow`，对外统一 `/api/etl/workflows/**`，与估值内部 `WorkflowEngineAdapter` 分离。

当前版本以“可编译、可运行、可拆分”的多模块实现为目标，便于后续继续扩展估值表标准化、分析和匹配能力。

## 全流程接口文档

- 接口调用说明：`docs/valuation-workflow-api.md`
- 数据库初始化说明：`docs/valuation-workflow-db-init.md`
- 文件管理设计：`docs/file-management-design.md`
- 估值内部工作流引擎适配设计：`docs/workflow-engine-internal-scope.md`
- 通用 ETL 平台设计：`docs/etl-platform-design.md`
- 本地链路观测（OTEL + Tempo）：`docs/observability/otel-local-collector-tempo.md`
- 文件管理接口：`/api/files/upload`、`/api/files/{fileId}`、`/api/files`、`/api/files/by-path`、`/api/files/by-path/ingest-logs`、`/api/files/by-path/sheet-styles`、`/api/files/{fileId}/ingest-logs`、`/api/files/{fileId}/sheet-styles`

## 数据库初始化

项目内不再保留数据库自动迁移配置与历史迁移入口。

当前数据库初始化以仓库中的静态 SQL 为准：

- MySQL 基线参考：`docs/ddl/mysql.sql`
- 估值链路补充脚本：`valset-standardizer/src/main/resources/db/migration/*.sql`
- ODS 抽取补充脚本：`valset-standardizer-tools/extract/src/main/resources/db/migration/*.sql`

如需初始化新环境，建议先执行基线 DDL，再按业务链路补齐迁移 SQL，并在执行前对照目标库现状确认差异。

## 数据源切换

当前主应用支持通过 Spring profile 切换数据库类型：

- 默认启动使用 `mysql` profile，并自动带上 `nacos`
- 切到 PostgreSQL 时把 `SPRING_PROFILES_ACTIVE` 设置为 `postgresql`
- MySQL 与 PostgreSQL 的连接参数分别放在 `valset-standardizer/src/main/resources/application-mysql.yml` 和 `valset-standardizer/src/main/resources/application-postgresql.yml`

数据库账号密码可以通过 `VALSET_DATASOURCE_USERNAME` 和 `VALSET_DATASOURCE_PASSWORD` 覆盖。
