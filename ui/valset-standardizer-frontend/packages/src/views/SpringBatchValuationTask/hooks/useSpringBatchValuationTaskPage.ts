import dayjs from "dayjs";
import { computed, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type {
  SpringBatchValuationTaskBatchRow,
  SpringBatchValuationTaskPageState,
  SpringBatchValuationTaskStage,
  SpringBatchValuationTaskStatus,
  SpringBatchValuationTaskStepRow,
} from "../types";
import {
  batchRetrySpringBatchValuationTasks,
  getSpringBatchValuationTask,
  getSpringBatchValuationTaskSummary,
  listSpringBatchValuationTaskSteps,
  pageSpringBatchValuationTasks,
  type SpringBatchValuationTaskBatchDetailDTO,
  type SpringBatchValuationTaskBatchDTO,
  type SpringBatchValuationTaskQueryParams,
  type SpringBatchValuationTaskStepDTO,
  type SpringBatchValuationTaskSummaryDTO,
} from "@/api/springBatchValuationTask";
import {
  springBatchValuationTaskStageCatalog,
  springBatchValuationTaskStatusCatalog,
  resolveSpringBatchValuationTaskSourceTypeLabel,
} from "../constants";

const PAGE_SIZE = 10;

const defaultQuery = (): SpringBatchValuationTaskQueryParams & {
  batchId: string;
  taskDate: string;
  managerName: string;
  productKeyword: string;
  taskStage: string;
  stage: string;
  status: string;
  sourceType: string;
} => ({
  batchId: "",
  taskDate: dayjs().format("YYYY-MM-DD"),
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

const summaryStatusMap: Record<string, SpringBatchValuationTaskStatus> = {
  RUNNING: "RUNNING",
  SUCCESS: "SUCCESS",
  FAILED: "FAILED",
};

const retryableStatusSet = new Set<SpringBatchValuationTaskStatus>([
  "SUCCESS",
  "FAILED",
  "STOPPED",
]);

const normalizeStatus = (value?: string): SpringBatchValuationTaskStatus => {
  const status = String(value ?? "").trim().toUpperCase();
  if (
    status === "PENDING" ||
    status === "RUNNING" ||
    status === "SUCCESS" ||
    status === "FAILED" ||
    status === "STOPPED"
  ) {
    return status;
  }
  return "PENDING";
};

const statusLabel = (value?: string) =>
  springBatchValuationTaskStatusCatalog.find((item) => item.status === normalizeStatus(value))
    ?.label ?? normalizeStatus(value);

const formatDuration = (durationMs?: number, status?: string) => {
  if (durationMs === undefined || durationMs === null) {
    return normalizeStatus(status) === "RUNNING" ? "运行中" : "-";
  }
  const seconds = Math.max(1, Math.ceil(durationMs / 1000));
  return seconds < 60 ? `${seconds}s` : `${Math.floor(seconds / 60)}m`;
};

const formatTime = (value?: string) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return "-";
  }
  return text.replace("T", " ");
};

const toBatchRow = (
  row: SpringBatchValuationTaskBatchDTO,
  steps: SpringBatchValuationTaskStepRow[] = [],
): SpringBatchValuationTaskBatchRow => ({
  ...row,
  currentStage: normalizeStage(row.currentStage),
  currentStep: normalizeStage(row.currentStep),
  status: normalizeStatus(row.status),
  startedAt: formatTime(row.startedAt),
  endedAt: formatTime(row.endedAt),
  durationText: row.durationText || formatDuration(row.durationMs, row.status),
  currentStageName: row.currentStageName || stageLabel(row.currentStage),
  currentStepName: row.currentStepName || stageLabel(row.currentStep),
  taskStageName: row.taskStageName || stageLabel(row.taskStage),
  sourceTypeName: row.sourceTypeName || resolveSpringBatchValuationTaskSourceTypeLabel(row.sourceType),
  statusName: row.statusName || statusLabel(row.status),
  steps,
});

const toStepRows = (steps: SpringBatchValuationTaskStepDTO[]) =>
  steps.map((step) => ({
    ...step,
    stage: normalizeStage(step.stage),
    step: normalizeStage(step.step),
    status: normalizeStatus(step.status),
    startedAt: formatTime(step.startedAt),
    endedAt: formatTime(step.endedAt),
    durationText: step.durationText || formatDuration(step.durationMs, step.status),
    stageName: step.stageName || stageLabel(step.stage),
    stepName: step.stepName || stageLabel(step.step),
    statusName: step.statusName || statusLabel(step.status),
  }));

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
    default:
      return "default";
  }
};

export const useSpringBatchValuationTaskPage = (): SpringBatchValuationTaskPageState => {
  const loading = ref(false);
  const detailLoading = ref(false);
  const batchRetryLoading = ref(false);
  const tableRef = ref<any>(null);
  const rows = ref<SpringBatchValuationTaskBatchRow[]>([]);
  const totalCount = ref(0);
  const summary = reactive<SpringBatchValuationTaskSummaryDTO>({
    workflowCode: "VALUATION_PARSE",
    totalCount: 0,
    runningCount: 0,
    successCount: 0,
    failedCount: 0,
    stageCatalog: [],
    stepSummaries: [],
  });
  const pagination = reactive({
    current: 1,
    pageSize: PAGE_SIZE,
    total: 0,
  });
  const query = reactive(defaultQuery());
  const selectedBatchIds = ref<string[]>([]);
  const selectedRows = ref<SpringBatchValuationTaskBatchRow[]>([]);
  const selectedRow = ref<SpringBatchValuationTaskBatchRow | null>(null);
  const detail = ref<SpringBatchValuationTaskBatchDetailDTO | null>(null);
  const detailVisible = ref(false);

  const setTableRef = (instance: any) => {
    tableRef.value = instance;
  };

  const load = async () => {
    loading.value = true;
    try {
      const requestQuery = {
        ...query,
        pageIndex: pagination.current,
        pageSize: pagination.pageSize,
      };
      const [summaryResp, pageResp] = await Promise.all([
        getSpringBatchValuationTaskSummary(requestQuery),
        pageSpringBatchValuationTasks(requestQuery),
      ]);
      const summaryData = summaryResp.data ?? {};
      Object.assign(summary, {
        workflowCode: summaryData.workflowCode ?? "VALUATION_PARSE",
        workflowId: summaryData.workflowId,
        versionNo: summaryData.versionNo,
        totalCount: summaryData.totalCount ?? 0,
        runningCount: summaryData.runningCount ?? 0,
        successCount: summaryData.successCount ?? 0,
        failedCount: summaryData.failedCount ?? 0,
        stageCatalog: summaryData.stageCatalog ?? [],
        stepSummaries: summaryData.stepSummaries ?? [],
      });
      const pageData = pageResp.data ?? [];
      rows.value = pageData.map((item) => toBatchRow(item));
      totalCount.value = Number(pageResp.totalCount ?? pageData.length ?? 0);
      pagination.total = totalCount.value;
      const visibleBatchIdSet = new Set(rows.value.map((item) => item.batchId));
      selectedRows.value = selectedRows.value.filter((item) => visibleBatchIdSet.has(item.batchId));
      selectedBatchIds.value = selectedRows.value
        .map((item) => item.batchId)
        .filter((batchId): batchId is string => Boolean(batchId));
      void syncTableSelection(selectedRows.value);
    } catch (error) {
      message.error("加载 批量任务 元数据失败");
      console.error(error);
    } finally {
      loading.value = false;
    }
  };

  const loadDetail = async (row: SpringBatchValuationTaskBatchRow) => {
    if (!row?.batchId) {
      detail.value = null;
      selectedRow.value = null;
      detailVisible.value = false;
      return;
    }
    detailLoading.value = true;
    try {
      const batchId = row.batchId;
      const [detailResp, stepsResp] = await Promise.all([
        getSpringBatchValuationTask(batchId),
        listSpringBatchValuationTaskSteps(batchId),
      ]);
      const detailData = detailResp.data ?? null;
      const stepRows = toStepRows(stepsResp.data ?? []);
      detail.value = detailData ? { ...detailData, steps: stepRows } : null;
      selectedRow.value = {
        ...row,
        ...(detailData?.batch ? toBatchRow(detailData.batch, stepRows) : {}),
        steps: stepRows,
      };
      detailVisible.value = true;
    } catch (error) {
      message.error("加载批次详情失败");
      console.error(error);
    } finally {
      detailLoading.value = false;
    }
  };

  const runQuery = () => {
    pagination.current = 1;
    void load();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    pagination.current = 1;
    void load();
  };

  const handleStatusSelect = (status: string) => {
    const normalizedStatus = normalizeStatus(status);
    const nextStatus = summaryStatusMap[normalizedStatus] ?? normalizedStatus;
    const currentStatus = normalizeStatus(query.status);
    const isSameStatus =
      nextStatus === currentStatus ||
      (nextStatus === "FAILED" && (currentStatus === "FAILED" || currentStatus === "STOPPED"));
    query.status = isSameStatus ? "" : nextStatus;
    pagination.current = 1;
    void load();
  };

  const currentFilterSummary = computed(() => {
    const parts: string[] = [];
    if (query.batchId) {
      parts.push(`批次ID ${query.batchId}`);
    }
    if (query.taskDate) {
      parts.push(`任务日期 ${query.taskDate}`);
    }
    if (query.managerName) {
      parts.push(`管理机构 ${query.managerName}`);
    }
    if (query.productKeyword) {
      parts.push(`产品 ${query.productKeyword}`);
    }
    if (query.taskStage) {
      parts.push(`阶段 ${stageLabel(query.taskStage)}`);
    }
    if (query.step) {
      parts.push(`步骤 ${stageLabel(query.step)}`);
    }
    if (query.status) {
      parts.push(`状态 ${statusLabel(query.status)}`);
    }
    if (query.sourceType) {
      parts.push(`来源类型 ${resolveSpringBatchValuationTaskSourceTypeLabel(query.sourceType)}`);
    }
    return parts.length ? parts.join(" · ") : "全部";
  });

  const handleStageSelect = (stage: string) => {
    const normalizedStage = String(stage ?? "").trim().toUpperCase();
    query.taskStage = normalizedStage;
    query.stage = "";
    query.step = "";
    pagination.current = 1;
    void load();
  };

  const handleStepSelect = (step: string) => {
    const normalizedStep = String(step ?? "").trim().toUpperCase();
    query.step = normalizedStep;
    pagination.current = 1;
    void load();
  };

  const isRetryable = (row?: SpringBatchValuationTaskBatchRow | null) => {
    if (!row || !row.batchId) {
      return false;
    }
    return retryableStatusSet.has(normalizeStatus(row.status));
  };

  const handleSelectionChange = (selected: SpringBatchValuationTaskBatchRow[]) => {
    const nextSelectedRows = (selected ?? []).filter((row) => isRetryable(row));
    selectedRows.value = nextSelectedRows;
    selectedBatchIds.value = nextSelectedRows
      .map((row) => row.batchId)
      .filter((batchId): batchId is string => Boolean(batchId));
  };

  const syncSelectionFromTable = () => {
    const instance = tableRef.value?.getTableInstance?.();
    if (!instance) {
      return;
    }
    const selected = instance.getCheckboxRecords?.() ?? [];
    handleSelectionChange(selected);
  };

  const syncTableSelection = async (nextRows?: SpringBatchValuationTaskBatchRow[]) => {
    const instance = tableRef.value?.getTableInstance?.();
    if (!instance?.clearCheckboxRow || !instance?.setCheckboxRow) {
      return;
    }
    await instance.clearCheckboxRow();
    const sourceRows = nextRows?.length ? nextRows : selectedRows.value;
    const selectableRows = sourceRows.filter((row) => isRetryable(row));
    if (selectableRows.length) {
      instance.setCheckboxRow(selectableRows, true);
    }
  };

  const clearSelection = async () => {
    selectedRows.value = [];
    selectedBatchIds.value = [];
    const instance = tableRef.value?.getTableInstance?.();
    if (instance?.clearCheckboxRow) {
      await instance.clearCheckboxRow();
    }
  };

  const selectCurrentPageRows = async () => {
    const currentRows = rows.value.filter((row) => isRetryable(row));
    selectedRows.value = currentRows;
    selectedBatchIds.value = currentRows
      .map((row) => row.batchId)
      .filter((batchId): batchId is string => Boolean(batchId));
    await syncTableSelection(currentRows);
    syncSelectionFromTable();
  };

  const refreshAfterBatchAction = async (preferredBatchId?: string) => {
    await load();
    const nextRows = rows.value.map((row) => ({
      ...row,
      steps: row.steps ?? [],
    }));
    const current = preferredBatchId
      ? nextRows.find((row) => row.batchId === preferredBatchId) ?? null
      : selectedRow.value
        ? nextRows.find((row) => row.batchId === selectedRow.value?.batchId) ?? null
        : null;
    if (current) {
      selectedRow.value = current;
      await loadDetail(current);
      return;
    }
    if (detailVisible.value) {
      detail.value = null;
      selectedRow.value = null;
      detailVisible.value = false;
    }
  };

  const batchRetrySelected = async () => {
    if (!selectedBatchIds.value.length) {
      message.warning("请先选择要重新解析的批次");
      return;
    }
    const invalidRows = selectedRows.value.filter((row) => !isRetryable(row));
    if (invalidRows.length > 0) {
      message.warning("当前选择中包含不可重试的批次，请仅保留已完成、失败或已停止的批次");
      return;
    }
    batchRetryLoading.value = true;
    try {
      await batchRetrySpringBatchValuationTasks({
        batchIds: [...selectedBatchIds.value],
        reason: "批量重新解析",
      });
      message.success(`已提交 ${selectedBatchIds.value.length} 个批次重新解析`);
      await clearSelection();
      await syncTableSelection([]);
      await refreshAfterBatchAction();
    } catch (error) {
      message.error("批量重新解析提交失败");
      console.error(error);
    } finally {
      batchRetryLoading.value = false;
    }
  };

  const retryBatchRow = async (row: SpringBatchValuationTaskBatchRow) => {
    if (!isRetryable(row)) {
      message.warning("当前批次不支持重新解析");
      return;
    }
    batchRetryLoading.value = true;
    try {
      await batchRetrySpringBatchValuationTasks({
        batchIds: [row.batchId].filter((batchId): batchId is string => Boolean(batchId)),
        reason: "单批次重新解析",
      });
      message.success("已提交批次重新解析");
      await refreshAfterBatchAction(row.batchId);
    } catch (error) {
      message.error("重新解析提交失败");
      console.error(error);
    } finally {
      batchRetryLoading.value = false;
    }
  };

  const handlePageChange = (params: { current: number; pageSize: number }) => {
    pagination.current = params.current;
    pagination.pageSize = params.pageSize;
    void load();
  };

  const openDetailDrawer = (row: SpringBatchValuationTaskBatchRow) => {
    void loadDetail(row);
  };

  const closeDetailDrawer = () => {
    detailVisible.value = false;
  };

  void load();

  return reactive({
    tableRef,
    setTableRef,
    loading: computed(() => loading.value),
    detailLoading: computed(() => detailLoading.value),
    batchRetryLoading: computed(() => batchRetryLoading.value),
    rows: computed(() => rows.value),
    totalCount: computed(() => totalCount.value),
    summary,
    currentFilterSummary,
    pagination,
    query,
    selectedBatchIds: computed(() => selectedBatchIds.value),
    selectedRows: computed(() => selectedRows.value),
    selectedRow: computed(() => selectedRow.value),
    detail: computed(() => detail.value),
    detailVisible: computed(() => detailVisible.value),
    runQuery,
    resetQuery,
    handlePageChange,
    handleStatusSelect,
    handleStageSelect,
    handleStepSelect,
    handleSelectionChange,
    syncSelectionFromTable,
    clearSelection,
    selectCurrentPageRows,
    canRetryBatch: isRetryable,
    batchRetrySelected,
    retryBatchRow,
    syncTableSelection,
    openDetailDrawer,
    closeDetailDrawer,
    formatStatusColor,
  }) as unknown as SpringBatchValuationTaskPageState;
};
