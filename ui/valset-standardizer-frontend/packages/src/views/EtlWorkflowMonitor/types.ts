import type { Edge, Node } from "@vue-flow/core";
import type { YTablePagination } from "@yss-ui/components";
import type { EtlPlatformType } from "@/api/etlWorkflowConfig";
import type { WorkflowInstanceStatus } from "@/api/etlWorkflowInstance";

export type OptionItem = {
  label: string;
  value: string;
};

export type WorkflowMonitorQueryState = {
  workflowName: string;
  status: string;
  triggerTimeFrom: string;
  triggerTimeTo: string;
};

export type WorkflowMonitorInstanceRow = {
  instanceId: string;
  workflowCode: string;
  workflowVersionNo: number;
  workflowName: string;
  workflowLabel: string;
  platformType: EtlPlatformType | null;
  platformLabel: string;
  businessKey: string;
  status: WorkflowInstanceStatus | string;
  statusLabel: string;
  rawStatus: string;
  currentStageCode: string;
  currentStageName: string;
  triggerTime: string;
  startTime: string;
  duration: string;
  endTime: string;
  message: string;
  stageCount: number;
};

export type WorkflowMonitorStageLogRow = {
  logKey: string;
  stageCode: string;
  stageName: string;
  stageOrder: number;
  status: WorkflowInstanceStatus | string;
  rawStatus: string;
  message: string;
  startTime: string;
  endTime: string;
  payload: Record<string, unknown>;
};

export type WorkflowMonitorTaskRow = {
  taskKey: string;
  id: number | null;
  name: string;
  taskType: string;
  workflowInstanceId: string;
  workflowInstanceName: string;
  state: string;
  stateLabel: string;
  startTime: string;
  endTime: string;
  host: string;
  retryTimes: number | null;
  taskParams: string;
};

export type WorkflowMonitorNodeKind =
  | "trigger"
  | "instance"
  | "stage"
  | "task"
  | "result";

export type WorkflowMonitorNodeData = {
  kind: WorkflowMonitorNodeKind;
  title: string;
  subtitle: string;
  status: string;
  statusLabel: string;
  timeLabel: string;
  meta: string;
  message: string;
  active: boolean;
  failed: boolean;
  muted: boolean;
  payload?: Record<string, unknown>;
  stageLog?: WorkflowMonitorStageLogRow;
  taskRow?: WorkflowMonitorTaskRow;
};

export type WorkflowMonitorNode = Node<WorkflowMonitorNodeData>;
export type WorkflowMonitorEdge = Edge<{ state: string }>;

export type WorkflowMonitorDetail = {
  node: WorkflowMonitorNodeData;
  stageLogs: WorkflowMonitorStageLogRow[];
  taskRows: WorkflowMonitorTaskRow[];
  taskLog: string;
};

export type WorkflowMonitorPage = {
  loading: boolean;
  listLoading: boolean;
  graphLoading: boolean;
  actionLoading: boolean;
  taskLogLoading: boolean;
  autoRefresh: boolean;
  query: WorkflowMonitorQueryState;
  rows: WorkflowMonitorInstanceRow[];
  tableData: WorkflowMonitorInstanceRow[];
  total: number;
  pagination: YTablePagination;
  statusOptions: OptionItem[];
  selectedInstance: WorkflowMonitorInstanceRow | null;
  selectedInstanceId: string;
  selectedStageLogs: WorkflowMonitorStageLogRow[];
  selectedTaskRows: WorkflowMonitorTaskRow[];
  nodes: WorkflowMonitorNode[];
  edges: WorkflowMonitorEdge[];
  detailVisible: boolean;
  selectedDetail: WorkflowMonitorDetail | null;
  graphSummary: string;
  canAutoRefresh: boolean;
  runQuery: () => void;
  resetQuery: () => void;
  refreshList: () => Promise<void>;
  refreshGraph: () => Promise<void>;
  selectInstance: (row: WorkflowMonitorInstanceRow) => Promise<void>;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  openNodeDetail: (node: WorkflowMonitorNodeData) => Promise<void>;
  closeDetail: () => void;
  toggleAutoRefresh: (checked: boolean) => void;
  rerunInstance: () => Promise<void>;
  rerunFailedTasks: () => Promise<void>;
  stopInstance: () => Promise<void>;
  pauseInstance: () => Promise<void>;
  resumeInstance: () => Promise<void>;
  canRerunInstance: () => boolean;
  canRerunFailedTasks: () => boolean;
  canStopInstance: () => boolean;
  canPauseInstance: () => boolean;
  canResumeInstance: () => boolean;
  formatStatusLabel: (value?: string) => string;
  formatJson: (value: unknown) => string;
};
