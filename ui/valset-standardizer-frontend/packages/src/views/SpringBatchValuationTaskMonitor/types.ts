import type { Edge, Node } from "@vue-flow/core";
import type {
  SpringBatchValuationTaskBatchRow,
  SpringBatchValuationTaskQueryState,
  SpringBatchValuationTaskStepRow,
} from "@/views/SpringBatchValuationTask/types";
import type { SpringBatchValuationTaskActionResultDTO } from "@/api/springBatchValuationTask";
import type { SpringBatchValuationTaskTraceDTO } from "@/api/springBatchValuationTaskTrace";

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
  trace: SpringBatchValuationTaskTraceDTO | null;
  steps: SpringBatchValuationTaskStepRow[];
};

export type SpringBatchValuationTaskMonitorPageState = {
  loading: boolean;
  graphLoading: boolean;
  loadingMore: boolean;
  actionLoading: boolean;
  autoRefresh: boolean;
  query: SpringBatchValuationTaskQueryState;
  rows: SpringBatchValuationTaskBatchRow[];
  totalCount: number;
  hasMoreRows: boolean;
  selectedRow: SpringBatchValuationTaskBatchRow | null;
  selectedBatchId: string;
  selectedTrace: SpringBatchValuationTaskTraceDTO | null;
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
  selectTask: (row: SpringBatchValuationTaskBatchRow) => Promise<void>;
  refreshGraph: () => Promise<void>;
  openNodeDetail: (node: ValuationTraceNodeData) => void;
  closeDetail: () => void;
  toggleAutoRefresh: (checked: boolean) => void;
  executeSelected: () => Promise<SpringBatchValuationTaskActionResultDTO | null>;
  retrySelected: () => Promise<SpringBatchValuationTaskActionResultDTO | null>;
  stopSelected: () => Promise<SpringBatchValuationTaskActionResultDTO | null>;
  retryStep: (step: SpringBatchValuationTaskStepRow) => Promise<SpringBatchValuationTaskActionResultDTO | null>;
  canExecute: () => boolean;
  canRetry: () => boolean;
  canStop: () => boolean;
  canRetryStep: (step?: SpringBatchValuationTaskStepRow | null) => boolean;
  formatStatusColor: (status?: string) => string;
  getTaskInitial: (row?: SpringBatchValuationTaskBatchRow | null) => string;
  formatJson: (value: unknown) => string;
};
