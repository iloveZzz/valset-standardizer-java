import type { YTablePagination } from "@yss-ui/components";
import type {
  SpringBatchValuationTaskBatchDetailDTO,
  SpringBatchValuationTaskBatchDTO,
  SpringBatchValuationTaskQueryParams,
  SpringBatchValuationTaskStepDTO,
  SpringBatchValuationTaskSummaryDTO,
} from "@/api/springBatchValuationTask";

export type SpringBatchValuationTaskStatus =
  | "PENDING"
  | "RUNNING"
  | "SUCCESS"
  | "FAILED"
  | "STOPPED";

export type SpringBatchValuationTaskStage =
  | "FILE_PARSE"
  | "STRUCTURE_STANDARDIZE"
  | "STANDARD_LANDING";

export type SpringBatchValuationTaskQueryState = SpringBatchValuationTaskQueryParams & {
  batchId: string;
  taskDate: string;
  managerName: string;
  productKeyword: string;
  stage: string;
  status: string;
  sourceType: string;
};

export type SpringBatchValuationTaskBatchRow = SpringBatchValuationTaskBatchDTO & {
  steps: SpringBatchValuationTaskStepRow[];
};

export type SpringBatchValuationTaskStepRow = SpringBatchValuationTaskStepDTO & {
  currentFlag?: boolean;
};

export type SpringBatchValuationTaskPageState = {
  loading: boolean;
  detailLoading: boolean;
  rows: SpringBatchValuationTaskBatchRow[];
  totalCount: number;
  summary: SpringBatchValuationTaskSummaryDTO;
  pagination: YTablePagination;
  query: SpringBatchValuationTaskQueryState;
  selectedRow: SpringBatchValuationTaskBatchRow | null;
  detail: SpringBatchValuationTaskBatchDetailDTO | null;
  detailVisible: boolean;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleStageSelect: (stage: string) => void;
  openDetailDrawer: (row: SpringBatchValuationTaskBatchRow) => void;
  closeDetailDrawer: () => void;
  formatStatusColor: (status?: string) => string;
};
