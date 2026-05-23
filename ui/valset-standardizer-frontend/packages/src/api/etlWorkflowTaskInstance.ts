import { getJavaApi } from "./generated/valset";
import type {
  CountTaskStateParams,
  ForceTaskSuccessParams,
  GetTaskLog1Params,
  ListTaskInstancesParams,
  WorkflowTaskInstanceDTO,
  WorkflowTaskInstancePageDTO,
} from "./generated/valset/schemas";

const generatedApi = getJavaApi();

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
export type WorkflowTaskInstanceQueryParams = ListTaskInstancesParams;
export type WorkflowTaskInstanceLogQueryParams = GetTaskLog1Params;
export type WorkflowTaskInstanceStateCountQueryParams = CountTaskStateParams & {
  startDate: string;
  endDate: string;
  projectCode: number;
};
export type WorkflowTaskInstanceStateCountDTO = {
  state?: WorkflowTaskInstanceStatus;
  count?: number | string;
};
export type WorkflowTaskInstanceStateCountPageDTO = {
  totalCount?: number;
  taskInstanceStatusCounts?: WorkflowTaskInstanceStateCountDTO[];
};
export type SingleResult<T> = {
  data?: T;
  success?: boolean;
  message?: string;
};

export type {
  ForceTaskSuccessParams,
  WorkflowTaskInstanceDTO,
  WorkflowTaskInstancePageDTO,
};

export const listEtlWorkflowTaskInstances = (
  params?: WorkflowTaskInstanceQueryParams,
) =>
  generatedApi.listTaskInstances(
    params,
  ) as Promise<SingleResult<WorkflowTaskInstancePageDTO>>;

export const forceSuccessEtlWorkflowTaskInstance = (
  taskInstanceId: number | string,
  params: WorkflowTaskInstanceLogQueryParams,
) =>
  generatedApi.forceTaskSuccess(
    Number(taskInstanceId),
    {
      workflowCode: params.workflowCode,
      workflowVersionNo: params.workflowVersionNo,
    } satisfies ForceTaskSuccessParams,
  ) as Promise<SingleResult<boolean>>;

export const getEtlWorkflowTaskInstanceLog = (
  taskInstanceId: number | string,
  params: WorkflowTaskInstanceLogQueryParams,
) =>
  generatedApi.getTaskLog1(
    Number(taskInstanceId),
    {
      workflowCode: params.workflowCode,
      workflowVersionNo: params.workflowVersionNo,
      skipLineNum: params.skipLineNum ?? 0,
      limit: params.limit ?? 1000,
    },
  ) as Promise<SingleResult<string>>;

export const getEtlWorkflowTaskInstanceStateCount = (
  params: WorkflowTaskInstanceStateCountQueryParams,
) =>
  generatedApi.countTaskState(params) as Promise<{
    code?: number;
    msg?: string;
    data?: WorkflowTaskInstanceStateCountPageDTO;
    failed?: boolean;
    success?: boolean;
  }>;
