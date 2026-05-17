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
  taskStage: string;
  stage: string;
  step: string;
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
  tableRef: any;
  loading: boolean;
  detailLoading: boolean;
  batchRetryLoading: boolean;
  rows: SpringBatchValuationTaskBatchRow[];
  totalCount: number;
  summary: SpringBatchValuationTaskSummaryDTO;
  currentFilterSummary: string;
  pagination: YTablePagination;
  query: SpringBatchValuationTaskQueryState;
  selectedBatchIds: string[];
  selectedRows: SpringBatchValuationTaskBatchRow[];
  selectedRow: SpringBatchValuationTaskBatchRow | null;
  detail: SpringBatchValuationTaskBatchDetailDTO | null;
  detailVisible: boolean;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleStatusSelect: (status: string) => void;
  handleStageSelect: (stage: string) => void;
  handleStepSelect: (step: string) => void;
  handleSelectionChange: (rows: SpringBatchValuationTaskBatchRow[]) => void;
  setTableRef: (instance: any) => void;
  syncSelectionFromTable: () => void;
  clearSelection: () => Promise<void>;
  selectCurrentPageRows: () => Promise<void>;
  canRetryBatch: (row?: SpringBatchValuationTaskBatchRow | null) => boolean;
  batchRetrySelected: () => void;
  retryBatchRow: (row: SpringBatchValuationTaskBatchRow) => void;
  syncTableSelection: (rows?: SpringBatchValuationTaskBatchRow[]) => void;
  openDetailDrawer: (row: SpringBatchValuationTaskBatchRow) => void;
  closeDetailDrawer: () => void;
  formatStatusColor: (status?: string) => string;
};
