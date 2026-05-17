import {
  getValuationParseTaskTrace,
  type OutsourcedDataTaskTraceDTO,
  type OutsourcedDataTaskTraceLogDTO,
  type OutsourcedDataTaskTraceRecordDTO,
  type OutsourcedDataTaskTraceResultSummaryDTO,
  type SingleResultOutsourcedDataTaskTraceDTO,
} from "./outsourcedDataTask";

export type SpringBatchValuationTaskTraceRecordDTO = OutsourcedDataTaskTraceRecordDTO;
export type SpringBatchValuationTaskTraceLogDTO = OutsourcedDataTaskTraceLogDTO;
export type SpringBatchValuationTaskTraceResultSummaryDTO =
  OutsourcedDataTaskTraceResultSummaryDTO;
export type SpringBatchValuationTaskTraceDTO = OutsourcedDataTaskTraceDTO;
export type SingleResultSpringBatchValuationTaskTraceDTO =
  SingleResultOutsourcedDataTaskTraceDTO;

export const getSpringBatchValuationTaskTrace = getValuationParseTaskTrace;
