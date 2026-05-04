import type { YTablePagination } from "@yss-ui/components";

export type OutsourcedDataTaskStatus =
  | "PENDING"
  | "RUNNING"
  | "SUCCESS"
  | "FAILED"
  | "STOPPED"
  | "BLOCKED";

export type OutsourcedDataTaskStage =
  | "FILE_PARSE"
  | "STRUCTURE_STANDARDIZE"
  | "SUBJECT_RECOGNIZE"
  | "STANDARD_LANDING"
  | "DATA_PROCESSING"
  | "VERIFY_ARCHIVE";

export type OutsourcedDataTaskQueryState = {
  batchId: string;
  taskDate: string;
  managerName: string;
  productKeyword: string;
  step: string;
  stage?: string;
  status: string;
  sourceType: string;
  errorType: string;
  includeHistory: boolean;
};

export type OutsourcedDataTaskStepSummary = {
  step: OutsourcedDataTaskStage;
  stepName: string;
  stepDescription: string;
  stage?: OutsourcedDataTaskStage;
  stageName?: string;
  stageDescription?: string;
  totalCount: number;
  runningCount: number;
  failedCount: number;
  pendingCount: number;
};

export type OutsourcedDataTaskBatchRow = {
  batchId: string;
  batchName: string;
  businessDate: string;
  productCode: string;
  productName: string;
  managerName: string;
  fileId: string;
  filesysFileId: string;
  originalFileName: string;
  sourceType: string;
  currentStep: OutsourcedDataTaskStage;
  currentStepName: string;
  currentStage?: OutsourcedDataTaskStage;
  currentStageName?: string;
  status: OutsourcedDataTaskStatus;
  statusName: string;
  progress: number;
  startedAt: string;
  endedAt?: string;
  durationMs?: number;
  durationText: string;
  lastErrorCode?: string;
  lastErrorMessage?: string;
  steps: OutsourcedDataTaskStepRow[];
};

export type OutsourcedDataTaskStepRow = {
  stepId: string;
  batchId: string;
  step: OutsourcedDataTaskStage;
  stepName: string;
  stage?: OutsourcedDataTaskStage;
  stageName?: string;
  taskId: string;
  taskType: string;
  runNo: number;
  triggerMode: string;
  triggerModeName: string;
  status: OutsourcedDataTaskStatus;
  statusName: string;
  progress: number;
  startedAt: string;
  endedAt?: string;
  durationText: string;
  inputSummary?: string;
  outputSummary?: string;
  errorCode?: string;
  errorMessage?: string;
  errorStack?: string;
  logRef?: string;
};

export type OutsourcedDataTaskDataEntry = {
  key: string;
  name: string;
  description: string;
  status: "READY" | "WAITING" | "ERROR";
  statusName: string;
  href?: string;
};

export type OutsourcedDataTaskLogRow = {
  key: string;
  stepName: string;
  stageName?: string;
  status: OutsourcedDataTaskStatus;
  statusName: string;
  startedAt: string;
  durationText: string;
  message: string;
  errorStack?: string;
  logLevel?: string;
  occurredAt?: string;
};

export type OutsourcedDataTaskManualState = {
  currentBlockPoint: string;
  exceptionConfirmText: string;
  rerunPrerequisites: string[];
};

export type OutsourcedDataTaskPage = {
  loading: boolean;
  rows: OutsourcedDataTaskBatchRow[];
  tableData: OutsourcedDataTaskBatchRow[];
  historyLoading: boolean;
  historyRows: OutsourcedDataTaskBatchRow[];
  stepSummaries: OutsourcedDataTaskStepSummary[];
  totalCount: number;
  runningCount: number;
  successCount: number;
  failedCount: number;
  pagination: YTablePagination;
  historyPagination: YTablePagination;
  query: OutsourcedDataTaskQueryState;
  selectedRowKeys: string[];
  selectedRow: OutsourcedDataTaskBatchRow | null;
  detailDataEntries: OutsourcedDataTaskDataEntry[];
  detailLogRows: OutsourcedDataTaskLogRow[];
  manualState: OutsourcedDataTaskManualState;
  detailVisible: boolean;
  historyVisible: boolean;
  historyDrawerTitle: string;
  historyDrawerDescription: string;
  historyDrawerFilterSummary: string;
  stepLogVisible: boolean;
  stepLogLoading: boolean;
  stepLogRows: OutsourcedDataTaskLogRow[];
  stepDataVisible: boolean;
  activeStep: OutsourcedDataTaskStepRow | null;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  selectStep: (stage: string) => void;
  selectStatus: (status: string) => void;
  handleExpandChange: (params: { row?: OutsourcedDataTaskBatchRow; expanded?: boolean }) => void;
  getOrderedSteps: (row: OutsourcedDataTaskBatchRow) => OutsourcedDataTaskStepRow[];
  getStepRowClassName: (params: { row: OutsourcedDataTaskStepRow }) => string;
  openDetailDrawer: (row: OutsourcedDataTaskBatchRow) => void;
  closeDetailDrawer: () => void;
  openStepLogs: (row: OutsourcedDataTaskStepRow) => void;
  closeStepLogs: () => void;
  openStepData: (row: OutsourcedDataTaskStepRow) => void;
  closeStepData: () => void;
  openHistoryDrawer: (row?: OutsourcedDataTaskBatchRow) => void;
  closeHistoryDrawer: () => void;
  handleHistoryPageChange: (params: { current: number; pageSize: number }) => void;
  executeBatch: (row: OutsourcedDataTaskBatchRow) => void;
  retryBatch: (row: OutsourcedDataTaskBatchRow) => void;
  stopBatch: (row: OutsourcedDataTaskBatchRow) => void;
  retryStep: (row: OutsourcedDataTaskStepRow) => void;
  batchExecute: () => void;
  batchRetry: () => void;
  batchStop: () => void;
  formatStatusColor: (status: string) => string;
  canManualExecute: (status?: string) => boolean;
  isContinueExecuteStatus: (status?: string) => boolean;
};
