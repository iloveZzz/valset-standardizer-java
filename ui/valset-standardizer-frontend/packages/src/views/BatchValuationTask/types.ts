import type { YTablePagination } from "@yss-ui/components";
import type {
  BatchValuationTaskBatchDetailDTO,
  BatchValuationTaskBatchDTO,
  BatchValuationTaskQueryParams,
  BatchValuationTaskRawWorkbookDTO,
  BatchValuationTaskStandardBasicDTO,
  BatchValuationTaskStandardMetricDTO,
  BatchValuationTaskStandardRawColumnDTO,
  BatchValuationTaskStandardSubjectDTO,
  BatchValuationTaskStepDTO,
  BatchValuationTaskSummaryDTO,
} from "@/api/batchValuationTask";

export type BatchValuationTaskStatus =
  | "PENDING"
  | "RUNNING"
  | "SUCCESS"
  | "FAILED"
  | "STOPPED";

export type BatchValuationTaskStage =
  | "FILE_PARSE"
  | "STRUCTURE_STANDARDIZE"
  | "STANDARD_LANDING";

export type BatchValuationTaskQueryState = BatchValuationTaskQueryParams & {
  batchId: string;
  taskDate: string;
  businessDate: string;
  managerName: string;
  productKeyword: string;
  taskStage: string;
  stage: string;
  step: string;
  status: string;
  sourceType: string;
};

export type BatchValuationTaskBatchRow = BatchValuationTaskBatchDTO & {
  steps: BatchValuationTaskStepRow[];
};

export type BatchValuationTaskStepRow = BatchValuationTaskStepDTO & {
  currentFlag?: boolean;
};

export type BatchValuationTaskStandardTab = "basic" | "subjects" | "metrics" | "raw";

export type BatchValuationTaskStandardRawColumn = BatchValuationTaskStandardRawColumnDTO;

export type BatchValuationTaskAutoRefreshInterval = 0 | 5 | 10 | 30 | 60;

export type BatchValuationTaskPageState = {
  tableRef: any;
  loading: boolean;
  detailLoading: boolean;
  batchRetryLoading: boolean;
  autoRefreshInterval: BatchValuationTaskAutoRefreshInterval;
  autoRefreshOptions: Array<{
    label: string;
    value: BatchValuationTaskAutoRefreshInterval;
  }>;
  lastUpdatedAt: string;
  rows: BatchValuationTaskBatchRow[];
  totalCount: number;
  summary: BatchValuationTaskSummaryDTO;
  currentFilterSummary: string;
  pagination: YTablePagination;
  query: BatchValuationTaskQueryState;
  selectedBatchIds: string[];
  selectedRows: BatchValuationTaskBatchRow[];
  selectedRow: BatchValuationTaskBatchRow | null;
  detail: BatchValuationTaskBatchDetailDTO | null;
  detailVisible: boolean;
  standardDataVisible: boolean;
  standardDataActiveTab: BatchValuationTaskStandardTab;
  standardDataSelectedRow: BatchValuationTaskBatchRow | null;
  standardDataBasic: BatchValuationTaskStandardBasicDTO | null;
  standardDataRawWorkbook: BatchValuationTaskRawWorkbookDTO | null;
  standardDataBasicRows: NonNullable<BatchValuationTaskStandardBasicDTO["basicRows"]>;
  standardDataSubjects: BatchValuationTaskStandardSubjectDTO[];
  standardDataMetrics: BatchValuationTaskStandardMetricDTO[];
  standardDataBasicLoading: boolean;
  standardDataSubjectsLoading: boolean;
  standardDataMetricsLoading: boolean;
  standardDataRawLoading: boolean;
  standardDataRawError: string;
  standardDataExportLoading: boolean;
  standardDataRawDownloadLoading: boolean;
  standardDataSubjectsKeyword: string;
  standardDataMetricsKeyword: string;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleStatusSelect: (status: string) => void;
  handleStageSelect: (stage: string) => void;
  handleStepSelect: (step: string) => void;
  handleSelectionChange: (rows: BatchValuationTaskBatchRow[]) => void;
  setTableRef: (instance: any) => void;
  syncSelectionFromTable: () => void;
  clearSelection: () => Promise<void>;
  selectCurrentPageRows: () => Promise<void>;
  canRetryBatch: (row?: BatchValuationTaskBatchRow | null) => boolean;
  batchRetrySelected: () => void;
  retryBatchRow: (row: BatchValuationTaskBatchRow) => void;
  syncTableSelection: (rows?: BatchValuationTaskBatchRow[]) => void;
  openDetailDrawer: (row: BatchValuationTaskBatchRow) => void;
  closeDetailDrawer: () => void;
  openStandardDataModal: (row: BatchValuationTaskBatchRow) => void;
  closeStandardDataModal: () => void;
  handleStandardDataTabChange: (tab: BatchValuationTaskStandardTab | string) => void;
  refreshStandardData: () => void;
  exportStandardDataSheet: (workbookData: Record<string, unknown> | null, sheetName?: string) => Promise<void>;
  downloadRawWorkbook: () => Promise<void>;
  searchStandardDataSubjects: () => void;
  searchStandardDataMetrics: () => void;
  setAutoRefreshInterval: (value: number) => void;
  formatStatusColor: (status?: string) => string;
};
