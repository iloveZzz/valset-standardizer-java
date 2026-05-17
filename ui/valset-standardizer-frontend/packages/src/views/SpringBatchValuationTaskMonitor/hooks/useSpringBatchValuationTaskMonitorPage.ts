import { computed, nextTick, onBeforeUnmount, reactive, ref, watch, type ComponentPublicInstance } from "vue";
import dayjs from "dayjs";
import { message } from "ant-design-vue";
import { Position } from "@vue-flow/core";
import {
  executeSpringBatchValuationTask,
  pageSpringBatchValuationTasks,
  retrySpringBatchValuationTask,
  retrySpringBatchValuationTaskStep,
  stopSpringBatchValuationTask,
  type SpringBatchValuationTaskActionResultDTO,
  type SpringBatchValuationTaskBatchDTO,
  type SpringBatchValuationTaskQueryParams,
  type SpringBatchValuationTaskStepDTO,
} from "@/api/springBatchValuationTask";
import {
  getSpringBatchValuationTaskTrace,
  type SpringBatchValuationTaskTraceDTO,
  type SpringBatchValuationTaskTraceRecordDTO,
} from "@/api/springBatchValuationTaskTrace";
import { unwrapSingleResult } from "@/utils/api-response";
import {
  springBatchValuationTaskStageCatalog,
  springBatchValuationTaskStatusCatalog,
  resolveSpringBatchValuationTaskSourceTypeLabel,
} from "@/views/SpringBatchValuationTask/constants";
import type {
  SpringBatchValuationTaskBatchRow,
  SpringBatchValuationTaskQueryState,
  SpringBatchValuationTaskStage,
  SpringBatchValuationTaskStatus,
  SpringBatchValuationTaskStepRow,
} from "@/views/SpringBatchValuationTask/types";
import type {
  SpringBatchValuationTaskMonitorPageState,
  ValuationTraceEdge,
  ValuationTraceNode,
  ValuationTraceNodeData,
} from "../types";

const PAGE_SIZE = 15;
const LIST_PREFETCH_THRESHOLD = 160;
const AUTO_REFRESH_INTERVAL = 5000;
const FLOW_MAIN_X = 520;
const FLOW_START_Y = 30;
const FLOW_VERTICAL_GAP = 150;

const defaultQuery = (): SpringBatchValuationTaskQueryState => ({
  batchId: "",
  taskDate: dayjs().format("YYYY-MM-DD"),
  businessDate: "",
  managerName: "",
  productKeyword: "",
  taskStage: "",
  stage: "",
  step: "",
  status: "",
  sourceType: "",
  pageIndex: 1,
  pageSize: PAGE_SIZE,
});

const normalizeStage = (value?: string): SpringBatchValuationTaskStage => {
  const stage = String(value ?? "").trim().toUpperCase();
  return (
    springBatchValuationTaskStageCatalog.find((item) => item.stage === stage)?.stage ??
    "FILE_PARSE"
  );
};

const stageLabel = (value?: string) =>
  springBatchValuationTaskStageCatalog.find((item) => item.stage === normalizeStage(value))
    ?.label ?? normalizeStage(value);

const normalizeStatus = (value?: string): SpringBatchValuationTaskStatus | "MISSING" => {
  const status = String(value ?? "").trim().toUpperCase();
  if (status === "COMPLETED" || status === "PARSED" || status === "DELIVERED") {
    return "SUCCESS";
  }
  if (status === "STARTING" || status === "STARTED" || status === "PARSING") {
    return "RUNNING";
  }
  if (
    status === "PENDING" ||
    status === "RUNNING" ||
    status === "SUCCESS" ||
    status === "FAILED" ||
    status === "STOPPED" ||
    status === "MISSING"
  ) {
    return status as SpringBatchValuationTaskStatus | "MISSING";
  }
  return status ? "PENDING" : "MISSING";
};

const statusLabel = (value?: string) => {
  const status = normalizeStatus(value);
  if (status === "MISSING") {
    return "缺失";
  }
  return springBatchValuationTaskStatusCatalog.find((item) => item.status === status)?.label ?? status;
};

const isActiveStatus = (value?: string) =>
  ["RUNNING", "PENDING"].includes(normalizeStatus(value));

const isFailedStatus = (value?: string) => normalizeStatus(value) === "FAILED";

const isSuccessStatus = (value?: string) => normalizeStatus(value) === "SUCCESS";

const formatDuration = (durationMs?: number, status?: string) => {
  if (durationMs === undefined || durationMs === null) {
    return normalizeStatus(status) === "RUNNING" ? "运行中" : "-";
  }
  const seconds = Math.max(1, Math.ceil(durationMs / 1000));
  return seconds < 60 ? `${seconds}s` : `${Math.floor(seconds / 60)}m`;
};

const formatTime = (value?: string) => {
  const text = String(value ?? "").trim();
  return text ? text.replace("T", " ").replace(/\.\d+$/, "").slice(0, 19) : "-";
};

const formatText = (value?: string | number | null) => {
  const text = String(value ?? "").trim();
  return text || "-";
};

const toBatchRow = (row: SpringBatchValuationTaskBatchDTO): SpringBatchValuationTaskBatchRow => ({
  ...row,
  currentStage: normalizeStage(row.currentStage),
  currentStep: normalizeStage(row.currentStep),
  status: normalizeStatus(row.status) === "MISSING" ? "PENDING" : normalizeStatus(row.status),
  startedAt: formatTime(row.startedAt),
  endedAt: formatTime(row.endedAt),
  durationText: row.durationText || formatDuration(row.durationMs, row.status),
  currentStageName: row.currentStageName || stageLabel(row.currentStage),
  currentStepName: row.currentStepName || stageLabel(row.currentStep),
  taskStageName: row.taskStageName || stageLabel(row.taskStage),
  sourceTypeName: row.sourceTypeName || resolveSpringBatchValuationTaskSourceTypeLabel(row.sourceType),
  statusName: row.statusName || statusLabel(row.status),
  steps: [],
});

const toStepRow = (step: SpringBatchValuationTaskStepDTO): SpringBatchValuationTaskStepRow => ({
  ...step,
  stage: normalizeStage(step.stage),
  step: normalizeStage(step.step),
  status: normalizeStatus(step.status) === "MISSING" ? "PENDING" : normalizeStatus(step.status),
  startedAt: formatTime(step.startedAt),
  endedAt: formatTime(step.endedAt),
  durationText: step.durationText || formatDuration(step.durationMs, step.status),
  stageName: step.stageName || stageLabel(step.stage),
  stepName: step.stepName || stageLabel(step.step),
  statusName: step.statusName || statusLabel(step.status),
});

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

const createNodeData = (
  data: Partial<ValuationTraceNodeData> & {
    kind: ValuationTraceNodeData["kind"];
    title: string;
  },
): ValuationTraceNodeData => {
  const status = normalizeStatus(data.status);
  return {
    kind: data.kind,
    title: data.title,
    subtitle: data.subtitle ?? "",
    status,
    statusLabel: data.statusLabel ?? statusLabel(status),
    timeLabel: data.timeLabel ?? "-",
    meta: data.meta ?? "",
    message: data.message ?? "",
    active: data.active ?? isActiveStatus(status),
    failed: data.failed ?? isFailedStatus(status),
    missing: data.missing ?? status === "MISSING",
    payload: data.payload,
  };
};

const createEdge = (source: string, target: string, state: string): ValuationTraceEdge => {
  const status = normalizeStatus(state);
  const active = isActiveStatus(status);
  const success = isSuccessStatus(status);
  const failed = isFailedStatus(status);
  return {
    id: `${source}-${target}`,
    source,
    target,
    type: "smoothstep",
    animated: active,
    class: {
      "valuation-monitor-edge": true,
      "valuation-monitor-edge--active": active,
      "valuation-monitor-edge--success": success,
      "valuation-monitor-edge--failed": failed,
      "valuation-monitor-edge--muted": !active && !success && !failed,
    },
    data: { state: status },
  };
};

const createMonitorNode = (
  id: string,
  y: number,
  data: ValuationTraceNodeData,
): ValuationTraceNode => ({
  id,
  type: "monitor",
  position: { x: FLOW_MAIN_X, y },
  sourcePosition: Position.Bottom,
  targetPosition: Position.Top,
  data,
});

const recordPayload = (record?: SpringBatchValuationTaskTraceRecordDTO) => ({
  ...record,
  attributes: record?.attributes ?? {},
});

const recordNodeData = (
  kind: ValuationTraceNodeData["kind"],
  title: string,
  fallbackSubtitle: string,
  record?: SpringBatchValuationTaskTraceRecordDTO,
): ValuationTraceNodeData =>
  createNodeData({
    kind,
    title: record?.name || title,
    subtitle: record?.id || fallbackSubtitle,
    status: record?.status || "MISSING",
    statusLabel: record?.statusName || statusLabel(record?.status),
    timeLabel: formatTime(record?.startedAt || record?.endedAt),
    meta: record?.logRef || "-",
    message: record?.errorMessage || "",
    payload: recordPayload(record),
  });

const buildGraph = (
  trace: SpringBatchValuationTaskTraceDTO | null,
  fallbackBatch: SpringBatchValuationTaskBatchRow | null,
): { nodes: ValuationTraceNode[]; edges: ValuationTraceEdge[] } => {
  if (!trace && !fallbackBatch) {
    return { nodes: [], edges: [] };
  }

  const batch = trace?.batch ?? fallbackBatch ?? {};
  const steps = (trace?.taskSteps ?? []).map(toStepRow);
  const stepStatus = steps.find((item) => isFailedStatus(item.status))?.status ||
    steps.find((item) => isActiveStatus(item.status))?.status ||
    steps[steps.length - 1]?.status ||
    trace?.jobExecution?.status ||
    batch.status ||
    "MISSING";

  const parseTaskRecord: SpringBatchValuationTaskTraceRecordDTO = {
    id: formatText(batch.batchId),
    type: "PARSE_TASK",
    name: "估值解析任务",
    status: batch.status || trace?.jobExecution?.status || "MISSING",
    statusName: batch.statusName || statusLabel(batch.status),
    upstreamId: trace?.parseQueue?.id,
    downstreamId: trace?.jobExecution?.id,
    startedAt: batch.startedAt,
    endedAt: batch.endedAt,
    durationMs: batch.durationMs,
    errorCode: batch.lastErrorCode,
    errorMessage: batch.lastErrorMessage,
    inputSummary: batch.batchName,
    outputSummary: `当前阶段 ${batch.currentStageName || stageLabel(batch.currentStage)}`,
    logRef: batch.batchId ? `valuation-task:${batch.batchId}` : undefined,
    attributes: {
      batchId: batch.batchId,
      productCode: batch.productCode,
      productName: batch.productName,
      managerName: batch.managerName,
      businessDate: batch.businessDate,
    },
  };

  const resultRecord: SpringBatchValuationTaskTraceRecordDTO = {
    id: batch.batchId || trace?.resultSummary?.status || "result",
    type: "RESULT",
    name: "解析结果",
    status: trace?.resultSummary?.status || batch.status || "MISSING",
    statusName: trace?.resultSummary?.statusName || batch.statusName || statusLabel(batch.status),
    startedAt: trace?.resultSummary?.startedAt || batch.startedAt,
    endedAt: trace?.resultSummary?.endedAt || batch.endedAt,
    durationMs: trace?.resultSummary?.durationMs || batch.durationMs,
    errorCode: trace?.resultSummary?.errorCode || batch.lastErrorCode,
    errorMessage: trace?.resultSummary?.errorMessage || batch.lastErrorMessage,
    inputSummary: trace?.resultSummary?.inputSummary,
    outputSummary: trace?.resultSummary?.outputSummary,
    logRef: batch.batchId ? `result:${batch.batchId}` : undefined,
  };

  const nodes: ValuationTraceNode[] = [
    createMonitorNode(
      "transfer-object",
      FLOW_START_Y,
      recordNodeData("transferObject", "投递对象", "未找到投递对象", trace?.transferObject),
    ),
    createMonitorNode(
      "parse-queue",
      FLOW_START_Y + FLOW_VERTICAL_GAP,
      recordNodeData("parseQueue", "解析队列", "未生成队列", trace?.parseQueue),
    ),
    createMonitorNode(
      "parse-task",
      FLOW_START_Y + FLOW_VERTICAL_GAP * 2,
      recordNodeData("parseTask", "估值解析任务", "未生成任务", parseTaskRecord),
    ),
    createMonitorNode(
      "job-execution",
      FLOW_START_Y + FLOW_VERTICAL_GAP * 3,
      recordNodeData("jobExecution", "批量任务 Job", "无 Batch Execution", trace?.jobExecution),
    ),
  ];

  if (steps.length) {
    steps.forEach((step, index) => {
      nodes.push(
        createMonitorNode(
          `step-${index}`,
          FLOW_START_Y + FLOW_VERTICAL_GAP * (4 + index),
          createNodeData({
            kind: "stepExecution",
            title: step.stepName || "Batch Step",
            subtitle: step.stepId || "-",
            status: step.status,
            statusLabel: step.statusName || statusLabel(step.status),
            timeLabel: step.startedAt || "-",
            meta: step.logRef || `runNo ${step.runNo ?? "-"}`,
            message: step.errorMessage || "",
            payload: step as unknown as Record<string, unknown>,
          }),
        ),
      );
    });
  } else {
    nodes.push(
      createMonitorNode(
        "step-empty",
        FLOW_START_Y + FLOW_VERTICAL_GAP * 4,
        createNodeData({
          kind: "stepExecution",
          title: "Batch Step",
          subtitle: "暂无 Step",
          status: "MISSING",
          timeLabel: "-",
          meta: "BATCH_STEP_EXECUTION",
          message: "未查询到步骤执行记录",
        }),
      ),
    );
  }

  nodes.push(
    createMonitorNode(
      "result",
      FLOW_START_Y + FLOW_VERTICAL_GAP * (5 + Math.max(0, steps.length - 1)),
      recordNodeData("result", "解析结果", "暂无解析结果", resultRecord),
    ),
  );

  const edges: ValuationTraceEdge[] = [
    createEdge("transfer-object", "parse-queue", trace?.parseQueue?.status || trace?.transferObject?.status || "MISSING"),
    createEdge("parse-queue", "parse-task", batch.status || trace?.parseQueue?.status || "MISSING"),
    createEdge("parse-task", "job-execution", trace?.jobExecution?.status || batch.status || "MISSING"),
  ];
  const stepNodeIds = steps.length ? steps.map((_, index) => `step-${index}`) : ["step-empty"];
  stepNodeIds.forEach((stepNodeId, index) => {
    edges.push(createEdge(index === 0 ? "job-execution" : stepNodeIds[index - 1], stepNodeId, steps[index]?.status || stepStatus));
  });
  edges.push(createEdge(stepNodeIds[stepNodeIds.length - 1], "result", resultRecord.status || stepStatus));
  return { nodes, edges };
};

export const useSpringBatchValuationTaskMonitorPage = (): {
  page: SpringBatchValuationTaskMonitorPageState;
} => {
  const query = reactive(defaultQuery());
  const rows = ref<SpringBatchValuationTaskBatchRow[]>([]);
  const currentPage = ref(1);
  const total = ref(0);
  const loading = ref(false);
  const loadingMore = ref(false);
  const hasMoreRows = ref(true);
  const graphLoading = ref(false);
  const actionLoading = ref(false);
  const autoRefresh = ref(false);
  const listContainerRef = ref<HTMLElement | null>(null);
  const selectedRow = ref<SpringBatchValuationTaskBatchRow | null>(null);
  const selectedTrace = ref<SpringBatchValuationTaskTraceDTO | null>(null);
  const nodes = ref<ValuationTraceNode[]>([]);
  const edges = ref<ValuationTraceEdge[]>([]);
  const detailVisible = ref(false);
  const selectedDetail = ref(null as SpringBatchValuationTaskMonitorPageState["selectedDetail"]);
  let listRequestId = 0;
  let graphRequestId = 0;
  let refreshTimer: number | null = null;

  const totalCount = computed(() => Number(total.value ?? 0));
  const selectedBatchId = computed(() => selectedRow.value?.batchId ?? "");
  const canAutoRefresh = computed(() =>
    ["RUNNING", "PENDING"].includes(normalizeStatus(selectedRow.value?.status)),
  );
  const graphSummary = computed(() => {
    if (!selectedRow.value) {
      return "请选择左侧任务后查看投递、队列、解析任务、Batch Job、Step 与解析结果链路。";
    }
    const stepCount = selectedTrace.value?.taskSteps?.length ?? 0;
    return `${selectedRow.value.batchName || selectedRow.value.batchId} · ${selectedRow.value.statusName || selectedRow.value.status} · Step ${stepCount} 个`;
  });

  const resetListState = () => {
    rows.value = [];
    currentPage.value = 1;
    total.value = 0;
    hasMoreRows.value = true;
  };

  const maybeAutoLoadMore = async () => {
    if (loading.value || loadingMore.value) {
      return;
    }
    await nextTick();
    const container = listContainerRef.value;
    if (!container || !hasMoreRows.value) {
      return;
    }
    if (container.scrollHeight <= container.clientHeight + 8) {
      await loadList({ append: true });
      await maybeAutoLoadMore();
    }
  };

  const loadList = async (options: { append?: boolean } = {}) => {
    const append = options.append === true;
    const requestId = ++listRequestId;
    if (append) {
      loadingMore.value = true;
    } else {
      loading.value = true;
      resetListState();
    }
    try {
      const pageIndex = append ? currentPage.value + 1 : 1;
      const params: SpringBatchValuationTaskQueryParams = {
        ...query,
        pageIndex,
        pageSize: PAGE_SIZE,
      };
      const res = await pageSpringBatchValuationTasks(params);
      if (requestId !== listRequestId) {
        return;
      }
      const data = (res.data ?? []).map(toBatchRow);
      rows.value = append ? [...rows.value, ...data] : data;
      currentPage.value = pageIndex;
      total.value = Number(res.totalCount ?? rows.value.length);
      hasMoreRows.value =
        rows.value.length < total.value && data.length === PAGE_SIZE;
      if (selectedRow.value?.batchId) {
        const nextSelected = rows.value.find((item) => item.batchId === selectedRow.value?.batchId);
        if (nextSelected) {
          selectedRow.value = nextSelected;
        }
      }
    } catch (error) {
      message.error("加载估值解析任务列表失败");
      console.error(error);
      if (!append) {
        resetListState();
      } else {
        hasMoreRows.value = false;
      }
    } finally {
      if (requestId === listRequestId) {
        loading.value = false;
        loadingMore.value = false;
      }
    }
  };

  const loadTrace = async (batchId: string, options: { silent?: boolean } = {}) => {
    if (!batchId) {
      selectedTrace.value = null;
      nodes.value = [];
      edges.value = [];
      return;
    }
    const requestId = ++graphRequestId;
    if (!options.silent) {
      graphLoading.value = true;
    }
    try {
      const res = await getSpringBatchValuationTaskTrace(batchId);
      if (requestId !== graphRequestId) {
        return;
      }
      const trace = unwrapSingleResult(res) ?? null;
      selectedTrace.value = trace;
      const graph = buildGraph(trace, selectedRow.value);
      nodes.value = graph.nodes;
      edges.value = graph.edges;
    } catch (error) {
      message.error("加载解析链路失败，已保留当前链路图");
      console.error(error);
    } finally {
      if (requestId === graphRequestId) {
        graphLoading.value = false;
      }
    }
  };

  const stopAutoRefresh = () => {
    if (refreshTimer !== null) {
      window.clearInterval(refreshTimer);
      refreshTimer = null;
    }
  };

  const clearSelectedTask = () => {
    selectedRow.value = null;
    selectedTrace.value = null;
    nodes.value = [];
    edges.value = [];
    detailVisible.value = false;
    selectedDetail.value = null;
    autoRefresh.value = false;
    stopAutoRefresh();
  };

  const startAutoRefresh = () => {
    stopAutoRefresh();
    if (!autoRefresh.value || !canAutoRefresh.value || !selectedBatchId.value) {
      return;
    }
    refreshTimer = window.setInterval(() => {
      if (!selectedBatchId.value || !canAutoRefresh.value) {
        autoRefresh.value = false;
        stopAutoRefresh();
        return;
      }
      void loadTrace(selectedBatchId.value, { silent: true });
    }, AUTO_REFRESH_INTERVAL);
  };

  const selectTask = async (row: SpringBatchValuationTaskBatchRow) => {
    selectedRow.value = row;
    detailVisible.value = false;
    selectedDetail.value = null;
    await loadTrace(row.batchId || "");
    autoRefresh.value = canAutoRefresh.value;
    startAutoRefresh();
  };

  const refreshGraph = async () => {
    if (!selectedBatchId.value) {
      message.warning("请先选择任务");
      return;
    }
    await Promise.all([loadList(), loadTrace(selectedBatchId.value)]);
    await maybeAutoLoadMore();
  };

  const runQuery = () => {
    clearSelectedTask();
    void loadList().then(() => {
      void maybeAutoLoadMore();
    });
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    clearSelectedTask();
    void loadList().then(() => {
      void maybeAutoLoadMore();
    });
  };

  const setListContainerRef = (el: Element | ComponentPublicInstance | null) => {
    if (el instanceof HTMLElement) {
      listContainerRef.value = el;
      return;
    }
    const element = el && "$el" in el ? el.$el : null;
    listContainerRef.value = element instanceof HTMLElement ? element : null;
  };

  const loadMoreRows = () => {
    if (loading.value || loadingMore.value || !hasMoreRows.value) {
      return;
    }
    void loadList({ append: true }).then(() => {
      void maybeAutoLoadMore();
    });
  };

  const handleListScroll = (event: Event) => {
    const target = event.currentTarget as HTMLElement | null;
    if (!target || loading.value || loadingMore.value || !hasMoreRows.value) {
      return;
    }
    if (
      target.scrollTop + target.clientHeight >=
      target.scrollHeight - LIST_PREFETCH_THRESHOLD
    ) {
      loadMoreRows();
    }
  };

  const openNodeDetail = (node: ValuationTraceNodeData) => {
    selectedDetail.value = {
      node,
      trace: selectedTrace.value,
      steps: (selectedTrace.value?.taskSteps ?? []).map(toStepRow),
    };
    detailVisible.value = true;
  };

  const closeDetail = () => {
    detailVisible.value = false;
  };

  const toggleAutoRefresh = (checked: boolean) => {
    autoRefresh.value = checked && canAutoRefresh.value;
    startAutoRefresh();
  };

  const runAction = async (
    runner: (batchId: string) => Promise<{ data?: SpringBatchValuationTaskActionResultDTO }>,
    successText: string,
  ) => {
    if (!selectedBatchId.value) {
      message.warning("请先选择任务");
      return null;
    }
    actionLoading.value = true;
    try {
      const res = await runner(selectedBatchId.value);
      message.success(successText);
      await refreshGraph();
      return res.data ?? null;
    } catch (error) {
      message.error(`${successText}失败`);
      console.error(error);
      return null;
    } finally {
      actionLoading.value = false;
    }
  };

  const executeSelected = () =>
    runAction((batchId) => executeSpringBatchValuationTask(batchId, { reason: "链路监控手动执行" }), "已提交执行");

  const retrySelected = () =>
    runAction((batchId) => retrySpringBatchValuationTask(batchId, { reason: "链路监控全流程重跑" }), "已提交全流程重跑");

  const stopSelected = () =>
    runAction((batchId) => stopSpringBatchValuationTask(batchId, { reason: "链路监控停止" }), "已提交停止");

  const retryStep = async (step: SpringBatchValuationTaskStepRow) => {
    if (!selectedBatchId.value || !step.stepId) {
      message.warning("缺少可重跑步骤");
      return null;
    }
    actionLoading.value = true;
    try {
      const res = await retrySpringBatchValuationTaskStep(selectedBatchId.value, step.stepId, {
        reason: "链路监控步骤重跑",
      });
      message.success("已提交步骤重跑");
      await refreshGraph();
      return res.data ?? null;
    } catch (error) {
      message.error("步骤重跑提交失败");
      console.error(error);
      return null;
    } finally {
      actionLoading.value = false;
    }
  };

  const canExecute = () =>
    ["PENDING", "STOPPED", "FAILED"].includes(normalizeStatus(selectedRow.value?.status));
  const canRetry = () =>
    ["SUCCESS", "FAILED", "STOPPED"].includes(normalizeStatus(selectedRow.value?.status));
  const canStop = () => normalizeStatus(selectedRow.value?.status) === "RUNNING";
  const canRetryStep = (step?: SpringBatchValuationTaskStepRow | null) =>
    Boolean(step?.stepId) && ["SUCCESS", "FAILED", "STOPPED"].includes(normalizeStatus(step?.status));

  const formatStatusColor = (status?: string) => {
    switch (normalizeStatus(status)) {
      case "SUCCESS":
        return "green";
      case "FAILED":
        return "red";
      case "RUNNING":
        return "blue";
      case "STOPPED":
        return "orange";
      case "MISSING":
        return "default";
      default:
        return "default";
    }
  };

  const getTaskInitial = (row?: SpringBatchValuationTaskBatchRow | null) => {
    const text =
      row?.productName ||
      row?.productCode ||
      row?.managerName ||
      row?.batchName ||
      row?.batchId ||
      "任";
    return String(text).trim().slice(0, 1).toUpperCase();
  };

  watch([autoRefresh, canAutoRefresh, selectedBatchId], startAutoRefresh);
  onBeforeUnmount(stopAutoRefresh);

  void loadList().then(() => {
    void maybeAutoLoadMore();
  });

  return {
    page: reactive({
      loading: computed(() => loading.value),
      loadingMore: computed(() => loadingMore.value),
      graphLoading: computed(() => graphLoading.value),
      actionLoading: computed(() => actionLoading.value),
      autoRefresh: computed(() => autoRefresh.value),
      query,
      rows: computed(() => rows.value),
      totalCount,
      hasMoreRows: computed(() => hasMoreRows.value),
      selectedRow: computed(() => selectedRow.value),
      selectedBatchId,
      selectedTrace: computed(() => selectedTrace.value),
      nodes: computed(() => nodes.value),
      edges: computed(() => edges.value),
      detailVisible: computed(() => detailVisible.value),
      selectedDetail: computed(() => selectedDetail.value),
      graphSummary,
      canAutoRefresh,
      runQuery,
      resetQuery,
      setListContainerRef,
      handleListScroll,
      loadMoreRows,
      selectTask,
      refreshGraph,
      openNodeDetail,
      closeDetail,
      toggleAutoRefresh,
      executeSelected,
      retrySelected,
      stopSelected,
      retryStep,
      canExecute,
      canRetry,
      canStop,
      canRetryStep,
      formatStatusColor,
      getTaskInitial,
      formatJson,
    }) as unknown as SpringBatchValuationTaskMonitorPageState,
  };
};
