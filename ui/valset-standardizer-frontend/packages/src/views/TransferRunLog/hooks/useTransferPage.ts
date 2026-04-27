import { computed, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import type {
  AnalyzeLogsParams,
  PageLogsParams,
  TransferRunLogAnalysisViewDTO,
  TransferRunLogStageAnalysisViewDTO,
  TransferRunLogStatusCountViewDTO,
  TransferRunLogViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import { customInstance } from "@/api/mutator";
import { unwrapSingleResult } from "@/utils/api-response";
import type {
  RunLogAnalysis,
  RunLogConsoleSeedItem,
  RunLogPage,
  RunLogQueryState,
} from "../types";

const api = getJavaSpringBootQuartzApi();
type RunLogPageParams = PageLogsParams & { pageIndex?: number };

const stageOrder = ["INGEST", "ROUTE", "DELIVER"] as const;

const defaultQuery = (): RunLogQueryState => ({
  sourceId: "",
  transferId: "",
  routeId: "",
  runStage: "",
  runStatus: "",
  triggerType: "",
  keyword: "",
});

const createPagination = (): YTablePagination => ({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ["10", "20", "50", "100"],
});

const formatStageLabel = (value: string | undefined) => {
  const stage = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!stage) {
    return "-";
  }
  if (stage === "INGEST") {
    return "来源";
  }
  if (stage === "ROUTE") {
    return "路由";
  }
  if (stage === "DELIVER") {
    return "目标";
  }
  return stage;
};

const formatStatusLabel = (value: string | undefined) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!status) {
    return "-";
  }
  if (status === "SUCCESS") {
    return "成功";
  }
  if (status === "FAILED") {
    return "失败";
  }
  return status;
};

const safeJson = (value: unknown) => {
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

const normalizeText = (value: unknown) => {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return String(value);
};

const buildStatusColor = (value?: string) => {
  const status = String(value ?? "").toUpperCase();
  if (!status) {
    return "default";
  }
  if (["SUCCESS", "SUCCEEDED", "DONE", "COMPLETED"].includes(status)) {
    return "green";
  }
  if (["FAIL", "FAILED", "ERROR"].includes(status)) {
    return "red";
  }
  if (["RUNNING", "PROCESSING", "PENDING"].includes(status)) {
    return "orange";
  }
  return "blue";
};

const getStatusChipClass = (value?: string) => {
  const status = String(value ?? "").toUpperCase();
  if (["SUCCESS", "SUCCEEDED", "DONE", "COMPLETED"].includes(status)) {
    return "analysis-status-chip--success";
  }
  if (["FAIL", "FAILED", "ERROR"].includes(status)) {
    return "analysis-status-chip--failure";
  }
  return "analysis-status-chip--default";
};

const defaultStageAnalyses = (): RunLogAnalysis["stageAnalyses"] =>
  stageOrder.map((stage) => ({
    runStage: stage,
    stageLabel: formatStageLabel(stage),
    totalCount: 0,
    statusCounts: [],
  }));

const toPageIndex = (current: number) => Math.max(current - 1, 0);

const normalizeStatusCounts = (
  statusCounts: TransferRunLogStatusCountViewDTO[] | undefined,
): RunLogAnalysis["stageAnalyses"][number]["statusCounts"] => {
  const source = statusCounts ?? [];
  const statusMap = new Map<string, TransferRunLogStatusCountViewDTO>();
  source.forEach((item) => {
    const key = String(item.runStatus ?? "")
      .trim()
      .toUpperCase();
    if (!key) {
      return;
    }
    statusMap.set(key, item);
  });

  const orderedKeys = ["SUCCESS", "FAILED"];
  const orderedStatusCounts = orderedKeys
    .filter((key) => statusMap.has(key))
    .map((key) => {
      const item = statusMap.get(key)!;
      return {
        runStatus: item.runStatus,
        statusLabel: item.statusLabel || formatStatusLabel(item.runStatus),
        count: Number(item.count ?? 0),
      };
    });

  const remainingStatusCounts = [...statusMap.entries()]
    .filter(([key]) => !orderedKeys.includes(key))
    .map(([, item]) => ({
      runStatus: item.runStatus,
      statusLabel: item.statusLabel || formatStatusLabel(item.runStatus),
      count: Number(item.count ?? 0),
    }));

  return [...orderedStatusCounts, ...remainingStatusCounts];
};

const normalizeStageAnalyses = (
  value: TransferRunLogAnalysisViewDTO | undefined,
): RunLogAnalysis => {
  const stageMap = new Map<string, TransferRunLogStageAnalysisViewDTO>();
  (value?.stageAnalyses ?? []).forEach((item) => {
    const key = String(item.runStage ?? "")
      .trim()
      .toUpperCase();
    if (!key) {
      return;
    }
    stageMap.set(key, item);
  });

  return {
    totalCount: Number(value?.totalCount ?? 0),
    sourceCount: Number(value?.sourceCount ?? 0),
    routeCount: Number(value?.routeCount ?? 0),
    targetCount: Number(value?.targetCount ?? 0),
    stageAnalyses: stageOrder.map((stage) => {
      const item = stageMap.get(stage);
      return {
        runStage: item?.runStage ?? stage,
        stageLabel: item?.stageLabel || formatStageLabel(stage),
        totalCount: Number(item?.totalCount ?? 0),
        statusCounts: normalizeStatusCounts(item?.statusCounts),
      };
    }),
  };
};

export const useTransferPage = (): { page: RunLogPage } => {
  const query = reactive<RunLogQueryState>(defaultQuery());
  const rows = ref<TransferRunLogViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>(createPagination());
  const analysis = ref<RunLogAnalysis>({
    totalCount: 0,
    sourceCount: 0,
    routeCount: 0,
    targetCount: 0,
    stageAnalyses: defaultStageAnalyses(),
  });
  const selectedRow = ref<TransferRunLogViewDTO | null>(null);
  const cleanupLoading = ref(false);
  const detailVisible = ref(false);

  const listLoading = ref(false);
  const analysisLoading = ref(false);
  let listRequestId = 0;
  let analysisRequestId = 0;
  const consoleItems = computed<RunLogConsoleSeedItem[]>(() =>
    rows.value.slice(0, 6).map((item, index) => {
      const status = formatStatusLabel(item.runStatus);
      const fallbackTitle =
        item.originalName ||
        item.routeName ||
        item.sourceName ||
        item.transferId ||
        "运行日志";
      const description =
        item.errorMessage ||
        item.logMessage ||
        [item.sourceName, item.routeName, item.targetName]
          .filter(Boolean)
          .join(" / ") ||
        "暂无运行说明";

      return {
        key: item.runLogId || `${item.transferId ?? "run-log"}-${index}`,
        title: fallbackTitle,
        stageLabel: formatStageLabel(item.runStage),
        statusLabel: item.runStatusLabel || status,
        createdAt: item.createdAt,
        description,
      };
    }),
  );
  const mapListQuery = (
    pageNo = pagination.value.current || 1,
    pageSize = pagination.value.pageSize || 10,
  ): RunLogPageParams => ({
    sourceId: query.sourceId || undefined,
    transferId: query.transferId || undefined,
    routeId: query.routeId || undefined,
    runStage: query.runStage || undefined,
    runStatus: query.runStatus || undefined,
    triggerType: query.triggerType || undefined,
    keyword: query.keyword || undefined,
    pageIndex: toPageIndex(pageNo),
    pageSize,
  });

  const mapAnalysisQuery = (): AnalyzeLogsParams => ({
    sourceId: query.sourceId || undefined,
    transferId: query.transferId || undefined,
    routeId: query.routeId || undefined,
    runStage: query.runStage || undefined,
    runStatus: query.runStatus || undefined,
    triggerType: query.triggerType || undefined,
    keyword: query.keyword || undefined,
  });

  const loadAnalysis = async () => {
    const requestId = ++analysisRequestId;
    analysisLoading.value = true;
    try {
      const res = await api.analyzeLogs(mapAnalysisQuery());
      if (requestId !== analysisRequestId) {
        return;
      }
      analysis.value = normalizeStageAnalyses(unwrapSingleResult(res));
    } catch (error) {
      if (requestId !== analysisRequestId) {
        return;
      }
      console.error("加载运行日志分析失败:", error);
      message.error("加载运行日志分析失败");
      analysis.value = {
        totalCount: 0,
        sourceCount: 0,
        routeCount: 0,
        targetCount: 0,
        stageAnalyses: defaultStageAnalyses(),
      };
    } finally {
      if (requestId === analysisRequestId) {
        analysisLoading.value = false;
      }
    }
  };

  const loadList = async (
    pageNo = pagination.value.current || 1,
    pageSize = pagination.value.pageSize || 10,
  ) => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const res = await api.pageLogs(mapListQuery(pageNo, pageSize));
      if (requestId !== listRequestId) {
        return;
      }
      const records = res;
      rows.value = records?.data || [];
      total.value = records.totalCount || 0;
      pagination.value.total = total.value;
      pagination.value.current = pageNo;
      pagination.value.pageSize = pageSize;
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      console.error("加载运行日志列表失败:", error);
      message.error("加载运行日志列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const reloadAnalysisAndList = async (
    pageNo = 1,
    pageSize = pagination.value.pageSize || 10,
  ) => {
    pagination.value.current = pageNo;
    pagination.value.pageSize = pageSize;
    await Promise.all([loadAnalysis(), loadList(pageNo, pageSize)]);
  };

  const cleanupLogs = async () => {
    if (cleanupLoading.value) {
      return;
    }

    cleanupLoading.value = true;
    try {
      const res = await customInstance<any>({
        url: "/transfer-run-logs/cleanup-yesterday",
        method: "POST",
      });
      const result = unwrapSingleResult(res);
      const deletedCount = Number(result?.deletedCount ?? 0);
      const cleanupDate = String(result?.cleanupDate ?? "").trim() || "前一天";
      message.success(`已清理 ${cleanupDate} 的运行日志，共 ${deletedCount} 条`);
      await reloadAnalysisAndList(
        pagination.value.current || 1,
        pagination.value.pageSize || 10,
      );
    } catch (error) {
      console.error("清理运行日志失败:", error);
      message.error("清理运行日志失败");
    } finally {
      cleanupLoading.value = false;
    }
  };

  const openDetailDrawer = (row: TransferRunLogViewDTO) => {
    selectedRow.value = row;
    detailVisible.value = true;
  };

  const runQuery = async () => {
    await reloadAnalysisAndList(1, pagination.value.pageSize || 10);
  };

  const resetQuery = async () => {
    Object.assign(query, defaultQuery());
    await reloadAnalysisAndList(1, pagination.value.pageSize || 10);
  };

  const handlePageChange = ({
    current,
    pageSize: nextPageSize,
  }: {
    current: number;
    pageSize: number;
  }) => {
    pagination.value.current = current;
    pagination.value.pageSize = nextPageSize;
    void loadList(current, nextPageSize);
  };

  const applyStageFilter = (runStage?: string) => {
    query.runStage = runStage ?? "";
    query.runStatus = "";
    void runQuery();
  };

  const applyStageStatusFilter = (runStage?: string, runStatus?: string) => {
    query.runStage = runStage ?? "";
    query.runStatus = runStatus ?? "";
    void runQuery();
  };

  void reloadAnalysisAndList();

  const page = reactive({
    loading: listLoading,
    analysisLoading,
    cleanupLoading,
    rows,
    total,
    analysis,
    pagination,
    query,
    selectedRow,
    detailVisible,
    consoleItems,
    openDetailDrawer,
    runQuery,
    resetQuery,
    cleanupLogs,
    closeDetail: () => {
      detailVisible.value = false;
    },
    handlePageChange,
    applyStageFilter,
    applyStageStatusFilter,
    formatText: normalizeText,
    formatStageLabel,
    formatStatus: formatStatusLabel,
    formatStatusLabel,
    getStatusChipClass,
    runStatusTagColor: buildStatusColor,
    safeJson,
  });

  return {
    page,
  };
};
