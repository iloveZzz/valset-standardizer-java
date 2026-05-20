import { getJavaApi } from "./generated/valset";
import { customInstance } from "./mutator";

export type OutsourcedDataTaskQueryParams = {
  batchId?: string;
  taskDate?: string;
  businessDate?: string;
  managerName?: string;
  productKeyword?: string;
  taskStage?: string;
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
  sourceTypeName?: string;
  taskStage?: string;
  taskStageName?: string;
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
};

export type OutsourcedDataTaskTraceRecordDTO = {
  id?: string;
  type?: string;
  name?: string;
  status?: string;
  statusName?: string;
  upstreamId?: string;
  downstreamId?: string;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  errorCode?: string;
  errorMessage?: string;
  inputSummary?: string;
  outputSummary?: string;
  logRef?: string;
  attributes?: Record<string, unknown>;
};

export type OutsourcedDataTaskTraceLogDTO = {
  logId?: string;
  nodeType?: string;
  nodeId?: string;
  level?: string;
  message?: string;
  loggedAt?: string;
  logRef?: string;
};

export type OutsourcedDataTaskTraceResultSummaryDTO = {
  status?: string;
  statusName?: string;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  inputSummary?: string;
  outputSummary?: string;
  errorCode?: string;
  errorMessage?: string;
};

export type OutsourcedDataTaskTraceDTO = {
  batch?: OutsourcedDataTaskBatchDTO;
  parseQueue?: OutsourcedDataTaskTraceRecordDTO;
  transferObject?: OutsourcedDataTaskTraceRecordDTO;
  jobExecution?: OutsourcedDataTaskTraceRecordDTO;
  stepExecutions?: OutsourcedDataTaskTraceRecordDTO[];
  taskSteps?: OutsourcedDataTaskStepDTO[];
  logs?: OutsourcedDataTaskTraceLogDTO[];
  resultSummary?: OutsourcedDataTaskTraceResultSummaryDTO;
};

export type OutsourcedDataTaskActionCommand = {
  reason?: string;
  operator?: string;
};

export type OutsourcedDataTaskBatchCommand = {
  batchIds: string[];
  reason?: string;
};

export type OutsourcedDataTaskStandardDataExportCommand = {
  tab?: string;
  sheetName?: string;
  workbookData?: Record<string, unknown>;
};

export type OutsourcedDataTaskRawWorkbookDTO = {
  batchId?: string;
  fileId?: number;
  fileName?: string;
  sourceType?: string;
  sheetCount?: number;
  rowCount?: number;
  workbookData?: Record<string, unknown>;
  downloadedFromTarget?: boolean;
  fallbackMessage?: string;
};

export type OutsourcedDataTaskActionResultDTO = {
  batchId?: string;
  stepId?: string;
  accepted?: boolean;
  action?: string;
  message?: string;
};

export type OutsourcedDataTaskStandardBasicRowDTO = {
  category?: string;
  fieldName?: string;
  fieldValue?: string;
};

export type OutsourcedDataTaskStandardRawColumnDTO = {
  fieldKey?: string;
  title?: string;
  columnIndex?: number;
};

export type OutsourcedDataTaskStandardBasicDTO = {
  batchId?: string;
  valuationId?: number;
  fileId?: number;
  taskId?: number;
  workbookPath?: string;
  sheetName?: string;
  title?: string;
  headerRowNumber?: number;
  dataStartRowNumber?: number;
  basicInfoCount?: number;
  subjectCount?: number;
  metricCount?: number;
  basicRows?: OutsourcedDataTaskStandardBasicRowDTO[];
  rawColumns?: OutsourcedDataTaskStandardRawColumnDTO[];
};

export type OutsourcedDataTaskStandardSubjectDTO = {
  id?: number;
  valuationId?: number;
  sheetName?: string;
  rowDataNumber?: number;
  subjectCode?: string;
  subjectName?: string;
  levelNo?: number;
  parentCode?: string;
  rootCode?: string;
  leaf?: boolean;
  rawValuesJson?: string;
  rawValues?: Record<string, string>;
};

export type OutsourcedDataTaskStandardMetricDTO = {
  id?: number;
  valuationId?: number;
  sheetName?: string;
  rowDataNumber?: number;
  metricName?: string;
  metricType?: string;
  metricValue?: string;
  rawValuesJson?: string;
  rawValues?: Record<string, string>;
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

export type SingleResultOutsourcedDataTaskStandardBasicDTO = {
  data?: OutsourcedDataTaskStandardBasicDTO;
};

export type SingleResultOutsourcedDataTaskBatchDetailDTO = {
  data?: OutsourcedDataTaskBatchDetailDTO;
};

export type SingleResultOutsourcedDataTaskTraceDTO = {
  data?: OutsourcedDataTaskTraceDTO;
};

export type SingleResultOutsourcedDataTaskRawWorkbookDTO = {
  data?: OutsourcedDataTaskRawWorkbookDTO;
};

export type MultiResultOutsourcedDataTaskActionResultDTO = {
  data?: OutsourcedDataTaskActionResultDTO[];
};

export type MultiResultOutsourcedDataTaskStepDTO = {
  data?: OutsourcedDataTaskStepDTO[];
};

export type MultiResultOutsourcedDataTaskStandardSubjectDTO = {
  data?: OutsourcedDataTaskStandardSubjectDTO[];
};

export type MultiResultOutsourcedDataTaskStandardMetricDTO = {
  data?: OutsourcedDataTaskStandardMetricDTO[];
};

export type SingleResultOutsourcedDataTaskActionResultDTO = {
  data?: OutsourcedDataTaskActionResultDTO;
};

const generatedApi = getJavaApi();

export const getValuationParseTaskSummary = (
  params?: OutsourcedDataTaskQueryParams,
) =>
  generatedApi.summary(params) as Promise<SingleResultOutsourcedDataTaskSummaryDTO>;

export const pageValuationParseTasks = (
  params?: OutsourcedDataTaskQueryParams,
) =>
  generatedApi.pageTasks(params) as Promise<PageResultOutsourcedDataTaskBatchDTO>;

export const getValuationParseTask = (batchId: string) =>
  generatedApi.getTask(batchId) as Promise<SingleResultOutsourcedDataTaskBatchDetailDTO>;

export const getValuationParseTaskTrace = (batchId: string) =>
  customInstance<SingleResultOutsourcedDataTaskTraceDTO>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/trace`,
    method: "GET",
  });

export const getValuationParseTaskStandardBasic = (batchId: string) =>
  customInstance<SingleResultOutsourcedDataTaskStandardBasicDTO>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/standard-data/basic`,
    method: "GET",
  });

export const listValuationParseTaskStandardSubjects = (
  batchId: string,
  params?: { keyword?: string },
) =>
  customInstance<MultiResultOutsourcedDataTaskStandardSubjectDTO>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/standard-data/subjects`,
    method: "GET",
    params,
  });

export const listValuationParseTaskStandardMetrics = (
  batchId: string,
  params?: { keyword?: string },
) =>
  customInstance<MultiResultOutsourcedDataTaskStandardMetricDTO>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/standard-data/metrics`,
    method: "GET",
    params,
  });

export const getValuationParseTaskRawWorkbook = (batchId: string) =>
  customInstance<SingleResultOutsourcedDataTaskRawWorkbookDTO>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/standard-data/raw-workbook`,
    method: "GET",
  });

export const downloadValuationParseTaskRawWorkbook = (batchId: string) =>
  customInstance<Blob>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/standard-data/raw-workbook/download`,
    method: "GET",
    responseType: "blob",
  });

export const exportValuationParseTaskStandardDataSheet = (
  batchId: string,
  command: OutsourcedDataTaskStandardDataExportCommand,
) =>
  customInstance<Blob>({
    url: `/outsourced-data-tasks/${encodeURIComponent(batchId)}/standard-data/export`,
    method: "POST",
    data: command,
    responseType: "blob",
  });

export const listValuationParseTaskSteps = (batchId: string) =>
  generatedApi.listSteps(batchId) as Promise<MultiResultOutsourcedDataTaskStepDTO>;

export const executeValuationParseTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.execute(batchId, command ?? {}) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const retryValuationParseTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.retry1(batchId, command ?? {}) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const stopValuationParseTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.stop1(batchId, command ?? {}) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const retryValuationParseTaskStep = (
  batchId: string,
  stepId: string,
  command?: OutsourcedDataTaskActionCommand,
) =>
  generatedApi.retryStep(
    batchId,
    stepId,
    command ?? {},
  ) as Promise<SingleResultOutsourcedDataTaskActionResultDTO>;

export const batchExecuteValuationParseTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  generatedApi.batchExecute(command) as Promise<MultiResultOutsourcedDataTaskActionResultDTO>;

export const batchRetryValuationParseTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  generatedApi.batchRetry(command) as Promise<MultiResultOutsourcedDataTaskActionResultDTO>;

export const batchStopValuationParseTasks = (
  command: OutsourcedDataTaskBatchCommand,
) =>
  generatedApi.batchStop(command) as Promise<MultiResultOutsourcedDataTaskActionResultDTO>;

export const getOutsourcedDataTaskSummary = getValuationParseTaskSummary;
export const pageOutsourcedDataTasks = pageValuationParseTasks;
export const getOutsourcedDataTask = getValuationParseTask;
export const getOutsourcedDataTaskTrace = getValuationParseTaskTrace;
export const getOutsourcedDataTaskStandardBasic = getValuationParseTaskStandardBasic;
export const listOutsourcedDataTaskStandardSubjects = listValuationParseTaskStandardSubjects;
export const listOutsourcedDataTaskStandardMetrics = listValuationParseTaskStandardMetrics;
export const getOutsourcedDataTaskRawWorkbook = getValuationParseTaskRawWorkbook;
export const exportOutsourcedDataTaskStandardDataSheet = exportValuationParseTaskStandardDataSheet;
export const listOutsourcedDataTaskSteps = listValuationParseTaskSteps;
export const executeOutsourcedDataTask = executeValuationParseTask;
export const retryOutsourcedDataTask = retryValuationParseTask;
export const stopOutsourcedDataTask = stopValuationParseTask;
export const retryOutsourcedDataTaskStep = retryValuationParseTaskStep;
export const batchExecuteOutsourcedDataTasks = batchExecuteValuationParseTasks;
export const batchRetryOutsourcedDataTasks = batchRetryValuationParseTasks;
export const batchStopOutsourcedDataTasks = batchStopValuationParseTasks;
