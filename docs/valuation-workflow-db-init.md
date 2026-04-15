# 外部估值全流程数据库初始化说明

## 必需表

当前全流程依赖以下数据表：

### ODS 原始表

- `t_ods_valuation_filedata`

SQL 文件：

- `tools/extract/src/main/resources/db/migration/t_ods_valuation_filedata.sql`

用途：

- 保存 Excel / CSV 原始行数据
- 每行一条记录
- 通过 `file_id` 和后续分析、匹配阶段关联

### 文件主数据表

- `t_subject_match_file_info`
- `t_subject_match_file_ingest_log`

SQL 文件：

- `subject-match-infra/src/main/resources/db/migration/t_subject_match_file_info.sql`

用途：

- 保存文件身份、来源渠道、存储位置、处理状态和内容指纹
- 记录每一次文件接入事件，支持手动上传、邮件收取和对象存储接入
- `file_id` 由文件主表统一发放，不再从任务表倒推

### DWD 外部估值标准表

- `t_dwd_external_valuation`
- `t_dwd_external_valuation_basic_info`
- `t_dwd_external_valuation_header`
- `t_dwd_external_valuation_subject`
- `t_dwd_external_valuation_metric`

SQL 文件：

- `subject-match-infra/src/main/resources/db/migration/t_dwd_external_valuation.sql`

用途：

- 保存外部估值标准化后的主数据
- 支撑 `/api/valuation-workflows/{fileId}/dwd-data` 查询
- 支撑匹配阶段优先从 DWD 读取标准化结果
- `t_dwd_external_valuation_header.header_column_meta_json` 保存按列展开的表头元数据，包含列序号、完整路径、分层片段和空列标识
- `t_dwd_external_valuation_subject` 只保存外部估值明细的科目编码、名称、层级、路径和原始列，不再保存币种、市值、占比、成本等字段

### 匹配结果表

- `t_subject_match_result`

用途：

- 保存外部估值明细与内部标准科目的匹配打标结果
- 支撑 `/api/valuation-workflows/{fileId}/match-results` 查询

### 任务表

- `t_subject_match_task`

用途：

- 保存外部估值流程任务记录
- 记录任务阶段 `task_stage`
- 记录任务开始时间 `task_start_time`
- 记录阶段耗时：
  - `parse_task_time_ms`：文件解析耗时，主要由 `EXTRACT_DATA` 任务写入
  - `standardize_time_ms`：元数据到标准结构化耗时，主要由 `PARSE_WORKBOOK` 任务写入
  - `match_standard_subject_time_ms`：匹配标准科目耗时，主要由 `MATCH_SUBJECT` 任务写入
- 任务复用逻辑默认开启：同一份文件的同一阶段任务在成功后会被复用，除非请求显式传入 `forceRebuild=true`

### 兼容解析结果表

- `t_subject_match_parsed_workbook`
  - `header_columns_json`：按列保存表头完整路径、分层片段和空列标识，便于前端渲染和回查

用途：

- 保留原有解析结果持久化，兼容现有实现和历史功能
- 当前 DWD 已独立落表，不再依赖这张表做对外查询

## 初始化顺序

建议按下面顺序执行：

1. 初始化文件主数据表 `t_subject_match_file_info` / `t_subject_match_file_ingest_log`
2. 初始化 ODS 表 `t_ods_valuation_filedata`
3. 初始化 DWD 表 `t_dwd_external_valuation*`
4. 初始化任务与匹配结果相关表
5. 启动应用

## 与接口的关系

- `/upload` 先写文件主表，再触发 ODS 提取并写 ODS 表
- `/analyze` 写 DWD 标准表
- `/match` 写匹配结果表
- `/files/*` 负责文件信息管理和文件接入日志查询
- `/raw-data` 读 ODS 表
- `/dwd-data` 读 DWD 标准表
- `/match-results` 读匹配结果表
