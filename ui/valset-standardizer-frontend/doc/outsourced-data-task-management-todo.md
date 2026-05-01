# 委外数据任务管理落地 TODO

## 1. 目标

新增“委外数据任务管理”页面，用于串联委外核心模块的数据处理链路，不纳入 transfer 分拣投递模块。

页面覆盖从委外估值文件解析、结构标准化、标准表落地，到后续数据加工任务的全链路任务管理能力，重点解决以下问题：

- 用户可以按业务日期、产品、机构、阶段、状态查询数据处理批次。
- 用户可以看到每个批次当前卡在哪个阶段。
- 用户可以定位失败原因、查看阶段日志、重跑单阶段或整批次。
- 用户可以从一个工作台进入文件、解析结果、标准化结果和后续加工结果。

## 2. 范围边界

### 2.1 本期包含

- 委外数据任务管理一级页面。
- 批次级状态总览。
- 阶段级任务链路展示。
- 批次列表、筛选、分页、状态标签。
- 批次展开后的阶段执行明细。
- 任务详情抽屉。
- 批量执行、批量重跑、批量停止。
- 单批次执行、重跑、停止、查看日志、查看数据。

### 2.2 本期不包含

- transfer 来源、目标、规则、路由、投递配置。
- 文件分拣规则管理。
- 新的估值解析规则设计器。
- 后续加工任务的具体业务算法实现。
- 大屏看板。

## 3. 业务链路定义

建议把页面固定为 6 个阶段：

| 阶段编码 | 阶段名称 | 说明 | 典型来源 |
|---|---|---|---|
| `FILE_PARSE` | 文件解析 | 文件识别、Sheet 解析、原始行列抽取 | `valuation-workflows/analyze`、解析任务 |
| `STRUCTURE_STANDARDIZE` | 结构标准化 | 字段映射、数据清洗、STG 结构转换 | 标准化任务 |
| `SUBJECT_RECOGNIZE` | 科目识别 | 科目匹配、属性识别、标签补全 | match 任务、知识库匹配 |
| `STANDARD_LANDING` | 标准表落地 | STG/DWD/标准持仓/估值数据写入 | 落地任务 |
| `DATA_PROCESSING` | 加工任务 | 后续 TODO 数据加工、派生数据生成 | 后续加工调度 |
| `VERIFY_ARCHIVE` | 校验归档 | 一致性校验、结果确认、归档完成 | 校验任务 |

状态建议统一为：

| 状态编码 | 状态名称 | 前端颜色 |
|---|---|---|
| `PENDING` | 待处理 | default |
| `RUNNING` | 处理中 | processing |
| `SUCCESS` | 已完成 | success |
| `FAILED` | 失败 | error |
| `STOPPED` | 已停止 | warning |
| `BLOCKED` | 阻塞 | error |

## 4. 后端 TODO

### P0. 批次聚合模型

- [x] 新增批次视图 DTO：`OutsourcedDataTaskBatchDTO`。
- [x] 新增或确认批次聚合实体：`OutsourcedDataTaskBatch`。
- [x] 明确运行期批次聚合键：优先 `FILE-{fileId}`，缺少文件标识时退回 `BIZ-{businessKey}` 或 `TASK-{taskId}`。
- [ ] 明确数据库业务唯一键：建议 `businessDate + productCode + fileFingerprint`。
- [x] 批次需要保存文件主键、filesys 文件标识、产品代码、产品名称、管理机构、估值日期、来源类型。
- [x] 批次状态由阶段明细聚合得到，不允许前端自行推断。
- [x] 批次当前阶段由最近一个 `RUNNING`、`FAILED`、`BLOCKED` 阶段决定。

建议字段：

| 字段 | 说明 |
|---|---|
| `batchId` | 批次主键 |
| `batchName` | 批次名称 |
| `businessDate` | 业务日期 |
| `valuationDate` | 估值日期 |
| `productCode` | 产品代码 |
| `productName` | 产品名称 |
| `managerName` | 管理人或机构 |
| `fileId` | 文件主键 |
| `filesysFileId` | 文件服务文件标识 |
| `originalFileName` | 原始文件名 |
| `sourceType` | 数据来源 |
| `currentStage` | 当前阶段 |
| `status` | 批次状态 |
| `progress` | 批次进度 |
| `startedAt` | 开始时间 |
| `endedAt` | 结束时间 |
| `durationMs` | 耗时 |
| `lastErrorCode` | 最近错误编码 |
| `lastErrorMessage` | 最近错误摘要 |

### P0. 阶段明细模型

- [x] 新增阶段枚举：`OutsourcedDataTaskStage`。
- [x] 新增状态枚举：`OutsourcedDataTaskStatus`。
- [x] 新增阶段视图 DTO：`OutsourcedDataTaskStepDTO`。
- [x] 新增阶段持久化实体：`OutsourcedDataTaskStepPO`。
- [x] 新增日志持久化实体：`OutsourcedDataTaskLogPO`。
- [x] 新增或确认阶段明细实体：`OutsourcedDataTaskStep`。
- [x] 每个批次下最多一条当前有效阶段记录，历史重跑记录通过 `runNo` 区分。
- [x] 阶段明细需要关联底层任务 ID，例如 parse task、match task、landing task。
- [x] 阶段明细需要保留输入摘要、输出摘要、错误摘要和日志定位信息。

建议字段：

| 字段 | 说明 |
|---|---|
| `stepId` | 阶段明细主键 |
| `batchId` | 批次主键 |
| `stage` | 阶段编码 |
| `stageName` | 阶段名称 |
| `taskId` | 底层任务 ID |
| `taskType` | 底层任务类型 |
| `runNo` | 执行次数 |
| `currentFlag` | 是否当前有效阶段记录 |
| `triggerMode` | 调度执行、手动执行、依赖触发 |
| `status` | 阶段状态 |
| `progress` | 阶段进度 |
| `startedAt` | 开始时间 |
| `endedAt` | 结束时间 |
| `durationMs` | 耗时 |
| `inputSummary` | 输入摘要 |
| `outputSummary` | 输出摘要 |
| `errorCode` | 错误编码 |
| `errorMessage` | 错误摘要 |
| `logRef` | 日志定位 |

### P0. 查询接口

- [x] 新增 `tools/task` 独立模块。
- [x] 新增批次统计接口。
- [x] 新增批次分页查询接口。
- [x] 新增批次详情接口。
- [x] 新增批次阶段明细接口。
- [x] 所有 Controller 返回统一使用 `SingleResult`、`MultiResult`、`PageResult`。
- [x] 查询接口接入持久化网关。
- [x] 从 `analysis`、`extract`、`batch` 接入真实阶段事件。

建议接口：

| 方法 | 路径 | 返回 |
|---|---|---|
| `GET` | `/api/outsourced-data-tasks/summary` | `SingleResult<OutsourcedDataTaskSummaryDTO>` |
| `GET` | `/api/outsourced-data-tasks` | `PageResult<OutsourcedDataTaskBatchDTO>` |
| `GET` | `/api/outsourced-data-tasks/{batchId}` | `SingleResult<OutsourcedDataTaskBatchDetailDTO>` |
| `GET` | `/api/outsourced-data-tasks/{batchId}/steps` | `MultiResult<OutsourcedDataTaskStepDTO>` |
| `GET` | `/api/outsourced-data-tasks/{batchId}/logs` | `PageResult<OutsourcedDataTaskLogDTO>` |

### P1. 操作接口

- [x] 新增批次执行接口。
- [x] 新增批次重跑接口。
- [x] 新增批次停止接口。
- [x] 新增阶段重跑接口。
- [x] 新增批量执行、批量重跑、批量停止接口。
- [x] 操作接口返回操作结果。
- [ ] 操作接口接入真实任务编排并返回不可操作原因。

建议接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/outsourced-data-tasks/{batchId}/execute` | 执行批次 |
| `POST` | `/api/outsourced-data-tasks/{batchId}/retry` | 重跑批次 |
| `POST` | `/api/outsourced-data-tasks/{batchId}/stop` | 停止批次 |
| `POST` | `/api/outsourced-data-tasks/{batchId}/steps/{stepId}/retry` | 重跑阶段 |
| `POST` | `/api/outsourced-data-tasks/batch-execute` | 批量执行 |
| `POST` | `/api/outsourced-data-tasks/batch-retry` | 批量重跑 |
| `POST` | `/api/outsourced-data-tasks/batch-stop` | 批量停止 |

### P1. 任务事件与进度

- [x] 打通底层任务事件到批次阶段明细。
- [x] 监听解析生命周期事件并写入批次、阶段、日志。
- [x] 监听通用工作流任务事件，将后续加工类任务写入 `DATA_PROCESSING`。
- [x] 每个阶段开始、成功、失败、停止时写入完整阶段事件。
- [x] 批次聚合状态在阶段事件落库后同步刷新。
- [x] 如已有 SSE 能力，新增批次维度事件订阅；否则本期先使用列表轮询。

建议 SSE 路径：

- `/api/outsourced-data-tasks/events/stream`

## 5. 前端 TODO

### P0. 页面骨架

- [x] 新增页面目录：`packages/src/views/OutsourcedDataTask`。
- [x] 新增页面入口：`packages/src/views/OutsourcedDataTask/index.vue`。
- [x] 新增业务 Hook：`packages/src/views/OutsourcedDataTask/hooks/useOutsourcedDataTaskPage.ts`。
- [x] 新增类型定义：`packages/src/views/OutsourcedDataTask/types.ts`。
- [x] 新增样式文件：`packages/src/views/OutsourcedDataTask/index.less`。
- [x] 在路由中新增入口：`/outsourced-data-tasks`。
- [x] 菜单名称：`委外数据任务`。

### P0. 顶部统计区

- [x] 实现 4 个统计卡片。
- [x] 字段对应后端 summary：
  - `totalCount`
  - `runningCount`
  - `successCount`
  - `failedCount`
- [x] 点击统计卡后刷新列表筛选。
- [x] 卡片视觉沿用 Figma：灰、蓝、绿、红。

### P0. 阶段链路区

- [x] 实现 6 段横向阶段条。
- [x] 阶段显示名称和说明。
- [x] 阶段显示异常数角标。
- [x] 点击阶段后设置 `stage` 筛选条件。
- [x] 当前选中阶段高亮。

### P0. 查询区

- [x] 实现任务日期筛选。
- [x] 实现管理机构筛选。
- [x] 实现产品名称或代码搜索。
- [x] 实现处理阶段筛选。
- [x] 实现任务状态筛选。
- [x] 实现数据来源筛选。
- [x] 实现异常类型筛选。
- [x] 查询条件以 tag 形式展示。

### P0. 主表

- [x] 使用 `YTable` 实现批次列表。
- [x] 支持分页。
- [x] 支持多选。
- [x] 支持展开行。
- [x] 支持行操作。

建议列：

| 列名 | 字段 |
|---|---|
| 数据批次名称 | `batchName` |
| 产品代码 | `productCode` |
| 产品名称 | `productName` |
| 管理人 | `managerName` |
| 估值日期 | `valuationDate` |
| 文件名称 | `originalFileName` |
| 当前阶段 | `currentStageName` |
| 状态 | `statusName` |
| 进度 | `progress` |
| 开始时间 | `startedAt` |
| 耗时 | `durationText` |
| 异常原因 | `lastErrorMessage` |
| 操作 | 查看、执行、重跑、停止、展开 |

### P0. API 联调

- [x] 新增手写 API 封装：`packages/src/api/outsourcedDataTask.ts`。
- [x] 通过 `customInstance` 调用 `/outsourced-data-tasks`，不重复拼接 `/api`。
- [x] 页面 Hook 优先请求真实后端接口。
- [x] 后端不可用时保留样例数据兜底，保证页面可预览。
- [ ] 等后端 OpenAPI 文档刷新后，切换到 generated client 或确认保留手写 API。

### P0. 展开阶段明细

- [x] 展开行展示阶段执行明细表。
- [x] 阶段明细需要按业务阶段顺序排序。
- [x] 失败阶段高亮错误摘要。
- [x] 阶段操作支持查看日志、查看数据、重跑本阶段。

建议列：

| 列名 | 字段 |
|---|---|
| 阶段名称 | `stageName` |
| 任务开始时间 | `startedAt` |
| 执行耗时 | `durationText` |
| 执行次数 | `runNo` |
| 触发方式 | `triggerModeName` |
| 状态 | `statusName` |
| 错误摘要 | `errorMessage` |
| 操作 | 查看日志、查看数据、重跑 |

### P1. 详情抽屉

- [x] 点击批次名称或查看按钮打开详情抽屉。
- [x] 抽屉包含 4 个 Tab：
  - `链路概览`
  - `文件与数据`
  - `执行日志`
  - `人工处理`
- [x] 链路概览展示阶段状态、当前阻塞点、最近错误。
- [x] 文件与数据展示原始文件基础信息。
- [x] 文件与数据展示解析结果、STG/DWD/标准表入口。
- [x] 执行日志展示阶段日志和错误堆栈。
- [x] 人工处理展示异常确认、处理备注、重跑前置提示。

### P1. 操作确认与反馈

- [ ] 执行、重跑、停止前弹出确认框。
- [ ] 批量操作前展示已选数量和不可操作数量。
- [ ] 操作成功后刷新 summary 和列表。
- [ ] 操作失败展示后端返回的可读原因。
- [ ] 禁用不可执行操作，例如已完成任务不显示停止。

### P2. 实时刷新

- [ ] 如果后端提供 SSE，新增 `outsourcedDataTaskEventSse.ts`。
- [ ] 支持按批次、阶段、状态订阅事件。
- [ ] 列表页展示连接状态。
- [ ] 用户可暂停实时刷新。
- [x] SSE 不可用时回退为 10 秒轮询。

## 6. API 生成 TODO

- [ ] 后端 OpenAPI 增加上述 DTO 和接口。
- [ ] 前端执行 `pnpm generate:api`。
- [ ] 禁止手工修改 `packages/src/api/generated`。
- [ ] 在页面 Hook 中只引用生成后的 API 方法和类型。
- [ ] 对 `PageResult` 统一按 `data`、`total`、`pageIndex`、`pageSize` 解析。

## 7. 前端状态模型

建议页面 Hook 内部维护：

```ts
export type OutsourcedDataTaskQueryState = {
  businessDate: string;
  managerName: string;
  productKeyword: string;
  stage: string;
  status: string;
  sourceType: string;
  errorType: string;
};
```

```ts
export type OutsourcedDataTaskPage = {
  loading: boolean;
  summaryLoading: boolean;
  rows: OutsourcedDataTaskBatchRow[];
  total: number;
  query: OutsourcedDataTaskQueryState;
  selectedRowKeys: string[];
  detailVisible: boolean;
  selectedRow: OutsourcedDataTaskBatchRow | null;
  runQuery: () => void;
  resetQuery: () => void;
  selectStage: (stage: string) => void;
  selectStatus: (status: string) => void;
  openDetailDrawer: (row: OutsourcedDataTaskBatchRow) => void;
  closeDetailDrawer: () => void;
  executeBatch: (row: OutsourcedDataTaskBatchRow) => void;
  retryBatch: (row: OutsourcedDataTaskBatchRow) => void;
  stopBatch: (row: OutsourcedDataTaskBatchRow) => void;
  retryStep: (row: OutsourcedDataTaskStepRow) => void;
  batchExecute: () => void;
  batchRetry: () => void;
  batchStop: () => void;
};
```

## 8. 实施顺序

### 第一阶段：接口契约和静态页面

- [ ] 确认后端 DTO 字段。
- [ ] 确认阶段编码和状态编码。
- [x] 前端创建页面目录和路由。
- [x] 使用 mock 数据完成页面静态布局。
- [x] 对齐 Figma 视觉：统计卡、阶段条、筛选区、主表、详情阶段表。

交付标准：

- 页面可从菜单进入。
- 不依赖后端即可完整展示目标形态。
- 操作按钮先使用 mock loading 和消息反馈。

### 第二阶段：列表查询联调

- [x] 后端提供 summary 接口。
- [x] 后端提供分页查询接口。
- [x] 后端提供阶段明细接口。
- [x] 后端提供详情接口。
- [ ] 前端接入 summary 接口。
- [ ] 前端接入分页查询接口。
- [x] 前端接入阶段明细接口。
- [ ] 前端接入详情接口。
- [ ] 移除 mock 数据。

交付标准：

- 筛选、分页、阶段点击、状态点击均可刷新真实数据。
- 主表展开可以看到真实阶段明细。
- 异常原因来自后端可读字段。

### 第三阶段：操作闭环

- [ ] 接入执行接口。
- [ ] 接入重跑接口。
- [ ] 接入停止接口。
- [ ] 接入阶段重跑接口。
- [ ] 接入批量操作接口。

交付标准：

- 用户可以对失败批次重跑。
- 用户可以对失败阶段单独重跑。
- 用户可以停止运行中批次。
- 不可操作状态有明确提示。

### 第四阶段：详情和日志

- [ ] 完成详情抽屉。
- [ ] 接入日志分页接口。
- [ ] 接入文件和数据结果入口。
- [ ] 接入人工处理备注或异常确认能力。

交付标准：

- 用户能从批次定位到文件、阶段、日志、数据结果。
- 用户能看到失败的具体阶段和错误摘要。

### 第五阶段：实时刷新

- [x] 后端提供 SSE 或确认轮询策略。
- [ ] 前端接入事件订阅。
- [ ] 增加连接状态、暂停刷新、自动刷新。

交付标准：

- 运行中任务进度无需手动刷新即可更新。
- SSE 断开后有可见提示并可重连。

## 9. 验收清单

- [ ] 页面名称、菜单名称不再使用 transfer 语义。
- [ ] 页面只展示委外核心数据处理链路。
- [ ] 6 个阶段顺序和业务定义稳定。
- [x] 批次状态由后端聚合返回。
- [x] 当前阶段由后端返回，不由前端猜测。
- [ ] 失败原因有可读文案。
- [ ] 批量操作有确认和结果反馈。
- [ ] 详情抽屉可以定位文件、数据、日志。
- [ ] 分页、筛选、排序不会丢失查询条件。
- [ ] `packages/src/api/generated` 只由 Orval 生成。
- [ ] 页面在 1440 宽度下不出现文字重叠。
- [ ] 表格横向字段过多时使用横向滚动或列宽约束。

## 10. 风险点

- 当前已有 `ParseQueue`、`ParseLifecycle` 偏 transfer 解析队列语义，不能直接作为本页面的领域模型。
- `FullWorkflowResponse` 目前只覆盖上传、解析、匹配、提取等局部任务，不足以表达完整批次链路。
- 后续 TODO 数据加工任务尚未明确领域边界，需要先抽象为 `DATA_PROCESSING` 阶段，再逐步接具体任务。
- 如果后端不保存阶段明细，前端无法稳定展示展开表和重跑本阶段。
- 如果错误原因只存在原始 payload 中，前端异常筛选和可读提示会很弱，需要后端结构化错误字段。
