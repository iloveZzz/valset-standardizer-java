import { computed, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import type {
  AnalyzeObjectsParams,
  PageObjectsParams,
  TransferObjectAnalysisViewDTO,
  TransferObjectSourceAnalysisViewDTO,
  TransferObjectStatusCountViewDTO,
  TransferObjectViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import { unwrapSingleResult } from "@/utils/api-response";
import type { ObjectAnalysis, ObjectPage, ObjectQueryState } from "../types";

const api = getJavaSpringBootQuartzApi();

const statusOrder = [
  "PENDING",
  "RECEIVED",
  "IDENTIFIED",
  "ROUTED",
  "DELIVERING",
  "DELIVERED",
  "ARCHIVED",
  "SKIPPED",
  "QUARANTINED",
  "FAILED",
] as const;

const defaultQuery = (): ObjectQueryState => ({
  sourceId: "",
  sourceType: "",
  sourceCode: "",
  status: "",
  mailId: "",
  fingerprint: "",
  routeId: "",
});

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

const formatStatusLabel = (value: string | undefined) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!status) {
    return "-";
  }
  if (status === "PENDING") return "待处理";
  if (status === "RECEIVED") return "已收取";
  if (status === "IDENTIFIED") return "已识别";
  if (status === "ROUTED") return "已路由";
  if (status === "DELIVERING") return "投递中";
  if (status === "DELIVERED") return "已投递";
  if (status === "ARCHIVED") return "已归档";
  if (status === "SKIPPED") return "已跳过";
  if (status === "QUARANTINED") return "已隔离";
  if (status === "FAILED") return "失败";
  return status;
};

const normalizeStatusCounts = (
  statusCounts: TransferObjectStatusCountViewDTO[] | undefined,
): ObjectAnalysis["sourceAnalyses"][number]["statusCounts"] => {
  const source = statusCounts ?? [];
  const statusMap = new Map<string, TransferObjectStatusCountViewDTO>();
  source.forEach((item) => {
    const key = String(item.status ?? "")
      .trim()
      .toUpperCase();
    if (!key) {
      return;
    }
    statusMap.set(key, item);
  });

  const orderedStatusCounts = statusOrder
    .filter((key) => statusMap.has(key))
    .map((key) => {
      const item = statusMap.get(key)!;
      return {
        status: item.status,
        statusLabel: item.statusLabel || formatStatusLabel(item.status),
        count: Number(item.count ?? 0),
      };
    });

  const remainingStatusCounts = [...statusMap.entries()]
    .filter(
      ([key]) => !statusOrder.includes(key as (typeof statusOrder)[number]),
    )
    .map(([, item]) => ({
      status: item.status,
      statusLabel: item.statusLabel || formatStatusLabel(item.status),
      count: Number(item.count ?? 0),
    }));

  return [...orderedStatusCounts, ...remainingStatusCounts];
};

  const normalizeAnalysis = (
    value: TransferObjectAnalysisViewDTO | undefined,
  ): ObjectAnalysis => {
  const sourceAnalyses = (value?.sourceAnalyses ?? [])
    .map((item: TransferObjectSourceAnalysisViewDTO) => ({
      sourceType: item.sourceType,
      totalCount: Number(item.totalCount ?? 0),
      statusCounts: normalizeStatusCounts(item.statusCounts),
    }))
    .sort((left, right) => {
      const countCompare = right.totalCount - left.totalCount;
      if (countCompare !== 0) {
        return countCompare;
      }
      return String(left.sourceType ?? "").localeCompare(
        String(right.sourceType ?? ""),
      );
    });

  return {
    totalCount: Number(value?.totalCount ?? 0),
    sourceAnalyses,
  };
};

export const useTransferPage = () => {
  const query = reactive<ObjectQueryState>(defaultQuery());
  const rows = ref<TransferObjectViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferObjectViewDTO | null>(null);
  const detailVisible = ref(false);
  const analysis = ref<ObjectAnalysis>({
    totalCount: 0,
    sourceAnalyses: [],
  });

  const listLoading = ref(false);
  const analysisLoading = ref(false);
  let listRequestId = 0;
  let analysisRequestId = 0;

  const pageSize = computed(() => pagination.value.pageSize || 10);
  const tableData = computed(() => rows.value);
  const errorCount = computed(
    () => rows.value.filter((row) => Boolean(row.errorMessage)).length,
  );
  const routeCount = computed(
    () =>
      new Set(
        rows.value
          .map((row) => row.routeId)
          .filter((value) => value !== undefined),
      ).size,
  );
  const sourceCount = computed(
    () =>
      new Set(
        rows.value
          .map((row) => row.sourceId)
          .filter((value) => value !== undefined),
      ).size,
  );
  const statusCount = computed(
    () => new Set(rows.value.map((row) => row.status).filter(Boolean)).size,
  );

  const mapQuery = (
    pageIndex = pagination.value.current || 1,
    pageSizeValue = pagination.value.pageSize || 10,
  ): PageObjectsParams => ({
    sourceId: query.sourceId || undefined,
    sourceType: query.sourceType || undefined,
    sourceCode: query.sourceCode || undefined,
    status: query.status || undefined,
    mailId: query.mailId || undefined,
    fingerprint: query.fingerprint || undefined,
    routeId: query.routeId || undefined,
    pageIndex: Math.max(pageIndex - 1, 0),
    pageSize: pageSizeValue,
  });

  const mapAnalysisQuery = (): AnalyzeObjectsParams => ({
    sourceId: query.sourceId || undefined,
    sourceType: query.sourceType || undefined,
    sourceCode: query.sourceCode || undefined,
    status: query.status || undefined,
    mailId: query.mailId || undefined,
    fingerprint: query.fingerprint || undefined,
    routeId: query.routeId || undefined,
  });

  const loadList = async (
    pageIndex = pagination.value.current || 1,
    pageSizeValue = pagination.value.pageSize || 10,
  ) => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const page = await api.pageObjects(mapQuery(pageIndex, pageSizeValue));
      if (requestId !== listRequestId) {
        return;
      }
      rows.value = page.data ?? [];
      total.value = page?.totalCount ?? 0;
      pagination.value.total = total.value;
      pagination.value.current = pageIndex;
      pagination.value.pageSize = pageSizeValue;
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      console.error("加载主对象列表失败:", error);
      message.error("加载主对象列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const loadAnalysis = async () => {
    const requestId = ++analysisRequestId;
    analysisLoading.value = true;
    try {
      const res = await api.analyzeObjects(mapAnalysisQuery());
      if (requestId !== analysisRequestId) {
        return;
      }
      analysis.value = normalizeAnalysis(unwrapSingleResult(res));
    } catch (error) {
      if (requestId !== analysisRequestId) {
        return;
      }
      console.error("加载主对象分析失败:", error);
      message.error("加载主对象分析失败");
      analysis.value = {
        totalCount: 0,
        sourceAnalyses: [],
      };
    } finally {
      if (requestId === analysisRequestId) {
        analysisLoading.value = false;
      }
    }
  };

  const reloadAnalysisAndList = async () => {
    await Promise.all([
      loadAnalysis(),
      loadList(1, pagination.value.pageSize || 10),
    ]);
  };

  const openDetailDrawer = async (row: TransferObjectViewDTO) => {
    try {
      const detail = row.transferId
        ? unwrapSingleResult(await api.getObject(row.transferId))
        : row;
      selectedRow.value = detail ?? null;
      detailVisible.value = true;
    } catch (error) {
      console.error("加载主对象详情失败:", error);
      message.error("加载主对象详情失败");
    }
  };

  const runQuery = async () => {
    pagination.value.current = 1;
    await reloadAnalysisAndList();
  };

  const resetQuery = async () => {
    Object.assign(query, defaultQuery());
    pagination.value.current = 1;
    await reloadAnalysisAndList();
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

  const formatStatus = (value: string | undefined) => value || "-";
  const formatSourceTypeLabel = (value: string | undefined) => {
    const text = String(value ?? "").trim();
    return text || "-";
  };

  const applySourceFilter = (sourceType?: string) => {
    query.sourceType = sourceType ?? "";
    query.status = "";
    pagination.value.current = 1;
    void runQuery();
  };

  const applySourceStatusFilter = (sourceType?: string, status?: string) => {
    query.sourceType = sourceType ?? "";
    query.status = status ?? "";
    pagination.value.current = 1;
    void runQuery();
  };

  void reloadAnalysisAndList();

  const page = reactive({
    loading: listLoading,
    analysisLoading,
    rows,
    tableData,
    total,
    pageSize,
    pagination,
    analysis,
    errorCount,
    routeCount,
    sourceCount,
    statusCount,
    query,
    selectedRow,
    detailVisible,
    handlePageChange,
    openDetailDrawer,
    runQuery,
    resetQuery,
    applySourceFilter,
    applySourceStatusFilter,
    closeDetail: () => {
      detailVisible.value = false;
    },
    formatStatus,
    formatSourceTypeLabel,
    formatStatusLabel,
    safeJson,
  }) as unknown as ObjectPage;

  return {
    page,
  };
};
