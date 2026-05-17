import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import dayjs from "dayjs";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import {
  getEtlWorkflowInstance,
  getEtlWorkflowInstanceTaskLog,
  listEtlWorkflowInstanceLogs,
  listEtlWorkflowInstanceTasks,
  listEtlWorkflowInstances,
  pauseEtlWorkflowInstance,
  resumeEtlWorkflowInstance,
  stopEtlWorkflowInstance,
  triggerEtlWorkflowInstance,
  type WorkflowInstanceDTO,
  type WorkflowInstanceQueryParams,
  type WorkflowInstanceStatus,
  type WorkflowInstanceViewDTO,
  type WorkflowStageLogDTO,
  type WorkflowTaskInstanceDTO,
  type WorkflowTriggerMode,
} from "@/api/etlWorkflowInstance";
import { unwrapMultiResult, unwrapSingleResult } from "@/utils/api-response";
import type {
  OptionItem,
  WorkflowMonitorDetail,
  WorkflowMonitorEdge,
  WorkflowMonitorInstanceRow,
  WorkflowMonitorNode,
  WorkflowMonitorNodeData,
  WorkflowMonitorPage,
  WorkflowMonitorQueryState,
  WorkflowMonitorStageLogRow,
  WorkflowMonitorTaskRow,
} from "../types";

const TIME_RANGE_FORMAT = "YYYY-MM-DD HH:mm:ss";
const AUTO_REFRESH_INTERVAL = 5000;

const STATUS_OPTIONS: OptionItem[] = [
  { value: "SUBMITTED", label: "已提交" },
  { value: "RUNNING", label: "运行中" },
  { value: "SUCCEEDED", label: "成功" },
  { value: "FAILED", label: "失败" },
  { value: "STOPPED", label: "已停止" },
  { value: "RETRYING", label: "重试中" },
  { value: "UNKNOWN", label: "未知" },
];

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "草稿",
  READY: "就绪",
  SUBMITTED: "已提交",
  RUNNING: "运行中",
  SUCCEEDED: "成功",
  FAILED: "失败",
  STOPPED: "已停止",
  RETRYING: "重试中",
  UNKNOWN: "未知",
};

const STATUS_ALIASES: Record<string, WorkflowInstanceStatus> = {
  SUBMITTED_SUCCESS: "SUBMITTED",
  RUNNING_EXECUTION: "RUNNING",
  SUCCESS: "SUCCEEDED",
  FAILURE: "FAILED",
  READY_PAUSE: "STOPPED",
  READY_STOP: "STOPPED",
  STOP: "STOPPED",
  PAUSE: "STOPPED",
  CANCELED: "STOPPED",
  CANCELLED: "STOPPED",
  TERMINATED: "STOPPED",
  RETRY: "RETRYING",
};

const TASK_STATE_LABELS: Record<string, string> = {
  SUCCESS: "成功",
  FAILED: "失败",
  RUNNING: "运行中",
  RUNNING_EXECUTION: "运行中",
  SUBMITTED_SUCCESS: "已提交",
  READY: "就绪",
  STOPPED: "已停止",
  KILL: "已终止",
  WAITTING: "等待中",
  WAITING: "等待中",
  DELAY_EXECUTION: "延迟执行",
  NEED_FAULT_TOLERANCE: "容错中",
  PAUSE: "暂停",
  FAILURE: "失败",
  SUBMITTED: "已提交",
  RETRYING: "重试中",
};

const buildDefaultTriggerTimeRange = (): [string, string] => [
  dayjs().startOf("day").format(TIME_RANGE_FORMAT),
  dayjs().add(1, "day").startOf("day").format(TIME_RANGE_FORMAT),
];

const defaultQuery = (): WorkflowMonitorQueryState => {
  const [triggerTimeFrom, triggerTimeTo] = buildDefaultTriggerTimeRange();
  return {
    workflowName: "",
    status: "",
    triggerTimeFrom,
    triggerTimeTo,
  };
};

const normalizeStatus = (
  value?: string | null,
): WorkflowInstanceStatus | string => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!status) {
    return "UNKNOWN";
  }
  return STATUS_ALIASES[status] || status;
};

const isActiveStatus = (value?: string | null) =>
  ["SUBMITTED", "RUNNING", "RETRYING"].includes(normalizeStatus(value));

const isSucceededStatus = (value?: string | null) =>
  normalizeStatus(value) === "SUCCEEDED";

const isFailedStatus = (value?: string | null) =>
  normalizeStatus(value) === "FAILED";

const isStoppedStatus = (value?: string | null) =>
  normalizeStatus(value) === "STOPPED";

const formatText = (value?: string | null) => {
  const text = String(value ?? "").trim();
  return text || "-";
};

const formatDateTime = (value?: string | null) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return "-";
  }
  return text
    .replace("T", " ")
    .replace(/\.\d+$/, "")
    .slice(0, 19);
};

const formatDuration = (value?: string | null) => {
  const text = String(value ?? "").trim();
  return text || "-";
};

const formatJson = (value: unknown) => {
  if (value === null || value === undefined || value === "") {
    return "{}";
  }
  if (typeof value === "string") {
    return value;
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return "{}";
  }
};

const formatStatusLabel = (value?: string) =>
  STATUS_LABELS[normalizeStatus(value)] || formatText(value);

const mapStageLog = (log: WorkflowStageLogDTO): WorkflowMonitorStageLogRow => {
  const stageCode = formatText(log.stageCode);
  const stageOrder = Number(log.stageOrder ?? 0);
  const startTime = formatDateTime(log.startTime);
  const endTime = formatDateTime(log.endTime);
  const rawStatus = formatText(log.rawStatus);
  return {
    logKey: [stageCode, stageOrder, startTime, endTime, rawStatus].join("|"),
    stageCode,
    stageName: formatText(log.stageName || log.stageCode),
    stageOrder,
    status: normalizeStatus(log.status || log.rawStatus),
    rawStatus,
    message: formatText(log.message),
    startTime,
    endTime,
    payload: (log.payload ?? {}) as Record<string, unknown>,
  };
};

const mapTaskRow = (task: WorkflowTaskInstanceDTO): WorkflowMonitorTaskRow => {
  const state = formatText(task.state);
  const taskType = String(task.taskType ?? "").trim();
  return {
    taskKey: [
      task.id ?? task.taskCode ?? task.name ?? task.taskParams ?? "-",
      task.startTime ?? "",
      task.endTime ?? "",
    ].join("|"),
    id: task.id ?? null,
    name: formatText(task.name),
    taskType: taskType.toUpperCase() === "SUB_WORKFLOW" ? "阶段任务" : "任务",
    workflowInstanceId: formatText(task.workflowInstanceId),
    workflowInstanceName: formatText(task.workflowInstanceName),
    state,
    stateLabel: TASK_STATE_LABELS[state.toUpperCase()] || state,
    startTime: formatDateTime(task.startTime),
    endTime: formatDateTime(task.endTime),
    host: formatText(task.host),
    retryTimes: task.retryTimes ?? null,
    taskParams: formatText(task.taskParams),
  };
};

const mapInstanceRow = (
  item: WorkflowInstanceViewDTO | WorkflowInstanceDTO,
): WorkflowMonitorInstanceRow => {
  const workflowCode = formatText(item.workflowCode);
  const workflowVersionNo = Number(item.workflowVersionNo ?? 0);
  const workflowName = formatText(item.workflowName || item.workflowCode);
  const status = normalizeStatus(item.status || item.rawStatus);
  return {
    instanceId: formatText(item.instanceId),
    workflowCode,
    workflowVersionNo,
    workflowName,
    workflowLabel: `${workflowName} · ${workflowCode} v${workflowVersionNo}`,
    platformType: item.platformType ?? null,
    platformLabel: item.platformType || "-",
    businessKey: formatText(item.businessKey),
    status,
    statusLabel: formatStatusLabel(status),
    rawStatus: formatText(item.rawStatus),
    currentStageCode: formatText(item.currentStageCode),
    currentStageName: formatText(item.currentStageName || item.currentStageCode),
    triggerTime: formatDateTime(item.triggerTime),
    startTime: formatDateTime(item.startTime),
    duration: formatDuration(item.duration),
    endTime: formatDateTime(item.endTime),
    message: formatText(item.message),
    stageCount: Number(item.stageCount ?? 0),
  };
};

const sortStageRows = (stageRows: WorkflowMonitorStageLogRow[]) =>
  stageRows.slice().sort((left, right) => {
    if (left.stageOrder !== right.stageOrder) {
      return left.stageOrder - right.stageOrder;
    }
    return String(left.startTime).localeCompare(String(right.startTime));
  });

const sortTaskRows = (taskRows: WorkflowMonitorTaskRow[]) =>
  taskRows.slice().sort((left, right) => {
    const leftId = left.id ?? Number.MAX_SAFE_INTEGER;
    const rightId = right.id ?? Number.MAX_SAFE_INTEGER;
    if (leftId !== rightId) {
      return leftId - rightId;
    }
    return String(left.startTime).localeCompare(String(right.startTime));
  });

const nodeData = (
  data: Partial<WorkflowMonitorNodeData> & {
    kind: WorkflowMonitorNodeData["kind"];
    title: string;
  },
): WorkflowMonitorNodeData => {
  const status = normalizeStatus(data.status);
  return {
    kind: data.kind,
    title: data.title,
    subtitle: data.subtitle ?? "",
    status,
    statusLabel: data.statusLabel ?? formatStatusLabel(status),
    timeLabel: data.timeLabel ?? "-",
    meta: data.meta ?? "",
    message: data.message ?? "",
    active: data.active ?? isActiveStatus(status),
    failed: data.failed ?? isFailedStatus(status),
    muted: data.muted ?? false,
    payload: data.payload,
    stageLog: data.stageLog,
    taskRow: data.taskRow,
  };
};

const createEdge = (
  source: string,
  target: string,
  state: string,
): WorkflowMonitorEdge => {
  const status = normalizeStatus(state);
  const failed = isFailedStatus(status);
  const active = isActiveStatus(status);
  const success = isSucceededStatus(status);
  return {
    id: `${source}-${target}`,
    source,
    target,
    type: "smoothstep",
    animated: active,
    class: {
      "workflow-monitor-edge": true,
      "workflow-monitor-edge--active": active,
      "workflow-monitor-edge--success": success,
      "workflow-monitor-edge--failed": failed,
      "workflow-monitor-edge--muted": !active && !success && !failed,
    },
    data: { state: status },
  };
};

const buildGraph = (
  instance: WorkflowMonitorInstanceRow | null,
  stageRows: WorkflowMonitorStageLogRow[],
  taskRows: WorkflowMonitorTaskRow[],
): { nodes: WorkflowMonitorNode[]; edges: WorkflowMonitorEdge[] } => {
  if (!instance) {
    return { nodes: [], edges: [] };
  }

  const nextNodes: WorkflowMonitorNode[] = [];
  const nextEdges: WorkflowMonitorEdge[] = [];
  const stageList = sortStageRows(stageRows);
  const taskList = sortTaskRows(taskRows);
  const stages = stageList.length
    ? stageList
    : [
        {
          logKey: "stage-placeholder",
          stageCode: instance.currentStageCode,
          stageName: instance.currentStageName,
          stageOrder: 1,
          status: instance.status,
          rawStatus: instance.rawStatus,
          message: instance.message,
          startTime: instance.startTime,
          endTime: instance.endTime,
          payload: {},
        },
      ];

  nextNodes.push({
    id: "trigger",
    type: "monitor",
    position: { x: 20, y: 160 },
    data: nodeData({
      kind: "trigger",
      title: "触发入口",
      subtitle: instance.businessKey,
      status: "SUCCEEDED",
      timeLabel: instance.triggerTime,
      meta: "业务键 / 触发时间",
      message: instance.message,
    }),
  });
  nextNodes.push({
    id: "instance",
    type: "monitor",
    position: { x: 300, y: 160 },
    data: nodeData({
      kind: "instance",
      title: instance.workflowName,
      subtitle: instance.instanceId,
      status: instance.status,
      statusLabel: instance.statusLabel,
      timeLabel: instance.startTime,
      meta: `${instance.workflowCode} v${instance.workflowVersionNo}`,
      message: instance.message,
    }),
  });
  nextEdges.push(createEdge("trigger", "instance", instance.status));

  stages.forEach((stage, index) => {
    const nodeId = `stage-${index}`;
    const y = 40 + index * 148;
    nextNodes.push({
      id: nodeId,
      type: "monitor",
      position: { x: 620, y },
      data: nodeData({
        kind: "stage",
        title: stage.stageName,
        subtitle: stage.stageCode,
        status: String(stage.status),
        timeLabel: stage.startTime,
        meta: `阶段顺序 ${stage.stageOrder || index + 1}`,
        message: stage.message,
        payload: stage.payload,
        stageLog: stage,
      }),
    });
    nextEdges.push(
      createEdge(index === 0 ? "instance" : `stage-${index - 1}`, nodeId, String(stage.status)),
    );
  });

  const taskStartY = Math.max(20, 84 - Math.min(taskList.length, 4) * 16);
  taskList.forEach((task, index) => {
    const nodeId = `task-${index}`;
    const relatedStageIndex = Math.max(
      stages.findIndex(
        (stage) =>
          task.workflowInstanceName.includes(stage.stageName) ||
          task.workflowInstanceName.includes(stage.stageCode),
      ),
      0,
    );
    const sourceStageId = `stage-${Math.min(relatedStageIndex, stages.length - 1)}`;
    nextNodes.push({
      id: nodeId,
      type: "monitor",
      position: { x: 940, y: taskStartY + index * 118 },
      data: nodeData({
        kind: "task",
        title: task.name,
        subtitle: task.workflowInstanceName,
        status: task.state,
        statusLabel: task.stateLabel,
        timeLabel: task.startTime,
        meta: `${task.taskType} · ${task.host}`,
        message: task.retryTimes == null ? "" : `重试次数 ${task.retryTimes}`,
        taskRow: task,
      }),
    });
    nextEdges.push(createEdge(sourceStageId, nodeId, task.state));
  });

  const resultStatus = isFailedStatus(instance.status)
    ? "FAILED"
    : isSucceededStatus(instance.status)
      ? "SUCCEEDED"
      : isStoppedStatus(instance.status)
        ? "STOPPED"
        : "RUNNING";
  nextNodes.push({
    id: "result",
    type: "monitor",
    position: { x: taskList.length ? 1260 : 940, y: 160 },
    data: nodeData({
      kind: "result",
      title: "输出结果",
      subtitle: instance.endTime,
      status: resultStatus,
      timeLabel: instance.duration,
      meta: taskList.length ? `任务 ${taskList.length} 个` : `阶段 ${stages.length} 个`,
      message: instance.message,
      muted: !isSucceededStatus(resultStatus) && !isFailedStatus(resultStatus),
    }),
  });
  const lastSources = taskList.length
    ? taskList.map((_, index) => `task-${index}`)
    : [`stage-${stages.length - 1}`];
  lastSources.forEach((source) => {
    nextEdges.push(createEdge(source, "result", resultStatus));
  });

  return { nodes: nextNodes, edges: nextEdges };
};

export const useWorkflowMonitorPage = (): { page: WorkflowMonitorPage } => {
  const query = reactive<WorkflowMonitorQueryState>(defaultQuery());
  const rows = ref<WorkflowMonitorInstanceRow[]>([]);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50"],
  });
  const loading = ref(false);
  const listLoading = ref(false);
  const graphLoading = ref(false);
  const actionLoading = ref(false);
  const taskLogLoading = ref(false);
  const autoRefresh = ref(false);
  const selectedInstance = ref<WorkflowMonitorInstanceRow | null>(null);
  const selectedStageLogs = ref<WorkflowMonitorStageLogRow[]>([]);
  const selectedTaskRows = ref<WorkflowMonitorTaskRow[]>([]);
  const nodes = ref<WorkflowMonitorNode[]>([]);
  const edges = ref<WorkflowMonitorEdge[]>([]);
  const detailVisible = ref(false);
  const selectedDetail = ref<WorkflowMonitorDetail | null>(null);
  let listRequestId = 0;
  let graphRequestId = 0;
  let refreshTimer: number | null = null;

  const tableData = computed(() => rows.value);
  const total = computed(() => Number(pagination.value.total ?? 0));
  const selectedInstanceId = computed(() => selectedInstance.value?.instanceId ?? "");
  const canAutoRefresh = computed(() => isActiveStatus(selectedInstance.value?.status));
  const graphSummary = computed(() => {
    if (!selectedInstance.value) {
      return "请选择一个工作流实例查看全链路拓扑。";
    }
    return `${selectedInstance.value.workflowLabel} · 阶段 ${selectedStageLogs.value.length} 个 · 任务 ${selectedTaskRows.value.length} 个`;
  });

  const loadList = async ({ silent = false }: { silent?: boolean } = {}) => {
    const requestId = ++listRequestId;
    if (!silent) {
      listLoading.value = true;
    }
    try {
      const pageIndex = Math.max(Number(pagination.value.current ?? 1) - 1, 0);
      const pageSize = Number(pagination.value.pageSize ?? 10);
      const params: WorkflowInstanceQueryParams = {
        workflowName: query.workflowName || undefined,
        status: query.status || undefined,
        triggerTimeFrom: query.triggerTimeFrom || undefined,
        triggerTimeTo: query.triggerTimeTo || undefined,
        pageIndex,
        pageSize,
      };
      const res = await listEtlWorkflowInstances(params);
      if (requestId !== listRequestId) {
        return;
      }
      rows.value = (res.data ?? []).map(mapInstanceRow);
      pagination.value.total = Number(res.totalCount ?? 0);
      pagination.value.current = Number(res.pageIndex ?? pageIndex) + 1;
      pagination.value.pageSize = Number(res.pageSize ?? pageSize);
      if (selectedInstance.value) {
        const next = rows.value.find(
          (row) => row.instanceId === selectedInstance.value?.instanceId,
        );
        if (next) {
          selectedInstance.value = next;
        }
      }
    } catch {
      message.error("加载工作流实例列表失败");
    } finally {
      if (!silent && requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const loadGraph = async (
    instance: WorkflowMonitorInstanceRow,
    { silent = false }: { silent?: boolean } = {},
  ) => {
    const requestId = ++graphRequestId;
    if (!silent) {
      graphLoading.value = true;
    }
    try {
      const [detailRes, logsRes, tasksRes] = await Promise.all([
        getEtlWorkflowInstance(instance.instanceId),
        listEtlWorkflowInstanceLogs(instance.instanceId),
        listEtlWorkflowInstanceTasks(instance.instanceId),
      ]);
      if (requestId !== graphRequestId) {
        return;
      }
      const detail = unwrapSingleResult(detailRes);
      const instanceRow = detail ? mapInstanceRow(detail) : instance;
      selectedInstance.value = {
        ...instance,
        ...instanceRow,
      };
      selectedStageLogs.value = (
        detail?.stageLogs?.length
          ? detail.stageLogs
          : unwrapMultiResult(logsRes)
      ).map(mapStageLog);
      selectedTaskRows.value = sortTaskRows(
        (unwrapSingleResult(tasksRes)?.taskList ?? []).map(mapTaskRow),
      );
      const graph = buildGraph(
        selectedInstance.value,
        selectedStageLogs.value,
        selectedTaskRows.value,
      );
      nodes.value = graph.nodes;
      edges.value = graph.edges;
      if (!canAutoRefresh.value) {
        autoRefresh.value = false;
      }
    } catch {
      message.error("加载工作流全链路失败，已保留当前拓扑");
    } finally {
      if (!silent && requestId === graphRequestId) {
        graphLoading.value = false;
      }
    }
  };

  const selectInstance = async (row: WorkflowMonitorInstanceRow) => {
    selectedInstance.value = row;
    selectedStageLogs.value = [];
    selectedTaskRows.value = [];
    nodes.value = [];
    edges.value = [];
    detailVisible.value = false;
    selectedDetail.value = null;
    await loadGraph(row);
  };

  const refreshGraph = async () => {
    if (!selectedInstance.value) {
      message.info("请先选择一个工作流实例");
      return;
    }
    await Promise.allSettled([
      loadGraph(selectedInstance.value),
      loadList({ silent: true }),
    ]);
  };

  const refreshList = async () => {
    await loadList();
  };

  const runQuery = () => {
    pagination.value.current = 1;
    void loadList();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    pagination.value.current = 1;
    void loadList();
  };

  const handlePageChange = ({
    current,
    pageSize,
  }: {
    current: number;
    pageSize: number;
  }) => {
    pagination.value.current = current;
    pagination.value.pageSize = pageSize;
    void loadList();
  };

  const openNodeDetail = async (node: WorkflowMonitorNodeData) => {
    let taskLog = "";
    selectedDetail.value = {
      node,
      stageLogs: selectedStageLogs.value,
      taskRows: selectedTaskRows.value,
      taskLog,
    };
    detailVisible.value = true;
    if (!selectedInstance.value || node.kind !== "task" || !node.taskRow?.id) {
      return;
    }
    taskLogLoading.value = true;
    try {
      const res = await getEtlWorkflowInstanceTaskLog(
        selectedInstance.value.instanceId,
        node.taskRow.id,
      );
      taskLog = unwrapSingleResult(res) || "暂无日志";
      selectedDetail.value = {
        node,
        stageLogs: selectedStageLogs.value,
        taskRows: selectedTaskRows.value,
        taskLog,
      };
    } catch {
      selectedDetail.value = {
        node,
        stageLogs: selectedStageLogs.value,
        taskRows: selectedTaskRows.value,
        taskLog: "加载任务日志失败",
      };
      message.error("加载任务日志失败");
    } finally {
      taskLogLoading.value = false;
    }
  };

  const closeDetail = () => {
    detailVisible.value = false;
    selectedDetail.value = null;
  };

  const submitTrigger = async (
    triggerMode: WorkflowTriggerMode,
    successMessage: string,
  ) => {
    const row = selectedInstance.value;
    if (!row) {
      return;
    }
    actionLoading.value = true;
    try {
      await triggerEtlWorkflowInstance({
        workflowCode: row.workflowCode,
        workflowVersionNo: row.workflowVersionNo,
        businessKey: row.businessKey === "-" ? undefined : row.businessKey,
        stageCode: row.currentStageCode === "-" ? undefined : row.currentStageCode,
        force: true,
        triggerMode,
      });
      await refreshGraph();
      message.success(successMessage);
    } finally {
      actionLoading.value = false;
    }
  };

  const rerunInstance = () => submitTrigger("START_PROCESS", "已提交重跑");

  const rerunFailedTasks = () =>
    submitTrigger("START_FAILURE_TASK_PROCESS", "已提交失败任务重跑");

  const stopInstance = async () => {
    const row = selectedInstance.value;
    if (!row) {
      return;
    }
    actionLoading.value = true;
    try {
      await stopEtlWorkflowInstance(row.instanceId, { reason: "人工停止" });
      await refreshGraph();
      message.success("已停止工作流实例");
    } finally {
      actionLoading.value = false;
    }
  };

  const pauseInstance = async () => {
    const row = selectedInstance.value;
    if (!row) {
      return;
    }
    actionLoading.value = true;
    try {
      await pauseEtlWorkflowInstance(row.instanceId, { reason: "人工暂停" });
      await refreshGraph();
      message.success("已暂停工作流实例");
    } finally {
      actionLoading.value = false;
    }
  };

  const resumeInstance = async () => {
    const row = selectedInstance.value;
    if (!row) {
      return;
    }
    actionLoading.value = true;
    try {
      await resumeEtlWorkflowInstance(row.instanceId, { reason: "恢复运行" });
      await refreshGraph();
      message.success("已恢复运行工作流实例");
    } finally {
      actionLoading.value = false;
    }
  };

  const canRerunInstance = () =>
    Boolean(
      selectedInstance.value &&
        (isSucceededStatus(selectedInstance.value.status) ||
          isStoppedStatus(selectedInstance.value.status)),
    );

  const canRerunFailedTasks = () =>
    Boolean(selectedInstance.value && isFailedStatus(selectedInstance.value.status));

  const canStopInstance = () =>
    Boolean(selectedInstance.value && isActiveStatus(selectedInstance.value.status));

  const canPauseInstance = () =>
    Boolean(selectedInstance.value && isActiveStatus(selectedInstance.value.status));

  const canResumeInstance = () =>
    Boolean(
      selectedInstance.value &&
        isStoppedStatus(selectedInstance.value.status) &&
        selectedInstance.value.platformType === "DOLPHIN_SCHEDULER",
    );

  const clearRefreshTimer = () => {
    if (refreshTimer) {
      window.clearInterval(refreshTimer);
      refreshTimer = null;
    }
  };

  const toggleAutoRefresh = (checked: boolean) => {
    autoRefresh.value = checked;
  };

  watch(
    [autoRefresh, canAutoRefresh, selectedInstanceId],
    ([enabled, refreshable]) => {
      clearRefreshTimer();
      if (!enabled || !refreshable) {
        return;
      }
      refreshTimer = window.setInterval(() => {
        if (selectedInstance.value) {
          void loadGraph(selectedInstance.value, { silent: true });
          void loadList({ silent: true });
        }
      }, AUTO_REFRESH_INTERVAL);
    },
  );

  onBeforeUnmount(clearRefreshTimer);

  const loadAll = async () => {
    loading.value = true;
    try {
      await loadList({ silent: true });
    } finally {
      loading.value = false;
    }
  };

  void loadAll();

  const page = reactive({
    loading,
    listLoading,
    graphLoading,
    actionLoading,
    taskLogLoading,
    autoRefresh,
    query,
    rows,
    tableData,
    total,
    pagination,
    statusOptions: STATUS_OPTIONS,
    selectedInstance,
    selectedInstanceId,
    selectedStageLogs,
    selectedTaskRows,
    nodes,
    edges,
    detailVisible,
    selectedDetail,
    graphSummary,
    canAutoRefresh,
    runQuery,
    resetQuery,
    refreshList,
    refreshGraph,
    selectInstance,
    handlePageChange,
    openNodeDetail,
    closeDetail,
    toggleAutoRefresh,
    rerunInstance,
    rerunFailedTasks,
    stopInstance,
    pauseInstance,
    resumeInstance,
    canRerunInstance,
    canRerunFailedTasks,
    canStopInstance,
    canPauseInstance,
    canResumeInstance,
    formatStatusLabel,
    formatJson,
  }) as unknown as WorkflowMonitorPage;

  return { page };
};
