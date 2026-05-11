import { computed, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import {
  listEtlWorkflowDefinitions,
  listEtlWorkflowPlatforms,
  type EtlPlatformType,
  type WorkflowDefinitionDTO,
  type WorkflowPlatformMetadataDTO,
} from "@/api/etlWorkflowConfig";
import {
  getEtlWorkflowInstance,
  getEtlWorkflowInstanceTaskLog,
  listEtlWorkflowInstanceLogs,
  listEtlWorkflowInstanceTasks,
  listEtlWorkflowInstances,
  pauseEtlWorkflowInstance,
  retryEtlWorkflowInstance,
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
  WorkflowInstanceDetailRow,
  WorkflowInstancePage,
  WorkflowInstanceQueryState,
  WorkflowInstanceRow,
  WorkflowStageLogRow,
  WorkflowTaskRow,
} from "../types";

const PLATFORM_ORDER: EtlPlatformType[] = [
  "SPRING_BATCH",
  "DOLPHIN_SCHEDULER",
  "XXL_JOB",
];

const PLATFORM_LABELS: Record<EtlPlatformType, string> = {
  SPRING_BATCH: "Spring Batch",
  DOLPHIN_SCHEDULER: "DolphinScheduler",
  XXL_JOB: "XXL-JOB",
};

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

const TASK_STATE_LABELS: Record<string, string> = {
  SUCCESS: "成功",
  FAILED: "失败",
  RUNNING: "运行中",
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

const defaultQuery = (): WorkflowInstanceQueryState => ({
  workflowCode: "",
  workflowVersionNo: "",
  platformType: "",
  status: "",
  businessKey: "",
  instanceId: "",
  externalInstanceId: "",
  stageCode: "",
  triggerTimeFrom: "",
  triggerTimeTo: "",
});

const workflowKey = (
  workflowCode?: string | null,
  workflowVersionNo?: number | null,
) =>
  `${String(workflowCode ?? "").trim()}@${String(
    workflowVersionNo ?? "",
  ).trim()}`;

const normalizeStatus = (value?: string | null): WorkflowInstanceStatus | string => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  return status || "UNKNOWN";
};

const isSucceededStatus = (value?: string | null) =>
  normalizeStatus(value) === "SUCCEEDED";

const isFailedStatus = (value?: string | null) =>
  normalizeStatus(value) === "FAILED";

const isRunningStatus = (value?: string | null) =>
  normalizeStatus(value) === "RUNNING";

const formatText = (value?: string | null) => {
  const text = String(value ?? "").trim();
  return text || "-";
};

const formatDateTime = (value?: string | null) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return "-";
  }
  return text.replace("T", " ").replace(/\.\d+$/, "").slice(0, 19);
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

const parseNumber = (value: string) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return undefined;
  }
  const next = Number(text);
  return Number.isFinite(next) ? next : undefined;
};

const mapStageLog = (log: WorkflowStageLogDTO): WorkflowStageLogRow => {
  const stageCode = formatText(log.stageCode);
  const startTime = formatText(log.startTime);
  const endTime = formatText(log.endTime);
  const rawStatus = formatText(log.rawStatus);
  return {
    logKey: [stageCode, startTime, endTime, rawStatus].join("|"),
    stageCode,
    stageName: formatText(log.stageName),
    stageOrder: Number(log.stageOrder ?? 0),
    status: normalizeStatus(log.status),
    rawStatus,
    message: formatText(log.message),
    startTime,
    endTime,
    payload: (log.payload ?? {}) as Record<string, unknown>,
  };
};

const mapTaskRow = (task: WorkflowTaskInstanceDTO): WorkflowTaskRow => {
  const state = formatText(task.state);
  return {
    taskKey: [
      task.id ?? task.taskCode ?? task.name ?? task.taskParams ?? "-",
      task.startTime ?? "",
      task.endTime ?? "",
    ].join("|"),
    id: task.id ?? null,
    name: formatText(task.name),
    taskType: formatText(task.taskType),
    workflowInstanceId: formatText(task.workflowInstanceId),
    workflowInstanceName: formatText(task.workflowInstanceName),
    projectCode: task.projectCode ?? null,
    taskCode: task.taskCode ?? null,
    taskDefinitionVersion: task.taskDefinitionVersion ?? null,
    processDefinitionName: formatText(task.processDefinitionName),
    taskGroupPriority: task.taskGroupPriority ?? null,
    state,
    firstSubmitTime: formatDateTime(task.firstSubmitTime),
    submitTime: formatDateTime(task.submitTime),
    startTime: formatDateTime(task.startTime),
    endTime: formatDateTime(task.endTime),
    host: formatText(task.host),
    executePath: formatText(task.executePath),
    retryTimes: task.retryTimes ?? null,
    alertFlag: formatText(task.alertFlag),
    appLink: formatText(task.appLink),
    flag: formatText(task.flag),
    duration: task.duration ?? null,
    maxRetryTimes: task.maxRetryTimes ?? null,
    retryInterval: task.retryInterval ?? null,
    taskInstancePriority: formatText(task.taskInstancePriority),
    workflowInstancePriority: formatText(task.workflowInstancePriority),
    workerGroup: formatText(task.workerGroup),
    environmentCode: task.environmentCode ?? null,
    executorId: task.executorId ?? null,
    executorName: formatText(task.executorName),
    delayTime: task.delayTime ?? null,
    taskParams: formatText(task.taskParams),
    dryRun: task.dryRun ?? null,
    taskGroupId: task.taskGroupId ?? null,
    cpuQuota: task.cpuQuota ?? null,
    memoryMax: task.memoryMax ?? null,
    taskExecuteType: formatText(task.taskExecuteType),
    stateLabel: TASK_STATE_LABELS[state.toUpperCase()] || state,
  };
};

const sortTaskRows = (taskRows: WorkflowTaskRow[]) =>
  taskRows.slice().sort((left, right) => {
    const leftId = left.id ?? Number.MAX_SAFE_INTEGER;
    const rightId = right.id ?? Number.MAX_SAFE_INTEGER;
    if (leftId !== rightId) {
      return leftId - rightId;
    }
    const leftCode = left.taskCode ?? Number.MAX_SAFE_INTEGER;
    const rightCode = right.taskCode ?? Number.MAX_SAFE_INTEGER;
    if (leftCode !== rightCode) {
      return leftCode - rightCode;
    }
    return String(left.startTime ?? "").localeCompare(String(right.startTime ?? ""));
  });

export const useWorkflowInstancePage = (): { page: WorkflowInstancePage } => {
  const query = reactive<WorkflowInstanceQueryState>(defaultQuery());
  const rows = ref<WorkflowInstanceRow[]>([]);
  const definitions = ref<WorkflowDefinitionDTO[]>([]);
  const platformMetadata = ref<WorkflowPlatformMetadataDTO[]>([]);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const loading = ref(false);
  const listLoading = ref(false);
  const detailLoading = ref(false);
  const actionLoading = ref(false);
  const detailVisible = ref(false);
  const selectedRow = ref<WorkflowInstanceDetailRow | null>(null);
  const selectedLogs = ref<WorkflowStageLogRow[]>([]);
  const selectedLogStageCode = ref("");
  const expandedRowKeys = ref<string[]>([]);
  const taskLogVisible = ref(false);
  const taskLogLoading = ref(false);
  const taskLogTitle = ref("");
  const taskLogContent = ref("");
  let listRequestId = 0;

  const definitionMap = computed(() => {
    const map = new Map<string, WorkflowDefinitionDTO>();
    definitions.value.forEach((definition) => {
      if (definition.workflowCode && definition.workflowVersionNo !== undefined) {
        map.set(
          workflowKey(definition.workflowCode, definition.workflowVersionNo),
          definition,
        );
      }
    });
    return map;
  });

  const platformMetadataMap = computed(() => {
    const map = new Map<EtlPlatformType, WorkflowPlatformMetadataDTO>();
    platformMetadata.value.forEach((item) => {
      if (item.platformType) {
        map.set(item.platformType, item);
      }
    });
    return map;
  });

  const tableData = computed(() => rows.value);
  const allRowsExpanded = computed(
    () =>
      rows.value.length > 0 &&
      rows.value.every((row) => expandedRowKeys.value.includes(row.instanceId)),
  );

  const total = computed(() => Number(pagination.value.total ?? 0));
  const runningCount = computed(
    () =>
      rows.value.filter((row) =>
        ["SUBMITTED", "RUNNING", "RETRYING"].includes(
          normalizeStatus(row.status),
        ),
      ).length,
  );
  const succeededCount = computed(
    () =>
      rows.value.filter(
        (row) => normalizeStatus(row.status) === "SUCCEEDED",
      ).length,
  );
  const failedCount = computed(
    () =>
      rows.value.filter((row) => normalizeStatus(row.status) === "FAILED")
        .length,
  );
  const submittedCount = computed(
    () =>
      rows.value.filter((row) => normalizeStatus(row.status) === "SUBMITTED")
        .length,
  );

  const workflowOptions = computed<OptionItem[]>(() =>
    Array.from(
      definitions.value
        .slice()
        .sort((left, right) => {
          const codeCompare = String(left.workflowCode ?? "").localeCompare(
            String(right.workflowCode ?? ""),
          );
          if (codeCompare !== 0) {
            return codeCompare;
          }
          return (
            Number(right.workflowVersionNo ?? 0) -
            Number(left.workflowVersionNo ?? 0)
          );
        })
        .reduce((map, definition) => {
          const code = String(definition.workflowCode ?? "").trim();
          if (code && !map.has(code)) {
            map.set(code, definition);
          }
          return map;
        }, new Map<string, WorkflowDefinitionDTO>())
        .entries(),
    ).map(([workflowCode, definition]) => ({
      value: workflowCode,
      label: `${workflowCode} v${Number(definition.workflowVersionNo ?? 0)}${
        definition.workflowName ? ` · ${definition.workflowName}` : ""
      }`,
    })),
  );

  const applyDefaultWorkflowSelection = () => {
    const currentWorkflowCode = String(query.workflowCode ?? "").trim();
    const availableWorkflowCodes = new Set(workflowOptions.value.map((item) => item.value));
    if (currentWorkflowCode && availableWorkflowCodes.has(currentWorkflowCode)) {
      return;
    }
    const fallbackWorkflowCode = workflowOptions.value[0]?.value ?? "";
    handleWorkflowCodeChange(fallbackWorkflowCode);
  };

  const stageOptions = computed<OptionItem[]>(() => {
    const selectedWorkflowCode = String(query.workflowCode ?? "").trim();
    const selectedWorkflowVersion = parseNumber(query.workflowVersionNo);
    const seen = new Set<string>();
    const items: OptionItem[] = [];
    definitions.value.forEach((definition) => {
      if (
        selectedWorkflowCode &&
        definition.workflowCode !== selectedWorkflowCode
      ) {
        return;
      }
      if (
        selectedWorkflowVersion !== undefined &&
        definition.workflowVersionNo !== selectedWorkflowVersion
      ) {
        return;
      }
      (definition.stages ?? []).forEach((stage) => {
        const stageCode = String(stage.stageCode ?? "").trim();
        if (!stageCode || seen.has(stageCode)) {
          return;
        }
        seen.add(stageCode);
        items.push({
          value: stageCode,
          label: `${formatText(stage.stageName || stage.stageCode)} · ${formatText(
            definition.workflowCode,
          )} v${Number(definition.workflowVersionNo ?? 0)}`,
        });
      });
    });
    return items;
  });

  const platformOptions = computed<OptionItem[]>(() =>
    PLATFORM_ORDER.map((platformType) => ({
      value: platformType,
      label:
        platformMetadataMap.value.get(platformType)?.platformName ||
        PLATFORM_LABELS[platformType],
    })),
  );

  const statusOptions = computed<OptionItem[]>(() => STATUS_OPTIONS);

  const currentFilterSummary = computed(() => {
    const segments = [
      query.workflowCode &&
        `工作流=${query.workflowCode}${query.workflowVersionNo ? ` v${query.workflowVersionNo}` : ""}`,
      query.platformType &&
        `平台=${formatPlatformLabel(query.platformType)}`,
      query.status && `状态=${formatStatusLabel(query.status)}`,
      query.businessKey && `业务键=${query.businessKey}`,
      query.instanceId && `实例ID=${query.instanceId}`,
      query.externalInstanceId && `外部实例ID=${query.externalInstanceId}`,
      query.stageCode && `阶段=${query.stageCode}`,
      query.triggerTimeFrom &&
        `触发时间从=${formatDateTime(query.triggerTimeFrom)}`,
      query.triggerTimeTo &&
        `触发时间到=${formatDateTime(query.triggerTimeTo)}`,
    ].filter(Boolean);
    return segments.length ? segments.join(" / ") : "全部条件";
  });

  const resolveWorkflowName = (
    workflowCode?: string | null,
    workflowVersionNo?: number | null,
  ) => {
    const definition = definitionMap.value.get(
      workflowKey(workflowCode, workflowVersionNo),
    );
    return definition?.workflowName || formatText(workflowCode);
  };

  const resolveStageName = (
    workflowCode?: string | null,
    workflowVersionNo?: number | null,
    stageCode?: string | null,
    fallbackStageName?: string | null,
  ) => {
    const code = String(stageCode ?? "").trim();
    if (!code) {
      return "-";
    }
    const definition = definitionMap.value.get(
      workflowKey(workflowCode, workflowVersionNo),
    );
    const stage = definition?.stages?.find(
      (item) => String(item.stageCode ?? "").trim() === code,
    );
    return formatText(stage?.stageName || fallbackStageName || code);
  };

  const mapListRow = (item: WorkflowInstanceViewDTO): WorkflowInstanceRow => {
    const workflowCode = formatText(item.workflowCode);
    const workflowVersionNo = Number(item.workflowVersionNo ?? 0);
    const platformType = item.platformType ?? null;
    const status = normalizeStatus(item.status ?? item.rawStatus);
    return {
      rowKey: formatText(item.instanceId),
      instanceId: formatText(item.instanceId),
      workflowCode,
      workflowVersionNo,
      workflowName: resolveWorkflowName(workflowCode, workflowVersionNo),
      workflowLabel: `${workflowCode} v${workflowVersionNo}`,
      platformType,
      platformLabel: formatPlatformLabel(platformType),
      businessKey: formatText(item.businessKey),
      externalInstanceId: formatText(item.externalInstanceId),
      externalWorkflowId: formatText(item.externalWorkflowId),
      status,
      statusLabel: formatStatusLabel(status),
      rawStatus: formatText(item.rawStatus),
      currentStageCode: formatText(item.currentStageCode),
      currentStageName: resolveStageName(
        workflowCode,
        workflowVersionNo,
        item.currentStageCode,
        item.currentStageName,
      ),
      triggerTime: formatDateTime(item.triggerTime),
      startTime: formatDateTime(item.startTime),
      endTime: formatDateTime(item.endTime),
      message: formatText(item.message),
      stageCount: Number(item.stageCount ?? 0),
      taskRows: [],
      taskRowsLoaded: false,
    };
  };

  const mapDetailRow = (
    item: WorkflowInstanceDTO,
    fallback?: WorkflowInstanceRow | null,
  ): WorkflowInstanceDetailRow => {
    const listRow = mapListRow(item);
    return {
      ...listRow,
      workflowName: fallback?.workflowName || listRow.workflowName,
      workflowLabel: fallback?.workflowLabel || listRow.workflowLabel,
      platformLabel: fallback?.platformLabel || listRow.platformLabel,
      statusLabel: fallback?.statusLabel || listRow.statusLabel,
      currentStageName: fallback?.currentStageName || listRow.currentStageName,
      context: (item.context ?? {}) as Record<string, unknown>,
      stageLogs: (item.stageLogs ?? []).map(mapStageLog),
      taskRows: fallback?.taskRows ?? [],
      taskRowsLoaded: fallback?.taskRowsLoaded ?? false,
    };
  };

  const updateRowTaskRows = (
    instanceId: string,
    taskRows: WorkflowTaskRow[],
  ) => {
    const sortedTaskRows = sortTaskRows(taskRows);
    rows.value = rows.value.map((row) =>
      row.instanceId === instanceId
        ? {
            ...row,
            taskRows: sortedTaskRows,
            taskRowsLoaded: true,
          }
        : row,
    );
    if (selectedRow.value?.instanceId === instanceId) {
      selectedRow.value = {
        ...selectedRow.value,
        taskRows: sortedTaskRows,
        taskRowsLoaded: true,
      };
    }
  };

  const hydrateTaskRows = async (targetRows: WorkflowInstanceRow[]) => {
    await Promise.all(
      targetRows.map(async (row) => {
        if (!row.instanceId || row.taskRowsLoaded) {
          return;
        }
        try {
          const res = await listEtlWorkflowInstanceTasks(row.instanceId);
          const taskRows = (unwrapSingleResult(res)?.taskList ?? []).map(mapTaskRow);
          updateRowTaskRows(row.instanceId, taskRows);
        } catch {
          updateRowTaskRows(row.instanceId, []);
        }
      }),
    );
  };

  const syncSelectedRow = () => {
    if (!selectedRow.value) {
      return;
    }
    const next = rows.value.find(
      (row) => row.instanceId === selectedRow.value?.instanceId,
    );
    if (next) {
      selectedRow.value = {
        ...selectedRow.value,
        ...next,
        context: selectedRow.value.context,
        stageLogs: selectedRow.value.stageLogs,
        taskRows: selectedRow.value.taskRows,
        taskRowsLoaded: selectedRow.value.taskRowsLoaded,
      };
    }
  };

  const loadCatalogs = async () => {
    const [definitionsRes, platformsRes] = await Promise.all([
      listEtlWorkflowDefinitions(),
      listEtlWorkflowPlatforms(),
    ]);
    definitions.value = unwrapMultiResult(definitionsRes) ?? [];
    platformMetadata.value = unwrapMultiResult(platformsRes) ?? [];
  };

  const loadList = async ({ silent = false }: { silent?: boolean } = {}) => {
    const requestId = ++listRequestId;
    if (!silent) {
      listLoading.value = true;
    }
    try {
      const pageIndex = Math.max(Number(pagination.value.current ?? 1) - 1, 0);
      const pageSize = Number(pagination.value.pageSize ?? 10);
      const params: WorkflowInstanceQueryParams = {
        workflowCode: query.workflowCode || undefined,
        workflowVersionNo: parseNumber(query.workflowVersionNo),
        platformType: (query.platformType || undefined) as EtlPlatformType | undefined,
        status: query.status || undefined,
        businessKey: query.businessKey || undefined,
        instanceId: query.instanceId || undefined,
        externalInstanceId: query.externalInstanceId || undefined,
        stageCode: query.stageCode || undefined,
        triggerTimeFrom: query.triggerTimeFrom || undefined,
        triggerTimeTo: query.triggerTimeTo || undefined,
        pageIndex,
        pageSize,
      };
      const res = await listEtlWorkflowInstances(params);
      if (requestId !== listRequestId) {
        return;
      }
      rows.value = (res.data ?? []).map(mapListRow);
      expandedRowKeys.value = rows.value.length > 0 ? [rows.value[0].instanceId] : [];
      pagination.value.total = Number(res.totalCount ?? 0);
      pagination.value.current = Number(res.pageIndex ?? pageIndex) + 1;
      pagination.value.pageSize = Number(res.pageSize ?? pageSize);
      if (rows.value.length > 0) {
        await hydrateTaskRows([rows.value[0]]);
      }
      syncSelectedRow();
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      message.error("加载 ETL 工作流实例列表失败");
      throw error;
    } finally {
      if (!silent && requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const refreshList = async () => {
    await loadList();
  };

  const handleWorkflowCodeChange = (workflowCode: unknown) => {
    const nextWorkflowCode = String(workflowCode ?? "").trim();
    query.workflowCode = nextWorkflowCode;
    if (!nextWorkflowCode) {
      query.workflowVersionNo = "";
      query.stageCode = "";
      return;
    }
    const latestDefinition = definitions.value
      .filter(
        (definition) =>
          String(definition.workflowCode ?? "").trim() === nextWorkflowCode,
      )
      .sort(
        (left, right) =>
          Number(right.workflowVersionNo ?? 0) - Number(left.workflowVersionNo ?? 0),
      )[0];
    query.workflowVersionNo =
      latestDefinition?.workflowVersionNo !== undefined &&
      latestDefinition.workflowVersionNo !== null
        ? String(latestDefinition.workflowVersionNo)
        : "";
    query.stageCode = "";
  };

  const handleWorkflowCodeSelect = (workflowCode: unknown) => {
    handleWorkflowCodeChange(workflowCode);
    pagination.value.current = 1;
    void loadList();
  };

  const runQuery = () => {
    applyDefaultWorkflowSelection();
    pagination.value.current = 1;
    void loadList();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    applyDefaultWorkflowSelection();
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

  const handleToggleRowExpand = (params: {
    row?: WorkflowInstanceRow;
    expanded?: boolean;
  }) => {
    const rowKey = String(params.row?.instanceId ?? "").trim();
    if (!rowKey) {
      return;
    }
    if (params.expanded) {
      if (!expandedRowKeys.value.includes(rowKey)) {
        expandedRowKeys.value = [...expandedRowKeys.value, rowKey];
      }
      if (params.row && !params.row.taskRowsLoaded) {
        void hydrateTaskRows([params.row]);
      }
      return;
    }
    expandedRowKeys.value = expandedRowKeys.value.filter(
      (item) => item !== rowKey,
    );
  };

  const expandAllRows = async () => {
    expandedRowKeys.value = rows.value
      .map((row) => row.instanceId)
      .filter(Boolean);
    await hydrateTaskRows(rows.value);
  };

  const collapseAllRows = () => {
    expandedRowKeys.value = [];
  };

  const refreshDetailLogs = async (stageCode = selectedLogStageCode.value) => {
    const instanceId = selectedRow.value?.instanceId;
    if (!instanceId) {
      selectedLogs.value = [];
      return;
    }
    detailLoading.value = true;
    try {
      const res = await listEtlWorkflowInstanceLogs(
        instanceId,
        stageCode || undefined,
      );
      selectedLogs.value = (unwrapMultiResult(res) ?? []).map(mapStageLog);
      selectedLogStageCode.value = stageCode || "";
    } catch {
      selectedLogs.value = [];
      message.error("加载阶段日志失败");
    } finally {
      detailLoading.value = false;
    }
  };

  const openTaskLog = async (row: WorkflowInstanceRow, taskRow: WorkflowTaskRow) => {
    const taskInstanceId = taskRow.id;
    if (taskInstanceId == null) {
      message.warning("当前任务实例没有可用的任务 ID");
      return;
    }
    taskLogTitle.value = `${row.workflowLabel} · ${taskRow.name} · 日志`;
    taskLogContent.value = "";
    taskLogVisible.value = true;
    taskLogLoading.value = true;
    try {
      const res = await getEtlWorkflowInstanceTaskLog(row.instanceId, taskInstanceId);
      const content = unwrapSingleResult(res);
      taskLogContent.value =
        typeof content === "string" && content.length > 0 ? content : "暂无日志";
    } catch {
      taskLogContent.value = "加载任务日志失败";
      message.error("加载任务日志失败");
    } finally {
      taskLogLoading.value = false;
    }
  };

  const closeTaskLog = () => {
    taskLogVisible.value = false;
    taskLogLoading.value = false;
    taskLogTitle.value = "";
    taskLogContent.value = "";
  };

  const openDetailDrawer = async (row: WorkflowInstanceRow) => {
    detailVisible.value = true;
    selectedLogStageCode.value = "";
    selectedRow.value = {
      ...row,
      context: {},
      stageLogs: [],
      taskRows: row.taskRows,
      taskRowsLoaded: row.taskRowsLoaded,
    };
    detailLoading.value = true;
    try {
      const res = await getEtlWorkflowInstance(row.instanceId);
      const next = unwrapSingleResult(res);
      if (next) {
        selectedRow.value = mapDetailRow(next, row);
      }
    } catch {
      message.warning("未能加载实例详情，已回退到列表数据");
    } finally {
      detailLoading.value = false;
    }
    await refreshDetailLogs("");
  };

  const closeDetailDrawer = () => {
    detailVisible.value = false;
  };

  const refreshAfterMutation = async (response: WorkflowInstanceDTO | undefined) => {
    if (response) {
      const detail = mapDetailRow(response, selectedRow.value);
      if (selectedRow.value?.instanceId === detail.instanceId) {
        selectedRow.value = detail;
      }
      const index = rows.value.findIndex(
        (row) => row.instanceId === detail.instanceId,
      );
      if (index >= 0) {
        rows.value.splice(index, 1, {
          ...mapListRow(response),
          taskRows: detail.taskRows,
          taskRowsLoaded: detail.taskRowsLoaded,
        });
      }
    }
    await loadList({ silent: true });
    if (detailVisible.value) {
      await refreshDetailLogs();
    }
  };

  const submitTrigger = async (
    row: WorkflowInstanceRow,
    triggerMode?: WorkflowTriggerMode,
    successMessage = "已触发新的工作流实例",
  ) => {
    actionLoading.value = true;
    try {
      const res = await triggerEtlWorkflowInstance({
        workflowCode: row.workflowCode,
        workflowVersionNo: row.workflowVersionNo,
        businessKey: row.businessKey || undefined,
        stageCode: row.currentStageCode || undefined,
        force: true,
        triggerMode,
      });
      const next = unwrapSingleResult(res);
      await refreshAfterMutation(next);
      message.success(successMessage);
    } finally {
      actionLoading.value = false;
    }
  };

  const rerunInstance = async (row: WorkflowInstanceRow) =>
    submitTrigger(row, "START_PROCESS", "已提交重跑");

  const rerunFailedTasks = async (row: WorkflowInstanceRow) =>
    submitTrigger(
      row,
      "START_FAILURE_TASK_PROCESS",
      "已提交失败任务重跑",
    );

  const triggerWorkflow = async (row: WorkflowInstanceRow) => {
    actionLoading.value = true;
    try {
      const res = await triggerEtlWorkflowInstance({
        workflowCode: row.workflowCode,
        workflowVersionNo: row.workflowVersionNo,
        businessKey: row.businessKey || undefined,
        stageCode: row.currentStageCode || undefined,
        force: true,
      });
      const next = unwrapSingleResult(res);
      await refreshAfterMutation(next);
      message.success("已触发新的工作流实例");
    } finally {
      actionLoading.value = false;
    }
  };

  const retryInstance = async (row: WorkflowInstanceRow) => {
    actionLoading.value = true;
    try {
      const res = await retryEtlWorkflowInstance(row.instanceId, {
        stageCode: row.currentStageCode || undefined,
      });
      const next = unwrapSingleResult(res);
      await refreshAfterMutation(next);
      message.success("已提交重试");
    } finally {
      actionLoading.value = false;
    }
  };

  const stopInstance = async (row: WorkflowInstanceRow) => {
    actionLoading.value = true;
    try {
      const res = await stopEtlWorkflowInstance(row.instanceId, {
        reason: "人工停止",
      });
      const next = unwrapSingleResult(res);
      await refreshAfterMutation(next);
      message.success("已停止工作流实例");
    } finally {
      actionLoading.value = false;
    }
  };

  const pauseInstance = async (row: WorkflowInstanceRow) => {
    actionLoading.value = true;
    try {
      const res = await pauseEtlWorkflowInstance(row.instanceId, {
        reason: "人工暂停",
      });
      const next = unwrapSingleResult(res);
      await refreshAfterMutation(next);
      message.success("已暂停工作流实例");
    } finally {
      actionLoading.value = false;
    }
  };

  const syncCurrentStageFilter = async () => {
    if (!query.stageCode) {
      return;
    }
    const validStageCodes = new Set(stageOptions.value.map((item) => item.value));
    if (!validStageCodes.has(query.stageCode)) {
      query.stageCode = "";
    }
  };

  const loadAll = async () => {
    loading.value = true;
    try {
      await loadCatalogs();
      applyDefaultWorkflowSelection();
      await syncCurrentStageFilter();
      await loadList({ silent: true });
    } catch (error) {
      message.error("初始化 ETL 工作流实例页失败");
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const formatStatusLabel = (value?: string) =>
    STATUS_LABELS[normalizeStatus(value)] || formatText(value);

  const formatPlatformLabel = (value?: EtlPlatformType | string | null) => {
    if (!value) {
      return "-";
    }
    const platformType = String(value).trim() as EtlPlatformType;
    const metadata = platformMetadataMap.value.get(platformType);
    return metadata?.platformName || PLATFORM_LABELS[platformType] || platformType;
  };

  const getTaskRows = (row: WorkflowInstanceRow) =>
    row.taskRowsLoaded ? row.taskRows : [];

  const canRerunInstance = (row: WorkflowInstanceRow) =>
    isSucceededStatus(row.status);

  const canRerunFailedTasks = (row: WorkflowInstanceRow) =>
    isFailedStatus(row.status);

  const canStopInstance = (row: WorkflowInstanceRow) =>
    isRunningStatus(row.status);

  const canPauseInstance = (row: WorkflowInstanceRow) =>
    isRunningStatus(row.status);

  void loadAll();

  const page = reactive({
    loading,
    listLoading,
    detailLoading,
    actionLoading,
    rows,
    tableData,
    total,
    pagination,
    query,
    currentFilterSummary,
    runningCount,
    succeededCount,
    failedCount,
    submittedCount,
    workflowOptions,
    stageOptions,
    platformOptions,
    statusOptions,
    detailVisible,
    selectedRow,
    selectedLogs,
    selectedLogStageCode,
    expandedRowKeys,
    allRowsExpanded,
    taskLogVisible,
    taskLogLoading,
    taskLogTitle,
    taskLogContent,
    runQuery,
    resetQuery,
    refreshList,
    handleWorkflowCodeChange,
    handleWorkflowCodeSelect,
    applyDefaultWorkflowSelection,
    handlePageChange,
    handleToggleRowExpand,
    expandAllRows,
    collapseAllRows,
    openDetailDrawer,
    closeDetailDrawer,
    refreshDetailLogs,
    getTaskRows,
    openTaskLog,
    closeTaskLog,
    triggerWorkflow,
    rerunInstance,
    rerunFailedTasks,
    retryInstance,
    stopInstance,
    pauseInstance,
    canRerunInstance,
    canRerunFailedTasks,
    canStopInstance,
    canPauseInstance,
    formatStatusLabel,
    formatPlatformLabel,
    formatDateTime,
    formatJson,
  });

  return { page };
};
