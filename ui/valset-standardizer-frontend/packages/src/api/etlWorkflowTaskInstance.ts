import { customInstance } from "./mutator";

export type WorkflowTaskInstanceStatus =
  | "SUBMITTED_SUCCESS"
  | "RUNNING_EXECUTION"
  | "PAUSE"
  | "FAILURE"
  | "SUCCESS"
  | "NEED_FAULT_TOLERANCE"
  | "KILL"
  | "DELAY_EXECUTION"
  | "FORCED_SUCCESS"
  | "DISPATCH"
  | string;

export type WorkflowTaskInstanceDTO = {
  id?: number | null;
  name?: string;
  taskType?: string;
  workflowInstanceId?: string;
  workflowInstanceName?: string;
  projectCode?: number | null;
  taskCode?: number | null;
  taskDefinitionVersion?: number | null;
  processDefinitionName?: string;
  taskGroupPriority?: number | null;
  state?: WorkflowTaskInstanceStatus;
  firstSubmitTime?: string;
  submitTime?: string;
  startTime?: string;
  endTime?: string;
  host?: string;
  executePath?: string;
  logPath?: string;
  retryTimes?: number | null;
  alertFlag?: string;
  pid?: number | null;
  appLink?: string;
  flag?: string;
  duration?: number | null;
  maxRetryTimes?: number | null;
  retryInterval?: number | null;
  taskInstancePriority?: string;
  workflowInstancePriority?: string;
  workerGroup?: string;
  environmentCode?: number | null;
  executorId?: number | null;
  executorName?: string;
  delayTime?: number | null;
  taskParams?: string;
  dryRun?: number | null;
  taskGroupId?: number | null;
  cpuQuota?: number | null;
  memoryMax?: number | null;
  taskExecuteType?: string;
};

export type WorkflowTaskInstancePageDTO = {
  workflowInstanceState?: string;
  taskList?: WorkflowTaskInstanceDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type WorkflowTaskInstanceStateCountDTO = {
  state?: WorkflowTaskInstanceStatus;
  count?: number | string;
};

export type WorkflowTaskInstanceStateCountPageDTO = {
  totalCount?: number;
  taskInstanceStatusCounts?: WorkflowTaskInstanceStateCountDTO[];
};

export type WorkflowTaskInstanceQueryParams = {
  workflowCode?: string;
  workflowVersionNo?: number;
  taskName?: string;
  workflowInstanceName?: string;
  status?: string;
  startTimeFrom?: string;
  endTimeTo?: string;
  pageIndex?: number;
  pageSize?: number;
};

export type WorkflowTaskInstanceLogQueryParams = {
  workflowCode: string;
  workflowVersionNo: number;
  skipLineNum?: number;
  limit?: number;
};

export type WorkflowTaskInstanceStateCountQueryParams = {
  startDate: string;
  endDate: string;
  projectCode: string | number;
};

export type SingleResult<T> = {
  data?: T;
  success?: boolean;
  message?: string;
};

export const listEtlWorkflowTaskInstances = (
  params?: WorkflowTaskInstanceQueryParams,
) =>
  customInstance<SingleResult<WorkflowTaskInstancePageDTO>>({
    url: "/etl/workflows/task-instances",
    method: "GET",
    params,
  });

export const forceSuccessEtlWorkflowTaskInstance = (
  taskInstanceId: number | string,
  params: WorkflowTaskInstanceLogQueryParams,
) =>
  customInstance<SingleResult<boolean>>({
    url: `/etl/workflows/task-instances/${encodeURIComponent(String(taskInstanceId))}/force-success`,
    method: "POST",
    params: {
      workflowCode: params.workflowCode,
      workflowVersionNo: params.workflowVersionNo,
    },
  });

export const getEtlWorkflowTaskInstanceLog = (
  taskInstanceId: number | string,
  params: WorkflowTaskInstanceLogQueryParams,
) =>
  customInstance<SingleResult<string>>({
    url: `/etl/workflows/task-instances/${encodeURIComponent(String(taskInstanceId))}/log`,
    method: "GET",
    params: {
      workflowCode: params.workflowCode,
      workflowVersionNo: params.workflowVersionNo,
      skipLineNum: params.skipLineNum ?? 0,
      limit: params.limit ?? 1000,
    },
  });

export const getEtlWorkflowTaskInstanceStateCount = (
  params: WorkflowTaskInstanceStateCountQueryParams,
) =>
  customInstance<{
    code?: number;
    msg?: string;
    data?: WorkflowTaskInstanceStateCountPageDTO;
    failed?: boolean;
    success?: boolean;
  }>({
    url: "/dolphinscheduler/projects/analysis/task-state-count",
    method: "GET",
    params,
  });
