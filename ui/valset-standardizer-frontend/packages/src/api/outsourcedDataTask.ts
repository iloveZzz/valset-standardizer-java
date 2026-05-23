import { getJavaApi } from "./generated/valset";
import { customInstance } from "./mutator";
import type {
  ListExternalMetricsParams,
  ListExternalSubjectsParams,
  ListStandardMetricsParams,
  ListStandardSubjectsParams,
  OutsourcedDataTaskExternalMetricDTO as GeneratedOutsourcedDataTaskExternalMetricDTO,
  OutsourcedDataTaskExternalSubjectDTO as GeneratedOutsourcedDataTaskExternalSubjectDTO,
  OutsourcedDataTaskStandardBasicDTO as GeneratedOutsourcedDataTaskStandardBasicDTO,
  OutsourcedDataTaskTraceDTO as GeneratedOutsourcedDataTaskTraceDTO,
  OutsourcedDataTaskStandardBasicRowDTO as GeneratedOutsourcedDataTaskStandardBasicRowDTO,
  OutsourcedDataTaskStandardMetricDTO as GeneratedOutsourcedDataTaskStandardMetricDTO,
  OutsourcedDataTaskStandardSubjectDTO as GeneratedOutsourcedDataTaskStandardSubjectDTO,
  OutsourcedDataTaskTraceRecordDTO as GeneratedOutsourcedDataTaskTraceRecordDTO,
  OutsourcedDataTaskActionCommand,
  OutsourcedDataTaskBatchCommand,
  OutsourcedDataTaskStandardDataExportCommand,
  PageTasksParams,
} from "./generated/valset/schemas";

const generatedApi = getJavaApi();

export type OutsourcedDataTaskQueryParams = PageTasksParams;
export type OutsourcedDataTaskStandardBasicRowDTO =
  GeneratedOutsourcedDataTaskStandardBasicRowDTO & Record<string, unknown>;
export type OutsourcedDataTaskStandardSubjectDTO = Omit<
  GeneratedOutsourcedDataTaskStandardSubjectDTO,
  "rawValues"
> & {
  rawValues?: Record<string, string>;
};
export type OutsourcedDataTaskStandardMetricDTO = Omit<
  GeneratedOutsourcedDataTaskStandardMetricDTO,
  "rawValues"
> & {
  rawValues?: Record<string, string>;
};
export type OutsourcedDataTaskExternalSubjectDTO =
  GeneratedOutsourcedDataTaskExternalSubjectDTO & Record<string, unknown>;
export type OutsourcedDataTaskExternalMetricDTO =
  GeneratedOutsourcedDataTaskExternalMetricDTO & Record<string, unknown>;
export type OutsourcedDataTaskTraceRecordDTO = Omit<
  GeneratedOutsourcedDataTaskTraceRecordDTO,
  "attributes"
> & {
  attributes?: Record<string, unknown>;
};
export type OutsourcedDataTaskStandardBasicDTO = Omit<
  GeneratedOutsourcedDataTaskStandardBasicDTO,
  "basicRows"
> & {
  basicRows?: OutsourcedDataTaskStandardBasicRowDTO[];
};
export type OutsourcedDataTaskTraceDTO = Omit<
  GeneratedOutsourcedDataTaskTraceDTO,
  "parseQueue" | "transferObject" | "jobExecution" | "stepExecutions"
> & {
  parseQueue?: OutsourcedDataTaskTraceRecordDTO;
  transferObject?: OutsourcedDataTaskTraceRecordDTO;
  jobExecution?: OutsourcedDataTaskTraceRecordDTO;
  stepExecutions?: OutsourcedDataTaskTraceRecordDTO[];
};
export type MultiResultOutsourcedDataTaskStandardSubjectDTO = {
  data?: OutsourcedDataTaskStandardSubjectDTO[];
};
export type MultiResultOutsourcedDataTaskStandardMetricDTO = {
  data?: OutsourcedDataTaskStandardMetricDTO[];
};
export type MultiResultOutsourcedDataTaskExternalSubjectDTO = {
  data?: OutsourcedDataTaskExternalSubjectDTO[];
};
export type MultiResultOutsourcedDataTaskExternalMetricDTO = {
  data?: OutsourcedDataTaskExternalMetricDTO[];
};
export type SingleResultOutsourcedDataTaskTraceDTO = {
  data?: OutsourcedDataTaskTraceDTO;
};
export type SingleResultOutsourcedDataTaskStandardBasicDTO = {
  data?: OutsourcedDataTaskStandardBasicDTO;
};

export type {
  MultiResultOutsourcedDataTaskActionResultDTO,
  MultiResultOutsourcedDataTaskStepDTO,
  OutsourcedDataTaskActionCommand,
  OutsourcedDataTaskActionResultDTO,
  OutsourcedDataTaskBatchCommand,
  OutsourcedDataTaskBatchDTO,
  OutsourcedDataTaskBatchDetailDTO,
  OutsourcedDataTaskRawWorkbookDTO,
  OutsourcedDataTaskStageSummaryDTO,
  OutsourcedDataTaskStandardDataExportCommand,
  OutsourcedDataTaskStandardRawColumnDTO,
  OutsourcedDataTaskStepDTO,
  OutsourcedDataTaskSummaryDTO,
  OutsourcedDataTaskTraceLogDTO,
  OutsourcedDataTaskTraceResultSummaryDTO,
  PageResultOutsourcedDataTaskBatchDTO,
  SingleResultOutsourcedDataTaskActionResultDTO,
  SingleResultOutsourcedDataTaskBatchDetailDTO,
  SingleResultOutsourcedDataTaskRawWorkbookDTO,
  SingleResultOutsourcedDataTaskSummaryDTO,
} from "./generated/valset/schemas";

export const getValuationParseTaskSummary = generatedApi.summary;
export const pageValuationParseTasks = generatedApi.pageTasks;
export const getValuationParseTask = generatedApi.getTask;
export const getValuationParseTaskTrace = (batchId: string) =>
  generatedApi.getTrace(
    batchId,
  ) as Promise<SingleResultOutsourcedDataTaskTraceDTO>;
export const getValuationParseTaskStandardBasic = (batchId: string) =>
  generatedApi.queryStandardBasic(
    batchId,
  ) as Promise<SingleResultOutsourcedDataTaskStandardBasicDTO>;

export const listValuationParseTaskStandardSubjects = (
  batchId: string,
  params?: ListStandardSubjectsParams,
) =>
  generatedApi.listStandardSubjects(
    batchId,
    params,
  ) as Promise<MultiResultOutsourcedDataTaskStandardSubjectDTO>;

export const listValuationParseTaskStandardMetrics = (
  batchId: string,
  params?: ListStandardMetricsParams,
) =>
  generatedApi.listStandardMetrics(
    batchId,
    params,
  ) as Promise<MultiResultOutsourcedDataTaskStandardMetricDTO>;

export const listValuationParseTaskExternalSubjects = (
  batchId: string,
  params?: ListExternalSubjectsParams,
) =>
  generatedApi.listExternalSubjects(
    batchId,
    params,
  ) as Promise<MultiResultOutsourcedDataTaskExternalSubjectDTO>;

export const listValuationParseTaskExternalMetrics = (
  batchId: string,
  params?: ListExternalMetricsParams,
) =>
  generatedApi.listExternalMetrics(
    batchId,
    params,
  ) as Promise<MultiResultOutsourcedDataTaskExternalMetricDTO>;

export const getValuationParseTaskRawWorkbook = generatedApi.queryRawWorkbook;
export const downloadValuationParseTaskRawWorkbook = (batchId: string) =>
  generatedApi.downloadRawWorkbook(batchId) as Promise<Blob>;

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

export const listValuationParseTaskSteps = generatedApi.listSteps;
export const executeValuationParseTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) => generatedApi.execute(batchId, command ?? {});
export const retryValuationParseTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) => generatedApi.retry1(batchId, command ?? {});
export const stopValuationParseTask = (
  batchId: string,
  command?: OutsourcedDataTaskActionCommand,
) => generatedApi.stop1(batchId, command ?? {});
export const retryValuationParseTaskStep = (
  batchId: string,
  stepId: string,
  command?: OutsourcedDataTaskActionCommand,
) => generatedApi.retryStep(batchId, stepId, command ?? {});
export const batchExecuteValuationParseTasks = (
  command: OutsourcedDataTaskBatchCommand,
) => generatedApi.batchExecute(command);
export const batchRetryValuationParseTasks = (
  command: OutsourcedDataTaskBatchCommand,
) => generatedApi.batchRetry(command);
export const batchStopValuationParseTasks = (
  command: OutsourcedDataTaskBatchCommand,
) => generatedApi.batchStop(command);

export const getOutsourcedDataTaskSummary = getValuationParseTaskSummary;
export const pageOutsourcedDataTasks = pageValuationParseTasks;
export const getOutsourcedDataTask = getValuationParseTask;
export const getOutsourcedDataTaskTrace = getValuationParseTaskTrace;
export const getOutsourcedDataTaskStandardBasic =
  getValuationParseTaskStandardBasic;
export const listOutsourcedDataTaskStandardSubjects =
  listValuationParseTaskStandardSubjects;
export const listOutsourcedDataTaskStandardMetrics =
  listValuationParseTaskStandardMetrics;
export const listOutsourcedDataTaskExternalSubjects =
  listValuationParseTaskExternalSubjects;
export const listOutsourcedDataTaskExternalMetrics =
  listValuationParseTaskExternalMetrics;
export const getOutsourcedDataTaskRawWorkbook = getValuationParseTaskRawWorkbook;
export const exportOutsourcedDataTaskStandardDataSheet =
  exportValuationParseTaskStandardDataSheet;
export const listOutsourcedDataTaskSteps = listValuationParseTaskSteps;
export const executeOutsourcedDataTask = executeValuationParseTask;
export const retryOutsourcedDataTask = retryValuationParseTask;
export const stopOutsourcedDataTask = stopValuationParseTask;
export const retryOutsourcedDataTaskStep = retryValuationParseTaskStep;
export const batchExecuteOutsourcedDataTasks = batchExecuteValuationParseTasks;
export const batchRetryOutsourcedDataTasks = batchRetryValuationParseTasks;
export const batchStopOutsourcedDataTasks = batchStopValuationParseTasks;
