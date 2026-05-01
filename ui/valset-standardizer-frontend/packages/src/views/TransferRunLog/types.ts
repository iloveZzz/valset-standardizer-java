import type { YTablePagination } from "@yss-ui/components";
import type { TransferRunLogViewDTO } from "@/api/generated/valset/schemas";

export type RunLogStatusStat = {
  runStatus?: string;
  statusLabel: string;
  count: number;
};

export type RunLogStageStat = {
  runStage?: string;
  stageLabel: string;
  totalCount: number;
  statusCounts: RunLogStatusStat[];
};

export type RunLogAnalysis = {
  totalCount: number;
  sourceCount: number;
  routeCount: number;
  targetCount: number;
  stageAnalyses: RunLogStageStat[];
};

export type RunLogConsoleSeedItem = {
  key: string;
  title: string;
  stageLabel: string;
  statusLabel: string;
  createdAt?: string;
  description: string;
};

export type RunLogQueryState = {
  sourceId: string;
  transferId: string;
  routeId: string;
  runStage: string;
  runStatus: string;
  triggerType: string;
  keyword: string;
};

export type RunLogCleanupCommand = {
  startInclusive: string;
  endExclusive: string;
  cleanupLabel: string;
};

export type RunLogPage = {
  loading: boolean;
  analysisLoading: boolean;
  cleanupLoading: boolean;
  rows: TransferRunLogViewDTO[];
  total: number;
  pagination: YTablePagination;
  query: RunLogQueryState;
  selectedRow: TransferRunLogViewDTO | null;
  detailVisible: boolean;
  analysis: RunLogAnalysis;
  consoleItems: RunLogConsoleSeedItem[];
  openDetailDrawer: (row: TransferRunLogViewDTO) => void;
  runQuery: () => void;
  resetQuery: () => void;
  cleanupLogs: (command: RunLogCleanupCommand) => Promise<void>;
  closeDetail: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  applyStageFilter: (runStage?: string) => void;
  applyStageStatusFilter: (runStage?: string, runStatus?: string) => void;
  formatText: (value: unknown) => string;
  formatStageLabel: (value: string | undefined) => string;
  formatStatus: (value: string | undefined) => string;
  formatStatusLabel: (value: string | undefined) => string;
  getStatusChipClass: (value?: string) => string;
  runStatusTagColor: (value?: string) => string;
  safeJson: (value: unknown) => string;
};
