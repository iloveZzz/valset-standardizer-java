# 生成 API 使用分析与替换计划

## Summary

- 目标：分析 `packages/src/api/generated/valset/index.ts` 中生成接口在项目源码内的实际使用情况，并规划将源码中写死的接口请求统一替换为生成客户端调用。
- 范围：仅覆盖当前前端源码 `packages/src`，重点关注 `packages/src/api/*.ts` 中的手写 API 封装，以及视图层中直接使用 `customInstance` 写死 URL 的位置。
- 结论概览：
  - 生成客户端方法总数：135
  - 已使用方法数：39
  - 未使用方法数：96
  - 当前仍存在 5 个手写 API 封装文件和 3 个页面 Hook 直接写死 URL，适合统一替换为生成客户端。

## Current State Analysis

### 1. 生成 API 的来源与调用方式

- OpenAPI 源文件位于 `openapi/openapi.json`。
- Orval 配置位于 `orval.config.ts`，生成入口为 `packages/src/api/generated/valset/index.ts`，底层统一通过 `packages/src/api/mutator.ts` 发起请求。
- `packages/src/api/index.ts` 同时导出生成 API 和手写 API，因此当前项目处于“生成 API + 手写 API 并存”的过渡状态。

### 2. 生成 API 使用情况

- 已扫描 `packages/src` 下所有 `ts/tsx/js/jsx/vue` 文件。
- 生成方法总数：135。
- 已使用方法数：39。
- 未使用方法数：96。

### 3. 已使用生成 API 的主要模块

- Transfer 相关页面已大面积使用生成客户端：
  - `packages/src/views/TransferTarget/hooks/useTransferPage.ts`
  - `packages/src/views/TransferTag/hooks/useTransferPage.ts`
  - `packages/src/views/TransferSource/hooks/useTransferPage.ts`
  - `packages/src/views/TransferRule/hooks/useTransferPage.ts`
  - `packages/src/views/TransferRouteConfig/hooks/useTransferPage.ts`
  - `packages/src/views/TransferRunLog/hooks/useTransferPage.ts`
  - `packages/src/views/TransferOverview/hooks/useTransferPage.ts`
  - `packages/src/views/TransferObject/hooks/useTransferPage.ts`
  - `packages/src/views/TransferInbox/hooks/useTransferPage.ts`

### 4. 仍在使用手写封装的 API 文件

- `packages/src/api/transferDeliveryRecord.ts`
  - 手写接口：`/transfer-delivery-records/summary`
  - 对应生成方法：`summarizeToday`
- `packages/src/api/outsourcedDataTask.ts`
  - 手写接口组：`/outsourced-data-tasks/**`
  - 对应生成方法：`summary` `pageTasks` `getTask` `listSteps` `execute` `retry1` `stop1` `retryStep` `batchExecute` `batchRetry` `batchStop`
- `packages/src/api/etlWorkflowConfig.ts`
  - 手写接口组：`/etl/workflows/**`
  - 对应生成方法：`saveDefinition` `listDefinitions` `syncDefinition` `onlineDefinition` `offlineDefinition` `getDefinition` `deleteDefinition` `runDefinition` `validateDefinition` `listPlatforms`
- `packages/src/api/etlWorkflowInstance.ts`
  - 手写接口组：`/etl/workflows/instances/**` 与 `/dolphinscheduler/projects/analysis/workflow-state-count`
  - 对应生成方法：`listInstances` `countWorkflowState` `getInstance` `listTasks` `getTaskLog` `trigger` `stop` `pause` `resume` `retry` `callback`
  - 例外：`listEtlWorkflowInstanceLogs` 当前没有明确对应的生成方法，不能强行替换
- `packages/src/api/etlWorkflowTaskInstance.ts`
  - 手写接口组：`/etl/workflows/task-instances/**` 与 `/dolphinscheduler/projects/analysis/task-state-count`
  - 对应生成方法：`listTaskInstances` `forceTaskSuccess` `getTaskLog1` `countTaskState`

### 5. 视图层中直接写死 URL 的位置

- `packages/src/views/TransferRunLog/hooks/useTransferPage.ts`
  - 直接调用 `/transfer-run-logs/cleanup`
  - 对应生成方法：`cleanupLogs`
- `packages/src/views/TransferObject/hooks/useTransferPage.ts`
  - 直接调用 `/transfer-objects/redeliver`
  - 对应生成方法：`redeliver1`
  - 直接调用 `/transfer-objects/{transferId}/download`
  - 对应生成方法：`downloadObject`
  - 直接调用 `/transfer-objects/retag`
  - 对应生成方法：`retag`
- `packages/src/views/TransferInbox/hooks/useTransferPage.ts`
  - 直接调用 `/transfer-objects/{transferId}/mail-info`
  - 对应生成方法：`getMailInfo`
  - 直接调用 `/transfer-objects/mail-inbox/analysis`
  - 对应生成方法：`analyzeMailInbox`
  - 直接调用 `/transfer-objects/mail-inbox`
  - 对应生成方法：`pageMailInbox`
  - 直接调用 `/transfer-objects/{transferId}/download`
  - 对应生成方法：`downloadObject`

### 6. 当前未使用的生成方法类别

- ETL 工作流定义类方法基本未被生成客户端直接消费：
  - `saveDefinition` `listDefinitions` `syncDefinition` `onlineDefinition` `offlineDefinition` `getDefinition` `deleteDefinition` `runDefinition` `validateDefinition` `listPlatforms`
- ETL 实例与任务实例类方法基本未被生成客户端直接消费：
  - `listInstances` `trigger` `stop` `pause` `resume` `retry` `getInstance` `listTasks` `getTaskLog` `listTaskInstances` `forceTaskSuccess` `getTaskLog1` `callback` `countWorkflowState` `countTaskState`
- 外包任务类方法基本全部未被生成客户端直接消费：
  - `summary` `pageTasks` `getTask` `listSteps` `execute` `retry1` `stop1` `retryStep` `batchExecute` `batchRetry` `batchStop`
- 文件收发与邮件收件箱增强能力部分未被消费：
  - `cleanupLogs` `getMailInfo` `downloadObject` `pageMailInbox` `analyzeMailInbox` `redeliver1` `retag`
- 其余尚未落地到业务的能力还包括 Parse Queue、Schedule、Knowledge Import、File Management、Parse Rule 等多个分组。

## Proposed Changes

### 第一阶段：替换视图层直接写死 URL

#### 文件：`packages/src/views/TransferRunLog/hooks/useTransferPage.ts`

- 将 `customInstance` 直连 `/transfer-run-logs/cleanup` 替换为 `api.cleanupLogs(...)`。
- 保留现有业务提示文案与 `unwrapSingleResult` 解包逻辑。
- 价值：
  - 消除页面层硬编码 URL。
  - 与当前页面已使用的 `api.analyzeLogs`、`api.pageLogs` 保持同一调用风格。

#### 文件：`packages/src/views/TransferObject/hooks/useTransferPage.ts`

- 将以下手写请求替换为生成方法：
  - `/transfer-objects/redeliver` -> `api.redeliver1`
  - `/transfer-objects/{id}/download` -> `api.downloadObject`
  - `/transfer-objects/retag` -> `api.retag`
- 处理点：
  - `downloadObject` 返回 Blob，需要确认生成方法的返回结构与 `customInstance` 的 Blob 特殊处理兼容。
  - 如果生成方法未显式带 `responseType: "blob"`，执行时需要判断是补充生成逻辑、包装调用，还是保留这一处手写下载能力。
- 价值：
  - 把对象页中剩余硬编码接口全部收敛到生成客户端。

#### 文件：`packages/src/views/TransferInbox/hooks/useTransferPage.ts`

- 将以下手写请求替换为生成方法：
  - `/transfer-objects/{id}/mail-info` -> `api.getMailInfo`
  - `/transfer-objects/mail-inbox/analysis` -> `api.analyzeMailInbox`
  - `/transfer-objects/mail-inbox` -> `api.pageMailInbox`
  - `/transfer-objects/{id}/download` -> `api.downloadObject`
- 处理点：
  - `pageMailInbox` 当前页面使用自定义 URL 拼接方式进行分页和筛选；替换时需改成生成方法参数对象调用。
  - `downloadObject` 同样需验证 Blob 下载兼容性。
- 价值：
  - 去除收件箱页面内所有剩余 URL 硬编码。

### 第二阶段：替换手写 API 封装文件

#### 文件：`packages/src/api/transferDeliveryRecord.ts`

- 用生成方法 `summarizeToday` 替换内部手写请求。
- 推荐方式：
  - 直接删除该文件并让调用方改为使用生成客户端；或
  - 保留该文件作为薄包装层，但内部改为调用 `getJavaSpringBootQuartzApi().summarizeToday()`。
- 优先建议：删除薄包装，直接在使用方接入生成客户端，避免重复抽象。

#### 文件：`packages/src/api/outsourcedDataTask.ts`

- 将整个文件改造为对生成方法的包装，或直接移除并让调用方改用生成客户端。
- 对应映射：
  - `getOutsourcedDataTaskSummary` -> `summary`
  - `pageOutsourcedDataTasks` -> `pageTasks`
  - `getOutsourcedDataTask` -> `getTask`
  - `listOutsourcedDataTaskSteps` -> `listSteps`
  - `executeOutsourcedDataTask` -> `execute`
  - `retryOutsourcedDataTask` -> `retry1`
  - `stopOutsourcedDataTask` -> `stop1`
  - `retryOutsourcedDataTaskStep` -> `retryStep`
  - `batchExecuteOutsourcedDataTasks` -> `batchExecute`
  - `batchRetryOutsourcedDataTasks` -> `batchRetry`
  - `batchStopOutsourcedDataTasks` -> `batchStop`
- 处理点：
  - 该文件同时自定义了大量类型；如果替换为生成类型，需要同步调整依赖这些类型的页面与 `types.ts` 文件。
  - 为降低改动面，第一步可以保留函数名与本地类型别名，只把内部请求改成调用生成客户端。

#### 文件：`packages/src/api/etlWorkflowConfig.ts`

- 使用生成方法替换工作流定义相关请求。
- 对应映射：
  - `listEtlWorkflowDefinitions` -> `listDefinitions`
  - `listEtlWorkflowPlatforms` -> `listPlatforms`
  - `getEtlWorkflowDefinition` -> `getDefinition`
  - `validateEtlWorkflowDefinition` -> `validateDefinition`
  - `saveEtlWorkflowDefinition` -> `saveDefinition`
  - `syncEtlWorkflowDefinition` -> `syncDefinition`
  - `onlineEtlWorkflowDefinition` -> `onlineDefinition`
  - `offlineEtlWorkflowDefinition` -> `offlineDefinition`
  - `deleteEtlWorkflowDefinition` -> `deleteDefinition`
  - `runEtlWorkflowDefinition` -> `runDefinition`
- 处理点：
  - 当前文件定义了前端侧友好的 DTO 类型，可先保留类型定义，函数内部切换到生成 API。

#### 文件：`packages/src/api/etlWorkflowInstance.ts`

- 使用生成方法替换实例管理相关请求。
- 对应映射：
  - `listEtlWorkflowInstances` -> `listInstances`
  - `getEtlWorkflowInstanceStateCount` -> `countWorkflowState`
  - `getEtlWorkflowInstance` -> `getInstance`
  - `listEtlWorkflowInstanceTasks` -> `listTasks`
  - `getEtlWorkflowInstanceTaskLog` -> `getTaskLog`
  - `triggerEtlWorkflowInstance` -> `trigger`
  - `stopEtlWorkflowInstance` -> `stop`
  - `pauseEtlWorkflowInstance` -> `pause`
  - `resumeEtlWorkflowInstance` -> `resume`
  - `retryEtlWorkflowInstance` -> `retry`
  - `callbackEtlWorkflowInstance` -> `callback`
- 保留项：
  - `listEtlWorkflowInstanceLogs` 暂不改，因为生成客户端中未发现直接对应方法。

#### 文件：`packages/src/api/etlWorkflowTaskInstance.ts`

- 使用生成方法替换任务实例相关请求。
- 对应映射：
  - `listEtlWorkflowTaskInstances` -> `listTaskInstances`
  - `forceSuccessEtlWorkflowTaskInstance` -> `forceTaskSuccess`
  - `getEtlWorkflowTaskInstanceLog` -> `getTaskLog1`
  - `getEtlWorkflowTaskInstanceStateCount` -> `countTaskState`

### 第三阶段：收敛导出与清理遗留层

#### 文件：`packages/src/api/index.ts`

- 在第一、二阶段完成后，重新整理导出：
  - 若手写 API 文件继续保留为兼容层，则仅保留必要导出。
  - 若页面已全部改为直接消费生成客户端，则删除对应手写导出。

#### 文件：`packages/src/views/TransferOverview/hooks/useTransferPage.ts`

- 在 `transferDeliveryRecord.ts` / `outsourcedDataTask.ts` 被替换后，统一改为直接使用生成客户端，避免一部分逻辑走 `api.*`，另一部分仍走旧包装函数。

#### 文件：`packages/src/views/OutsourcedDataTask/hooks/useOutsourcedDataTaskPage.ts`

- 在 `outsourcedDataTask.ts` 切换后，确认调用方无需再依赖手写 URL 语义，必要时同步引入生成类型或包装层类型。

#### 文件：ETL 页面相关 Hook / types

- 相关文件包括但不限于：
  - `packages/src/views/EtlWorkflowConfig/hooks/useEtlWorkflowConfigPage.ts`
  - `packages/src/views/EtlWorkflowInstance/hooks/useWorkflowInstancePage.ts`
  - `packages/src/views/EtlWorkflowTaskInstance/hooks/useEtlWorkflowTaskInstancePage.ts`
  - 对应 `types.ts`
- 这些文件在第二阶段若继续依赖手写 API 类型，需要确认类型兼容；若不兼容，补齐类型映射。

## Assumptions & Decisions

- 决策 1：本次替换以“优先统一请求入口”为主，不以“立即删除所有本地 DTO 类型”为目标。
  - 原因：多个页面和类型文件依赖手写 API 模块中自定义的 DTO，直接全部切到生成类型会扩大改动面。
- 决策 2：优先替换“视图层直接写死 URL”与“纯请求转发型手写 API”。
  - 原因：这类改动收益最高、风险最低，最符合“统一通过生成 API 访问接口”的目标。
- 决策 3：`listEtlWorkflowInstanceLogs` 暂时保留手写实现。
  - 原因：在当前生成客户端中未发现明确等价方法。
- 决策 4：下载类接口需要先验证生成代码与 `customInstance` 对 Blob 的兼容性。
  - 原因：当前 `mutator.ts` 对 Blob 响应做了特殊返回处理，若生成方法未透传合适的 `responseType`，替换可能导致下载失效。
- 决策 5：不修改 `packages/src/api/generated/valset/index.ts`。
  - 原因：该文件为生成产物，后续应通过 OpenAPI / Orval 再生，而不是手改。

## Verification

### 代码核对

- 确认以下文件中不再出现页面层硬编码 URL：
  - `packages/src/views/TransferRunLog/hooks/useTransferPage.ts`
  - `packages/src/views/TransferObject/hooks/useTransferPage.ts`
  - `packages/src/views/TransferInbox/hooks/useTransferPage.ts`
- 确认以下文件中不再直接调用 `customInstance` 拼写业务接口 URL，或仅保留确有必要的兼容项：
  - `packages/src/api/transferDeliveryRecord.ts`
  - `packages/src/api/outsourcedDataTask.ts`
  - `packages/src/api/etlWorkflowConfig.ts`
  - `packages/src/api/etlWorkflowInstance.ts`
  - `packages/src/api/etlWorkflowTaskInstance.ts`

### 搜索验证

- 在 `packages/src` 范围内搜索以下模式，应只剩生成文件或必要例外：
  - `url: "/transfer-`
  - `url: "/etl/`
  - `url: "/outsourced-data-tasks`
  - `customInstance<`

### 类型与构建验证

- 运行类型检查：
  - 在 `packages` 目录执行 `pnpm type-check`
- 运行代码检查：
  - 在 `packages` 目录执行 `pnpm lint`

### 功能回归验证

- 人工验证以下页面功能：
  - Transfer Run Log：列表加载、分析加载、清理日志
  - Transfer Object：列表、分析、详情、重新投递、重新打标、下载
  - Transfer Inbox：列表、分析、详情、邮件信息、下载
  - Transfer Overview：总览统计正常
  - Outsourced Data Task：列表、详情、批量操作正常
  - ETL Workflow Config / Instance / Task Instance：列表、详情、执行类操作正常
