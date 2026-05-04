import { customInstance } from "./mutator";
import type {
  OutsourcedDataTaskActionCommand,
  OutsourcedDataTaskActionResultDTO,
  OutsourcedDataTaskBatchCommand,
  OutsourcedDataTaskBatchDTO,
  OutsourcedDataTaskBatchDetailDTO,
  OutsourcedDataTaskLogDTO,
  OutsourcedDataTaskQueryParams,
  OutsourcedDataTaskStageSummaryDTO,
  OutsourcedDataTaskStepDTO,
  OutsourcedDataTaskSummaryDTO,
  MultiResultOutsourcedDataTaskActionResultDTO,
  MultiResultOutsourcedDataTaskStepDTO,
  PageResultOutsourcedDataTaskBatchDTO,
  PageResultOutsourcedDataTaskLogDTO,
  SingleResultOutsourcedDataTaskActionResultDTO,
  SingleResultOutsourcedDataTaskBatchDetailDTO,
  SingleResultOutsourcedDataTaskSummaryDTO,
} from "./outsourcedDataTask";

export type HoldingPenetrationTaskQueryParams = OutsourcedDataTaskQueryParams;
export type HoldingPenetrationTaskBatchDTO = OutsourcedDataTaskBatchDTO;
export type HoldingPenetrationTaskStepDTO = OutsourcedDataTaskStepDTO;
export type HoldingPenetrationTaskLogDTO = OutsourcedDataTaskLogDTO;
export type HoldingPenetrationTaskStageSummaryDTO = OutsourcedDataTaskStageSummaryDTO;
export type HoldingPenetrationTaskSummaryDTO = OutsourcedDataTaskSummaryDTO;
export type HoldingPenetrationTaskBatchDetailDTO = OutsourcedDataTaskBatchDetailDTO;
export type HoldingPenetrationTaskActionCommand = OutsourcedDataTaskActionCommand;
export type HoldingPenetrationTaskBatchCommand = OutsourcedDataTaskBatchCommand;
export type HoldingPenetrationTaskActionResultDTO = OutsourcedDataTaskActionResultDTO;
export type PageResultHoldingPenetrationTaskBatchDTO =
  PageResultOutsourcedDataTaskBatchDTO;
export type SingleResultHoldingPenetrationTaskSummaryDTO =
  SingleResultOutsourcedDataTaskSummaryDTO;
export type SingleResultHoldingPenetrationTaskBatchDetailDTO =
  SingleResultOutsourcedDataTaskBatchDetailDTO;
export type MultiResultHoldingPenetrationTaskActionResultDTO =
  MultiResultOutsourcedDataTaskActionResultDTO;
export type MultiResultHoldingPenetrationTaskStepDTO =
  MultiResultOutsourcedDataTaskStepDTO;
export type PageResultHoldingPenetrationTaskLogDTO =
  PageResultOutsourcedDataTaskLogDTO;
export type SingleResultHoldingPenetrationTaskActionResultDTO =
  SingleResultOutsourcedDataTaskActionResultDTO;

export const getHoldingPenetrationTaskSummary = (
  params?: HoldingPenetrationTaskQueryParams,
) =>
  customInstance<SingleResultHoldingPenetrationTaskSummaryDTO>({
    url: "/holding-penetration-tasks/summary",
    method: "GET",
    params,
  });

export const pageHoldingPenetrationTasks = (
  params?: HoldingPenetrationTaskQueryParams,
) =>
  customInstance<PageResultHoldingPenetrationTaskBatchDTO>({
    url: "/holding-penetration-tasks",
    method: "GET",
    params,
  });

export const getHoldingPenetrationTask = (batchId: string) =>
  customInstance<SingleResultHoldingPenetrationTaskBatchDetailDTO>({
    url: `/holding-penetration-tasks/${batchId}`,
    method: "GET",
  });

export const listHoldingPenetrationTaskSteps = (batchId: string) =>
  customInstance<MultiResultHoldingPenetrationTaskStepDTO>({
    url: `/holding-penetration-tasks/${batchId}/steps`,
    method: "GET",
  });

export const pageHoldingPenetrationTaskLogs = (
  batchId: string,
  params?: {
    stage?: string;
    step?: string;
    pageIndex?: number;
    pageSize?: number;
  },
) =>
  customInstance<PageResultHoldingPenetrationTaskLogDTO>({
    url: `/holding-penetration-tasks/${batchId}/logs`,
    method: "GET",
    params,
  });

export const executeHoldingPenetrationTask = (
  batchId: string,
  command?: HoldingPenetrationTaskActionCommand,
) =>
  customInstance<SingleResultHoldingPenetrationTaskActionResultDTO>({
    url: `/holding-penetration-tasks/${batchId}/execute`,
    method: "POST",
    data: command ?? {},
  });

export const retryHoldingPenetrationTask = (
  batchId: string,
  command?: HoldingPenetrationTaskActionCommand,
) =>
  customInstance<SingleResultHoldingPenetrationTaskActionResultDTO>({
    url: `/holding-penetration-tasks/${batchId}/retry`,
    method: "POST",
    data: command ?? {},
  });

export const stopHoldingPenetrationTask = (
  batchId: string,
  command?: HoldingPenetrationTaskActionCommand,
) =>
  customInstance<SingleResultHoldingPenetrationTaskActionResultDTO>({
    url: `/holding-penetration-tasks/${batchId}/stop`,
    method: "POST",
    data: command ?? {},
  });

export const retryHoldingPenetrationTaskStep = (
  batchId: string,
  stepId: string,
  command?: HoldingPenetrationTaskActionCommand,
) =>
  customInstance<SingleResultHoldingPenetrationTaskActionResultDTO>({
    url: `/holding-penetration-tasks/${batchId}/steps/${stepId}/retry`,
    method: "POST",
    data: command ?? {},
  });

export const batchExecuteHoldingPenetrationTasks = (
  command: HoldingPenetrationTaskBatchCommand,
) =>
  customInstance<MultiResultHoldingPenetrationTaskActionResultDTO>({
    url: "/holding-penetration-tasks/batch-execute",
    method: "POST",
    data: command,
  });

export const batchRetryHoldingPenetrationTasks = (
  command: HoldingPenetrationTaskBatchCommand,
) =>
  customInstance<MultiResultHoldingPenetrationTaskActionResultDTO>({
    url: "/holding-penetration-tasks/batch-retry",
    method: "POST",
    data: command,
  });

export const batchStopHoldingPenetrationTasks = (
  command: HoldingPenetrationTaskBatchCommand,
) =>
  customInstance<MultiResultHoldingPenetrationTaskActionResultDTO>({
    url: "/holding-penetration-tasks/batch-stop",
    method: "POST",
    data: command,
  });
