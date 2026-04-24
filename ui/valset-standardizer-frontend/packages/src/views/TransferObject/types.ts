import type { YTablePagination } from "@yss-ui/components";
import type { TransferObjectViewDTO } from "@/api/generated/valset/schemas";

export type ObjectStatusStat = {
  status?: string;
  statusLabel: string;
  count: number;
};

export type ObjectSourceStat = {
  sourceType?: string;
  totalCount: number;
  statusCounts: ObjectStatusStat[];
};

export type ObjectAnalysis = {
  totalCount: number;
  sourceAnalyses: ObjectSourceStat[];
};

export type ObjectQueryState = {
  sourceId: string;
  sourceType: string;
  sourceCode: string;
  status: string;
  mailId: string;
  fingerprint: string;
  routeId: string;
};

export type ObjectPage = {
  loading: boolean;
  analysisLoading: boolean;
  rows: TransferObjectViewDTO[];
  tableData: TransferObjectViewDTO[];
  total: number;
  pageSize: number;
  pagination: YTablePagination;
  query: ObjectQueryState;
  analysis: ObjectAnalysis;
  errorCount: number;
  routeCount: number;
  sourceCount: number;
  statusCount: number;
  detailVisible: boolean;
  selectedRow: TransferObjectViewDTO | null;
  openDetailDrawer: (row: TransferObjectViewDTO) => void;
  runQuery: () => void;
  resetQuery: () => void;
  closeDetail: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  applySourceFilter: (sourceType?: string) => void;
  applySourceStatusFilter: (sourceType?: string, status?: string) => void;
  formatStatus: (value: string | undefined) => string;
  formatSourceTypeLabel: (value: string | undefined) => string;
  formatStatusLabel: (value: string | undefined) => string;
  safeJson: (value: unknown) => string;
};
