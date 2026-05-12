import { getJavaApi } from "./generated/valset";

export type OutsourcedDataTaskQueryParams = {
  batchId?: string;
  taskDate?: string;
  businessDate?: string;
  managerName?: string;
  productKeyword?: string;
  stage?: string;
  step?: string;
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
  productCode?: string;
  productName?: string;
  managerName?: string;
  fileId?: string;
  filesysFileId?: string;
  originalFileName?: string;
  sourceType?: string;
  currentStage?: string;
  currentStep?: string;
  currentStageName?: string;
  currentStepName?: string;
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
  step?: string;
  stageName?: string;
  stepName?: string;
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

export type OutsourcedDataTaskStageSummaryDTO = {
  stage?: string;
  step?: string;
  stageName?: string;
  stepName?: string;
  stageDescription?: string;
  stepDescription?: string;
  totalCount?: number;
  runningCount?: number;
  failedCount?: number;
  pendingCount?: number;
};

export type OutsourcedDataTaskSummaryDTO = {
  workflowCode?: string;
  workflowId?: string;
  versionNo?: number;
  totalCount?: number;
  runningCount?: number;
  successCount?: number;
  failedCount?: number;
  stageCatalog?: OutsourcedDataTaskStageSummaryDTO[];
  stepSummaries?: OutsourcedDataTaskStageSummaryDTO[];
};

export type OutsourcedDataTaskBatchDetailDTO = {
  batch?: OutsourcedDataTaskBatchDTO;
  steps?: OutsourcedDataTaskStepDTO[];
  currentBlockPoint?: string;
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

export type SingleResultOutsourcedDataTaskActionResultDTO = {
  data?: OutsourcedDataTaskActionResultDTO;
};

const generatedApi = getJavaApi();

export const getOutsourcedDataTaskSummary = (
  params?: OutsourcedDataTaskQueryParams,
) =>
  generatedApi.summary(params) as Promise<SingleResultOutsourcedDataTaskSummaryDTO>;

export const pageOutsourcedDataTasks = (
  params?: OutsourcedDataTaskQueryParams,
) =>
  generatedApi.pageTasks(params) as Promise<PageResultOutsourcedDataTaskBatchDTO>;

export const getOutsourcedDataTask = (batchId: string) =>
  generatedApi.getTask(batchId) as Promise<SingleResultOutsourcedDataTaskBatchDetailDTO>;

export const listOutsourcedDataTaskSteps = (batchId: string) =>
  generatedApi.listSteps(batchId) as Promise<MultiResultOutsourcedDataTaskStepDTO>;

export const executeOutsourcedDataTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.execute(batchId, command ?? {}) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const retryOutsourcedDataTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.retry1(batchId, command ?? {}) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const stopOutsourcedDataTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.stop1(batchId, command ?? {}) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const retryOutsourcedDataTaskStep = (
  batchId: string,
  stepId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.retryStep(
    batchId,
    stepId,
    command ?? {},
  ) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const batchExecuteOutsourcedDataTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  generatedApi.batchExecute(command) as Promise<MultiResultOutsourcedDataTaskActionResultDTO>;

export const batchRetryOutsourcedDataTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  generatedApi.batchRetry(command) as Promise<MultiResultOutsourcedDataTaskActionResultDTO>;

export const batchStopOutsourcedDataTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  generatedApi.batchStop(command) as Promise<MultiResultOutsourcedDataTaskActionResultDTO>;
