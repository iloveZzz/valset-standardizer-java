import dayjs from "dayjs";
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type {
  BatchValuationTaskBatchRow,
  BatchValuationTaskAutoRefreshInterval,
  BatchValuationTaskPageState,
  BatchValuationTaskStage,
  BatchValuationTaskStandardTab,
  BatchValuationTaskStatus,
  BatchValuationTaskStepRow,
} from "../types";
import {
  batchRetryBatchValuationTasks,
  downloadBatchValuationTaskRawWorkbook,
  exportBatchValuationTaskStandardDataSheet,
  getBatchValuationTask,
  getBatchValuationTaskRawWorkbook,
  getBatchValuationTaskSummary,
  getBatchValuationTaskStandardBasic,
  listBatchValuationTaskStandardMetrics,
  listBatchValuationTaskStandardSubjects,
  listBatchValuationTaskSteps,
  pageBatchValuationTasks,
  type BatchValuationTaskBatchDetailDTO,
  type BatchValuationTaskBatchDTO,
  type BatchValuationTaskQueryParams,
  type BatchValuationTaskRawWorkbookDTO,
  type BatchValuationTaskStandardBasicDTO,
  type BatchValuationTaskStandardMetricDTO,
  type BatchValuationTaskStandardSubjectDTO,
  type BatchValuationTaskStepDTO,
  type BatchValuationTaskSummaryDTO,
} from "@/api/batchValuationTask";
import {
  batchValuationTaskStageCatalog,
  batchValuationTaskStatusCatalog,
  resolveBatchValuationTaskSourceTypeLabel,
} from "../constants";

const PAGE_SIZE = 10;
const AUTO_REFRESH_OPTIONS: Array<{
  label: string;
  value: BatchValuationTaskAutoRefreshInterval;
}> = [
  { label: "0s（停止）", value: 0 },
  { label: "5s", value: 5 },
  { label: "10s", value: 10 },
  { label: "30s", value: 30 },
  { label: "60s", value: 60 },
];
const DEFAULT_AUTO_REFRESH_INTERVAL: BatchValuationTaskAutoRefreshInterval = 10;

const defaultQuery = (): BatchValuationTaskQueryParams & {
  batchId: string;
  taskDate: string;
  businessDate: string;
  managerName: string;
  productKeyword: string;
  taskStage: string;
  stage: string;
  status: string;
  sourceType: string;
} => ({
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

const normalizeStage = (value?: string): BatchValuationTaskStage => {
  const stage = String(value ?? "").trim().toUpperCase();
  return (
    batchValuationTaskStageCatalog.find((item) => item.stage === stage)?.stage ??
    "FILE_PARSE"
  );
};

const stageLabel = (value?: string) =>
  batchValuationTaskStageCatalog.find((item) => item.stage === normalizeStage(value))
    ?.label ?? normalizeStage(value);

const summaryStatusMap: Record<string, BatchValuationTaskStatus> = {
  RUNNING: "RUNNING",
  SUCCESS: "SUCCESS",
  FAILED: "FAILED",
};

const retryableStatusSet = new Set<BatchValuationTaskStatus>([
  "SUCCESS",
  "FAILED",
  "STOPPED",
]);

const normalizeStatus = (value?: string): BatchValuationTaskStatus => {
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
  batchValuationTaskStatusCatalog.find((item) => item.status === normalizeStatus(value))
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

const formatLastUpdatedAt = () => dayjs().format("HH:mm:ss");

const normalizeAutoRefreshInterval = (
  value?: number,
): BatchValuationTaskAutoRefreshInterval =>
  AUTO_REFRESH_OPTIONS.find((item) => item.value === Number(value))?.value ??
  DEFAULT_AUTO_REFRESH_INTERVAL;

const toBatchRow = (
  row: BatchValuationTaskBatchDTO,
  steps: BatchValuationTaskStepRow[] = [],
): BatchValuationTaskBatchRow => ({
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
  sourceTypeName: row.sourceTypeName || resolveBatchValuationTaskSourceTypeLabel(row.sourceType),
  statusName: row.statusName || statusLabel(row.status),
  steps,
});

const toStepRows = (steps: BatchValuationTaskStepDTO[]) =>
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

const normalizeStandardDataKeyword = (value?: string) => String(value ?? "").trim();

const parseContentDispositionFileName = (value: string | undefined) => {
  if (!value) {
    return "";
  }
  const utf8FileName = value.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8FileName?.[1]) {
    try {
      return decodeURIComponent(utf8FileName[1].trim());
    } catch {
      return utf8FileName[1].trim();
    }
  }
  const plainFileName = value.match(/filename="?([^";]+)"?/i);
  return plainFileName?.[1]?.trim() ?? "";
};

const triggerBrowserDownload = (blob: Blob, fileName: string) => {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = fileName || "估值标准数据.xlsx";
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
};

export const useBatchValuationTaskPage = (): BatchValuationTaskPageState => {
  const loading = ref(false);
  const detailLoading = ref(false);
  const batchRetryLoading = ref(false);
  const tableRef = ref<any>(null);
  const autoRefreshInterval = ref<BatchValuationTaskAutoRefreshInterval>(
    DEFAULT_AUTO_REFRESH_INTERVAL,
  );
  const lastUpdatedAt = ref("");
  let autoRefreshTimer: number | null = null;
  const rows = ref<BatchValuationTaskBatchRow[]>([]);
  const totalCount = ref(0);
  const summary = reactive<BatchValuationTaskSummaryDTO>({
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
  const selectedRows = ref<BatchValuationTaskBatchRow[]>([]);
  const selectedRow = ref<BatchValuationTaskBatchRow | null>(null);
  const detail = ref<BatchValuationTaskBatchDetailDTO | null>(null);
  const detailVisible = ref(false);
  const standardDataVisible = ref(false);
  const standardDataActiveTab = ref<BatchValuationTaskStandardTab>("basic");
  const standardDataSelectedRow = ref<BatchValuationTaskBatchRow | null>(null);
  const standardDataBasic = ref<BatchValuationTaskStandardBasicDTO | null>(null);
  const standardDataRawWorkbook = ref<BatchValuationTaskRawWorkbookDTO | null>(null);
  const standardDataSubjects = ref<BatchValuationTaskStandardSubjectDTO[]>([]);
  const standardDataMetrics = ref<BatchValuationTaskStandardMetricDTO[]>([]);
  const standardDataBasicLoading = ref(false);
  const standardDataSubjectsLoading = ref(false);
  const standardDataMetricsLoading = ref(false);
  const standardDataRawLoading = ref(false);
  const standardDataRawLoaded = ref(false);
  const standardDataRawError = ref("");
  const standardDataExportLoading = ref(false);
  const standardDataRawDownloadLoading = ref(false);
  const standardDataSubjectsLoaded = ref(false);
  const standardDataMetricsLoaded = ref(false);
  const standardDataSubjectsKeyword = ref("");
  const standardDataMetricsKeyword = ref("");

  const setTableRef = (instance: any) => {
    tableRef.value = instance;
  };

  const load = async (options?: { silent?: boolean; skipWhenLoading?: boolean }) => {
    if (options?.skipWhenLoading && loading.value) {
      return;
    }
    loading.value = true;
    try {
      const requestQuery = {
        ...query,
        pageIndex: pagination.current,
        pageSize: pagination.pageSize,
      };
      const [summaryResp, pageResp] = await Promise.all([
        getBatchValuationTaskSummary(requestQuery),
        pageBatchValuationTasks(requestQuery),
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
      lastUpdatedAt.value = formatLastUpdatedAt();
    } catch (error) {
      if (!options?.silent) {
        message.error("加载 批量任务 元数据失败");
      }
      console.error(error);
    } finally {
      loading.value = false;
    }
  };

  const stopAutoRefresh = () => {
    if (autoRefreshTimer !== null) {
      window.clearInterval(autoRefreshTimer);
      autoRefreshTimer = null;
    }
  };

  const startAutoRefresh = () => {
    stopAutoRefresh();
    if (autoRefreshInterval.value <= 0) {
      return;
    }
    autoRefreshTimer = window.setInterval(() => {
      void load({ silent: true, skipWhenLoading: true });
    }, autoRefreshInterval.value * 1000);
  };

  const setAutoRefreshInterval = (value: number) => {
    autoRefreshInterval.value = normalizeAutoRefreshInterval(value);
    startAutoRefresh();
  };

  const loadDetail = async (row: BatchValuationTaskBatchRow) => {
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
        getBatchValuationTask(batchId),
        listBatchValuationTaskSteps(batchId),
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

  const currentStandardDataBatchId = () => standardDataSelectedRow.value?.batchId || "";

  const loadStandardDataBasic = async () => {
    const batchId = currentStandardDataBatchId();
    if (!batchId) {
      standardDataBasic.value = null;
      return;
    }
    standardDataBasicLoading.value = true;
    try {
      const resp = await getBatchValuationTaskStandardBasic(batchId);
      standardDataBasic.value = resp.data ?? null;
    } catch (error) {
      standardDataBasic.value = null;
      message.error("加载估值基础数据失败");
      console.error(error);
    } finally {
      standardDataBasicLoading.value = false;
    }
  };

  const loadStandardDataSubjects = async () => {
    const batchId = currentStandardDataBatchId();
    if (!batchId) {
      standardDataSubjects.value = [];
      return;
    }
    standardDataSubjectsLoading.value = true;
    try {
      const resp = await listBatchValuationTaskStandardSubjects(batchId, {
        keyword: normalizeStandardDataKeyword(standardDataSubjectsKeyword.value) || undefined,
      });
      const data = resp.data ?? [];
      standardDataSubjects.value = data;
      standardDataSubjectsLoaded.value = true;
    } catch (error) {
      standardDataSubjects.value = [];
      message.error("加载估值明细失败");
      console.error(error);
    } finally {
      standardDataSubjectsLoading.value = false;
    }
  };

  const loadStandardDataMetrics = async () => {
    const batchId = currentStandardDataBatchId();
    if (!batchId) {
      standardDataMetrics.value = [];
      return;
    }
    standardDataMetricsLoading.value = true;
    try {
      const resp = await listBatchValuationTaskStandardMetrics(batchId, {
        keyword: normalizeStandardDataKeyword(standardDataMetricsKeyword.value) || undefined,
      });
      const data = resp.data ?? [];
      standardDataMetrics.value = data;
      standardDataMetricsLoaded.value = true;
    } catch (error) {
      standardDataMetrics.value = [];
      message.error("加载指标数据失败");
      console.error(error);
    } finally {
      standardDataMetricsLoading.value = false;
    }
  };

  const loadStandardDataRawWorkbook = async (force = false) => {
    const batchId = currentStandardDataBatchId();
    if (!batchId) {
      standardDataRawWorkbook.value = null;
      standardDataRawLoaded.value = false;
      standardDataRawError.value = "";
      return;
    }
    if (standardDataRawLoaded.value && !force) {
      return;
    }
    standardDataRawLoading.value = true;
    standardDataRawError.value = "";
    try {
      const resp = await getBatchValuationTaskRawWorkbook(batchId);
      standardDataRawWorkbook.value = resp.data ?? null;
      standardDataRawLoaded.value = true;
    } catch (error) {
      standardDataRawWorkbook.value = null;
      standardDataRawLoaded.value = false;
      standardDataRawError.value = "目标源下载失败，无法展示原始估值表";
      message.error("加载原始估值表失败");
      console.error(error);
    } finally {
      standardDataRawLoading.value = false;
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
    if (query.businessDate) {
      parts.push(`业务日期 ${query.businessDate}`);
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
      parts.push(`来源类型 ${resolveBatchValuationTaskSourceTypeLabel(query.sourceType)}`);
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

  const isRetryable = (row?: BatchValuationTaskBatchRow | null) => {
    if (!row || !row.batchId) {
      return false;
    }
    return retryableStatusSet.has(normalizeStatus(row.status));
  };

  const handleSelectionChange = (selected: BatchValuationTaskBatchRow[]) => {
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

  const syncTableSelection = async (nextRows?: BatchValuationTaskBatchRow[]) => {
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

  const closeDetailState = () => {
    detail.value = null;
    selectedRow.value = null;
    detailVisible.value = false;
  };

  const refreshAfterBatchAction = async () => {
    closeDetailState();
    await load();
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
      await batchRetryBatchValuationTasks({
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

  const retryBatchRow = async (row: BatchValuationTaskBatchRow) => {
    if (!isRetryable(row)) {
      message.warning("当前批次不支持重新解析");
      return;
    }
    batchRetryLoading.value = true;
    try {
      await batchRetryBatchValuationTasks({
        batchIds: [row.batchId].filter((batchId): batchId is string => Boolean(batchId)),
        reason: "单批次重新解析",
      });
      message.success("已提交批次重新解析");
      await refreshAfterBatchAction();
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

  const openDetailDrawer = (row: BatchValuationTaskBatchRow) => {
    void loadDetail(row);
  };

  const closeDetailDrawer = () => {
    detailVisible.value = false;
  };

  const resetStandardDataState = () => {
    standardDataActiveTab.value = "basic";
    standardDataBasic.value = null;
    standardDataRawWorkbook.value = null;
    standardDataSubjects.value = [];
    standardDataMetrics.value = [];
    standardDataSubjectsLoaded.value = false;
    standardDataMetricsLoaded.value = false;
    standardDataRawLoaded.value = false;
    standardDataRawError.value = "";
    standardDataSubjectsKeyword.value = "";
    standardDataMetricsKeyword.value = "";
  };

  const openStandardDataModal = (row: BatchValuationTaskBatchRow) => {
    standardDataSelectedRow.value = row;
    resetStandardDataState();
    standardDataVisible.value = true;
    void loadStandardDataBasic();
  };

  const closeStandardDataModal = () => {
    standardDataVisible.value = false;
  };

  const handleStandardDataTabChange = (tab: BatchValuationTaskStandardTab | string) => {
    const normalizedTab =
      tab === "subjects" || tab === "metrics" || tab === "raw" ? tab : "basic";
    standardDataActiveTab.value = normalizedTab;
    if (normalizedTab === "subjects" && !standardDataSubjectsLoaded.value) {
      void loadStandardDataSubjects();
    }
    if (normalizedTab === "metrics" && !standardDataMetricsLoaded.value) {
      void loadStandardDataMetrics();
    }
    if (normalizedTab === "raw") {
      void loadStandardDataRawWorkbook();
    }
  };

  const refreshStandardData = () => {
    if (standardDataActiveTab.value === "raw") {
      void loadStandardDataRawWorkbook(true);
      return;
    }
    if (standardDataActiveTab.value === "subjects") {
      void loadStandardDataSubjects();
      return;
    }
    if (standardDataActiveTab.value === "metrics") {
      void loadStandardDataMetrics();
      return;
    }
    void loadStandardDataBasic();
  };

  const standardDataTabName = () => {
    if (standardDataActiveTab.value === "subjects") {
      return "估值明细";
    }
    if (standardDataActiveTab.value === "metrics") {
      return "指标数据";
    }
    if (standardDataActiveTab.value === "raw") {
      return "原始估值表";
    }
    return "基础信息";
  };

  const defaultStandardDataExportFileName = () => {
    const row = standardDataSelectedRow.value;
    const batchName = row?.batchName || row?.batchId || "标准数据";
    return `估值标准数据_${batchName}_${standardDataTabName()}.xlsx`;
  };

  const defaultRawWorkbookDownloadFileName = () => {
    const fileName = standardDataRawWorkbook.value?.fileName;
    if (fileName) {
      return fileName;
    }
    const row = standardDataSelectedRow.value;
    const batchName = row?.batchName || row?.batchId || "原始估值表";
    return `${batchName}.xlsx`;
  };

  const exportStandardDataSheet = async (
    workbookData: Record<string, unknown> | null,
    sheetName?: string,
  ) => {
    const batchId = currentStandardDataBatchId();
    if (!batchId) {
      message.warning("请先选择要导出的批次");
      return;
    }
    if (!workbookData) {
      message.warning("当前 Sheet 尚未加载完成");
      return;
    }
    if (standardDataExportLoading.value) {
      return;
    }
    standardDataExportLoading.value = true;
    try {
      const response = (await exportBatchValuationTaskStandardDataSheet(batchId, {
        tab: standardDataActiveTab.value,
        sheetName: sheetName || standardDataTabName(),
        workbookData,
      })) as {
        data?: Blob;
        headers?: Record<string, string>;
      } | Blob;
      const blob = response instanceof Blob ? response : response.data;
      if (!blob) {
        throw new Error("导出响应为空");
      }
      const headers = response instanceof Blob ? undefined : response.headers;
      const fileName =
        parseContentDispositionFileName(headers?.["content-disposition"] || headers?.["Content-Disposition"]) ||
        defaultStandardDataExportFileName();
      triggerBrowserDownload(blob, fileName);
      message.success("导出成功");
    } catch (error) {
      console.error("导出标准数据 Sheet 失败:", error);
      message.error("导出失败，请稍后重试");
    } finally {
      standardDataExportLoading.value = false;
    }
  };

  const downloadRawWorkbook = async () => {
    const batchId = currentStandardDataBatchId();
    if (!batchId) {
      message.warning("请先选择要下载的批次");
      return;
    }
    if (standardDataRawDownloadLoading.value) {
      return;
    }
    standardDataRawDownloadLoading.value = true;
    try {
      const response = (await downloadBatchValuationTaskRawWorkbook(batchId)) as {
        data?: Blob;
        headers?: Record<string, string>;
      } | Blob;
      const blob = response instanceof Blob ? response : response.data;
      if (!blob) {
        throw new Error("下载响应为空");
      }
      const headers = response instanceof Blob ? undefined : response.headers;
      const fileName =
        parseContentDispositionFileName(headers?.["content-disposition"] || headers?.["Content-Disposition"]) ||
        defaultRawWorkbookDownloadFileName();
      triggerBrowserDownload(blob, fileName);
      message.success("下载成功");
    } catch (error) {
      console.error("下载原始估值表失败:", error);
      message.error("下载失败，请稍后重试");
    } finally {
      standardDataRawDownloadLoading.value = false;
    }
  };

  const searchStandardDataSubjects = () => {
    standardDataSubjectsKeyword.value = normalizeStandardDataKeyword(standardDataSubjectsKeyword.value);
    void loadStandardDataSubjects();
  };

  const searchStandardDataMetrics = () => {
    standardDataMetricsKeyword.value = normalizeStandardDataKeyword(standardDataMetricsKeyword.value);
    void loadStandardDataMetrics();
  };

  void load();
  startAutoRefresh();
  onBeforeUnmount(stopAutoRefresh);

  return reactive({
    tableRef,
    setTableRef,
    loading: computed(() => loading.value),
    detailLoading: computed(() => detailLoading.value),
    batchRetryLoading: computed(() => batchRetryLoading.value),
    autoRefreshInterval: computed({
      get: () => autoRefreshInterval.value,
      set: setAutoRefreshInterval,
    }),
    autoRefreshOptions: AUTO_REFRESH_OPTIONS,
    lastUpdatedAt: computed(() => lastUpdatedAt.value),
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
    standardDataVisible: computed(() => standardDataVisible.value),
    standardDataActiveTab: computed(() => standardDataActiveTab.value),
    standardDataSelectedRow: computed(() => standardDataSelectedRow.value),
    standardDataBasic: computed(() => standardDataBasic.value),
    standardDataRawWorkbook: computed(() => standardDataRawWorkbook.value),
    standardDataBasicRows: computed(() => standardDataBasic.value?.basicRows ?? []),
    standardDataSubjects: computed(() => standardDataSubjects.value),
    standardDataMetrics: computed(() => standardDataMetrics.value),
    standardDataBasicLoading: computed(() => standardDataBasicLoading.value),
    standardDataSubjectsLoading: computed(() => standardDataSubjectsLoading.value),
    standardDataMetricsLoading: computed(() => standardDataMetricsLoading.value),
    standardDataRawLoading: computed(() => standardDataRawLoading.value),
    standardDataRawError: computed(() => standardDataRawError.value),
    standardDataExportLoading: computed(() => standardDataExportLoading.value),
    standardDataRawDownloadLoading: computed(() => standardDataRawDownloadLoading.value),
    standardDataSubjectsKeyword: computed({
      get: () => standardDataSubjectsKeyword.value,
      set: (value) => {
        standardDataSubjectsKeyword.value = value;
      },
    }),
    standardDataMetricsKeyword: computed({
      get: () => standardDataMetricsKeyword.value,
      set: (value) => {
        standardDataMetricsKeyword.value = value;
      },
    }),
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
    openStandardDataModal,
    closeStandardDataModal,
    handleStandardDataTabChange,
    refreshStandardData,
    exportStandardDataSheet,
    downloadRawWorkbook,
    searchStandardDataSubjects,
    searchStandardDataMetrics,
    setAutoRefreshInterval,
    formatStatusColor,
  }) as unknown as BatchValuationTaskPageState;
};
