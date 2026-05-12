import { getJavaApi } from "./generated/valset";
import { customInstance } from "./mutator";
import type { EtlPlatformType } from "./etlWorkflowConfig";

export type WorkflowInstanceStatus =
  | "DRAFT"
  | "READY"
  | "SUBMITTED"
  | "RUNNING"
  | "SUCCEEDED"
  | "FAILED"
  | "STOPPED"
  | "RETRYING"
  | "UNKNOWN";

export type WorkflowStageLogStatus = WorkflowInstanceStatus | string;

export type WorkflowInstanceViewDTO = {
  instanceId?: string;
  workflowCode?: string;
  workflowName?: string;
  workflowVersionNo?: number;
  platformType?: EtlPlatformType;
  businessKey?: string;
  externalInstanceId?: string;
  externalWorkflowId?: string;
  status?: WorkflowInstanceStatus;
  rawStatus?: string;
  currentStageCode?: string;
  currentStageName?: string;
  triggerTime?: string;
  startTime?: string;
  duration?: string;
  endTime?: string;
  message?: string;
  stageCount?: number;
};

export type WorkflowInstanceStateCountDTO = {
  state?: string;
  count?: number | string;
};

export type WorkflowInstanceStateCountPageDTO = {
  totalCount?: number;
  workflowInstanceStatusCounts?: WorkflowInstanceStateCountDTO[];
};

export type WorkflowStageLogDTO = {
  instanceId?: string;
  workflowCode?: string;
  workflowVersionNo?: number;
  stageCode?: string;
  stageName?: string;
  stageOrder?: number;
  status?: WorkflowStageLogStatus;
  rawStatus?: string;
  message?: string;
  startTime?: string;
  endTime?: string;
  payload?: Record<string, unknown>;
};

export type WorkflowTaskInstanceDTO = {
  id?: number;
  name?: string;
  taskType?: string;
  workflowInstanceId?: string;
  workflowInstanceName?: string;
  projectCode?: number;
  taskCode?: number;
  taskDefinitionVersion?: number;
  processDefinitionName?: string;
  taskGroupPriority?: number;
  state?: string;
  firstSubmitTime?: string;
  submitTime?: string;
  startTime?: string;
  endTime?: string;
  host?: string;
  executePath?: string;
  retryTimes?: number;
  alertFlag?: string;
  workflowInstance?: Record<string, unknown>;
  workflowDefinition?: Record<string, unknown>;
  taskDefine?: Record<string, unknown>;
  pid?: number;
  appLink?: string;
  flag?: string;
  duration?: number;
  maxRetryTimes?: number;
  retryInterval?: number;
  taskInstancePriority?: string;
  workflowInstancePriority?: string;
  workerGroup?: string;
  environmentCode?: number;
  environmentConfig?: Record<string, unknown>;
  executorId?: number;
  varPool?: Record<string, unknown>;
  executorName?: string;
  delayTime?: number;
  taskParams?: string;
  dryRun?: number;
  taskGroupId?: number;
  cpuQuota?: number;
  memoryMax?: number;
  taskExecuteType?: string;
  taskInstanceDependentResults?: Record<string, unknown>;
};

export type WorkflowTaskListDTO = {
  workflowInstanceState?: string;
  taskList?: WorkflowTaskInstanceDTO[];
};

export type WorkflowInstanceDTO = WorkflowInstanceViewDTO & {
  context?: Record<string, unknown>;
  stageLogs?: WorkflowStageLogDTO[];
};

export type WorkflowInstanceQueryParams = {
  workflowCode?: string;
  workflowVersionNo?: number;
  platformType?: EtlPlatformType;
  workflowName?: string;
  status?: string;
  businessKey?: string;
  instanceId?: string;
  externalInstanceId?: string;
  stageCode?: string;
  triggerTimeFrom?: string;
  triggerTimeTo?: string;
  pageIndex?: number;
  pageSize?: number;
};

export type WorkflowInstanceStateCountQueryParams = {
  startDate: string;
  endDate: string;
  projectCode: string | number;
};

export type WorkflowTriggerRequest = {
  workflowCode: string;
  workflowVersionNo: number;
  businessKey?: string;
  stageCode?: string;
  context?: Record<string, unknown>;
  force?: boolean;
  triggerMode?: WorkflowTriggerMode;
};

export type WorkflowTriggerMode =
  | "START_PROCESS"
  | "START_FAILURE_TASK_PROCESS"
  | "START_SUSPEND_TASK_PROCESS";

export type WorkflowStopRequest = {
  reason?: string;
  context?: Record<string, unknown>;
};

export type WorkflowPauseRequest = {
  reason?: string;
  context?: Record<string, unknown>;
};

export type WorkflowResumeRequest = {
  reason?: string;
  context?: Record<string, unknown>;
};

export type WorkflowRetryRequest = {
  stageCode?: string;
  context?: Record<string, unknown>;
};

export type WorkflowCallbackRequest = {
  stageCode?: string;
  rawStatus?: string;
  message?: string;
  payload?: Record<string, unknown>;
};

type WorkflowSingleResult<T> = {
  data?: T;
  success?: boolean;
  message?: string;
};

type WorkflowMultiResult<T> = {
  data?: T[];
  success?: boolean;
  message?: string;
};

type WorkflowPageResult<T> = {
  data?: T[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

const generatedApi = getJavaApi();

export const listEtlWorkflowInstances = (params?: WorkflowInstanceQueryParams) =>
  generatedApi.listInstances(params) as Promise<WorkflowPageResult<WorkflowInstanceViewDTO>>;

export const getEtlWorkflowInstanceStateCount = (
  params: WorkflowInstanceStateCountQueryParams,
) =>
  generatedApi.countWorkflowState(params) as Promise<{
    code?: number;
    msg?: string;
    data?: WorkflowInstanceStateCountPageDTO;
    failed?: boolean;
    success?: boolean;
  }>;

export const getEtlWorkflowInstance = (instanceId: string) =>
  generatedApi.getInstance(instanceId) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const listEtlWorkflowInstanceLogs = (
  instanceId: string,
  stageCode?: string,
) =>
  customInstance<WorkflowMultiResult<WorkflowStageLogDTO>>({
    url: `/etl/workflows/instances/${encodeURIComponent(instanceId)}/logs`,
    method: "GET",
    params: stageCode ? { stageCode } : undefined,
  });

export const listEtlWorkflowInstanceTasks = (instanceId: string) =>
  generatedApi.listTasks(
    instanceId,
  ) as Promise<WorkflowSingleResult<WorkflowTaskListDTO>>;

export const getEtlWorkflowInstanceTaskLog = (
  instanceId: string,
  taskInstanceId: number | string,
) =>
  generatedApi.getTaskLog(
    instanceId,
    Number(taskInstanceId),
  ) as Promise<WorkflowSingleResult<string>>;

export const triggerEtlWorkflowInstance = (request: WorkflowTriggerRequest) =>
  generatedApi.trigger(
    request,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const stopEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowStopRequest,
) =>
  generatedApi.stop(
    instanceId,
    request ?? {},
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const pauseEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowPauseRequest,
) =>
  generatedApi.pause(
    instanceId,
    request ?? {},
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const resumeEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowResumeRequest,
) =>
  generatedApi.resume(
    instanceId,
    request ?? {},
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const retryEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowRetryRequest,
) =>
  generatedApi.retry(
    instanceId,
    request ?? {},
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const callbackEtlWorkflowInstance = (
  instanceId: string,
  request: WorkflowCallbackRequest,
) =>
  generatedApi.callback(
    instanceId,
    request,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;
