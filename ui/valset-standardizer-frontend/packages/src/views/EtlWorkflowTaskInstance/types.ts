import type { YTablePagination } from "@yss-ui/components";
import type {
  WorkflowDefinitionDTO,
  WorkflowStageDTO,
} from "@/api/etlWorkflowConfig";
import type {
  WorkflowTaskInstanceDTO,
  WorkflowTaskInstanceStatus,
} from "@/api/etlWorkflowTaskInstance";

export type OptionItem = {
  label: string;
  value: string;
};

export type WorkflowTaskInstanceQueryState = {
  workflowCode: string;
  workflowVersionNo: string;
  taskName: string;
  workflowInstanceName: string;
  status: string;
  startTimeFrom: string;
  endTimeTo: string;
};

export type WorkflowTaskStageRow = WorkflowStageDTO & {
  stageLabel: string;
};

export type WorkflowTaskStateStatRow = {
  statKey: string;
  label: string;
  value: number;
  desc: string;
  tone: "primary" | "success" | "processing" | "warning" | "danger" | "default";
};

export type WorkflowTaskInstanceRow = WorkflowTaskInstanceDTO & {
  taskKey: string;
  taskInstanceId: number | null;
  taskName: string;
  taskTypeLabel: string;
  statusLabel: string;
  statusColor: string;
  workflowInstanceLabel: string;
  workerGroupLabel: string;
  hostLabel: string;
  startTimeLabel: string;
  endTimeLabel: string;
  durationLabel: string;
};

export type WorkflowTaskInstancePage = {
  loading: boolean;
  listLoading: boolean;
  actionLoading: boolean;
  rows: WorkflowTaskInstanceRow[];
  tableData: WorkflowTaskInstanceRow[];
  total: number;
  pagination: YTablePagination;
  query: WorkflowTaskInstanceQueryState;
  definitions: WorkflowDefinitionDTO[];
  workflowOptions: OptionItem[];
  selectedDefinition: WorkflowDefinitionDTO | null;
  stageRows: WorkflowTaskStageRow[];
  workflowInstanceState: string;
  taskStateLoading: boolean;
  taskStateCards: WorkflowTaskStateStatRow[];
  taskStateSummary: string;
  currentFilterSummary: string;
  selectedWorkflowLabel: string;
  selectedWorkflowDescription: string;
  logVisible: boolean;
  logLoading: boolean;
  logTitle: string;
  logContent: string;
  runQuery: () => void;
  resetQuery: () => void;
  refreshList: () => Promise<void>;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleWorkflowCodeSelect: (workflowCode: unknown) => void;
  handleStatusSelect: (status: unknown) => void;
  applyDefaultWorkflowSelection: () => void;
  syncRouteQueryFilters: () => void;
  selectStageName: (stageName: string) => void;
  openTaskLog: (row: WorkflowTaskInstanceRow) => Promise<void>;
  closeTaskLog: () => void;
  forceSuccessTask: (row: WorkflowTaskInstanceRow) => Promise<void>;
  formatStatusLabel: (value?: WorkflowTaskInstanceStatus | string) => string;
  formatStatusColor: (value?: WorkflowTaskInstanceStatus | string) => string;
  formatDateTime: (value?: string) => string;
  formatDuration: (value?: number | null) => string;
};
