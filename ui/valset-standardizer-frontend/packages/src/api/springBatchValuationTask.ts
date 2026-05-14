import {
  batchExecuteValuationParseTasks,
  batchRetryValuationParseTasks,
  batchStopValuationParseTasks,
  executeValuationParseTask,
  getValuationParseTask,
  getValuationParseTaskSummary,
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
  type OutsourcedDataTaskStepDTO,
  type OutsourcedDataTaskSummaryDTO,
} from "./outsourcedDataTask";

export type SpringBatchValuationTaskQueryParams = OutsourcedDataTaskQueryParams;
export type SpringBatchValuationTaskBatchDTO = OutsourcedDataTaskBatchDTO;
export type SpringBatchValuationTaskStepDTO = OutsourcedDataTaskStepDTO;
export type SpringBatchValuationTaskSummaryDTO = OutsourcedDataTaskSummaryDTO;
export type SpringBatchValuationTaskBatchDetailDTO = OutsourcedDataTaskBatchDetailDTO;
export type SpringBatchValuationTaskActionCommand = OutsourcedDataTaskActionCommand;
export type SpringBatchValuationTaskBatchCommand = OutsourcedDataTaskBatchCommand;
export type SpringBatchValuationTaskActionResultDTO = OutsourcedDataTaskActionResultDTO;

export const getSpringBatchValuationTaskSummary = getValuationParseTaskSummary;
export const pageSpringBatchValuationTasks = pageValuationParseTasks;
export const getSpringBatchValuationTask = getValuationParseTask;
export const listSpringBatchValuationTaskSteps = listValuationParseTaskSteps;
export const executeSpringBatchValuationTask = executeValuationParseTask;
export const retrySpringBatchValuationTask = retryValuationParseTask;
export const stopSpringBatchValuationTask = stopValuationParseTask;
export const retrySpringBatchValuationTaskStep = retryValuationParseTaskStep;
export const batchExecuteSpringBatchValuationTasks = batchExecuteValuationParseTasks;
export const batchRetrySpringBatchValuationTasks = batchRetryValuationParseTasks;
export const batchStopSpringBatchValuationTasks = batchStopValuationParseTasks;
