# valset-standardizer 数据库设计说明

## 1. 文档目标

本文档说明 `valset-standardizer` 当前实现所依赖的核心数据库表、表之间的关系、各表职责、读写边界和关键约束，作为后续表结构维护、接口设计和排障定位的基线。

本文默认以当前仓库中的 SQL DDL 和实体类为准。

## 2. 数据分层总览

当前数据库可以分为 6 层：

1. 文件主数据层
2. 文件接入日志层
3. 原始数据 ODS 层
4. 结构化解析与标准化层
5. 匹配结果层
6. 任务与运行态层

此外还有一套独立的通用 ETL 工作流层，用于 `workflow/taskflow-adapter`。

## 3. 核心设计原则

### 3.1 文件主数据独立

文件不再只是任务的一个路径参数，而是独立实体。文件身份、来源、存储、状态、指纹都需要单独治理。

### 3.2 ODS 保留原始事实

原始文件抽取后先进入 ODS，避免后续解析直接依赖原始文件格式。

### 3.3 结构化结果和原始结果分离

解析阶段的中间结果和最终标准化结果分层存储，便于复用和回放。

### 3.4 任务状态和业务状态分离

任务执行状态主要依赖 批量任务 元数据，业务文件状态则由文件主数据表承载。

## 4. 文件主数据层

### 4.1 `t_transfer_object`

文件主数据表，表示一份被系统接入和治理的文件。

对应实体：

- [TransferObjectPO](/Users/zhudaoming/yss-subject-match/valset-standardizer-java/valset-standardizer-tools/transfer/src/main/java/com/yss/valset/transfer/infrastructure/entity/TransferObjectPO.java)

主要职责：

- 文件身份标识
- 文件来源信息
- 文件存储信息
- 文件状态
- 文件指纹
- 任务关联信息

关键字段：

- `transfer_id`
- `source_id`
- `source_type`
- `source_code`
- `original_name`
- `extension`
- `mime_type`
- `size_bytes`
- `fingerprint`
- `source_ref`
- `local_temp_path`
- `real_storage_path`
- `status`
- `received_at`
- `stored_at`
- `business_date`
- `business_id`
- `receive_date`
- `route_id`
- `error_message`
- `probe_result_json`
- `file_meta_json`

建议约束：

- `fingerprint` 建议唯一
- 常用查询可以按 `source_id`、`status`、`receive_date` 建索引

### 4.2 `t_transfer_source`

文件来源配置表。

对应实体：

- [TransferSourcePO](/Users/zhudaoming/yss-subject-match/valset-standardizer-java/valset-standardizer-tools/transfer/src/main/java/com/yss/valset/transfer/infrastructure/entity/TransferSourcePO.java)

主要职责：

- 维护文件来源配置
- 描述邮箱、本地目录、对象存储、SFTP 等来源
- 保存轮询表达式、连接配置和来源元数据

关键字段：

- `source_id`
- `source_code`
- `source_name`
- `source_type`
- `enabled`
- `poll_cron`
- `connection_config_json`
- `source_meta_json`
- `ingest_status`
- `ingest_trigger_type`
- `ingest_started_at`
- `ingest_finished_at`

### 4.3 `t_transfer_route`

文件路由配置表。

对应实体：

- [TransferRoutePO](/Users/zhudaoming/yss-subject-match/valset-standardizer-java/valset-standardizer-tools/transfer/src/main/java/com/yss/valset/transfer/infrastructure/entity/TransferRoutePO.java)

主要职责：

- 描述文件命中规则后应该发往哪里
- 维护目标类型、目标编码、目标路径、重命名规则

关键字段：

- `route_id`
- `source_id`
- `source_type`
- `source_code`
- `rule_id`
- `target_type`
- `target_code`
- `enabled`
- `poll_cron`
- `target_path`
- `rename_pattern`
- `route_status`
- `route_meta_json`

### 4.4 `t_transfer_tag`

标签定义表，用于定义文件标签。

典型用途：

- 标记 `VALUATION_TABLE`
- 标记来源类型
- 标记业务分类

### 4.5 `t_transfer_object_tag`

文件与标签的关联表。

对应实体：

- `TransferObjectTagPO`

主要职责：

- 为文件打标签
- 支持一份文件多个标签
- 支持按标签查询文件

### 4.6 `t_transfer_delivery_record`

具体投递结果表。

对应实体：

- `TransferDeliveryRecordPO`

主要职责：

- 记录一次路由投递执行结果
- 记录成功、失败和重试情况

### 4.7 `t_transfer_run_log`

历史运行日志表，当前流程已停止写入。

保留原因：

- 第一阶段兼容历史数据库和历史数据查询。
- 运行信息已改为应用日志输出，页面通过系统输出日志查看。
- 分拣状态和投递结果以 `t_transfer_object`、`t_transfer_delivery_record` 为准。

### 4.8 `t_transfer_source_checkpoint`

来源扫描游标表。

对应实体：

- `TransferSourceCheckpointPO`

主要职责：

- 保存来源扫描进度
- 记录轻量运行游标

### 4.9 `t_transfer_source_checkpoint_item`

来源去重记录表。

对应实体：

- `TransferSourceCheckpointItemPO`

主要职责：

- 记录已处理过的条目
- 防止重复收取、重复扫描、重复投递

## 5. 原始数据 ODS 层

### 5.1 `t_ods_valuation_filedata`

原始行数据表。

对应实体：

- `ValuationFileDataPO`

主要职责：

- 保存 Excel / CSV 原始行
- 每行一条记录
- 作为解析输入源

关键字段：

- `id`
- `task_id`
- `file_id`
- `row_data_number`
- `row_data_json`

常用索引：

- `task_id`
- `file_id`
- `task_id + row_data_number`

### 5.2 `t_ods_valuation_sheet_style`

sheet 样式快照表。

对应实体：

- `ValuationSheetStylePO`

主要职责：

- 保存 Excel sheet 级样式快照
- 只保留标题、header、合并区域和预览信息
- 用于排障和回溯

关键字段：

- `id`
- `task_id`
- `file_id`
- `sheet_name`
- `style_scope`
- `sheet_style_json`
- `preview_row_count`
- `created_at`

建议唯一约束：

- `(file_id, sheet_name, style_scope)`

## 6. 结构化解析与标准化层

### 6.1 `t_stg_external_valuation`

结构化解析主表。

对应实体：

- `StgExternalValuationPO`

主要职责：

- 描述一次结构化解析的主记录
- 保存 workbook、sheet、标题、行号等元数据

关键字段：

- `id`
- `task_id`
- `file_id`
- `workbook_path`
- `sheet_name`
- `header_row_number`
- `data_start_row_number`
- `title`

### 6.2 `t_stg_external_valuation_basic_info`

基础信息表。

主要职责：

- 保存解析出的基础信息 key/value
- 支撑解析详情页回放

### 6.3 `t_stg_external_valuation_header`

表头表。

主要职责：

- 保存列级表头信息
- 保存表头明细和列元数据

### 6.4 `t_stg_external_valuation_subject`

科目行表。

对应实体：

- `StgExternalValuationSubjectPO`

主要职责：

- 保存解析出的科目行
- 记录层级、父级、根节点、路径等信息

### 6.5 `t_stg_external_valuation_metric`

指标行表。

对应实体：

- `StgExternalValuationMetricPO`

主要职责：

- 保存解析出的指标行
- 记录指标名称、类型和值

### 6.6 STG 外部估值中间表移除说明

旧外部估值中间表已移除。标准化结果不再保存中间快照，改为基于最新 `t_stg_external_valuation*` 运行时标准化，并直接落到 `tr_spv_jjhzgzb` / `tr_spv_index`。
- 保留标准指标码、标准值和映射依据

## 7. 匹配结果层

### 7.1 `t_subject_match_result`

匹配结果表。

对应实体：

- `ValsetMatchResultPO`

主要职责：

- 保存外部估值科目和内部标准科目的匹配结果
- 保存候选、得分、置信度、复核标记

关键字段：

- `task_id`
- `file_id`
- `external_subject_code`
- `external_subject_name`
- `anchor_subject_code`
- `matched_standard_code`
- `matched_standard_name`
- `score`
- `confidence_level`
- `needs_review`
- `match_reason`
- `candidate_count`
- `top_candidates_json`

常用索引：

- `file_id`

## 8. 知识层

### 8.1 `t_ods_standard_subject`

标准科目表。

对应实体：

- `StandardSubjectPO`

主要职责：

- 保存内部标准科目树
- 供匹配阶段加载

### 8.2 `t_ods_mapping_hint`

历史映射提示表。

对应实体：

- `MappingHintPO`

主要职责：

- 保存历史映射经验
- 供匹配时提升召回和置信度

### 8.3 `t_ods_mapping_sample`

映射样本表。

对应实体：

- `MappingSamplePO`

主要职责：

- 保存样本数据
- 供评估和分析使用

## 9. 任务与运行态层

### 9.1 批量任务 元数据

任务执行状态主要由 批量任务 元数据承载：

- `BATCH_JOB_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_STEP_EXECUTION`

职责分工：

- `BATCH_JOB_EXECUTION`：任务级状态
- `BATCH_JOB_EXECUTION_PARAMS`：任务入参
- `BATCH_STEP_EXECUTION`：阶段级状态与耗时

### 9.2 `t_parse_queue`

待解析队列表。

对应实体：

- `ParseQueuePO`

主要职责：

- 记录投递成功后的待解析事件
- 记录订阅、完成、失败、重试状态
- 连接转发投递和解析执行

## 10. 通用 ETL 工作流层

### 10.1 `t_etl_workflow_definition`

工作流定义表。

对应实体：

- `WorkflowDefinitionPO`

主要职责：

- 保存工作流代码、版本、平台和描述

### 10.2 `t_etl_workflow_instance`

工作流实例表。

对应实体：

- `WorkflowInstancePO`

主要职责：

- 保存一次工作流实例执行的状态
- 记录 `business_key`、`external_instance_id`、`context_json`

### 10.3 `t_etl_workflow_stage`

工作流阶段表。

对应实体：

- `WorkflowStagePO`

主要职责：

- 保存工作流阶段定义
- 支持阶段顺序、超时、重试配置

### 10.4 `t_etl_workflow_engine_binding`

平台绑定表。

对应实体：

- `WorkflowEngineBindingPO`

主要职责：

- 记录工作流和底层平台的绑定关系

## 11. 关键关系

### 11.1 文件主数据与任务

- `t_transfer_object.transfer_id` 代表文件主键
- `fileId` 作为后续任务的业务关联键
- 任务不应反向承担文件主数据职责

### 11.2 文件主数据与 ODS

- 文件主数据决定原始文件位置
- ODS 记录原始事实
- 同一份文件复用时主要看 `fingerprint`

### 11.3 ODS 与解析结果

- `t_ods_valuation_filedata` 是原始输入
- `t_stg_external_valuation*` 是结构化解析结果
- `t_stg_external_valuation*` 是解析快照结果

### 11.4 解析结果与匹配结果

- 匹配优先消费 STG / 标准化结果
- 如果没有标准化结果，再回退到解析器

### 11.5 文件主数据与接入日志

- 文件主数据记录当前态
- 接入日志记录过程态

## 12. 索引与约束建议

### 12.1 文件主数据

建议重点关注：

- `fingerprint`
- `source_id`
- `status`
- `receive_date`

### 12.2 原始数据

建议重点关注：

- `task_id`
- `file_id`
- `task_id + row_data_number`

### 12.3 匹配结果

建议重点关注：

- `file_id`

### 12.4 接入日志

建议重点关注：

- `file_id`
- `source_channel + channel_message_id`

## 13. 数据生命周期

### 13.1 文件生命周期

1. 接收文件
2. 写入文件主数据
3. 记录接入日志
4. 落盘或入存储
5. 触发抽取
6. 进入解析
7. 完成匹配

### 13.2 任务生命周期

1. 创建任务
2. 调度或手动执行
3. 任务运行中
4. 成功或失败
5. 结果回写
6. 可复用或重新执行

### 13.3 检查点生命周期

1. 收取前读取游标
2. 扫描并跳过已处理项
3. 成功后写 checkpoint item
4. 成功后更新 scanCursor

## 14. 与接口的对应关系

- `/files/*` 主要读写文件主数据、接入日志和样式快照
- `/api/valuation-workflows/upload` 主要写文件主数据、接入日志和 ODS
- `/api/valuation-workflows/analyze` 主要写 STG / STG 解析快照
- `/api/valuation-workflows/match` 主要写匹配结果
- `/api/valuation-workflows/{fileId}/raw-data` 读 ODS
- `/api/valuation-workflows/{fileId}/stg-data` 读 STG
- `/api/valuation-workflows/{fileId}/stg-data` 读 STG
- `/api/valuation-workflows/{fileId}/match-results` 读匹配结果
- `/outsourced-data-tasks/*` 读 批量任务 元数据和任务读模型
- `/api/etl/workflows/*` 读写通用 ETL 定义、实例和阶段

## 15. 当前实现注意事项

1. 数据库设计里存在“历史遗留表”和“当前主线表”并存的情况，阅读时要区分主线和兼容层。
2. 文件主数据已向 `t_transfer_object` 收敛，但旧接口和旧读模型仍可能保留兼容字段。
3. 任务状态和业务状态分离，不要把 批量任务 元数据误当成文件主数据。
4. `t_parse_queue` 是待解析队列，不是完整的任务主表。

## 16. 小结

当前数据库设计的核心可以概括为：

**文件主数据负责“是谁”，ODS 负责“原始是什么”，STG/STG 负责“解析成什么”，匹配结果负责“最终对应什么”，批量任务 和 db-scheduler 负责“怎么跑”。**
