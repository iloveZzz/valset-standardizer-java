import {
  getValuationParseTaskTrace,
  type OutsourcedDataTaskTraceDTO,
  type OutsourcedDataTaskTraceLogDTO,
  type OutsourcedDataTaskTraceRecordDTO,
  type OutsourcedDataTaskTraceResultSummaryDTO,
  type SingleResultOutsourcedDataTaskTraceDTO,
} from "./outsourcedDataTask";

export type BatchValuationTaskTraceRecordDTO = OutsourcedDataTaskTraceRecordDTO;
export type BatchValuationTaskTraceLogDTO = OutsourcedDataTaskTraceLogDTO;
export type BatchValuationTaskTraceResultSummaryDTO =
  OutsourcedDataTaskTraceResultSummaryDTO;
export type BatchValuationTaskTraceDTO = OutsourcedDataTaskTraceDTO;
export type SingleResultBatchValuationTaskTraceDTO =
  SingleResultOutsourcedDataTaskTraceDTO;

export const getBatchValuationTaskTrace = getValuationParseTaskTrace;
