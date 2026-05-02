# 外部估值全流程数据库初始化说明

## 必需表

## Liquibase 说明

项目已提供一套独立的 Liquibase changelog 入口：

- `valset-standardizer-boot/src/main/resources/db/changelog/db.changelog-master.xml`

默认不自动执行，需要在启动时显式开启：

- `LIQUIBASE_ENABLED=true`

如需切换 changelog，可通过：

- `LIQUIBASE_CHANGE_LOG=classpath:/db/changelog/db.changelog-master.xml`

当前这套 Liquibase 脚本已经整理为项目当前使用的完整 schema 基线，覆盖任务、调度、文件主对象、接入日志、ODS 原始表、DWD 标准表、匹配结果表、标准科目表、知识样本表、`leaf_alloc` 以及 legacy 估值表。若你的环境已经使用本文档中的 SQL 或人工变更完成初始化，请先比对差异，再将 Liquibase 拆成增量 changeSet 执行迁移。

当前全流程依赖以下数据表：

### ODS 原始表

- `t_ods_valuation_filedata`
- `t_ods_valuation_sheet_style`

SQL 文件：

- `tools/extract/src/main/resources/db/migration/t_ods_valuation_filedata.sql`
- `tools/extract/src/main/resources/db/migration/t_ods_valuation_sheet_style.sql`

用途：

- 保存 Excel / CSV 原始行数据
- 每行一条记录
- 通过 `file_id` 和后续分析、匹配阶段关联
- `t_ods_valuation_sheet_style` 仅保存 Excel 的 sheet 级样式快照
- 样式快照只保留标题、header、合并单元格及其预览行，不保存所有明细行样式

### 文件主对象层

- `t_transfer_object`
- `t_transfer_object_tag`
- `t_valset_file_ingest_log`

SQL 文件：

- `tools/transfer/src/main/resources/db/migration/transfer.sql`
- `tools/transfer/src/main/resources/db/migration/transfer-tag.sql`

用途：

- 文件主数据统一写入 `t_transfer_object`
- 估值文件通过 `VALUATION_TABLE` 标签识别
- 文件相关的附加信息写入 `fileMeta` JSON
- `file_id` 继续作为任务关联键，不再依赖独立文件主表
- `t_valset_file_ingest_log` 记录每一次文件接入事件，支持手动上传、邮件收取和对象存储接入

### STG 外部估值解析表

- `t_stg_external_valuation`
- `t_stg_external_valuation_basic_info`
- `t_stg_external_valuation_header`
- `t_stg_external_valuation_subject`
- `t_stg_external_valuation_metric`

SQL 文件：

- `valset-standardizer-infra/src/main/resources/db/migration/t_dwd_external_valuation.sql`

用途：

- 保存一次解析得到的结构化中间结果
- 记录主表、基础信息、表头、科目行和指标行
- 作为标准化引擎的输入层
- 不承担最终标准化业务含义

### DWD 外部估值标准表

- `t_dwd_external_valuation_subject`
- `t_dwd_external_valuation_metric`

SQL 文件：

- `valset-standardizer-infra/src/main/resources/db/migration/t_dwd_external_valuation.sql`

用途：

- 保存外部估值标准化后的主数据
- 支撑 `/api/valuation-workflows/{fileId}/dwd-data` 查询
- 支撑匹配阶段优先从 DWD 读取标准化结果
- `t_dwd_external_valuation_subject` 保存标准化后的外部估值明细事实，并保留标准列值、映射依据和原始列
- `t_dwd_external_valuation_metric` 保存标准化后的指标事实，并保留标准指标码、标准值和映射依据

### 匹配结果表

- `t_subject_match_result`

用途：

- 保存外部估值明细与内部标准科目的匹配打标结果
- 支撑 `/api/valuation-workflows/{fileId}/match-results` 查询

### 任务表

- `t_valset_workflow_task`

用途：

- 保存外部估值流程任务记录
- 记录任务阶段 `task_stage`
- 记录任务开始时间 `task_start_time`
- 记录阶段耗时：
  - `parse_task_time_ms`：文件解析耗时，主要由文件解析任务写入
  - `standardize_time_ms`：元数据到标准结构化耗时，主要由 `PARSE_WORKBOOK` 任务写入
  - `match_standard_subject_time_ms`：匹配标准科目耗时，主要由 `MATCH_SUBJECT` 任务写入
- 任务复用逻辑默认开启：同一份文件的同一阶段任务在成功后会被复用，除非请求显式传入 `forceRebuild=true`

## 初始化顺序

建议按下面顺序执行：

1. 初始化文件主对象层 `t_transfer_object` / `t_transfer_object_tag` / `t_valset_file_ingest_log`
2. 初始化 ODS 表 `t_ods_valuation_filedata` / `t_ods_valuation_sheet_style`
3. 初始化 STG 表 `t_stg_external_valuation*` 和 DWD 表 `t_dwd_external_valuation_subject` / `t_dwd_external_valuation_metric`
4. 初始化任务与匹配结果相关表
5. 启动应用

## 与接口的关系

- `/upload` 先写文件主表，再触发 ODS 提取并写 ODS 表
- `/analyze` 写 DWD 标准表
- `/match` 写匹配结果表
- `/files/*` 负责文件信息管理和文件接入日志查询
- `/raw-data` 读 ODS 表
- `/stg-data` 读 STG 解析快照表
- `/dwd-data` 读 DWD 标准表，STG 仅用于回溯和排障
- `/match-results` 读匹配结果表
