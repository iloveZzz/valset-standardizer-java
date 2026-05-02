# 估值表解析任务管理技术架构设计

## 1. 模块定位

估值表解析任务管理单独落在后端 `tools/task` 模块，Maven artifact 为 `valset-standardizer-task`，Java 包名为 `com.yss.valset.task`。

该模块只负责“数据处理批次”和“阶段执行明细”的聚合管理，不承接具体解析、分拣、投递或标准化算法。

## 2. 与现有 tools 模块的边界

| 模块 | 现有职责 | 与任务模块关系 |
|---|---|---|
| `tools/transfer` | 文件来源、规则、路由、投递 | 不作为本页面主链路；仅作为文件来源事实的可选输入 |
| `tools/analysis` | 解析队列、解析生命周期、ODS/CSV 解析事件 | 向任务模块提供解析阶段事实 |
| `tools/extract` | 结构标准化、标准化落地能力 | 向任务模块提供标准化和落地阶段事实 |
| `tools/batch` | 调度和任务派发 | 触发任务阶段执行，后续写入阶段事件 |
| `tools/task` | 批次聚合、阶段链路、任务控制、页面接口 | 新增独立任务管理中枢 |

## 3. 后端分层

当前第一版采用和 `tools/transfer` 类似的责任分层：

```text
tools/task
└── src/main/java/com/yss/valset/task
    ├── domain
    │   └── model
    │       ├── OutsourcedDataTaskStage
    │       └── OutsourcedDataTaskStatus
    ├── application
    │   ├── command
    │   ├── dto
    │   ├── service
    │   └── impl/management
    └── web/controller
```

当前版本采用稳定 API 契约、阶段事件落库和列表轮询刷新：

```text
Controller
  -> OutsourcedDataTaskManagementAppService
    -> OutsourcedDataTaskGateway
      -> OutsourcedDataTaskRepository
        -> t_outsourced_data_task_batch
        -> t_outsourced_data_task_step
        -> t_outsourced_data_task_log
```

## 4. 核心模型

### 4.1 批次模型

批次代表一次产品估值数据从文件进入到标准化落地和后续加工的完整处理单元。

当前批次聚合键优先级：

```text
FILE-{fileId} -> BIZ-{businessKey} -> TASK-{taskId}
```

文件主数据已经修复或任务事件带有 `fileId` 时，解析、标准化、标准表落地、后续加工都会归到同一个 `FILE-{fileId}` 批次。缺少文件标识时才退回业务键或任务键。

数据库层建议唯一键：

```text
businessDate + productCode + fileFingerprint
```

当前 DTO：

- `OutsourcedDataTaskBatchDTO`
- `OutsourcedDataTaskBatchDetailDTO`
- `OutsourcedDataTaskSummaryDTO`

### 4.2 阶段模型

阶段代表批次下某个固定业务阶段的执行记录。

当前固定阶段：

1. `FILE_PARSE`
2. `STRUCTURE_STANDARDIZE`
3. `SUBJECT_RECOGNIZE`
4. `STANDARD_LANDING`
5. `DATA_PROCESSING`
6. `VERIFY_ARCHIVE`

当前 DTO：

- `OutsourcedDataTaskStepDTO`
- `OutsourcedDataTaskStageSummaryDTO`
- `OutsourcedDataTaskLogDTO`

## 5. API 契约

Controller 根路径不带 `/api` 前缀，继续遵守当前工程策略，由前端代理统一补 `/api`。

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/outsourced-data-tasks/summary` | 查询任务总览 |
| `GET` | `/outsourced-data-tasks` | 分页查询任务批次 |
| `GET` | `/outsourced-data-tasks/{batchId}` | 查询批次详情 |
| `GET` | `/outsourced-data-tasks/{batchId}/steps` | 查询阶段明细 |
| `GET` | `/outsourced-data-tasks/{batchId}/logs` | 分页查询日志 |
| `POST` | `/outsourced-data-tasks/{batchId}/execute` | 执行批次 |
| `POST` | `/outsourced-data-tasks/{batchId}/retry` | 重跑批次 |
| `POST` | `/outsourced-data-tasks/{batchId}/stop` | 停止批次 |
| `POST` | `/outsourced-data-tasks/{batchId}/steps/{stepId}/retry` | 重跑阶段 |
| `POST` | `/outsourced-data-tasks/batch-execute` | 批量执行 |
| `POST` | `/outsourced-data-tasks/batch-retry` | 批量重跑 |
| `POST` | `/outsourced-data-tasks/batch-stop` | 批量停止 |

## 6. 当前落地状态

已完成：

- 新增 `tools/task` Maven 模块。
- 将 `tools/task` 加入 `tools/pom.xml`。
- 将 `valset-standardizer-task` 加入 `valset-standardizer-boot` 依赖。
- 新增阶段枚举、状态枚举、查询/操作命令、DTO、应用服务接口、默认应用服务、Controller。
- 新增持久化端口、批次/阶段/日志 PO、Repository、MyBatis Gateway。
- 新增 Liquibase `task.sql`，包含 `t_outsourced_data_task_batch`、`t_outsourced_data_task_step`、`t_outsourced_data_task_log`。
- DDL 主键和关联字段使用字符串标识，匹配 `BATCH-*`、`FILE-*`、`TASK-*` 等业务批次和底层任务标识。
- 新增 `OutsourcedDataTaskLifecycleListener`，监听 `ParseLifecycleEvent` 并归档解析生命周期事件。
- 新增 `WorkflowTaskLifecycleEvent`，由 `DefaultTaskDispatcher` 发布通用工作流任务状态，事件包含文件标识、输入摘要、输出摘要和上下文字段，任务模块归档非解析任务到对应阶段，其中加工类任务落到 `DATA_PROCESSING`。
- `ParseQueueObserverJob` 在构建、创建、派发、完成、失败解析任务时补充 `fileId`、数据源类型和原始文件名，避免队列事件与解析执行事件拆成不同批次。
- 批次状态和当前阶段由阶段明细落库后统一聚合刷新，前端只消费后端返回结果。
- 默认应用服务在 Spring 环境中优先使用持久化 Gateway；静态样例只作为无 Gateway 的单元测试兜底。
- 前端新增独立 `OutsourcedDataTask` 页面、路由和一级菜单入口，完成静态工作台骨架。
- 前端新增 `packages/src/api/outsourcedDataTask.ts`，页面优先调用真实接口，后端不可用时回退样例数据。
- 批次维度暂未新增独立 SSE 通道，页面采用 10 秒列表轮询刷新。

待完成：

- 继续细化 `DATA_PROCESSING` 的业务子类型和操作编排，避免所有后续加工只显示为同一个阶段明细。
- 刷新 OpenAPI 产物后，决定保留当前手写 API 封装或迁移到 generated client。
