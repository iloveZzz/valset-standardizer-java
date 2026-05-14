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
} from "../constants";

const PAGE_SIZE = 10;

const defaultQuery = (): SpringBatchValuationTaskQueryParams & {
  batchId: string;
  taskDate: string;
  managerName: string;
  productKeyword: string;
  stage: string;
  status: string;
  sourceType: string;
} => ({
  batchId: "",
  taskDate: dayjs().format("YYYY-MM-DD"),
  managerName: "",
  productKeyword: "",
  stage: "",
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
  const selectedRow = ref<SpringBatchValuationTaskBatchRow | null>(null);
  const detail = ref<SpringBatchValuationTaskBatchDetailDTO | null>(null);
  const detailVisible = ref(false);

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
    } catch (error) {
      message.error("加载 Spring Batch 元数据失败");
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
      const [detailResp, stepsResp] = await Promise.all([
        getSpringBatchValuationTask(row.batchId),
        listSpringBatchValuationTaskSteps(row.batchId),
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

  const handleStageSelect = (stage: string) => {
    query.stage = String(stage ?? "").trim().toUpperCase();
    pagination.current = 1;
    void load();
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
    loading: computed(() => loading.value),
    detailLoading: computed(() => detailLoading.value),
    rows: computed(() => rows.value),
    totalCount: computed(() => totalCount.value),
    summary,
    pagination,
    query,
    selectedRow: computed(() => selectedRow.value),
    detail: computed(() => detail.value),
    detailVisible: computed(() => detailVisible.value),
    runQuery,
    resetQuery,
    handlePageChange,
    handleStageSelect,
    openDetailDrawer,
    closeDetailDrawer,
    formatStatusColor,
  }) as unknown as SpringBatchValuationTaskPageState;
};
