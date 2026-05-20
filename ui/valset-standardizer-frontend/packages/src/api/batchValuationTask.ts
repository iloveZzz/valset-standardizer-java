import {
  batchExecuteValuationParseTasks,
  batchRetryValuationParseTasks,
  batchStopValuationParseTasks,
  downloadValuationParseTaskRawWorkbook,
  executeValuationParseTask,
  exportValuationParseTaskStandardDataSheet,
  getValuationParseTask,
  getValuationParseTaskRawWorkbook,
  getValuationParseTaskSummary,
  getValuationParseTaskStandardBasic,
  listValuationParseTaskStandardMetrics,
  listValuationParseTaskStandardSubjects,
  listValuationParseTaskSteps,
  pageValuationParseTasks,
  retryValuationParseTask,
  retryValuationParseTaskStep,
  stopValuationParseTask,
  type OutsourcedDataTaskActionCommand,
  type OutsourcedDataTaskActionResultDTO,
  type OutsourcedDataTaskBatchCommand,
  type OutsourcedDataTaskBatchDetailDTO,
  type OutsourcedDataTaskBatchDTO,
  type OutsourcedDataTaskQueryParams,
  type OutsourcedDataTaskRawWorkbookDTO,
  type OutsourcedDataTaskStandardDataExportCommand,
  type OutsourcedDataTaskStandardBasicDTO,
  type OutsourcedDataTaskStandardBasicRowDTO,
  type OutsourcedDataTaskStandardMetricDTO,
  type OutsourcedDataTaskStandardRawColumnDTO,
  type OutsourcedDataTaskStandardSubjectDTO,
  type OutsourcedDataTaskStepDTO,
  type OutsourcedDataTaskSummaryDTO,
} from "./outsourcedDataTask";

export type BatchValuationTaskQueryParams = OutsourcedDataTaskQueryParams;
export type BatchValuationTaskBatchDTO = OutsourcedDataTaskBatchDTO;
export type BatchValuationTaskStepDTO = OutsourcedDataTaskStepDTO;
export type BatchValuationTaskSummaryDTO = OutsourcedDataTaskSummaryDTO;
export type BatchValuationTaskBatchDetailDTO = OutsourcedDataTaskBatchDetailDTO;
export type BatchValuationTaskActionCommand = OutsourcedDataTaskActionCommand;
export type BatchValuationTaskBatchCommand = OutsourcedDataTaskBatchCommand;
export type BatchValuationTaskActionResultDTO = OutsourcedDataTaskActionResultDTO;
export type BatchValuationTaskStandardDataExportCommand = OutsourcedDataTaskStandardDataExportCommand;
export type BatchValuationTaskRawWorkbookDTO = OutsourcedDataTaskRawWorkbookDTO;
export type BatchValuationTaskStandardBasicDTO = OutsourcedDataTaskStandardBasicDTO;
export type BatchValuationTaskStandardBasicRowDTO = OutsourcedDataTaskStandardBasicRowDTO;
export type BatchValuationTaskStandardRawColumnDTO = OutsourcedDataTaskStandardRawColumnDTO;
export type BatchValuationTaskStandardSubjectDTO = OutsourcedDataTaskStandardSubjectDTO;
export type BatchValuationTaskStandardMetricDTO = OutsourcedDataTaskStandardMetricDTO;

export const getBatchValuationTaskSummary = getValuationParseTaskSummary;
export const pageBatchValuationTasks = pageValuationParseTasks;
export const getBatchValuationTask = getValuationParseTask;
export const getBatchValuationTaskStandardBasic = getValuationParseTaskStandardBasic;
export const listBatchValuationTaskStandardSubjects = listValuationParseTaskStandardSubjects;
export const listBatchValuationTaskStandardMetrics = listValuationParseTaskStandardMetrics;
export const getBatchValuationTaskRawWorkbook = getValuationParseTaskRawWorkbook;
export const downloadBatchValuationTaskRawWorkbook = downloadValuationParseTaskRawWorkbook;
export const exportBatchValuationTaskStandardDataSheet = exportValuationParseTaskStandardDataSheet;
export const listBatchValuationTaskSteps = listValuationParseTaskSteps;
export const executeBatchValuationTask = executeValuationParseTask;
export const retryBatchValuationTask = retryValuationParseTask;
export const stopBatchValuationTask = stopValuationParseTask;
export const retryBatchValuationTaskStep = retryValuationParseTaskStep;
export const batchExecuteBatchValuationTasks = batchExecuteValuationParseTasks;
export const batchRetryBatchValuationTasks = batchRetryValuationParseTasks;
export const batchStopBatchValuationTasks = batchStopValuationParseTasks;
