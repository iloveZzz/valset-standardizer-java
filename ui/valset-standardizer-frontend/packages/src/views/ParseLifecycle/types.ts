import type { ParseLifecycleEventDTO } from "@/services/parseLifecycleEventSse";

export type ParseLifecycleQueryState = {
  source: string;
  queueId: string;
  transferId: string;
  taskId: string;
  stage: string;
  onlyError: boolean;
  keyword: string;
};

export type ParseLifecycleStatus = "RUNNING" | "SUCCESS" | "FAILED" | "IDLE";

export type ParseLifecycleEventRow = ParseLifecycleEventDTO & {
  displayTime: string;
  stageLabel: string;
  attributesText: string;
  identity: string;
  eventKey: string;
  repeatCount: number;
  firstSeenAt: string;
  lastSeenAt: string;
  severity: ParseLifecycleStatus;
  observerEvent: boolean;
};

export type ParseLifecycleTaskRow = {
  identity: string;
  queueId: string;
  transferId: string;
  taskId: string;
  businessKey: string;
  title: string;
  currentStage: string;
  currentStageLabel: string;
  status: ParseLifecycleStatus;
  statusLabel: string;
  statusColor: string;
  message: string;
  errorMessage: string;
  startedAt: string;
  updatedAt: string;
  durationText: string;
  eventCount: number;
  rawEvents: ParseLifecycleEventRow[];
};

export type ParseLifecyclePage = {
  connected: boolean;
  connecting: boolean;
  total: number;
  rawTotal: number;
  taskTotal: number;
  activeTaskCount: number;
  successTaskCount: number;
  failedTaskCount: number;
  observerStatus: string;
  latestStage: string;
  latestMessage: string;
  query: ParseLifecycleQueryState;
  rows: ParseLifecycleEventRow[];
  tableData: ParseLifecycleTaskRow[];
  rawTableData: ParseLifecycleEventRow[];
  detailVisible: boolean;
  rawDetailVisible: boolean;
  selectedRow: ParseLifecycleTaskRow | null;
  selectedEvent: ParseLifecycleEventRow | null;
  paused: boolean;
  showRawEvents: boolean;
  autoScroll: boolean;
  connect: () => void;
  disconnect: () => void;
  clear: () => void;
  togglePause: () => void;
  toggleRawEvents: () => void;
  toggleAutoScroll: () => void;
  openDetailDrawer: (row: ParseLifecycleTaskRow) => void;
  openRawDetailDrawer: (row: ParseLifecycleEventRow) => void;
  openQueuePage: () => void;
  closeDetail: () => void;
  closeRawDetail: () => void;
  formatStage: (value: string | undefined) => string;
};
