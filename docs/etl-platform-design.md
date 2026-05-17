# 通用 ETL 平台设计

本文档说明 `valset-standardizer` 中新增的通用 ETL 平台边界。它与估值内部工作流分离，用于对接外部数据任务处理平台和调度器。

## 目标

- 对外提供统一的工作流定义、实例、阶段日志和回调 API
- 统一 批量任务、DolphinScheduler、XXL-JOB 的参数映射和状态归一化
- 让底层执行平台可替换，但上层查询和日志协议保持稳定

## 模块

- `valset-standardizer-workflow`
  - `taskflow-adapter`：通用 DTO、状态、日志、控制接口和数据库运行态
  - `taskflow-springbatch`：批量任务 适配
  - `taskflow-dolohinscheduler`：DolphinScheduler 适配
  - `taskflow-xxljob`：XXL-JOB 适配

## 核心模型

- `WorkflowDefinitionDTO`：工作流定义
- `WorkflowStageDTO`：阶段定义
- `WorkflowInstanceDTO`：实例状态
- `WorkflowStageLogDTO`：阶段日志
- `WorkflowEngineBindingDTO`：底层平台绑定信息
- `WorkflowStatus`：统一状态枚举

## 接口

统一前缀：`/api/etl/workflows`

- `POST /api/etl/workflows`：保存工作流定义
- `GET /api/etl/workflows`：查询工作流定义列表
- `GET /api/etl/workflows/{workflowCode}/{workflowVersionNo}`：查询单个版本
- `POST /api/etl/workflows/instances/trigger`：触发任务实例
- `POST /api/etl/workflows/instances/{instanceId}/stop`：停止实例
- `POST /api/etl/workflows/instances/{instanceId}/retry`：重试实例
- `GET /api/etl/workflows/instances/{instanceId}`：查询实例
- `GET /api/etl/workflows/instances/{instanceId}/logs`：查询阶段日志
- `POST /api/etl/workflows/instances/{instanceId}/callbacks`：回调阶段状态

## 与估值内部工作流的关系

- 估值内部工作流仍由 `WorkflowEngineAdapter`、`WorkflowEngineDispatchService` 和 `tools/task` 负责
- 通用 ETL 平台只负责外部调度器对接，不参与估值 parse-task 的内部生命周期
- 两套模型共存，但不共享控制面命名，避免语义混淆

## 当前实现状态

- 已落地统一 DTO、状态映射、数据库运行态和通用控制器
- 已提供 批量任务、DolphinScheduler、XXL-JOB 三个适配器骨架
- 已补齐数据库表、Repository 和运行态回读逻辑
