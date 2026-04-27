import type { YTablePagination } from "@yss-ui/components";
import type { TransferObjectViewDTO as GeneratedTransferObjectViewDTO } from "@/api/generated/valset/schemas";

export type TransferObjectTagViewDTO = {
  id?: string;
  transferId?: string;
  tagId?: string;
  tagCode?: string;
  tagName?: string;
  tagValue?: string;
  matchStrategy?: string;
  matchReason?: string;
  matchedField?: string;
  matchedValue?: string;
  createdAt?: string;
};

export type TransferObjectViewDTO = GeneratedTransferObjectViewDTO & {
  tags?: TransferObjectTagViewDTO[];
};

export type ObjectStatusStat = {
  status?: string;
  statusLabel: string;
  count: number;
};

export type ObjectMailFolderStat = {
  mailFolder?: string;
  mailFolderLabel: string;
  count: number;
};

export type ObjectSourceStat = {
  sourceType?: string;
  totalCount: number;
  statusCounts: ObjectStatusStat[];
  mailFolderCounts: ObjectMailFolderStat[];
};

export type ObjectExtensionStat = {
  extension?: string;
  extensionLabel: string;
  count: number;
};

export type ObjectSizeAnalysis = {
  totalCount: number;
  totalSizeBytes: number;
  extensionCounts: ObjectExtensionStat[];
};

export type ObjectAnalysis = {
  totalCount: number;
  taggedCount: number;
  untaggedCount: number;
  sourceAnalyses: ObjectSourceStat[];
  sizeAnalysis: ObjectSizeAnalysis;
};

export type ObjectQueryState = {
  sourceId: string;
  sourceType: string;
  sourceCode: string;
  status: string;
  mailId: string;
  fingerprint: string;
  routeId: string;
  tagId: string;
  tagCode: string;
  tagValue: string;
};

export type ObjectTagFilter = {
  tagId?: string;
  tagCode?: string;
  tagName?: string;
  tagValue?: string;
  count: number;
};

export type ObjectPage = {
  loading: boolean;
  analysisLoading: boolean;
  redeliverLoading: boolean;
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
  tagFilters: ObjectTagFilter[];
  detailVisible: boolean;
  selectedRow: TransferObjectViewDTO | null;
  openDetailDrawer: (row: TransferObjectViewDTO) => void;
  redeliverObject: (row: TransferObjectViewDTO) => void;
  runQuery: () => void;
  resetQuery: () => void;
  closeDetail: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  applySourceFilter: (sourceType?: string) => void;
  applySourceStatusFilter: (sourceType?: string, status?: string) => void;
  applyTagFilter: (filter: ObjectTagFilter) => void;
  clearTagFilter: () => void;
  formatStatus: (value: string | undefined) => string;
  formatDeliveryStatus: (value: string | undefined) => string;
  formatSourceTypeLabel: (value: string | undefined) => string;
  formatStatusLabel: (value: string | undefined) => string;
  formatTagLabel: (value: string | undefined) => string;
  formatBytes: (value: number | undefined) => string;
  safeJson: (value: unknown) => string;
};
