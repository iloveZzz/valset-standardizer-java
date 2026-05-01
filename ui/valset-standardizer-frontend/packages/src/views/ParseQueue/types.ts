import type { YTablePagination } from "@yss-ui/components";

export type ParseQueueStatus = "PENDING" | "PARSING" | "PARSED" | "FAILED";

export type ParseQueueTriggerMode = "AUTO" | "MANUAL";

export type ParseQueueQueryState = {
  transferId: string;
  businessKey: string;
  sourceCode: string;
  routeId: string;
  tagCode: string;
  fileStatus: string;
  deliveryStatus: string;
  parseStatus: string;
  triggerMode: string;
};

export type ParseQueueRow = {
  queueId: string;
  businessKey: string;
  transferId: string;
  originalName: string;
  sourceId?: string;
  sourceType?: string;
  sourceCode?: string;
  routeId?: string;
  deliveryId?: string;
  tagId?: string;
  tagCode?: string;
  tagName?: string;
  fileStatus?: string;
  deliveryStatus?: string;
  parseStatus: ParseQueueStatus;
  triggerMode: ParseQueueTriggerMode;
  retryCount: number;
  subscribedBy?: string;
  subscribedAt?: string;
  parsedAt?: string;
  lastErrorMessage?: string;
  objectSnapshotJson?: unknown;
  deliverySnapshotJson?: unknown;
  parseRequestJson?: unknown;
  parseResultJson?: unknown;
  createdAt: string;
  updatedAt: string;
};

export type ParseQueueBatchResult = {
  totalCount: number;
  generatedCount: number;
  skippedCount: number;
};

export type ParseQueuePage = {
  loading: boolean;
  listLoading: boolean;
  backfillLoading: boolean;
  rows: ParseQueueRow[];
  tableData: ParseQueueRow[];
  total: number;
  pagination: YTablePagination;
  query: ParseQueueQueryState;
  pendingCount: number;
  parsingCount: number;
  parsedCount: number;
  failedCount: number;
  detailVisible: boolean;
  selectedRow: ParseQueueRow | null;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  openDetailDrawer: (row: ParseQueueRow) => void;
  closeDetail: () => void;
  openLifecyclePage: (row: ParseQueueRow) => void;
  generateQueue: (row: ParseQueueRow, forceRebuild?: boolean) => void;
  retryQueue: (row: ParseQueueRow) => void;
  subscribeQueue: (row: ParseQueueRow) => void;
  completeQueue: (row: ParseQueueRow) => void;
  failQueue: (row: ParseQueueRow) => void;
  backfillCurrentScope: (forceRebuild?: boolean) => void;
  formatParseStatus: (value: string | undefined) => string;
  formatTriggerMode: (value: string | undefined) => string;
  formatStatus: (value: string | undefined) => string;
  safeJson: (value: unknown) => string;
};
