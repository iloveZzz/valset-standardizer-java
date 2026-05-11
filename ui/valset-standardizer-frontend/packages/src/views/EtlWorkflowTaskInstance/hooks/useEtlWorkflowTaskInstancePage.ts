import dayjs from "dayjs";
import { computed, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import { useRoute } from "vue-router";
import {
  listEtlWorkflowDefinitions,
  type WorkflowDefinitionDTO,
} from "@/api/etlWorkflowConfig";
import {
  forceSuccessEtlWorkflowTaskInstance,
  getEtlWorkflowTaskInstanceLog,
  getEtlWorkflowTaskInstanceStateCount,
  listEtlWorkflowTaskInstances,
  type WorkflowTaskInstanceDTO,
  type WorkflowTaskInstanceLogQueryParams,
  type WorkflowTaskInstancePageDTO,
  type WorkflowTaskInstanceQueryParams,
  type WorkflowTaskInstanceStatus,
  type WorkflowTaskInstanceStateCountDTO,
} from "@/api/etlWorkflowTaskInstance";
import { unwrapSingleResult } from "@/utils/api-response";
import type {
  OptionItem,
  WorkflowTaskInstancePage,
  WorkflowTaskInstanceQueryState,
  WorkflowTaskInstanceRow,
  WorkflowTaskStateStatRow,
  WorkflowTaskStageRow,
} from "../types";

const PLATFORM_TYPE = "DOLPHIN_SCHEDULER";
const TIME_RANGE_FORMAT = "YYYY-MM-DD HH:mm:ss";

const STATUS_COLOR_MAP: Record<string, string> = {
  SUBMITTED_SUCCESS: "blue",
  RUNNING_EXECUTION: "processing",
  PAUSE: "orange",
  FAILURE: "red",
  SUCCESS: "green",
  NEED_FAULT_TOLERANCE: "gold",
  KILL: "volcano",
  DELAY_EXECUTION: "purple",
  FORCED_SUCCESS: "cyan",
  DISPATCH: "default",
};

const STATUS_LABELS: Record<string, string> = {
  SUBMITTED_SUCCESS: "已提交",
  RUNNING_EXECUTION: "运行中",
  PAUSE: "暂停",
  FAILURE: "失败",
  SUCCESS: "成功",
  NEED_FAULT_TOLERANCE: "容错中",
  KILL: "已终止",
  DELAY_EXECUTION: "延迟执行",
  FORCED_SUCCESS: "强制成功",
  DISPATCH: "派发中",
};

const STATE_CARD_ORDER: WorkflowTaskInstanceStatus[] = [
  "SUCCESS",
  "RUNNING_EXECUTION",
  "FAILURE",
  "SUBMITTED_SUCCESS",
  "PAUSE",
];

const HIDDEN_STATE_SET = new Set<WorkflowTaskInstanceStatus>([
  "NEED_FAULT_TOLERANCE",
  "KILL",
  "DELAY_EXECUTION",
  "FORCED_SUCCESS",
  "DISPATCH",
]);

const defaultQuery = (): WorkflowTaskInstanceQueryState => ({
  workflowCode: "",
  workflowVersionNo: "",
  taskName: "",
  workflowInstanceName: "",
  status: "",
  startTimeFrom: dayjs().startOf("day").format(TIME_RANGE_FORMAT),
  endTimeTo: dayjs().add(1, "day").startOf("day").format(TIME_RANGE_FORMAT),
});

const formatText = (value?: string | null) => {
  const text = String(value ?? "").trim();
  return text || "-";
};

const parseNumber = (value: string) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return undefined;
  }
  const next = Number(text);
  return Number.isFinite(next) ? next : undefined;
};

const formatDateTime = (value?: string | null) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return "-";
  }
  return text.replace("T", " ").replace(/\.\d+$/, "").slice(0, 19);
};

const formatDuration = (value?: number | null) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "-";
  }
  return `${Number(value)} ms`;
};

const formatCount = (value?: number | string | null) => {
  const next = Number(value ?? 0);
  return Number.isFinite(next) ? next : 0;
};

const TASK_TYPE_LABELS: Record<string, string> = {
  SUB_WORKFLOW: "子工作流",
  SHELL: "Shell",
  SQL: "SQL",
  SPARK: "Spark",
  FLINK: "Flink",
  PYTHON: "Python",
  DEPENDENT: "依赖",
  HTTP: "HTTP",
  SWITCH: "条件分支",
  PROCEDURE: "存储过程",
  MR: "MapReduce",
  OPENAPI: "开放接口",
  DINKY: "Dinky",
  DATAX: "DataX",
  CONDITION: "条件",
  SQL_SERVER: "SQL Server",
  SHELL_SCRIPT: "Shell",
};

const formatTaskTypeLabel = (value?: string | null) => {
  const taskType = String(value ?? "").trim();
  if (!taskType) {
    return "-";
  }
  const normalized = taskType.toUpperCase();
  return TASK_TYPE_LABELS[normalized] || taskType;
};

const normalizeStatus = (value?: WorkflowTaskInstanceStatus | string) => {
  const status = String(value ?? "").trim().toUpperCase();
  return status || "UNKNOWN";
};

const formatStatusLabel = (value?: WorkflowTaskInstanceStatus | string) =>
  STATUS_LABELS[normalizeStatus(value)] || formatText(String(value ?? ""));

const formatStatusColor = (value?: WorkflowTaskInstanceStatus | string) =>
  STATUS_COLOR_MAP[normalizeStatus(value)] || "default";

const formatStateTone = (value?: WorkflowTaskInstanceStatus | string) => {
  const status = normalizeStatus(value);
  if (status === "SUCCESS" || status === "FORCED_SUCCESS") {
    return "success";
  }
  if (status === "RUNNING_EXECUTION" || status === "DISPATCH") {
    return "processing";
  }
  if (status === "FAILURE" || status === "KILL") {
    return "danger";
  }
  if (
    status === "PAUSE" ||
    status === "NEED_FAULT_TOLERANCE" ||
    status === "DELAY_EXECUTION"
  ) {
    return "warning";
  }
  return "default";
};

const createWorkflowOptionLabel = (definition: WorkflowDefinitionDTO) => {
  const workflowName = formatText(definition.workflowName);
  const workflowCode = formatText(definition.workflowCode);
  const workflowVersionNo = definition.workflowVersionNo ?? 0;
  return `${workflowName} · ${workflowCode} v${workflowVersionNo}`;
};

const isDolphinSchedulerDefinition = (definition: WorkflowDefinitionDTO) =>
  String(definition.platformType ?? "").trim().toUpperCase() === PLATFORM_TYPE;

const sortDefinitions = (definitions: WorkflowDefinitionDTO[]) =>
  [...definitions].sort((left, right) => {
    const leftCode = String(left.workflowCode ?? "");
    const rightCode = String(right.workflowCode ?? "");
    if (leftCode !== rightCode) {
      return leftCode.localeCompare(rightCode);
    }
    return Number(right.workflowVersionNo ?? 0) - Number(left.workflowVersionNo ?? 0);
  });

const latestDefinitionByWorkflowCode = (definitions: WorkflowDefinitionDTO[]) => {
  const map = new Map<string, WorkflowDefinitionDTO>();
  for (const definition of sortDefinitions(definitions)) {
    const workflowCode = String(definition.workflowCode ?? "").trim();
    if (!workflowCode || map.has(workflowCode)) {
      continue;
    }
    map.set(workflowCode, definition);
  }
  return [...map.values()];
};

const mapStageRows = (
  definition?: WorkflowDefinitionDTO | null,
): WorkflowTaskStageRow[] =>
  (definition?.stages ?? []).map((stage) => ({
    ...stage,
    stageLabel: `${Number(stage.stageOrder ?? 0) > 0 ? `${stage.stageOrder}.` : ""}${formatText(stage.stageName)}`,
  }));

const mapTaskStateCards = (
  totalCount: number,
  counts: WorkflowTaskInstanceStateCountDTO[],
): WorkflowTaskStateStatRow[] => {
  const countMap = new Map<string, number>();
  for (const item of counts) {
    const state = normalizeStatus(item.state);
    countMap.set(state, formatCount(item.count));
  }
  const cards: WorkflowTaskStateStatRow[] = [
    {
      statKey: "TOTAL",
      label: "任务实例总数",
      value: formatCount(totalCount),
      desc: "当前工作流定义与时间范围内的任务实例总量",
      tone: "primary",
    },
  ];
  for (const state of STATE_CARD_ORDER) {
    cards.push({
      statKey: state,
      label: formatStatusLabel(state),
      value: countMap.get(state) ?? 0,
      desc: `状态 ${formatStatusLabel(state)} 的任务实例数量`,
      tone: formatStateTone(state),
    });
  }
  for (const [state, count] of countMap.entries()) {
    if (STATE_CARD_ORDER.includes(state as WorkflowTaskInstanceStatus)) {
      continue;
    }
    if (HIDDEN_STATE_SET.has(state as WorkflowTaskInstanceStatus)) {
      continue;
    }
    cards.push({
      statKey: state,
      label: formatStatusLabel(state),
      value: count,
      desc: `状态 ${formatStatusLabel(state)} 的任务实例数量`,
      tone: formatStateTone(state),
    });
  }
  return cards;
};

const mapTaskInstanceRow = (item: WorkflowTaskInstanceDTO): WorkflowTaskInstanceRow => {
  const taskInstanceId = item.id ?? null;
  const taskName = formatText(item.name);
  const state = normalizeStatus(item.state);
  const workflowInstanceName = formatText(item.workflowInstanceName);
  const workflowInstanceId = formatText(item.workflowInstanceId);
  return {
    ...item,
    taskKey: [
      taskInstanceId ?? item.taskCode ?? taskName,
      item.startTime ?? "",
      item.endTime ?? "",
    ].join("|"),
    taskInstanceId,
    taskName,
    taskTypeLabel: formatTaskTypeLabel(item.taskType),
    statusLabel: formatStatusLabel(state),
    statusColor: formatStatusColor(state),
    workflowInstanceLabel:
      workflowInstanceName === "-" && workflowInstanceId === "-"
        ? "-"
        : `${workflowInstanceName}${workflowInstanceId !== "-" ? ` · ${workflowInstanceId}` : ""}`,
    workerGroupLabel: formatText(item.workerGroup),
    hostLabel: formatText(item.host),
    startTimeLabel: formatDateTime(item.startTime),
    endTimeLabel: formatDateTime(item.endTime),
    durationLabel: formatDuration(item.duration),
  };
};

export const useEtlWorkflowTaskInstancePage = (): { page: WorkflowTaskInstancePage } => {
  const route = useRoute();
  const loading = ref(true);
  const listLoading = ref(false);
  const actionLoading = ref(false);
  const rows = ref<WorkflowTaskInstanceRow[]>([]);
  const total = ref(0);
  const definitions = ref<WorkflowDefinitionDTO[]>([]);
  const workflowInstanceState = ref("");
  const taskStateLoading = ref(false);
  const taskStateCards = ref<WorkflowTaskStateStatRow[]>([]);
  const logVisible = ref(false);
  const logLoading = ref(false);
  const logTitle = ref("");
  const logContent = ref("");
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const query = reactive<WorkflowTaskInstanceQueryState>(defaultQuery());

  const readRouteValue = (value: unknown) => {
    if (Array.isArray(value)) {
      return String(value[0] ?? "").trim();
    }
    return String(value ?? "").trim();
  };

  const syncRouteQueryFilters = () => {
    const nextWorkflowCode = readRouteValue(route.query.workflowCode);
    const nextWorkflowVersionNo = readRouteValue(route.query.workflowVersionNo);
    const nextTaskName = readRouteValue(route.query.taskName);
    const nextWorkflowInstanceName = readRouteValue(route.query.workflowInstanceName);
    const nextStatus = readRouteValue(route.query.status);
    const nextStartTimeFrom = readRouteValue(route.query.startTimeFrom);
    const nextEndTimeTo = readRouteValue(route.query.endTimeTo);

    if (nextWorkflowCode) {
      query.workflowCode = nextWorkflowCode;
      query.workflowVersionNo =
        nextWorkflowVersionNo ||
        String(
          latestDefinitionByWorkflowCode(definitions.value).find(
            (definition) =>
              String(definition.workflowCode ?? "").trim() === nextWorkflowCode,
          )?.workflowVersionNo ?? "",
        );
    }
    if (nextTaskName) {
      query.taskName = nextTaskName;
    }
    if (nextWorkflowInstanceName) {
      query.workflowInstanceName = nextWorkflowInstanceName;
    }
    if (nextStatus) {
      query.status = nextStatus;
    }
    if (nextStartTimeFrom) {
      query.startTimeFrom = nextStartTimeFrom;
    }
    if (nextEndTimeTo) {
      query.endTimeTo = nextEndTimeTo;
    }
  };

  const workflowOptions = computed<OptionItem[]>(() =>
    latestDefinitionByWorkflowCode(definitions.value).map((definition) => ({
      label: createWorkflowOptionLabel(definition),
      value: String(definition.workflowCode ?? ""),
    })),
  );

  const selectedDefinition = computed<WorkflowDefinitionDTO | null>(() => {
    const workflowCode = String(query.workflowCode ?? "").trim();
    const workflowVersionNo = parseNumber(query.workflowVersionNo);
    if (!workflowCode || workflowVersionNo === undefined) {
      return null;
    }
    return (
      definitions.value.find(
        (definition) =>
          String(definition.workflowCode ?? "").trim() === workflowCode &&
          Number(definition.workflowVersionNo ?? 0) === workflowVersionNo,
      ) ?? null
    );
  });

  const stageRows = computed(() => mapStageRows(selectedDefinition.value));

  const selectedWorkflowLabel = computed(() =>
    selectedDefinition.value ? createWorkflowOptionLabel(selectedDefinition.value) : "未选择工作流定义",
  );

  const selectedWorkflowDescription = computed(() => {
    const definition = selectedDefinition.value;
    if (!definition) {
      return "请选择一个 DolphinScheduler 工作流定义后查看对应任务实例。";
    }
    const stageCount = (definition.stages ?? []).length;
    return `当前定义包含 ${stageCount} 个阶段，列表将按该定义对应的项目编码查询任务实例。`;
  });

  const selectedProjectCode = computed(() =>
    String(
      selectedDefinition.value?.engineBinding?.externalProjectCode ?? "",
    ).trim(),
  );

  const taskStateSummary = computed(() => {
    const definition = selectedDefinition.value;
    if (!definition) {
      return "请选择工作流定义后查看任务实例状态统计。";
    }
    const projectCode = selectedProjectCode.value || "-";
    return `统计基于项目编码 ${projectCode} 及当前时间范围`;
  });

  const currentFilterSummary = computed(() => {
    const filters = [
      query.taskName && `任务名称：${query.taskName}`,
      query.workflowInstanceName && `阶段实例名称：${query.workflowInstanceName}`,
      query.status && `状态：${formatStatusLabel(query.status)}`,
      query.startTimeFrom && `开始：${query.startTimeFrom}`,
      query.endTimeTo && `结束：${query.endTimeTo}`,
    ].filter(Boolean);
    return filters.length ? filters.join(" / ") : "全部任务实例";
  });

  const tableData = computed(() => rows.value);

  const resetPagination = () => {
    pagination.value.current = 1;
  };

  const applyDefaultWorkflowSelection = () => {
    const fallbackDefinition = workflowOptions.value[0];
    if (!fallbackDefinition) {
      query.workflowCode = "";
      query.workflowVersionNo = "";
      return;
    }
    const defaultDefinition = latestDefinitionByWorkflowCode(definitions.value).find(
      (definition) => String(definition.workflowCode ?? "").trim() === fallbackDefinition.value,
    );
    if (!defaultDefinition) {
      query.workflowCode = "";
      query.workflowVersionNo = "";
      return;
    }
    query.workflowCode = String(defaultDefinition.workflowCode ?? "");
    query.workflowVersionNo = String(defaultDefinition.workflowVersionNo ?? "");
  };

  const loadDefinitions = async () => {
    const res = await listEtlWorkflowDefinitions();
    definitions.value = (unwrapSingleResult(res) ?? []).filter(isDolphinSchedulerDefinition);
    applyDefaultWorkflowSelection();
  };

  const loadTaskStateStats = async ({ silent = false }: { silent?: boolean } = {}) => {
    const definition = selectedDefinition.value;
    const projectCode = selectedProjectCode.value;
    if (!definition || !projectCode) {
      taskStateCards.value = [];
      return;
    }
    if (!silent) {
      taskStateLoading.value = true;
    }
    try {
      const res = await getEtlWorkflowTaskInstanceStateCount({
        startDate: query.startTimeFrom,
        endDate: query.endTimeTo,
        projectCode,
      });
      const page = unwrapSingleResult(res) ?? ({} as {
        totalCount?: number;
        taskInstanceStatusCounts?: WorkflowTaskInstanceStateCountDTO[];
      });
      taskStateCards.value = mapTaskStateCards(
        Number(page.totalCount ?? 0),
        page.taskInstanceStatusCounts ?? [],
      );
    } catch {
      taskStateCards.value = [];
      message.error("加载任务实例状态统计失败");
    } finally {
      if (!silent) {
        taskStateLoading.value = false;
      }
    }
  };

  const loadList = async ({ silent = false }: { silent?: boolean } = {}) => {
    const definition = selectedDefinition.value;
    if (!definition) {
      rows.value = [];
      total.value = 0;
      workflowInstanceState.value = "";
      return;
    }
    const requestParams: WorkflowTaskInstanceQueryParams = {
      workflowCode: definition.workflowCode,
      workflowVersionNo: definition.workflowVersionNo,
      taskName: query.taskName || undefined,
      workflowInstanceName: query.workflowInstanceName || undefined,
      status: query.status || undefined,
      startTimeFrom: query.startTimeFrom || undefined,
      endTimeTo: query.endTimeTo || undefined,
      pageIndex: Math.max(Number(pagination.value.current ?? 1) - 1, 0),
      pageSize: Number(pagination.value.pageSize ?? 10),
    };
    if (!silent) {
      listLoading.value = true;
    }
    try {
      const res = await listEtlWorkflowTaskInstances(requestParams);
      const page = unwrapSingleResult(res) ?? ({} as WorkflowTaskInstancePageDTO);
      const nextRows = (page.taskList ?? []).map(mapTaskInstanceRow);
      rows.value = nextRows;
      total.value = Number(page.totalCount ?? nextRows.length);
      workflowInstanceState.value = String(page.workflowInstanceState ?? "");
      pagination.value.current = Number(page.pageIndex ?? requestParams.pageIndex ?? 0) + 1;
      pagination.value.pageSize = Number(page.pageSize ?? requestParams.pageSize ?? 10);
      pagination.value.total = total.value;
    } catch (error) {
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
      message.error("加载任务实例列表失败");
      throw error;
    } finally {
      if (!silent) {
        listLoading.value = false;
      }
    }
  };

  const refreshListAndStats = async () => {
    await Promise.allSettled([loadList(), loadTaskStateStats()]);
  };

  const refreshList = async () => {
    await refreshListAndStats();
  };

  const runQuery = () => {
    resetPagination();
    void refreshListAndStats();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    applyDefaultWorkflowSelection();
    resetPagination();
    void refreshListAndStats();
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

  const handleWorkflowCodeSelect = (workflowCode: unknown) => {
    const nextWorkflowCode = String(workflowCode ?? "").trim();
    query.workflowCode = nextWorkflowCode;
    const selected = latestDefinitionByWorkflowCode(definitions.value).find(
      (definition) => String(definition.workflowCode ?? "").trim() === nextWorkflowCode,
    );
    query.workflowVersionNo = selected?.workflowVersionNo != null ? String(selected.workflowVersionNo) : "";
    resetPagination();
    void refreshListAndStats();
  };

  const handleStatusSelect = (status: unknown) => {
    const nextStatus = String(status ?? "").trim().toUpperCase();
    query.status = nextStatus;
    resetPagination();
    void loadList();
  };

  const selectStageName = (stageName: string) => {
    const nextTaskName = String(stageName ?? "").trim();
    if (!nextTaskName) {
      return;
    }
    query.taskName = nextTaskName;
    resetPagination();
    void loadList();
  };

  const openTaskLog = async (row: WorkflowTaskInstanceRow) => {
    const definition = selectedDefinition.value;
    if (!definition) {
      message.warning("请先选择工作流定义");
      return;
    }
    const taskInstanceId = row.taskInstanceId;
    if (taskInstanceId == null) {
      message.warning("当前任务实例没有可用的任务 ID");
      return;
    }
    logVisible.value = true;
    logLoading.value = true;
    logTitle.value = `${selectedWorkflowLabel.value} · ${row.taskName} · 日志`;
    logContent.value = "";
    try {
      const params: WorkflowTaskInstanceLogQueryParams = {
        workflowCode: String(definition.workflowCode ?? ""),
        workflowVersionNo: Number(definition.workflowVersionNo ?? 0),
        skipLineNum: 0,
        limit: 1000,
      };
      const res = await getEtlWorkflowTaskInstanceLog(taskInstanceId, params);
      const content = unwrapSingleResult(res);
      logContent.value = typeof content === "string" && content.length > 0 ? content : "暂无日志";
    } catch {
      logContent.value = "加载日志失败";
      message.error("加载任务日志失败");
    } finally {
      logLoading.value = false;
    }
  };

  const closeTaskLog = () => {
    logVisible.value = false;
    logLoading.value = false;
    logTitle.value = "";
    logContent.value = "";
  };

  const forceSuccessTask = async (row: WorkflowTaskInstanceRow) => {
    const definition = selectedDefinition.value;
    if (!definition) {
      message.warning("请先选择工作流定义");
      return;
    }
    const taskInstanceId = row.taskInstanceId;
    if (taskInstanceId == null) {
      message.warning("当前任务实例没有可用的任务 ID");
      return;
    }
    actionLoading.value = true;
    try {
      const params: WorkflowTaskInstanceLogQueryParams = {
        workflowCode: String(definition.workflowCode ?? ""),
        workflowVersionNo: Number(definition.workflowVersionNo ?? 0),
      };
      await forceSuccessEtlWorkflowTaskInstance(taskInstanceId, params);
      message.success("已提交强制成功");
      await loadList({ silent: true });
    } finally {
      actionLoading.value = false;
    }
  };

  const loadAll = async () => {
    loading.value = true;
    try {
      await loadDefinitions();
      syncRouteQueryFilters();
      await Promise.all([
        loadList({ silent: true }),
        loadTaskStateStats({ silent: true }),
      ]);
    } catch {
      message.error("初始化任务实例页失败");
    } finally {
      loading.value = false;
    }
  };

  void loadAll();

  watch(
    () => route.query,
    () => {
      if (loading.value) {
        return;
      }
      syncRouteQueryFilters();
      resetPagination();
      void refreshListAndStats();
    },
    { deep: true },
  );

  const page = reactive({
    loading,
    listLoading,
    actionLoading,
    rows,
    tableData,
    total,
    pagination,
    query,
    definitions,
    workflowOptions,
    selectedDefinition,
    stageRows,
    workflowInstanceState,
    taskStateLoading,
    taskStateCards,
    taskStateSummary,
    currentFilterSummary,
    selectedWorkflowLabel,
    selectedWorkflowDescription,
    logVisible,
    logLoading,
    logTitle,
    logContent,
    runQuery,
    resetQuery,
    refreshList,
    handlePageChange,
    handleWorkflowCodeSelect,
    handleStatusSelect,
    applyDefaultWorkflowSelection,
    syncRouteQueryFilters,
    selectStageName,
    openTaskLog,
    closeTaskLog,
    forceSuccessTask,
    formatStatusLabel,
    formatStatusColor,
    formatDateTime,
    formatDuration,
  });

  return { page };
};
