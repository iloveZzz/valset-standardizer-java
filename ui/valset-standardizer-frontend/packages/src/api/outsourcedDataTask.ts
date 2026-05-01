import { customInstance } from "./mutator";

export type OutsourcedDataTaskQueryParams = {
  businessDate?: string;
  managerName?: string;
  productKeyword?: string;
  stage?: string;
  status?: string;
  sourceType?: string;
  errorType?: string;
  pageIndex?: number;
  pageSize?: number;
};

export type OutsourcedDataTaskBatchDTO = {
  batchId?: string;
  batchName?: string;
  businessDate?: string;
  valuationDate?: string;
  productCode?: string;
  productName?: string;
  managerName?: string;
  fileId?: string;
  filesysFileId?: string;
  originalFileName?: string;
  sourceType?: string;
  currentStage?: string;
  currentStageName?: string;
  status?: string;
  statusName?: string;
  progress?: number;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  durationText?: string;
  lastErrorCode?: string;
  lastErrorMessage?: string;
};

export type OutsourcedDataTaskStepDTO = {
  stepId?: string;
  batchId?: string;
  stage?: string;
  stageName?: string;
  taskId?: string;
  taskType?: string;
  runNo?: number;
  triggerMode?: string;
  triggerModeName?: string;
  status?: string;
  statusName?: string;
  progress?: number;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  durationText?: string;
  inputSummary?: string;
  outputSummary?: string;
  errorCode?: string;
  errorMessage?: string;
  errorStack?: string;
  logRef?: string;
};

export type OutsourcedDataTaskLogDTO = {
  logId?: string;
  batchId?: string;
  stepId?: string;
  stage?: string;
  logLevel?: string;
  message?: string;
  occurredAt?: string;
};

export type OutsourcedDataTaskStageSummaryDTO = {
  stage?: string;
  stageName?: string;
  stageDescription?: string;
  totalCount?: number;
  runningCount?: number;
  failedCount?: number;
  pendingCount?: number;
};

export type OutsourcedDataTaskSummaryDTO = {
  totalCount?: number;
  runningCount?: number;
  successCount?: number;
  failedCount?: number;
  stageSummaries?: OutsourcedDataTaskStageSummaryDTO[];
};

export type OutsourcedDataTaskBatchDetailDTO = {
  batch?: OutsourcedDataTaskBatchDTO;
  steps?: OutsourcedDataTaskStepDTO[];
  currentBlockPoint?: string;
  fileResultUrl?: string;
  rawDataUrl?: string;
  stgDataUrl?: string;
  dwdDataUrl?: string;
  standardDataUrl?: string;
};

export type OutsourcedDataTaskActionCommand = {
  reason?: string;
  operator?: string;
};

export type OutsourcedDataTaskBatchCommand = {
  batchIds: string[];
  reason?: string;
};

export type OutsourcedDataTaskActionResultDTO = {
  batchId?: string;
  stepId?: string;
  accepted?: boolean;
  action?: string;
  message?: string;
};

export type PageResultOutsourcedDataTaskBatchDTO = {
  data?: OutsourcedDataTaskBatchDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type SingleResultOutsourcedDataTaskSummaryDTO = {
  data?: OutsourcedDataTaskSummaryDTO;
};

export type SingleResultOutsourcedDataTaskBatchDetailDTO = {
  data?: OutsourcedDataTaskBatchDetailDTO;
};

export type MultiResultOutsourcedDataTaskActionResultDTO = {
  data?: OutsourcedDataTaskActionResultDTO[];
};

export type MultiResultOutsourcedDataTaskStepDTO = {
  data?: OutsourcedDataTaskStepDTO[];
};

export type PageResultOutsourcedDataTaskLogDTO = {
  data?: OutsourcedDataTaskLogDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type SingleResultOutsourcedDataTaskActionResultDTO = {
  data?: OutsourcedDataTaskActionResultDTO;
};

export const getOutsourcedDataTaskSummary = (
  params?: OutsourcedDataTaskQueryParams,
) =>
  customInstance<SingleResultOutsourcedDataTaskSummaryDTO>({
    url: "/outsourced-data-tasks/summary",
    method: "GET",
    params,
  });

export const pageOutsourcedDataTasks = (
  params?: OutsourcedDataTaskQueryParams,
) =>
  customInstance<PageResultOutsourcedDataTaskBatchDTO>({
    url: "/outsourced-data-tasks",
    method: "GET",
    params,
  });

export const getOutsourcedDataTask = (batchId: string) =>
  customInstance<SingleResultOutsourcedDataTaskBatchDetailDTO>({
    url: `/outsourced-data-tasks/${batchId}`,
    method: "GET",
  });

export const listOutsourcedDataTaskSteps = (batchId: string) =>
  customInstance<MultiResultOutsourcedDataTaskStepDTO>({
    url: `/outsourced-data-tasks/${batchId}/steps`,
    method: "GET",
  });

export const pageOutsourcedDataTaskLogs = (
  batchId: string,
  params?: {
    stage?: string;
    pageIndex?: number;
    pageSize?: number;
  },
) =>
  customInstance<PageResultOutsourcedDataTaskLogDTO>({
    url: `/outsourced-data-tasks/${batchId}/logs`,
    method: "GET",
    params,
  });

export const executeOutsourcedDataTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  customInstance<SingleResultOutsourcedDataTaskActionResultDTO>({
    url: `/outsourced-data-tasks/${batchId}/execute`,
    method: "POST",
    data: command ?? {},
  });

export const retryOutsourcedDataTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  customInstance<SingleResultOutsourcedDataTaskActionResultDTO>({
    url: `/outsourced-data-tasks/${batchId}/retry`,
    method: "POST",
    data: command ?? {},
  });

export const stopOutsourcedDataTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  customInstance<SingleResultOutsourcedDataTaskActionResultDTO>({
    url: `/outsourced-data-tasks/${batchId}/stop`,
    method: "POST",
    data: command ?? {},
  });

export const retryOutsourcedDataTaskStep = (
  batchId: string,
  stepId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  customInstance<SingleResultOutsourcedDataTaskActionResultDTO>({
    url: `/outsourced-data-tasks/${batchId}/steps/${stepId}/retry`,
    method: "POST",
    data: command ?? {},
  });

export const batchExecuteOutsourcedDataTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  customInstance<MultiResultOutsourcedDataTaskActionResultDTO>({
    url: "/outsourced-data-tasks/batch-execute",
    method: "POST",
    data: command,
  });

export const batchRetryOutsourcedDataTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  customInstance<MultiResultOutsourcedDataTaskActionResultDTO>({
    url: "/outsourced-data-tasks/batch-retry",
    method: "POST",
    data: command,
  });

export const batchStopOutsourcedDataTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  customInstance<MultiResultOutsourcedDataTaskActionResultDTO>({
    url: "/outsourced-data-tasks/batch-stop",
    method: "POST",
    data: command,
  });
