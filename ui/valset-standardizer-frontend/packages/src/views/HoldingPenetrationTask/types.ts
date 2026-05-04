import type { YTablePagination } from "@yss-ui/components";

export type HoldingPenetrationTaskStatus =
  | "PENDING"
  | "RUNNING"
  | "SUCCESS"
  | "FAILED"
  | "STOPPED"
  | "BLOCKED";

export type HoldingPenetrationTaskStage =
  | "NET_VALUE_STANDARDIZE"
  | "POSITION_STANDARDIZE"
  | "TAG_VERIFY"
  | "POSITION_VERIFY"
  | "SECURITY_STANDARDIZE";

export type HoldingPenetrationTaskQueryState = {
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

export type HoldingPenetrationTaskStepSummary = {
  step: HoldingPenetrationTaskStage;
  stepName: string;
  stepDescription: string;
  stage?: HoldingPenetrationTaskStage;
  stageName?: string;
  stageDescription?: string;
  totalCount: number;
  runningCount: number;
  failedCount: number;
  pendingCount: number;
};

export type HoldingPenetrationTaskBatchRow = {
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
  currentStep: HoldingPenetrationTaskStage;
  currentStepName: string;
  currentStage?: HoldingPenetrationTaskStage;
  currentStageName?: string;
  status: HoldingPenetrationTaskStatus;
  statusName: string;
  progress: number;
  startedAt: string;
  endedAt?: string;
  durationMs?: number;
  durationText: string;
  lastErrorCode?: string;
  lastErrorMessage?: string;
  steps: HoldingPenetrationTaskStepRow[];
};

export type HoldingPenetrationTaskStepRow = {
  stepId: string;
  batchId: string;
  step: HoldingPenetrationTaskStage;
  stepName: string;
  stage?: HoldingPenetrationTaskStage;
  stageName?: string;
  taskId: string;
  taskType: string;
  runNo: number;
  triggerMode: string;
  triggerModeName: string;
  status: HoldingPenetrationTaskStatus;
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

export type HoldingPenetrationTaskDataEntry = {
  key: string;
  name: string;
  description: string;
  status: "READY" | "WAITING" | "ERROR";
  statusName: string;
  href?: string;
};

export type HoldingPenetrationTaskLogRow = {
  key: string;
  stepName: string;
  stageName?: string;
  status: HoldingPenetrationTaskStatus;
  statusName: string;
  startedAt: string;
  durationText: string;
  message: string;
  errorStack?: string;
  logLevel?: string;
  occurredAt?: string;
};

export type HoldingPenetrationTaskManualState = {
  currentBlockPoint: string;
  exceptionConfirmText: string;
  rerunPrerequisites: string[];
};

export type HoldingPenetrationTaskPage = {
  loading: boolean;
  rows: HoldingPenetrationTaskBatchRow[];
  tableData: HoldingPenetrationTaskBatchRow[];
  historyLoading: boolean;
  historyRows: HoldingPenetrationTaskBatchRow[];
  stepSummaries: HoldingPenetrationTaskStepSummary[];
  totalCount: number;
  runningCount: number;
  successCount: number;
  failedCount: number;
  pagination: YTablePagination;
  historyPagination: YTablePagination;
  query: HoldingPenetrationTaskQueryState;
  selectedRowKeys: string[];
  selectedRow: HoldingPenetrationTaskBatchRow | null;
  detailDataEntries: HoldingPenetrationTaskDataEntry[];
  detailLogRows: HoldingPenetrationTaskLogRow[];
  manualState: HoldingPenetrationTaskManualState;
  detailVisible: boolean;
  historyVisible: boolean;
  historyDrawerTitle: string;
  historyDrawerDescription: string;
  historyDrawerFilterSummary: string;
  stepLogVisible: boolean;
  stepLogLoading: boolean;
  stepLogRows: HoldingPenetrationTaskLogRow[];
  stepDataVisible: boolean;
  activeStep: HoldingPenetrationTaskStepRow | null;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  selectStep: (stage: string) => void;
  selectStatus: (status: string) => void;
  handleExpandChange: (params: { row?: HoldingPenetrationTaskBatchRow; expanded?: boolean }) => void;
  getOrderedSteps: (row: HoldingPenetrationTaskBatchRow) => HoldingPenetrationTaskStepRow[];
  getStepRowClassName: (params: { row: HoldingPenetrationTaskStepRow }) => string;
  openDetailDrawer: (row: HoldingPenetrationTaskBatchRow) => void;
  closeDetailDrawer: () => void;
  openStepLogs: (row: HoldingPenetrationTaskStepRow) => void;
  closeStepLogs: () => void;
  openStepData: (row: HoldingPenetrationTaskStepRow) => void;
  closeStepData: () => void;
  openHistoryDrawer: (row?: HoldingPenetrationTaskBatchRow) => void;
  closeHistoryDrawer: () => void;
  handleHistoryPageChange: (params: { current: number; pageSize: number }) => void;
  executeBatch: (row: HoldingPenetrationTaskBatchRow) => void;
  retryBatch: (row: HoldingPenetrationTaskBatchRow) => void;
  stopBatch: (row: HoldingPenetrationTaskBatchRow) => void;
  retryStep: (row: HoldingPenetrationTaskStepRow) => void;
  batchExecute: () => void;
  batchRetry: () => void;
  batchStop: () => void;
  formatStatusColor: (status: string) => string;
  canManualExecute: (status?: string) => boolean;
  isContinueExecuteStatus: (status?: string) => boolean;
};
