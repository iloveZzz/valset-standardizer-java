import type { Edge, Node } from "@vue-flow/core";
import type {
  BatchValuationTaskBatchRow,
  BatchValuationTaskQueryState,
  BatchValuationTaskStepRow,
} from "@/views/BatchValuationTask/types";
import type { BatchValuationTaskActionResultDTO } from "@/api/batchValuationTask";
import type { BatchValuationTaskTraceDTO } from "@/api/batchValuationTaskTrace";

export type ValuationTraceNodeKind =
  | "transferObject"
  | "parseQueue"
  | "parseTask"
  | "jobExecution"
  | "stepExecution"
  | "result";

export type ValuationTraceNodeData = {
  kind: ValuationTraceNodeKind;
  title: string;
  subtitle: string;
  status: string;
  statusLabel: string;
  timeLabel: string;
  meta: string;
  message: string;
  active: boolean;
  failed: boolean;
  missing: boolean;
  payload?: Record<string, unknown>;
};

export type ValuationTraceNode = Node<ValuationTraceNodeData>;
export type ValuationTraceEdge = Edge<{ state: string }>;

export type ValuationTraceDetail = {
  node: ValuationTraceNodeData;
  trace: BatchValuationTaskTraceDTO | null;
  steps: BatchValuationTaskStepRow[];
};

export type BatchValuationTaskMonitorPageState = {
  loading: boolean;
  graphLoading: boolean;
  loadingMore: boolean;
  actionLoading: boolean;
  autoRefresh: boolean;
  query: BatchValuationTaskQueryState;
  rows: BatchValuationTaskBatchRow[];
  totalCount: number;
  hasMoreRows: boolean;
  selectedRow: BatchValuationTaskBatchRow | null;
  selectedBatchId: string;
  selectedTrace: BatchValuationTaskTraceDTO | null;
  nodes: ValuationTraceNode[];
  edges: ValuationTraceEdge[];
  detailVisible: boolean;
  selectedDetail: ValuationTraceDetail | null;
  graphSummary: string;
  canAutoRefresh: boolean;
  runQuery: () => void;
  resetQuery: () => void;
  setListContainerRef: (el: Element | import("vue").ComponentPublicInstance | null) => void;
  handleListScroll: (event: Event) => void;
  loadMoreRows: () => void;
  selectTask: (row: BatchValuationTaskBatchRow) => Promise<void>;
  refreshGraph: () => Promise<void>;
  openNodeDetail: (node: ValuationTraceNodeData) => void;
  closeDetail: () => void;
  toggleAutoRefresh: (checked: boolean) => void;
  executeSelected: () => Promise<BatchValuationTaskActionResultDTO | null>;
  retrySelected: () => Promise<BatchValuationTaskActionResultDTO | null>;
  stopSelected: () => Promise<BatchValuationTaskActionResultDTO | null>;
  retryStep: (step: BatchValuationTaskStepRow) => Promise<BatchValuationTaskActionResultDTO | null>;
  canExecute: () => boolean;
  canRetry: () => boolean;
  canStop: () => boolean;
  canRetryStep: (step?: BatchValuationTaskStepRow | null) => boolean;
  formatStatusColor: (status?: string) => string;
  getTaskInitial: (row?: BatchValuationTaskBatchRow | null) => string;
  formatJson: (value: unknown) => string;
};
