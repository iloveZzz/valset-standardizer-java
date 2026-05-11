import type { YTablePagination } from "@yss-ui/components";
import type { EtlPlatformType } from "@/api/etlWorkflowConfig";
import type { WorkflowInstanceStatus } from "@/api/etlWorkflowInstance";

export type OptionItem = {
  label: string;
  value: string;
};

export type WorkflowInstanceQueryState = {
  workflowCode: string;
  workflowVersionNo: string;
  platformType: string;
  workflowName: string;
  status: string;
  businessKey: string;
  instanceId: string;
  externalInstanceId: string;
  stageCode: string;
  triggerTimeFrom: string;
  triggerTimeTo: string;
};

export type WorkflowInstanceStateStatRow = {
  statKey: string;
  label: string;
  value: number;
  desc: string;
  tone: "primary" | "success" | "processing" | "warning" | "danger" | "default";
};

export type WorkflowStageLogRow = {
  logKey: string;
  stageCode: string;
  stageName: string;
  stageOrder: number;
  status: WorkflowStageLogStatus;
  rawStatus: string;
  message: string;
  startTime: string;
  endTime: string;
  payload: Record<string, unknown>;
};

export type WorkflowStageLogStatus = WorkflowInstanceStatus | string;

export type WorkflowTaskRow = {
  taskKey: string;
  id: number | null;
  name: string;
  taskType: string;
  workflowInstanceId: string;
  workflowInstanceName: string;
  projectCode: number | null;
  taskCode: number | null;
  taskDefinitionVersion: number | null;
  processDefinitionName: string;
  taskGroupPriority: number | null;
  state: string;
  stateLabel: string;
  firstSubmitTime: string;
  submitTime: string;
  startTime: string;
  endTime: string;
  host: string;
  executePath: string;
  retryTimes: number | null;
  alertFlag: string;
  appLink: string;
  flag: string;
  duration: number | null;
  maxRetryTimes: number | null;
  retryInterval: number | null;
  taskInstancePriority: string;
  workflowInstancePriority: string;
  workerGroup: string;
  environmentCode: number | null;
  executorId: number | null;
  executorName: string;
  delayTime: number | null;
  taskParams: string;
  dryRun: number | null;
  taskGroupId: number | null;
  cpuQuota: number | null;
  memoryMax: number | null;
  taskExecuteType: string;
};

export type WorkflowInstanceRow = {
  rowKey?: string;
  instanceId: string;
  workflowCode: string;
  workflowVersionNo: number;
  workflowName: string;
  workflowLabel: string;
  platformType: EtlPlatformType | null;
  platformLabel: string;
  businessKey: string;
  externalInstanceId: string;
  externalWorkflowId: string;
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
  taskRows: WorkflowTaskRow[];
  taskRowsLoaded: boolean;
};

export type WorkflowInstanceDetailRow = WorkflowInstanceRow & {
  context: Record<string, unknown>;
  stageLogs: WorkflowStageLogRow[];
};

export type WorkflowInstancePage = {
  loading: boolean;
  listLoading: boolean;
  detailLoading: boolean;
  actionLoading: boolean;
  rows: WorkflowInstanceRow[];
  tableData: WorkflowInstanceRow[];
  total: number;
  pagination: YTablePagination;
  query: WorkflowInstanceQueryState;
  currentFilterSummary: string;
  selectedWorkflowDescription: string;
  runningCount: number;
  succeededCount: number;
  failedCount: number;
  submittedCount: number;
  workflowStateLoading: boolean;
  workflowStateCards: WorkflowInstanceStateStatRow[];
  workflowOptions: OptionItem[];
  stageOptions: OptionItem[];
  platformOptions: OptionItem[];
  statusOptions: OptionItem[];
  detailVisible: boolean;
  selectedRow: WorkflowInstanceDetailRow | null;
  selectedLogs: WorkflowStageLogRow[];
  selectedLogStageCode: string;
  expandedRowKeys: string[];
  allRowsExpanded: boolean;
  taskLogVisible: boolean;
  taskLogLoading: boolean;
  taskLogTitle: string;
  taskLogContent: string;
  runQuery: () => void;
  resetQuery: () => void;
  refreshList: () => Promise<void>;
  handleWorkflowCodeChange: (workflowCode: unknown) => void;
  handleWorkflowCodeSelect: (workflowCode: unknown) => void;
  handleStatusSelect: (status: unknown) => void;
  applyDefaultWorkflowSelection: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleToggleRowExpand: (params: {
    row?: WorkflowInstanceRow;
    expanded?: boolean;
  }) => void;
  expandAllRows: () => void;
  collapseAllRows: () => void;
  openDetailDrawer: (row: WorkflowInstanceRow) => Promise<void>;
  closeDetailDrawer: () => void;
  refreshDetailLogs: (stageCode?: string) => Promise<void>;
  getTaskRows: (row: WorkflowInstanceRow) => WorkflowTaskRow[];
  openTaskLog: (row: WorkflowInstanceRow, taskRow: WorkflowTaskRow) => Promise<void>;
  openTaskInstancePage: (
    row: WorkflowInstanceRow,
    taskRow: WorkflowTaskRow,
  ) => void;
  closeTaskLog: () => void;
  triggerWorkflow: (row: WorkflowInstanceRow) => Promise<void>;
  rerunInstance: (row: WorkflowInstanceRow) => Promise<void>;
  rerunFailedTasks: (row: WorkflowInstanceRow) => Promise<void>;
  retryInstance: (row: WorkflowInstanceRow) => Promise<void>;
  stopInstance: (row: WorkflowInstanceRow) => Promise<void>;
  pauseInstance: (row: WorkflowInstanceRow) => Promise<void>;
  resumeInstance: (row: WorkflowInstanceRow) => Promise<void>;
  canRerunInstance: (row: WorkflowInstanceRow) => boolean;
  canRerunFailedTasks: (row: WorkflowInstanceRow) => boolean;
  canStopInstance: (row: WorkflowInstanceRow) => boolean;
  canPauseInstance: (row: WorkflowInstanceRow) => boolean;
  canResumeInstance: (row: WorkflowInstanceRow) => boolean;
  formatStatusLabel: (value?: string) => string;
  formatPlatformLabel: (value?: string | null) => string;
  formatDateTime: (value?: string) => string;
  formatJson: (value: unknown) => string;
};
