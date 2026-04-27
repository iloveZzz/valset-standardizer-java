import { computed, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import type {
  AnalyzeObjectsParams,
  PageObjectsParams,
  TransferObjectAnalysisViewDTO,
  TransferObjectExtensionCountViewDTO,
  TransferObjectMailFolderCountViewDTO,
  TransferObjectSourceAnalysisViewDTO,
  TransferObjectStatusCountViewDTO,
  TransferObjectSizeAnalysisViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import { customInstance } from "@/api/mutator";
import { unwrapSingleResult } from "@/utils/api-response";
import type {
  ObjectAnalysis,
  ObjectPage,
  ObjectTagFilter,
  ObjectQueryState,
  TransferObjectViewDTO,
} from "../types";

type PageObjectsRequestParams = PageObjectsParams & {
  tagId?: string;
  tagCode?: string;
  tagValue?: string;
};

type AnalyzeObjectsRequestParams = AnalyzeObjectsParams & {
  tagId?: string;
  tagCode?: string;
  tagValue?: string;
};

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
  tagId: "",
  tagCode: "",
  tagValue: "",
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

const formatObjectStatus = (value: string | undefined) => {
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

const formatDeliveryStatus = (value: string | undefined) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (status === "已投递" || status === "DELIVERED" || status === "SUCCESS") {
    return "已投递";
  }
  return "未投递";
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

const normalizeFolderCounts = (
  folderCounts: TransferObjectMailFolderCountViewDTO[] | undefined,
): ObjectAnalysis["sourceAnalyses"][number]["mailFolderCounts"] => {
  return (folderCounts ?? []).map((item) => ({
    mailFolder: item.mailFolder,
    mailFolderLabel: item.mailFolderLabel || item.mailFolder || "未分类",
    count: Number(item.count ?? 0),
  }));
};

const normalizeExtensionCounts = (
  extensionCounts: TransferObjectExtensionCountViewDTO[] | undefined,
): ObjectAnalysis["sizeAnalysis"]["extensionCounts"] => {
  return (extensionCounts ?? []).map((item) => ({
    extension: item.extension,
    extensionLabel: item.extensionLabel || item.extension || "无后缀",
    count: Number(item.count ?? 0),
  }));
};

const normalizeSizeAnalysis = (
  value: TransferObjectSizeAnalysisViewDTO | undefined,
): ObjectAnalysis["sizeAnalysis"] => ({
  totalCount: Number(value?.totalCount ?? 0),
  totalSizeBytes: Number(value?.totalSizeBytes ?? 0),
  extensionCounts: normalizeExtensionCounts(value?.extensionCounts),
});

const normalizeAnalysis = (
  value: TransferObjectAnalysisViewDTO | undefined,
): ObjectAnalysis => {
  const sourceAnalyses = (value?.sourceAnalyses ?? [])
    .map((item: TransferObjectSourceAnalysisViewDTO) => ({
      sourceType: item.sourceType,
      totalCount: Number(item.totalCount ?? 0),
      statusCounts: normalizeStatusCounts(item.statusCounts),
      mailFolderCounts: normalizeFolderCounts(item.mailFolderCounts),
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
    taggedCount: Number(value?.taggedCount ?? 0),
    untaggedCount: Number(value?.untaggedCount ?? 0),
    sourceAnalyses,
    sizeAnalysis: normalizeSizeAnalysis(value?.sizeAnalysis),
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
  const redeliverLoading = ref(false);
  const analysis = ref<ObjectAnalysis>({
    totalCount: 0,
    taggedCount: 0,
    untaggedCount: 0,
    sourceAnalyses: [],
    sizeAnalysis: {
      totalCount: 0,
      totalSizeBytes: 0,
      extensionCounts: [],
    },
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
  const tagFilters = computed<ObjectTagFilter[]>(() => {
    const tagMap = new Map<string, ObjectTagFilter>();
    rows.value.forEach((row) => {
      (row.tags ?? []).forEach((tag) => {
        const key =
          tag.tagId ||
          `${String(tag.tagCode ?? "").trim()}::${String(tag.tagValue ?? "").trim()}`;
        if (!key) {
          return;
        }
        const current = tagMap.get(key);
        if (current) {
          current.count += 1;
          return;
        }
        tagMap.set(key, {
          tagId: tag.tagId,
          tagCode: tag.tagCode,
          tagName: tag.tagName,
          tagValue: tag.tagValue,
          count: 1,
        });
      });
    });
    return [...tagMap.values()].sort((left, right) => {
      const countCompare = right.count - left.count;
      if (countCompare !== 0) {
        return countCompare;
      }
      return String(left.tagName || left.tagCode || left.tagValue || "").localeCompare(
        String(right.tagName || right.tagCode || right.tagValue || ""),
      );
    });
  });

  const mapQuery = (
    pageIndex = pagination.value.current || 1,
    pageSizeValue = pagination.value.pageSize || 10,
  ): PageObjectsRequestParams => ({
    sourceId: query.sourceId || undefined,
    sourceType: query.sourceType || undefined,
    sourceCode: query.sourceCode || undefined,
    status: query.status || undefined,
    mailId: query.mailId || undefined,
    fingerprint: query.fingerprint || undefined,
    routeId: query.routeId || undefined,
    tagId: query.tagId || undefined,
    tagCode: query.tagCode || undefined,
    tagValue: query.tagValue || undefined,
    pageIndex: Math.max(pageIndex - 1, 0),
    pageSize: pageSizeValue,
  });

  const mapAnalysisQuery = (): AnalyzeObjectsRequestParams => ({
    sourceId: query.sourceId || undefined,
    sourceType: query.sourceType || undefined,
    sourceCode: query.sourceCode || undefined,
    status: query.status || undefined,
    mailId: query.mailId || undefined,
    fingerprint: query.fingerprint || undefined,
    routeId: query.routeId || undefined,
    tagId: query.tagId || undefined,
    tagCode: query.tagCode || undefined,
    tagValue: query.tagValue || undefined,
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
        taggedCount: 0,
        untaggedCount: 0,
        sourceAnalyses: [],
        sizeAnalysis: {
          totalCount: 0,
          totalSizeBytes: 0,
          extensionCounts: [],
        },
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

  const redeliverObject = async (row: TransferObjectViewDTO) => {
    const transferId = String(row?.transferId ?? "").trim();
    if (!transferId) {
      message.warning("文件主对象缺少主键，无法重新投递");
      return;
    }
    if (formatDeliveryStatus(row.deliveryStatus) === "已投递") {
      message.warning("该对象已经投递完成，无需重新投递");
      return;
    }
    if (redeliverLoading.value) {
      return;
    }

    redeliverLoading.value = true;
    try {
      const res = await customInstance<any>({
        url: "/transfer-objects/redeliver",
        method: "POST",
        data: {
          transferIds: [transferId],
        },
      });
      const result = unwrapSingleResult(res);
      const successCount = Number(result?.successCount ?? 0);
      const failureCount = Number(result?.failureCount ?? 0);
      const skippedCount = Number(result?.skippedCount ?? 0);
      const requestedCount = Number(result?.requestedCount ?? 1);
      const summary = `已处理 ${requestedCount} 条，成功 ${successCount} 条，失败 ${failureCount} 条，跳过 ${skippedCount} 条`;
      if (failureCount > 0 || skippedCount > 0) {
        message.warning(summary);
      } else {
        message.success(summary);
      }
      await reloadAnalysisAndList();
    } catch (error) {
      console.error("重新投递文件主对象失败:", error);
      message.error("重新投递文件主对象失败");
    } finally {
      redeliverLoading.value = false;
    }
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

  const formatStatus = (value: string | undefined) => formatObjectStatus(value);
  const formatDeliveryStatusText = (value: string | undefined) =>
    formatDeliveryStatus(value);
  const formatSourceTypeLabel = (value: string | undefined) => {
    const text = String(value ?? "").trim();
    return text || "-";
  };
  const formatTagLabel = (value: string | undefined) => {
    const text = String(value ?? "").trim();
    return text || "-";
  };
  const formatBytes = (value: number | undefined) => {
    const size = Number(value ?? 0);
    if (!Number.isFinite(size) || size <= 0) {
      return "0 B";
    }
    const units = ["B", "KB", "MB", "GB", "TB"];
    let current = size;
    let unitIndex = 0;
    while (current >= 1024 && unitIndex < units.length - 1) {
      current /= 1024;
      unitIndex += 1;
    }
    const precision = unitIndex === 0 ? 0 : current >= 10 ? 1 : 2;
    return `${current.toFixed(precision)} ${units[unitIndex]}`;
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

  const applyTagFilter = (filter: ObjectTagFilter) => {
    query.tagId = filter.tagId ?? "";
    query.tagCode = filter.tagCode ?? "";
    query.tagValue = filter.tagValue ?? "";
    pagination.value.current = 1;
    void runQuery();
  };

  const clearTagFilter = () => {
    query.tagId = "";
    query.tagCode = "";
    query.tagValue = "";
    pagination.value.current = 1;
    void runQuery();
  };

  void reloadAnalysisAndList();

  const page = reactive({
    loading: listLoading,
    analysisLoading,
    redeliverLoading,
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
    tagFilters,
    query,
    selectedRow,
    detailVisible,
    handlePageChange,
    openDetailDrawer,
    redeliverObject,
    runQuery,
    resetQuery,
    applySourceFilter,
    applySourceStatusFilter,
    applyTagFilter,
    clearTagFilter,
    closeDetail: () => {
      detailVisible.value = false;
    },
    formatStatus,
    formatDeliveryStatus: formatDeliveryStatusText,
    formatSourceTypeLabel,
    formatStatusLabel,
    formatTagLabel,
    formatBytes,
    safeJson,
  }) as unknown as ObjectPage;

  return {
    page,
  };
};
