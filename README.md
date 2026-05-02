# valset-standardizer

面向外部估值表标准化的新 Java 多模块工程，当前已经拆分为“文件解析”和“ODS 原始行分析”两段链路。目标技术栈：

- Spring Boot 3
- db-scheduler
- MyBatis
- Apache POI
- Apache Commons CSV

目录说明：

- `valset-standardizer-core`：领域模型、解析/标准化抽象、任务模型
- `valset-standardizer-tools`：非 DDD 的通用工具库聚合模块
  - `valset-standardizer-extract`：Excel / CSV 文件解析与 `t_ods_valuation_filedata` 持久化
  - `valset-standardizer-analysis`：基于 ODS 原始行数据的估值分析
  - `valset-standardizer-knowledge`：标准科目、历史映射提示和评估样本加载
  - `valset-standardizer-batch`：基于 db-scheduler 的任务调度与分发
  - `valset-standardizer-transfer`：文件收发分拣调度与任务分发，当前基于 db-scheduler
- `valset-standardizer-infra`：通用基础设施支持代码
- `valset-standardizer-boot`：启动类、应用服务、控制器

## 当前链路

1. 文件解析任务先把 Excel / CSV 按行解析并落到 ODS 表，底层保留内部实现以兼容历史任务。
2. `PARSE_WORKBOOK` 和 `MATCH_SUBJECT` 的文件输入以 `workbookPath` 为主，内部优先使用 `localTempPath` / `realStoragePath` 定位文件，不再把 `fileId` 作为文件读取前提。
3. `API` / `DB` 数据源继续走原有分析器。
4. 文件主数据现在统一由 `t_transfer_object` 承担，估值文件通过 `VALUATION_TABLE` 标签识别，`t_transfer_object_tag` 记录接入分类，`file_id` 仍作为任务关联键保留。
5. `t_valset_workflow_task` 记录流程阶段 `task_stage`，并分别记录 `task_start_time`、`parse_task_time_ms`、`standardize_time_ms`、`match_standard_subject_time_ms`，其中三段耗时分别对应文件解析、标准结构化、标准科目匹配。
6. 同一份文件默认会复用已成功完成的抽取任务、解析任务和匹配任务；如果需要重新执行，可在接口里传入 `forceRebuild=true`。上传返回里会带 `fileFingerprint`，便于排查是否命中同一份文件。

当前版本以“可编译、可运行、可拆分”的多模块实现为目标，便于后续继续扩展估值表标准化、分析和匹配能力。

## 全流程接口文档

- 接口调用说明：`docs/valuation-workflow-api.md`
- 数据库初始化说明：`docs/valuation-workflow-db-init.md`
- 文件管理设计：`docs/file-management-design.md`
- 本地链路观测（OTEL + Tempo）：`docs/observability/otel-local-collector-tempo.md`
- 文件管理接口：`/api/files/upload`、`/api/files/{fileId}`、`/api/files`、`/api/files/by-path`、`/api/files/by-path/ingest-logs`、`/api/files/by-path/sheet-styles`、`/api/files/{fileId}/ingest-logs`、`/api/files/{fileId}/sheet-styles`

## Liquibase

项目已补充 Liquibase changelog 入口：

- `valset-standardizer-boot/src/main/resources/db/changelog/db.changelog-master.xml`

启动配置默认关闭 Liquibase，需要显式开启：

- `LIQUIBASE_ENABLED=true`
- 可选覆盖 changelog：`LIQUIBASE_CHANGE_LOG=classpath:/db/changelog/db.changelog-master.xml`

当前这套 Liquibase 脚本已经整理为项目当前使用的完整 schema 基线，覆盖任务、调度、文件主表、接入日志、ODS 原始表、DWD 标准表、匹配结果表、标准科目表、知识样本表、`leaf_alloc` 以及 legacy 估值表。

注意：

- 它可以作为新环境建库的基线入口，但不应直接覆盖已经运行中的存量库。
- 如果目标环境已经按历史初始化脚本或人工变更建库，建议先对比字段差异，再拆分增量 changeSet 执行迁移。
