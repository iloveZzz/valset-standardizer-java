import { computed, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import { useRouter } from "vue-router";
import { useRoute } from "vue-router";
import {
  backfillParseQueue,
  completeParseQueue,
  failParseQueue,
  generateParseQueue,
  getParseQueue,
  listParseQueues,
  retryParseQueue,
  subscribeParseQueue,
  type ParseQueueViewDTO,
} from "@/api/parseQueue";
import { unwrapSingleResult } from "@/utils/api-response";
import type {
  ParseQueuePage,
  ParseQueueQueryState,
  ParseQueueRow,
  ParseQueueStatus,
  ParseQueueTriggerMode,
} from "../types";

const defaultQuery = (): ParseQueueQueryState => ({
  transferId: "",
  businessKey: "",
  sourceCode: "",
  routeId: "",
  tagCode: "",
  fileStatus: "",
  deliveryStatus: "",
  parseStatus: "",
  triggerMode: "",
});

const formatParseStatusLabel = (value: string | undefined) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!status) {
    return "-";
  }
  if (status === "PENDING") return "待订阅";
  if (status === "PARSING") return "解析中";
  if (status === "PARSED") return "已解析";
  if (status === "FAILED") return "解析失败";
  return status;
};

const formatTriggerModeLabel = (value: string | undefined) => {
  const mode = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!mode) {
    return "-";
  }
  if (mode === "AUTO") return "自动生成";
  if (mode === "MANUAL") return "手工生成";
  return mode;
};

const formatFileStatusLabel = (value: string | undefined) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!status) {
    return "-";
  }
  if (status === "IDENTIFIED") return "已识别";
  if (status === "DELIVERED") return "已投递";
  if (status === "UNDELIVERED") return "未投递";
  return value || status;
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

const cloneJson = (value: unknown) => {
  if (value === null || value === undefined || value === "") {
    return value;
  }
  if (typeof value === "string") {
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  }
  try {
    return JSON.parse(JSON.stringify(value));
  } catch {
    return value;
  }
};

const normalizePageIndex = (value?: number) =>
  typeof value === "number" && value > 0 ? value : 0;

const mapStatus = (value?: string): ParseQueueStatus => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (status === "PARSING") return "PARSING";
  if (status === "PARSED") return "PARSED";
  if (status === "FAILED") return "FAILED";
  return "PENDING";
};

const mapTriggerMode = (value?: string): ParseQueueTriggerMode => {
  const mode = String(value ?? "")
    .trim()
    .toUpperCase();
  return mode === "MANUAL" ? "MANUAL" : "AUTO";
};

const mapRow = (row: ParseQueueViewDTO): ParseQueueRow => ({
  queueId: String(row.queueId ?? ""),
  businessKey: String(row.businessKey ?? ""),
  transferId: String(row.transferId ?? ""),
  originalName: String(row.originalName ?? ""),
  sourceId: row.sourceId,
  sourceType: row.sourceType,
  sourceCode: row.sourceCode,
  routeId: row.routeId,
  deliveryId: row.deliveryId,
  tagId: row.tagId,
  tagCode: row.tagCode,
  tagName: row.tagName,
  fileStatus: formatFileStatusLabel(row.fileStatus),
  deliveryStatus: formatFileStatusLabel(row.deliveryStatus),
  parseStatus: mapStatus(row.parseStatus),
  triggerMode: mapTriggerMode(row.triggerMode),
  retryCount: Number(row.retryCount ?? 0),
  subscribedBy: row.subscribedBy,
  subscribedAt: row.subscribedAt ?? "",
  parsedAt: row.parsedAt ?? "",
  lastErrorMessage: row.lastErrorMessage,
  objectSnapshotJson: cloneJson(row.objectSnapshotJson),
  deliverySnapshotJson: cloneJson(row.deliverySnapshotJson),
  parseRequestJson: cloneJson(row.parseRequestJson),
  parseResultJson: cloneJson(row.parseResultJson),
  createdAt: row.createdAt ?? "",
  updatedAt: row.updatedAt ?? "",
});

const isPageResult = (
  value: unknown,
): value is {
  data?: ParseQueueViewDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
} => Boolean(value) && typeof value === "object" && !Array.isArray(value);

export const useParseQueuePage = (): { page: ParseQueuePage } => {
  const router = useRouter();
  const route = useRoute();
  const query = reactive<ParseQueueQueryState>(defaultQuery());
  const rows = ref<ParseQueueRow[]>([]);
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
  const backfillLoading = ref(false);
  const detailVisible = ref(false);
  const selectedRow = ref<ParseQueueRow | null>(null);

  const total = computed(() => Number(pagination.value.total ?? 0));
  const pendingCount = computed(
    () => rows.value.filter((row) => row.parseStatus === "PENDING").length,
  );
  const parsingCount = computed(
    () => rows.value.filter((row) => row.parseStatus === "PARSING").length,
  );
  const parsedCount = computed(
    () => rows.value.filter((row) => row.parseStatus === "PARSED").length,
  );
  const failedCount = computed(
    () => rows.value.filter((row) => row.parseStatus === "FAILED").length,
  );
  const tableData = computed(() => rows.value);

  const syncSelectedRow = () => {
    if (!selectedRow.value) {
      return;
    }
    const next = rows.value.find(
      (row) => row.queueId === selectedRow.value?.queueId,
    );
    if (next) {
      selectedRow.value = next;
    }
  };

  const normalizeRouteValue = (value: unknown) => {
    if (Array.isArray(value)) {
      return String(value[0] ?? "");
    }
    return String(value ?? "");
  };

  const openQueueFromRoute = async () => {
    const queueId = normalizeRouteValue(route.query.queueId);
    if (!queueId) {
      return;
    }
    await openDetailDrawer({ queueId } as ParseQueueRow);
  };

  const loadList = async () => {
    listLoading.value = true;
    try {
      const pageIndex = normalizePageIndex(pagination.value.current) - 1;
      const pageSize = Number(pagination.value.pageSize ?? 10);
      const res = await listParseQueues({
        transferId: query.transferId || undefined,
        businessKey: query.businessKey || undefined,
        sourceCode: query.sourceCode || undefined,
        routeId: query.routeId || undefined,
        tagCode: query.tagCode || undefined,
        fileStatus: query.fileStatus || undefined,
        deliveryStatus: query.deliveryStatus || undefined,
        parseStatus: query.parseStatus || undefined,
        triggerMode: query.triggerMode || undefined,
        pageIndex,
        pageSize,
      });
      const page = isPageResult(res)
        ? res
        : { data: Array.isArray(res) ? res : [] };
      rows.value = (page.data ?? []).map(mapRow);
      pagination.value.total = Number(page.totalCount ?? 0);
      pagination.value.current = Number(page.pageIndex ?? pageIndex);
      pagination.value.pageSize = Number(page.pageSize ?? pageSize);
      syncSelectedRow();
    } catch (error) {
      message.error("加载待解析任务列表失败");
      throw error;
    } finally {
      listLoading.value = false;
    }
  };

  const runQuery = () => {
    pagination.value.current = 0;
    void loadList();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    pagination.value.current = 0;
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

  const openDetailDrawer = async (row: ParseQueueRow) => {
    detailVisible.value = true;
    selectedRow.value = row;
    try {
      const res = await getParseQueue(row.queueId);
      selectedRow.value = mapRow(unwrapSingleResult(res) ?? row);
    } catch {
      selectedRow.value = row;
    }
  };

  const openLifecyclePage = (row: ParseQueueRow) => {
    void router.push({
      path: "/transfer/parse-lifecycle",
      query: {
        queueId: row.queueId || undefined,
        transferId: row.transferId || undefined,
      },
    });
  };

  const closeDetail = () => {
    detailVisible.value = false;
  };

  const refreshAndKeepSelection = async () => {
    await loadList();
  };

  const generateQueue = async (row: ParseQueueRow, forceRebuild = false) => {
    loading.value = true;
    try {
      const res = await generateParseQueue({
        transferId: row.transferId,
        businessKey: row.businessKey,
        sourceId: row.sourceId,
        routeId: row.routeId,
        tagCode: row.tagCode,
        forceRebuild,
      });
      const next = unwrapSingleResult(res);
      if (next) {
        const mapped = mapRow(next);
        const index = rows.value.findIndex(
          (item) => item.queueId === mapped.queueId,
        );
        if (index >= 0) {
          rows.value.splice(index, 1, mapped);
        }
        selectedRow.value =
          selectedRow.value?.queueId === mapped.queueId
            ? mapped
            : selectedRow.value;
      }
      await refreshAndKeepSelection();
      message.success(forceRebuild ? "已重建待解析任务" : "已生成待解析任务");
    } finally {
      loading.value = false;
    }
  };

  const backfillCurrentScope = async (forceRebuild = false) => {
    backfillLoading.value = true;
    try {
      const res = await backfillParseQueue({
        transferId: query.transferId || undefined,
        sourceCode: query.sourceCode || undefined,
        routeId: query.routeId || undefined,
        tagCode: query.tagCode || undefined,
        status: query.fileStatus || undefined,
        deliveryStatus: query.deliveryStatus || undefined,
        parseStatus: query.parseStatus || undefined,
        forceRebuild,
        dryRun: false,
      });
      const generated = (
        isPageResult(res) ? (res.data ?? []) : Array.isArray(res) ? res : []
      ).map(mapRow);
      if (generated.length > 0) {
        message.success(
          forceRebuild
            ? `已重建 ${generated.length} 条待解析任务`
            : `已补漏 ${generated.length} 条待解析任务`,
        );
      } else {
        message.info("当前条件下没有可补漏的待解析任务");
      }
      await refreshAndKeepSelection();
    } finally {
      backfillLoading.value = false;
    }
  };

  const retryQueue = async (row: ParseQueueRow) => {
    loading.value = true;
    try {
      const res = await retryParseQueue(row.queueId, {
        forceRebuild: false,
      });
      const next = unwrapSingleResult(res);
      if (next) {
        const mapped = mapRow(next);
        const index = rows.value.findIndex(
          (item) => item.queueId === mapped.queueId,
        );
        if (index >= 0) {
          rows.value.splice(index, 1, mapped);
        }
        if (selectedRow.value?.queueId === mapped.queueId) {
          selectedRow.value = mapped;
        }
      }
      await refreshAndKeepSelection();
      message.success("已回到待订阅状态");
    } finally {
      loading.value = false;
    }
  };

  const subscribeQueue = async (row: ParseQueueRow) => {
    loading.value = true;
    try {
      const res = await subscribeParseQueue(row.queueId, {
        subscribedBy: "当前用户",
      });
      const next = unwrapSingleResult(res);
      if (next) {
        const mapped = mapRow(next);
        const index = rows.value.findIndex(
          (item) => item.queueId === mapped.queueId,
        );
        if (index >= 0) {
          rows.value.splice(index, 1, mapped);
        }
        if (selectedRow.value?.queueId === mapped.queueId) {
          selectedRow.value = mapped;
        }
      }
      await refreshAndKeepSelection();
      message.success("已订阅待解析事件");
    } finally {
      loading.value = false;
    }
  };

  const completeQueue = async (row: ParseQueueRow) => {
    loading.value = true;
    try {
      const res = await completeParseQueue(row.queueId, {
        parseResultJson: row.parseResultJson ?? {
          sheetCount: 3,
          rowCount: 128,
          status: "PARSED",
        },
      });
      const next = unwrapSingleResult(res);
      if (next) {
        const mapped = mapRow(next);
        const index = rows.value.findIndex(
          (item) => item.queueId === mapped.queueId,
        );
        if (index >= 0) {
          rows.value.splice(index, 1, mapped);
        }
        if (selectedRow.value?.queueId === mapped.queueId) {
          selectedRow.value = mapped;
        }
      }
      await refreshAndKeepSelection();
      message.success("已标记为已解析");
    } finally {
      loading.value = false;
    }
  };

  const failQueue = async (row: ParseQueueRow) => {
    loading.value = true;
    try {
      const res = await failParseQueue(row.queueId, {
        errorMessage: "结构化解析失败：示例异常",
      });
      const next = unwrapSingleResult(res);
      if (next) {
        const mapped = mapRow(next);
        const index = rows.value.findIndex(
          (item) => item.queueId === mapped.queueId,
        );
        if (index >= 0) {
          rows.value.splice(index, 1, mapped);
        }
        if (selectedRow.value?.queueId === mapped.queueId) {
          selectedRow.value = mapped;
        }
      }
      await refreshAndKeepSelection();
      message.warning("已标记为解析失败");
    } finally {
      loading.value = false;
    }
  };

  const formatParseStatus = (value: string | undefined) =>
    formatParseStatusLabel(value);

  const formatTriggerMode = (value: string | undefined) =>
    formatTriggerModeLabel(value);

  const formatStatus = (value: string | undefined) =>
    formatFileStatusLabel(value);

  void loadList();

  watch(
    () => route.query,
    () => {
      void openQueueFromRoute();
    },
    {
      deep: true,
      immediate: true,
    },
  );

  const page = reactive({
    loading,
    listLoading,
    backfillLoading,
    rows,
    tableData,
    total,
    pagination,
    query,
    pendingCount,
    parsingCount,
    parsedCount,
    failedCount,
    detailVisible,
    selectedRow,
    runQuery,
    resetQuery,
    handlePageChange,
    openDetailDrawer,
    closeDetail,
    openLifecyclePage,
    generateQueue,
    retryQueue,
    subscribeQueue,
    completeQueue,
    failQueue,
    backfillCurrentScope,
    formatParseStatus,
    formatTriggerMode,
    formatStatus,
    safeJson,
  });

  return { page };
};
