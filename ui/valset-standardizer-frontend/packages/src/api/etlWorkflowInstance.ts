import { getJavaApi } from "./generated/valset";
import { customInstance } from "./mutator";
import type {
  CountWorkflowStateParams,
  ListInstancesParams,
  PageResultWorkflowInstanceViewDTO,
  WorkflowCallbackRequest as GeneratedWorkflowCallbackRequest,
  WorkflowInstanceDTO as GeneratedWorkflowInstanceDTO,
  WorkflowInstanceViewDTO,
  WorkflowPauseRequest as GeneratedWorkflowPauseRequest,
  WorkflowResumeRequest as GeneratedWorkflowResumeRequest,
  WorkflowRetryRequest as GeneratedWorkflowRetryRequest,
  WorkflowStageLogDTO as GeneratedWorkflowStageLogDTO,
  WorkflowStopRequest as GeneratedWorkflowStopRequest,
  WorkflowTaskInstanceDTO,
  WorkflowTaskListDTO,
  WorkflowTriggerRequest as GeneratedWorkflowTriggerRequest,
} from "./generated/valset/schemas";

const generatedApi = getJavaApi();

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
export type WorkflowStageLogDTO = Omit<GeneratedWorkflowStageLogDTO, "payload"> & {
  payload?: Record<string, unknown>;
};
export type WorkflowInstanceDTO = Omit<
  GeneratedWorkflowInstanceDTO,
  "context" | "stageLogs"
> & {
  context?: Record<string, unknown>;
  stageLogs?: WorkflowStageLogDTO[];
  workflowName?: string;
  currentStageName?: string;
  stageCount?: number;
};
export type WorkflowInstanceQueryParams = ListInstancesParams;
export type WorkflowInstanceStateCountQueryParams = CountWorkflowStateParams & {
  startDate: string;
  endDate: string;
  projectCode: number;
};
export type WorkflowInstanceStateCountDTO = {
  state?: string;
  count?: number | string;
};
export type WorkflowInstanceStateCountPageDTO = {
  totalCount?: number;
  workflowInstanceStatusCounts?: WorkflowInstanceStateCountDTO[];
};
export type WorkflowTriggerMode =
  | "START_PROCESS"
  | "START_FAILURE_TASK_PROCESS"
  | "START_SUSPEND_TASK_PROCESS";
export type WorkflowTriggerRequest = Omit<
  GeneratedWorkflowTriggerRequest,
  "context"
> & {
  context?: Record<string, unknown>;
};
export type WorkflowStopRequest = Omit<GeneratedWorkflowStopRequest, "context"> & {
  context?: Record<string, unknown>;
};
export type WorkflowPauseRequest = Omit<GeneratedWorkflowPauseRequest, "context"> & {
  context?: Record<string, unknown>;
};
export type WorkflowResumeRequest = Omit<
  GeneratedWorkflowResumeRequest,
  "context"
> & {
  context?: Record<string, unknown>;
};
export type WorkflowRetryRequest = Omit<GeneratedWorkflowRetryRequest, "context"> & {
  context?: Record<string, unknown>;
};
export type WorkflowCallbackRequest = Omit<
  GeneratedWorkflowCallbackRequest,
  "payload"
> & {
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

export type {
  PageResultWorkflowInstanceViewDTO,
  WorkflowInstanceViewDTO,
  WorkflowTaskInstanceDTO,
  WorkflowTaskListDTO,
};

export const listEtlWorkflowInstances = (params?: WorkflowInstanceQueryParams) =>
  generatedApi.listInstances(
    params,
  ) as Promise<WorkflowPageResult<WorkflowInstanceViewDTO>>;

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
  generatedApi.getInstance(
    instanceId,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

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
    request as GeneratedWorkflowTriggerRequest,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const stopEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowStopRequest,
) =>
  generatedApi.stop(
    instanceId,
    (request ?? {}) as GeneratedWorkflowStopRequest,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const pauseEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowPauseRequest,
) =>
  generatedApi.pause(
    instanceId,
    (request ?? {}) as GeneratedWorkflowPauseRequest,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const resumeEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowResumeRequest,
) =>
  generatedApi.resume(
    instanceId,
    (request ?? {}) as GeneratedWorkflowResumeRequest,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const retryEtlWorkflowInstance = (
  instanceId: string,
  request?: WorkflowRetryRequest,
) =>
  generatedApi.retry(
    instanceId,
    (request ?? {}) as GeneratedWorkflowRetryRequest,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;

export const callbackEtlWorkflowInstance = (
  instanceId: string,
  request: WorkflowCallbackRequest,
) =>
  generatedApi.callback(
    instanceId,
    request as GeneratedWorkflowCallbackRequest,
  ) as Promise<WorkflowSingleResult<WorkflowInstanceDTO>>;
