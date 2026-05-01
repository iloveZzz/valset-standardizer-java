# 文件信息管理设计

## 背景

当前外部估值文件虽然已经支持上传、落盘、抽取 ODS、结构化分析和匹配，但“文件本身”的管理能力仍然散落在上传服务、任务表和落盘路径中。

这会带来几个问题：

- 文件只是落到本地目录，没有独立的文件主数据
- 相同文件复用依赖任务表和文件指纹，缺少统一文件实体
- 未来如果接入邮件、对象存储等渠道，需要重新拼接文件入口逻辑
- 无法方便地按文件渠道、状态、来源、内容指纹做查询和治理

因此需要新增“文件信息管理”模块，用 `t_transfer_object` 统一管理文件身份、来源、存储位置、处理状态和任务关联，估值文件再通过 `t_transfer_object_tag` 的 `VALUATION_TABLE` 标签识别。

## 设计目标

1. 文件和任务解耦，文件成为独立的一等实体。
2. 文件接入支持多渠道：
   - 手动上传
   - 邮件收取
   - 对象存储获取
3. 同一份文件按内容指纹去重，避免重复抽取和重复落库。
4. 文件主表记录文件生命周期，任务表只记录流程执行。
5. 保留当前本地落盘能力，作为一个存储实现，而不是唯一文件模型。

## 核心模型

### 1. 文件主对象 `t_transfer_object`

文件主对象负责描述一份外部估值文件的“身份”和“状态”。

建议字段：

- `file_id`：文件主键
- `file_name_original`：原始文件名
- `file_name_normalized`：规范化后的文件名
- `file_extension`：扩展名
- `mime_type`：MIME 类型
- `file_size_bytes`：文件大小
- `file_fingerprint`：SHA-256 指纹，建议唯一
- `source_channel`：来源渠道
- `source_uri`：渠道侧原始引用，例如本地临时路径、邮件附件引用、对象存储 key
- `storage_type`：存储类型，例如 `LOCAL`、`OSS`、`S3`、`MINIO`
- `storage_uri`：落盘或对象存储后的访问地址
- `file_format`：文件格式，例如 `EXCEL`、`CSV`
- `file_status`：文件状态
- `created_by`：发起人
- `received_at`：接收时间
- `stored_at`：落盘时间
- `last_processed_at`：最后一次处理时间
- `last_task_id`：最近一次关联任务
- `error_message`：最近一次失败原因
- `source_meta_json`：来源渠道扩展信息
- `storage_meta_json`：存储扩展信息
- `remark`：备注

建议约束：

- `UNIQUE(file_fingerprint)`
- `INDEX(source_channel, file_status)`
- `INDEX(received_at)`
- `INDEX(last_task_id)`

### 2. 文件接入日志表 `t_valset_file_ingest_log`

文件主表记录当前态，接入日志表记录每一次接入事件。

建议字段：

- `ingest_id`：日志主键
- `file_id`：关联文件主表
- `source_channel`：接入渠道
- `source_uri`：接入来源
- `channel_message_id`：渠道消息标识，例如邮件 message-id、对象存储事件 id
- `ingest_status`：接入结果
- `ingest_time`：接入时间
- `ingest_meta_json`：接入扩展信息
- `created_by`：操作者或系统
- `error_message`：错误原因

建议用途：

- 审计文件从哪里来的
- 追踪同一份文件被重复投递、重复扫描、重复上传的历史
- 辅助排查邮件/对象存储接入问题

## 枚举建议

### `source_channel`

- `MANUAL_UPLOAD`
- `EMAIL_ATTACHMENT`
- `OBJECT_STORAGE`

### `storage_type`

- `LOCAL`
- `OSS`
- `S3`
- `MINIO`

### `file_status`

建议按生命周期推进：

- `RECEIVED`
- `STORED`
- `READY_FOR_EXTRACT`
- `EXTRACTED`
- `PARSED`
- `MATCHED`
- `FAILED`
- `ARCHIVED`

## 渠道设计

### 1. 手动上传

流程：

1. 用户上传文件
2. 系统落盘或存储到指定后端
3. 计算文件指纹
4. 查 `t_transfer_object`
5. 若 fingerprint 已存在，则复用已有 `file_id`
6. 若不存在，则创建文件主记录
7. 写一条接入日志
8. 触发后续 `EXTRACT_DATA`

说明：

- 当前已有本地落盘实现，可以保留，但要变成 `FileStorage` 的一种实现。
- 文件读取和管理的优先入口应是 `local_temp_path` / `real_storage_path`，`file_id` 只作为业务关联键保留。
- 上传接口返回的不应只是临时路径，而应返回文件主记录信息。

### 2. 邮件收取文件

流程：

1. 定时扫描邮箱或订阅邮箱通知
2. 识别估值文件附件
3. 下载附件到临时存储
4. 计算 fingerprint
5. 写文件主表或复用已有 `file_id`
6. 写接入日志
7. 触发后续 `EXTRACT_DATA`

建议先做最小可行版本：

- 仅支持附件，不支持正文提取
- 仅支持指定邮箱或收件箱目录
- 以 `message-id + fingerprint` 做幂等控制

### 3. 对象存储获取文件

当前先只设计接口和数据模型，不实现具体接入。

建议预留：

- 对象存储文件标识
- bucket/key
- etag/versionId
- 回调事件 id

后续可从 OSS、S3、MinIO 等渠道拉取或监听事件入库。

## 与现有流程的关系

### 现有任务表继续保留

`t_valset_workflow_task` 继续负责：

- `EXTRACT_DATA`
- `PARSE_WORKBOOK`
- `MATCH_SUBJECT`

它不再承担文件主数据职责，只记录流程执行状态、阶段和耗时。

### `file_id` 的来源变化

现在 `file_id` 不应再从任务表倒推，而应该直接来自文件主表。

也就是说：

- 文件主表先创建
- `EXTRACT_DATA` 任务关联 `file_id`
- `PARSE_WORKBOOK` / `MATCH_SUBJECT` 继续使用该 `file_id`
- 文件内容读取优先走 `workbookPath` / `local_temp_path` / `real_storage_path`，不再用 `file_id` 反查文件路径

### 去重规则

建议以 `file_fingerprint` 作为“同一份文件”的唯一识别条件。

规则：

- fingerprint 存在且状态成功时，默认复用已有文件记录和抽取结果
- `forceRebuild=true` 时，可重新创建任务，但文件主记录仍然保持同一条

## 建议接口

### 文件管理接口

- `POST /api/files/upload`
- `GET /api/files/{fileId}`
- `GET /api/files/by-path?path=`
- `GET /api/files?sourceChannel=&status=&fingerprint=`
- `GET /api/files/by-path/ingest-logs?path=`
- `GET /api/files/by-path/sheet-styles?path=`
- `GET /api/files/{fileId}/ingest-logs`
- `POST /api/files/{fileId}/reprocess`

### 与现有流程接口的关系

- `/api/valuation-workflows/upload` 可改为复用文件管理接口
- `/api/valuation-workflows/analyze` 继续消费 `fileId` 作为任务关联键，但真正读取文件时应走路径
- `/api/valuation-workflows/match` 继续消费 `fileId` 作为任务关联键，但真正读取文件时应走路径
- `/api/valuation-workflows/full-process` 先创建文件记录，再串联后续流程

## 建议表结构草案

当前实现已经将文件主数据下沉到 `t_transfer_object`，并通过 `t_transfer_object_tag` 标记 `VALUATION_TABLE`。如果需要继续扩展文件管理能力，建议优先围绕这两个表补齐查询、标签和接入日志能力，而不是再引入独立的文件主表。

## 执行任务计划

### 阶段 1：领域和表结构

目标：

- 新增文件主表和接入日志表
- 定义文件实体、状态、渠道枚举
- 新增文件网关和持久层

产出：

- `TransferObject`
- `TransferObjectTag`
- `FileIngestLog`
- `TransferObjectGateway`
- `t_transfer_object`
- `t_transfer_object_tag`
- `t_valset_file_ingest_log`

### 阶段 2：手动上传改造

目标：

- `/upload` 从“落盘 + 任务”改成“文件管理 + 任务”
- 返回 `fileId`、`fileFingerprint`、`sourceChannel`、`fileStatus`
- 默认复用同 fingerprint 文件

产出：

- 上传服务重构
- 文件管理应用服务
- 上传接口响应升级

### 阶段 3：邮件接入

目标：

- 实现邮件附件拉取
- 附件入库、去重、写日志
- 触发后续抽取任务

产出：

- 邮件拉取定时任务
- 邮件附件获取适配器
- 文件接入日志

### 阶段 4：对象存储通道预留

目标：

- 定义对象存储文件获取接口
- 预留 DTO 和元数据字段
- 暂不接真实供应商实现

产出：

- `ObjectStorageFileFetcher`
- `ObjectStorageFileSourceDTO`
- 相关枚举与元数据字段

### 阶段 5：接口与测试

目标：

- 新增文件查询接口
- 更新工作流接口的入参和返回
- 补 E2E：上传、复用、重新处理、查询

产出：

- 文件管理 API
- OpenAPI 文档
- Playwright 场景测试
- 数据库初始化脚本更新

## 推荐落地顺序

1. 先做文件主表和接入日志表。
2. 再把手动上传接入文件主表。
3. 然后把 `fileId` 的来源统一到文件主表。
4. 最后补邮件通道和对象存储通道。

## 设计边界

- 不把文件主数据塞回任务表。
- 不让任务表承担文件生命周期管理。
- 不把对象存储通道一次性实现完，先预留结构。
- 不破坏现有 ODS / DWD / 匹配链路。
