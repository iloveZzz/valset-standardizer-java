# 估值内部工作流引擎适配设计

## 目标

`WorkflowEngineAdapter` 仅服务于委外估值系统内部的文件解析、结构标准化、科目匹配和结果落库流程。

它的职责是把“估值内部任务如何触发、如何重试、如何查询状态”收敛成统一接口，而不是抽象通用 ETL 平台。

## 设计边界

### 保留的职责

- 触发内部估值任务
- 重试内部估值任务
- 查询内部估值任务状态
- 按估值阶段路由到对应执行器

### 不承担的职责

- 通用数据 ETL 编排
- DolphinScheduler 适配
- XXL-JOB 适配
- Spring Batch 平台适配
- 任何与估值主链路无关的外部工作流抽象

## 运行模型

### 1. 工作流上下文

`WorkflowExecutionContextDTO` 继续承载以下信息：

- `workflowCode`
- `workflowId`
- `workflowVersionNo`
- `workflowStageCode`
- `workflowStageName`
- `workflowStageDescription`
- `engineType`
- `externalRef`
- `configJson`
- `bindingId`
- `bindingResolved`

其中 `engineType` 目前只允许 `INTERNAL`。

### 2. 适配器

`WorkflowEngineAdapter` 只保留内部实现：

- `InternalWorkflowEngineAdapter`

该实现直接把内部任务转交给现有调度服务，并回写任务状态。

### 3. 分发服务

`DefaultWorkflowEngineDispatchService` 的策略是：

1. 解析上下文。
2. 如果 `engineType != INTERNAL`，直接回退到内部调度。
3. 如果找到内部适配器，则调用适配器执行 `trigger` / `retry` / `query`。

## 相关模块

- `tools/task`：内部流程适配、分发和状态查询
- `yss-valset-standardizer`：任务创建与应用编排
- `yss-valset-standardizer`：上下文 DTO、任务模型和整合应用入口

## 维护原则

- 新增外部调度平台时，不要把实现挂到 `WorkflowEngineAdapter` 上。
- 如果后续要支持通用 ETL，请新建独立抽象和独立模块。
- 估值内部流程的枚举、日志和接口命名都要优先体现“内部估值”语义。
