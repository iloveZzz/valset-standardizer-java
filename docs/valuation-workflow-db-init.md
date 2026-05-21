# 外部估值全流程数据库初始化说明

## 必需表

## 初始化方式

项目已移除内置数据库自动迁移能力，当前数据库初始化以仓库中的静态 SQL 为准。

建议准备以下脚本：

- MySQL 基线：`docs/ddl/mysql.sql`
- ODS 原始表：`valset-standardizer-tools/extract/src/main/resources/db/migration/t_ods_valuation_filedata.sql`
- ODS 样式表：`valset-standardizer-tools/extract/src/main/resources/db/migration/t_ods_valuation_sheet_style.sql`
- STG / STG / 知识样本 / 规则字典：`valset-standardizer/src/main/resources/db/migration/*.sql`

若你的环境已经通过历史脚本或人工变更完成初始化，请先比对目标库差异，再决定是否补执行其中部分 SQL。

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

- `yss-valset-standardizer/src/main/resources/db/migration/t_stg_external_valuation.sql`

用途：

- 保存一次解析得到的结构化中间结果
- 记录主表、基础信息、表头、科目行和指标行
- 作为标准化引擎的输入层
- 不承担最终标准化业务含义

### STG 外部估值标准表

旧外部估值中间表已移除。解析快照统一进入 `t_stg_external_valuation*`，标准化结果运行时生成后直接进入 `tr_spv_jjhzgzb` / `tr_spv_index`。

### 匹配结果表

- `t_subject_match_result`

用途：

- 保存外部估值明细与内部标准科目的匹配打标结果
- 支撑 `/api/valuation-workflows/{fileId}/match-results` 查询

### 任务表

任务执行状态已切换为 批量任务 元数据模型，默认依赖：

- `t_parse_queue`
- `BATCH_JOB_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_STEP_EXECUTION`

其中：

- `t_parse_queue` 负责待解析队列
- `BATCH_JOB_EXECUTION` 负责任务级状态
- `BATCH_JOB_EXECUTION_PARAMS` 负责 `taskId` / `businessKey` / `taskType` 等作业参数
- `BATCH_STEP_EXECUTION` 负责阶段级执行明细与耗时
  - `match_standard_subject_time_ms`：匹配标准科目耗时，主要由 `MATCH_SUBJECT` 任务写入
- 任务复用逻辑默认开启：同一份文件的同一阶段任务在成功后会被复用，除非请求显式传入 `forceRebuild=true`

## 初始化顺序

建议按下面顺序执行：

1. 初始化文件主对象层 `t_transfer_object` / `t_transfer_object_tag` / `t_valset_file_ingest_log`
2. 初始化 ODS 表 `t_ods_valuation_filedata` / `t_ods_valuation_sheet_style`
3. 初始化 STG 表 `t_stg_external_valuation*` 和 TR_SPV 标准表 `tr_spv_jjhzgzb` / `tr_spv_index`
4. 初始化任务与匹配结果相关表
5. 启动应用

## 与接口的关系

- `/upload` 先写文件主表，再触发 ODS 提取并写 ODS 表
- `/analyze` 写 STG 解析表
- `/match` 写匹配结果表
- `/files/*` 负责文件信息管理和文件接入日志查询
- `/raw-data` 读 ODS 表
- `/stg-data` 读 STG 解析快照表
- `/stg-data` 读 STG 解析表，STG 仅用于回溯和排障
- `/match-results` 读匹配结果表
