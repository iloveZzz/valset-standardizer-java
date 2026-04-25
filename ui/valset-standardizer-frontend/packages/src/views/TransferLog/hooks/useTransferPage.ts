import { computed, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import type {
  PageRecordsParams,
  PageResultTransferDeliveryRecordViewDTO,
  TransferDeliveryRecordViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import { unwrapSingleResult } from "@/utils/api-response";

type QueryState = {
  routeId: string;
  transferId: string;
  targetCode: string;
  executeStatus: string;
};

const api = getJavaSpringBootQuartzApi();

const defaultQuery = (): QueryState => ({
  routeId: "",
  transferId: "",
  targetCode: "",
  executeStatus: "",
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

const formatExecuteStatusLabel = (value: string | undefined) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!status) {
    return "-";
  }
  if (status === "SUCCESS" || status === "SUCCEEDED" || status === "DONE" || status === "COMPLETED") {
    return "成功";
  }
  if (status === "FAILED" || status === "FAIL" || status === "ERROR") {
    return "失败";
  }
  if (status === "PENDING") {
    return "待处理";
  }
  if (status === "RUNNING" || status === "PROCESSING") {
    return "执行中";
  }
  if (status === "RETRYING") {
    return "重试中";
  }
  if (status === "SKIPPED") {
    return "已跳过";
  }
  return status;
};

export const useTransferPage = () => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TransferDeliveryRecordViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferDeliveryRecordViewDTO | null>(null);
  const detailVisible = ref(false);
  const listLoading = ref(false);
  let listRequestId = 0;
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
  const transferCount = computed(
    () =>
      new Set(
        rows.value
          .map((row) => row.transferId)
          .filter((value) => value !== undefined),
      ).size,
  );
  const statusCount = computed(
    () =>
      new Set(rows.value.map((row) => row.executeStatus).filter(Boolean)).size,
  );

  const mapQuery = (
    pageIndex = pagination.value.current || 1,
    pageSize = pagination.value.pageSize || 10,
  ): PageRecordsParams => ({
    routeId: query.routeId || undefined,
    transferId: query.transferId || undefined,
    targetCode: query.targetCode || undefined,
    executeStatus: query.executeStatus || undefined,
    pageIndex: Math.max(pageIndex - 1, 0),
    pageSize,
  });

  const loadList = async (
    pageIndex = pagination.value.current || 1,
    pageSize = pagination.value.pageSize || 10,
  ) => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const res = await api.pageRecords(mapQuery(pageIndex, pageSize));
      if (requestId !== listRequestId) {
        return;
      }
      const page = res as PageResultTransferDeliveryRecordViewDTO | undefined;
      const records = page?.data ?? [];
      rows.value = records;
      total.value = Number(page?.totalCount ?? records.length);
      pagination.value.total = total.value;
      pagination.value.current =
        Number(page?.pageIndex ?? Math.max(pageIndex - 1, 0)) + 1;
      pagination.value.pageSize = Number(page?.pageSize ?? pageSize);
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      console.error("加载投递结果列表失败:", error);
      message.error("加载投递结果列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const openDetailDrawer = async (row: TransferDeliveryRecordViewDTO) => {
    try {
      const detail = row.deliveryId
        ? unwrapSingleResult(await api.getRecord(row.deliveryId))
        : row;
      selectedRow.value = detail ?? null;
      detailVisible.value = true;
    } catch (error) {
      console.error("加载投递结果详情失败:", error);
      message.error("加载投递结果详情失败");
    }
  };

  const runQuery = async () => {
    await loadList(1, pagination.value.pageSize || 10);
  };

  const resetQuery = async () => {
    Object.assign(query, defaultQuery());
    await loadList(1, pagination.value.pageSize || 10);
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

  const formatStatus = (value: string | undefined) => formatExecuteStatusLabel(value);

  void loadList();

  const page = reactive({
    loading: listLoading,
    rows,
    tableData,
    total,
    pageSize,
    pagination,
    errorCount,
    routeCount,
    transferCount,
    statusCount,
    query,
    selectedRow,
    detailVisible,
    openDetailDrawer,
    runQuery,
    resetQuery,
    handlePageChange,
    closeDetail: () => {
      detailVisible.value = false;
    },
    formatStatus,
    safeJson,
  });

  return {
    page,
  };
};
