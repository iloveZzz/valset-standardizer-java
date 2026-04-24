import { computed, reactive, ref } from "vue";
import { message, Modal } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import { copyToClipboard } from "@/utils";
import { GetTemplateName1SourceType } from "@/api/generated/valset/schemas/getTemplateName1SourceType";
import { GetTemplateName2TargetType } from "@/api/generated/valset/schemas/getTemplateName2TargetType";
import type {
  ListRulesParams,
  ListRoutes1Params,
  ListSourcesParams,
  ListTargetsParams,
  TransferRouteUpsertCommand,
  TransferRouteViewDTO,
  TransferRuleViewDTO,
  TransferSourceViewDTO,
  TransferTargetViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import {
  unwrapMultiResult,
  unwrapSingleResult,
} from "@/utils/api-response";
import type {
  RouteConfigPage,
  RouteFlowAnchor,
  RouteFlowPreview,
  RouteFlowStats,
} from "../types";

type QueryState = {
  sourceCode: string;
  sourceType: string;
  ruleId: string;
  targetCode: string;
  targetType: string;
  limit: number;
};

type RouteFormState = RouteConfigPage["formState"];

const SELECT_LIMIT = 200;
const api = getJavaSpringBootQuartzApi();

const defaultQuery = (): QueryState => ({
  sourceCode: "",
  sourceType: "",
  ruleId: "",
  targetCode: "",
  targetType: "",
  limit: 100,
});

const defaultForm = (): RouteFormState => ({
  routeId: undefined,
  sourceId: undefined,
  sourceCode: "",
  sourceType: "",
  targetCode: "",
  targetType: "",
  targetPath: "",
  ruleId: undefined,
  renamePattern: "",
});

const uniqueByKey = <T>(
  items: T[],
  keyGetter: (item: T) => string | number | undefined,
) => {
  const seen = new Set<string | number>();
  return items.filter((item) => {
    const key = keyGetter(item);
    if (key === undefined || key === null) {
      return false;
    }
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

const buildSourceOption = (item: TransferSourceViewDTO) => ({
  label: [item.sourceCode, item.sourceName, item.sourceType]
    .filter(Boolean)
    .join(" · "),
  value: item.sourceCode ?? "",
});

const buildTargetOption = (item: TransferTargetViewDTO) => ({
  label: [
    item.targetCode,
    item.targetName,
    item.targetType,
    item.targetPathTemplate,
  ]
    .filter(Boolean)
    .join(" · "),
  value: item.targetCode ?? "",
});

const buildRuleOption = (item: TransferRuleViewDTO) => ({
  label: [item.ruleId, item.ruleCode, item.ruleName, item.ruleVersion]
    .filter(Boolean)
    .join(" · "),
  value: item.ruleId ?? "",
});

const buildFlowPreview = (
  row: TransferRouteViewDTO | null,
): RouteFlowPreview => {
  const sourceTitle = row?.sourceCode?.trim() || "请选择一条路由";
  const sourceMeta = [
    row?.sourceType || "来源类型待补全",
    row?.sourceId != null ? `来源ID ${row.sourceId}` : "",
  ]
    .filter(Boolean)
    .join(" · ");
  const ruleTitle =
    row?.ruleId != null ? `规则 #${row.ruleId}` : "路由规则待选择";
  const ruleMeta = [
    row?.renamePattern ? `模板 ${row.renamePattern}` : "默认命名",
    row?.ruleId != null ? `规则ID ${row.ruleId}` : "规则待选择",
  ]
    .filter(Boolean)
    .join(" · ");
  const targetTitle = row?.targetCode?.trim() || "目标待配置";
  const targetMeta = [
    row?.targetType || "目标类型待补全",
    row?.targetPath || "目标路径待配置",
  ]
    .filter(Boolean)
    .join(" · ");
  const routeMetaSummary = formatRouteMetaCount(row);

  return {
    sourceTitle,
    sourceMeta,
    ruleTitle,
    ruleMeta,
    targetTitle,
    targetMeta,
    routeMetaSummary,
  };
};

const buildFlowAnchor = (
  selectedRow: TransferRouteViewDTO | null,
  rows: TransferRouteViewDTO[],
) => {
  if (selectedRow?.routeId != null) {
    return {
      label: "当前映射",
      detail:
        [selectedRow.sourceCode, selectedRow.ruleId, selectedRow.targetCode]
          .filter(Boolean)
          .join(" → ") || `路由 ${selectedRow.routeId}`,
    };
  }

  const firstRow = rows[0];
  if (firstRow?.routeId != null) {
    return {
      label: "默认映射",
      detail:
        [firstRow.sourceCode, firstRow.ruleId, firstRow.targetCode]
          .filter(Boolean)
          .join(" → ") || `路由 ${firstRow.routeId}`,
    };
  }

  return {
    label: "暂无映射",
    detail: "请先创建或选择一条路由映射",
  } satisfies RouteFlowAnchor;
};

const formatRouteMetaCount = (row: TransferRouteViewDTO | null) => {
  if (!row?.routeMeta) {
    return "无扩展信息";
  }
  const keys = Object.keys(row.routeMeta);
  return keys.length ? `${keys.length} 个扩展字段` : "无扩展信息";
};

export const useTransferPage = (): { page: RouteConfigPage } => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TransferRouteViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferRouteViewDTO | null>(null);
  const detailVisible = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const formState = reactive<RouteFormState>(defaultForm());
  const sourceRows = ref<TransferSourceViewDTO[]>([]);
  const targetRows = ref<TransferTargetViewDTO[]>([]);
  const ruleRows = ref<TransferRuleViewDTO[]>([]);
  const selectLoading = ref(false);
  const listLoading = ref(false);
  const formSubmitting = ref(false);
  const formRef = ref<{ validate: () => Promise<void> } | null>(null);
  const sourceTypeOptions = Object.values(GetTemplateName1SourceType).map(
    (value) => ({
      label: value,
      value,
    }),
  );
  const targetTypeOptions = Object.values(GetTemplateName2TargetType).map(
    (value) => ({
      label: value,
      value,
    }),
  );

  let listRequestId = 0;
  let selectRequestId = 0;

  const sourceOptions = computed(() => sourceRows.value.map(buildSourceOption));
  const targetOptions = computed(() => targetRows.value.map(buildTargetOption));
  const ruleOptions = computed(() => ruleRows.value.map(buildRuleOption));

  const resetForm = () => {
    Object.assign(formState, defaultForm());
  };

  const hydrateFormFromRow = (row: TransferRouteViewDTO) => {
    Object.assign(formState, defaultForm(), {
      routeId: row.routeId,
      sourceId: row.sourceId,
      sourceCode: row.sourceCode ?? "",
      sourceType: row.sourceType ?? "",
      targetCode: row.targetCode ?? "",
      targetType: row.targetType ?? "",
      targetPath: row.targetPath ?? "",
      ruleId: row.ruleId,
      renamePattern: row.renamePattern ?? "",
    });
  };

  const ensureCurrentOptionRows = (row: TransferRouteViewDTO) => {
    if (
      row.sourceCode &&
      !sourceRows.value.some((item) => item.sourceCode === row.sourceCode)
    ) {
      sourceRows.value = uniqueByKey(
        [
          {
            sourceId: row.sourceId,
            sourceCode: row.sourceCode,
            sourceType: row.sourceType,
            sourceName: row.sourceCode,
          } as TransferSourceViewDTO,
          ...sourceRows.value,
        ],
        (item) => item.sourceCode,
      );
    }

    if (
      row.targetCode &&
      !targetRows.value.some((item) => item.targetCode === row.targetCode)
    ) {
      targetRows.value = uniqueByKey(
        [
          {
            targetCode: row.targetCode,
            targetType: row.targetType,
            targetPathTemplate: row.targetPath,
            targetName: row.targetCode,
          } as TransferTargetViewDTO,
          ...targetRows.value,
        ],
        (item) => item.targetCode,
      );
    }

    if (
      row.ruleId &&
      !ruleRows.value.some((item) => item.ruleId === row.ruleId)
    ) {
      ruleRows.value = uniqueByKey(
        [
          {
            ruleId: row.ruleId,
            ruleCode: String(row.ruleId),
            ruleName: `规则 ${row.ruleId}`,
          } as TransferRuleViewDTO,
          ...ruleRows.value,
        ],
        (item) => item.ruleId,
      );
    }
  };

  const loadSelectRows = async () => {
    const requestId = ++selectRequestId;
    selectLoading.value = true;
    try {
      const [sources, targets, rules] = await Promise.all([
        api.listSources({
          limit: SELECT_LIMIT,
          enabled: true,
        } as ListSourcesParams),
        api.listTargets({
          limit: SELECT_LIMIT,
          enabled: true,
        } as ListTargetsParams),
        api.listRules({
          limit: SELECT_LIMIT,
          enabled: true,
        } as ListRulesParams),
      ]);

      if (requestId !== selectRequestId) {
        return;
      }
      sourceRows.value = unwrapMultiResult(sources);
      targetRows.value = unwrapMultiResult(targets);
      ruleRows.value = unwrapMultiResult(rules);
    } catch (error) {
      if (requestId !== selectRequestId) {
        return;
      }
      console.error("加载下拉数据失败:", error);
      message.error("加载下拉数据失败");
    } finally {
      if (requestId === selectRequestId) {
        selectLoading.value = false;
      }
    }
  };

  const mapQuery = (): ListRoutes1Params => {
    const next: ListRoutes1Params = {};
    if (query.sourceCode) next.sourceCode = query.sourceCode;
    if (query.sourceType) next.sourceType = query.sourceType;
    if (query.ruleId) next.ruleId = query.ruleId;
    if (query.targetCode) next.targetCode = query.targetCode;
    if (query.targetType) next.targetType = query.targetType;
    next.limit = query.limit;
    return next;
  };

  const loadList = async () => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const res = await api.listRoutes1(mapQuery());
      if (requestId !== listRequestId) {
        return;
      }
      const records = unwrapMultiResult(res);
      rows.value = records;
      total.value = records.length;
      pagination.value.total = total.value;
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      console.error("加载路由列表失败:", error);
      message.error("加载路由列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const loadFormContext = async (row?: TransferRouteViewDTO) => {
    await loadSelectRows();
    if (row) {
      ensureCurrentOptionRows(row);
    }
  };

  const openCreateDialog = async () => {
    formMode.value = "create";
    resetForm();
    await loadFormContext();
    formVisible.value = true;
  };

  const openEditDialog = async (row: TransferRouteViewDTO) => {
    formMode.value = "edit";
    try {
      const detail = row.routeId
        ? unwrapSingleResult(await api.getRoute1(row.routeId))
        : row;
      if (!detail) {
        message.error("加载路由详情失败");
        return;
      }

      hydrateFormFromRow(detail);
      await loadFormContext(detail);
      formVisible.value = true;
    } catch (error) {
      console.error("加载路由详情失败:", error);
      message.error("加载路由详情失败");
    }
  };

  const openDetailDrawer = async (row: TransferRouteViewDTO) => {
    try {
      selectedRow.value = row.routeId
        ? unwrapSingleResult(await api.getRoute1(row.routeId)) ?? null
        : row;
      detailVisible.value = true;
    } catch (error) {
      console.error("加载路由详情失败:", error);
      message.error("加载路由详情失败");
    }
  };

  const confirmDelete = (row: TransferRouteViewDTO) => {
    Modal.confirm({
      title: "删除路由配置",
      content: `确认删除路由配置「${row.sourceCode || row.targetCode || row.routeId}」吗？`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: async () => {
        if (!row.routeId) {
          message.error("路由主键缺失，无法删除");
          return;
        }
        try {
          await api.deleteRoute(row.routeId);
          message.success("删除成功");
          await loadList();
        } catch (error) {
          console.error("删除路由失败:", error);
          message.error("删除路由失败");
        }
      },
    });
  };

  const handleSourceChange = (value: unknown) => {
    const nextValue = String(value ?? "");
    if (!nextValue) {
      formState.sourceId = undefined;
      formState.sourceCode = "";
      formState.sourceType = "";
      return;
    }

    const source = sourceRows.value.find(
      (item) => item.sourceCode === nextValue,
    );
    formState.sourceId = source?.sourceId;
    formState.sourceCode = source?.sourceCode ?? nextValue;
    formState.sourceType = source?.sourceType ?? "";
  };

  const handleTargetChange = (value: unknown) => {
    const nextValue = String(value ?? "");
    if (!nextValue) {
      formState.targetCode = "";
      formState.targetType = "";
      formState.targetPath = "";
      return;
    }

    const target = targetRows.value.find(
      (item) => item.targetCode === nextValue,
    );
    formState.targetCode = target?.targetCode ?? nextValue;
    formState.targetType = target?.targetType ?? "";
    formState.targetPath = target?.targetPathTemplate ?? "";
  };

  const handleRuleChange = (value: unknown) => {
    const nextValue = String(value ?? "").trim();
    if (!nextValue) {
      formState.ruleId = undefined;
      return;
    }

    const rule = ruleRows.value.find((item) => item.ruleId === nextValue);
    formState.ruleId = rule?.ruleId ?? nextValue;
  };

  const buildPayload = (): TransferRouteUpsertCommand => {
    const sourceCode = String(formState.sourceCode ?? "").trim();
    const sourceType = String(formState.sourceType ?? "").trim();
    const targetCode = String(formState.targetCode ?? "").trim();
    const targetType = String(formState.targetType ?? "").trim();
    const targetPath = String(formState.targetPath ?? "").trim();
    const renamePattern = String(formState.renamePattern ?? "").trim();
    const ruleId = String(formState.ruleId ?? "").trim();

    if (!sourceCode) {
      throw new Error("请选择来源规则");
    }
    if (!sourceType) {
      throw new Error("来源类型不能为空");
    }
    if (!targetCode) {
      throw new Error("请选择目标规则");
    }
    if (!targetType) {
      throw new Error("目标类型不能为空");
    }
    if (!targetPath) {
      throw new Error("目标路径不能为空");
    }
    if (!ruleId) {
      throw new Error("请选择分拣规则");
    }

    return {
      routeId: formState.routeId,
      sourceId: formState.sourceId,
      sourceCode,
      sourceType,
      targetCode,
      targetType,
      targetPath,
      ruleId,
      renamePattern: renamePattern || undefined,
    };
  };

  const submitForm = async () => {
    formSubmitting.value = true;
    try {
      if (formRef.value) {
        try {
          await formRef.value.validate();
        } catch {
          return;
        }
      }
      const payload = buildPayload();

      if (formMode.value === "create") {
        await api.createRoute(payload);
        message.success("新建路由配置成功");
      } else if (formState.routeId) {
        await api.updateRoute(formState.routeId, payload);
        message.success("更新路由配置成功");
      } else {
        message.error("路由主键缺失，无法更新");
        return;
      }

      formVisible.value = false;
      await loadList();
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
        return;
      }
      console.error("保存路由配置失败:", error);
      message.error("保存路由配置失败");
    } finally {
      formSubmitting.value = false;
    }
  };

  const runQuery = async () => {
    pagination.value.current = 1;
    await loadList();
  };

  const resetQuery = async () => {
    Object.assign(query, defaultQuery());
    pagination.value.current = 1;
    await loadList();
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
  };

  const formatRouteMetaSummary = (row: TransferRouteViewDTO | null) =>
    formatRouteMetaCount(row);

  const copyRouteField = async (
    value: string | number | undefined | null,
    label: string,
  ) => {
    const text =
      value === null || value === undefined ? "" : String(value).trim();
    if (!text) {
      message.warning(`${label} 为空，无法复制`);
      return;
    }

    const copied = await copyToClipboard(text);
    if (copied) {
      message.success(`${label} 已复制`);
      return;
    }

    message.error(`${label} 复制失败`);
  };

  const flowStats = computed<RouteFlowStats>(() => {
    const sourceSet = new Set<string>();
    const targetSet = new Set<string>();
    let routedCount = 0;
    let pendingCount = 0;

    rows.value.forEach((row) => {
      if (row.sourceCode) {
        sourceSet.add(row.sourceCode);
      }
      if (row.targetCode) {
        targetSet.add(row.targetCode);
      }
      routedCount += 1;
    });

    return {
      sourceCount: sourceSet.size,
      targetCount: targetSet.size,
      routedCount,
      pendingCount,
    };
  });

  const flowPreview = computed(() =>
    buildFlowPreview(selectedRow.value ?? rows.value[0] ?? null),
  );

  const flowAnchor = computed(() =>
    buildFlowAnchor(selectedRow.value, rows.value),
  );

  const flowTableLegend = computed(() => [
    {
      label: "来源",
      hint: "来源编码 / 来源类型",
      tone: "source",
    },
    {
      label: "路由 + 规则",
      hint: "规则ID / 模板 / 扩展信息",
      tone: "route",
    },
    {
      label: "目标",
      hint: "目标编码 / 目标类型 / 路径",
      tone: "target",
    },
  ]);

  void loadSelectRows();
  void loadList();

  const page = reactive({
    loading: listLoading,
    rows,
    flowPreview,
    flowAnchor,
    flowTableLegend,
    flowStats,
    tableData: computed(() => {
      const current = pagination.value.current || 1;
      const pageSizeValue = pagination.value.pageSize || 10;
      const start = (current - 1) * pageSizeValue;
      return rows.value.slice(start, start + pageSizeValue);
    }),
    total,
    pageSize: computed(() => query.limit),
    pagination,
    query,
    sourceOptions,
    targetOptions,
    ruleOptions,
    sourceTypeOptions,
    targetTypeOptions,
    selectLoading,
    formVisible,
    formMode,
    formState,
    detailVisible,
    selectedRow,
    formSubmitting,
    handlePageChange,
    openCreateDialog,
    openEditDialog,
    openDetailDrawer,
    confirmDelete,
    runQuery,
    resetQuery,
    submitForm,
    closeForm: () => {
      formVisible.value = false;
      resetForm();
    },
    closeDetail: () => {
      detailVisible.value = false;
    },
    handleSourceChange,
    handleTargetChange,
    handleRuleChange,
    setFormRef: (instance: unknown) => {
      formRef.value = instance as { validate: () => Promise<void> } | null;
    },
    copyRouteField,
    formatRouteMetaSummary,
  }) as RouteConfigPage;

  return {
    page,
  };
};
