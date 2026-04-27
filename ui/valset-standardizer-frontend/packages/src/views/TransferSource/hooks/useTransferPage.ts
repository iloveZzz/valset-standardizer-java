import { computed, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import type { ISchema, YTablePagination } from "@yss-ui/components";
import { GetTemplateName1SourceType } from "@/api/generated/valset/schemas/getTemplateName1SourceType";
import type {
  GetTemplateName2Params,
  ListSourcesParams,
  TransferSourceCheckpointItemViewDTO,
  TransferSourceCheckpointViewDTO,
  TransferFormTemplateViewDTO,
  TransferSourceUpsertCommand,
  TransferSourceViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import {
  injectFormilyAsyncValidator,
  normalizeFormilySchema,
  normalizeFormilyValues,
} from "@/utils/formily";
import {
  unwrapMultiResult,
  unwrapSingleResult,
} from "@/utils/api-response";
import { createMailConditionInitial } from "../../TransferShared/constants/mailCondition";
import type { SourcePage } from "../types";

type QueryState = {
  sourceType: string;
  sourceCode: string;
  sourceName: string;
  enabled: string;
  limit: number;
};

type SourceFormState = {
  sourceId?: string;
  sourceCode: string;
  sourceName: string;
  sourceType: string;
  enabled: boolean;
  connectionConfigText: string;
  sourceMetaText: string;
};

const SOURCE_TEMPLATE_BASE_KEYS = [
  "sourceCode",
  "sourceName",
  "enabled",
] as const;

const api = getJavaSpringBootQuartzApi();

const sourceTypeOptions = Object.values(GetTemplateName1SourceType).map(
  (value) => ({
    label: value,
    value,
  }),
);

const defaultQuery = (): QueryState => ({
  sourceType: "",
  sourceCode: "",
  sourceName: "",
  enabled: "",
  limit: 100,
});

const defaultForm = (): SourceFormState => ({
  sourceCode: "",
  sourceName: "",
  sourceType: GetTemplateName1SourceType.LOCAL_DIR,
  enabled: true,
  connectionConfigText: "{}",
  sourceMetaText: "{}",
});

const normalizeMailCondition = (value: unknown) => {
  if (value && typeof value === "object") {
    return value;
  }

  return createMailConditionInitial();
};

const stringifyJson = (value: unknown) => {
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

  const extractTemplateConfigValues = (values: Record<string, any>) => {
    const next = { ...values };
    SOURCE_TEMPLATE_BASE_KEYS.forEach((key) => {
      delete next[key];
    });
    return next;
  };

  const normalizeConnectionConfig = (values: Record<string, any>) => {
    const next = { ...values };
    const directory = next.directory;
    if (typeof directory === "string") {
      next.directory = directory.trim();
    }
    return next;
  };

export const useTransferPage = (): { page: SourcePage } => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TransferSourceViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferSourceViewDTO | null>(null);
  const detailVisible = ref(false);
  const checkpointRows = ref<TransferSourceCheckpointViewDTO[]>([]);
  const checkpointItemRows = ref<TransferSourceCheckpointItemViewDTO[]>([]);
  const checkpointLoading = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const editingRow = ref<TransferSourceViewDTO | null>(null);
  const formState = reactive<SourceFormState>(defaultForm());
  const templateNamePreview = ref("");
  const templateDescription = ref("");
  const templateVersion = ref("");
  const templateMode = ref<0 | 1 | 2>(1);
  const templateReadPretty = ref(false);
  const templateSchema = ref<ISchema | null>(null);
  const templateInitialValues = ref<Record<string, any>>({});
  const templateValues = ref<Record<string, any>>({});
  const syncingTemplateType = ref(false);
  const templateScope = {
    asyncSourceCodeValidator: async (value: string | undefined) => {
      const nextValue = String(value ?? "").trim();
      if (!nextValue) {
        return undefined;
      }

      const result = await api.listSources({
        sourceCode: nextValue,
        limit: 100,
      });
      const duplicated = unwrapMultiResult(result).some(
        (item) =>
          item.sourceCode === nextValue && item.sourceId !== formState.sourceId,
      );

      if (duplicated) {
        return "来源编码已存在";
      }

      return undefined;
    },
  };
  const templateDetailOptions = ref<Record<string, any>>({
    bordered: true,
    maxColumns: 2,
  });
  const templateGridDefaults = ref<Record<string, any>>({
    maxColumns: 2,
    minColumns: 1,
    minWidth: 260,
    columnGap: 16,
    rowGap: 0,
  });
  const templateFormRef = ref<{ submit: () => Promise<unknown> } | null>(null);
  const templateLoading = ref(false);
  const listLoading = ref(false);
  const formSubmitting = ref(false);
  const enabledUpdatingIds = reactive<Record<string, boolean>>({});
  let listRequestId = 0;
  let detailRequestId = 0;
  let templateRequestId = 0;

  const resetTemplateState = () => {
    templateNamePreview.value = "";
    templateDescription.value = "";
    templateVersion.value = "";
    templateMode.value = 1;
    templateReadPretty.value = false;
    templateSchema.value = null;
    templateInitialValues.value = {
      mailCondition: createMailConditionInitial(),
    };
    templateValues.value = {
      mailCondition: createMailConditionInitial(),
    };
    templateDetailOptions.value = { bordered: true, maxColumns: 2 };
    templateGridDefaults.value = {
      maxColumns: 2,
      minColumns: 1,
      minWidth: 260,
      columnGap: 16,
      rowGap: 0,
    };
  };

  const hydrateTemplateValuesFromRow = (row?: TransferSourceViewDTO) => {
    const connectionConfig = normalizeFormilyValues(row?.connectionConfig);
    const currentMailCondition =
      (connectionConfig as Record<string, unknown>).mailCondition ??
      templateValues.value.mailCondition ??
      templateInitialValues.value.mailCondition;
    templateValues.value = {
      ...templateInitialValues.value,
      ...extractTemplateConfigValues(templateValues.value),
      ...connectionConfig,
      ...normalizeFormilyValues(row?.sourceMeta),
      sourceCode: row?.sourceCode ?? templateValues.value.sourceCode ?? "",
      sourceName: row?.sourceName ?? templateValues.value.sourceName ?? "",
      enabled: row?.enabled ?? templateValues.value.enabled ?? true,
      mailCondition: normalizeMailCondition(currentMailCondition),
    };
  };

  const applyTemplate = (template: TransferFormTemplateViewDTO) => {
    const formDefinition = template.formDefinition;
    const initialValues = normalizeFormilyValues(
      formDefinition?.initialValues ?? template.initialValues,
    );
    const detailOptions = normalizeFormilyValues(formDefinition?.detailOptions);
    const gridDefaults = normalizeFormilyValues(formDefinition?.gridDefaults);

    templateDescription.value = template.description ?? "";
    templateVersion.value = template.version ?? "";
    templateMode.value = (formDefinition?.mode as 0 | 1 | 2 | undefined) ?? 1;
    templateReadPretty.value = formDefinition?.readPretty ?? false;
    templateSchema.value =
      injectFormilyAsyncValidator(
        normalizeFormilySchema(formDefinition?.schema),
        "sourceCode",
        "asyncSourceCodeValidator",
        "来源编码已存在",
      ) ?? null;
    templateInitialValues.value = {
      ...initialValues,
      mailCondition: normalizeMailCondition(
        (initialValues as Record<string, unknown>).mailCondition,
      ),
    };
    templateValues.value = { ...templateInitialValues.value };
    templateDetailOptions.value = {
      bordered: detailOptions.bordered ?? true,
      ...detailOptions,
    };
    templateGridDefaults.value = {
      maxColumns: 2,
      minColumns: 1,
      minWidth: 260,
      columnGap: 16,
      rowGap: 0,
      ...gridDefaults,
    };
  };

  const syncTemplateBySourceType = async (sourceType: string) => {
    const requestId = ++templateRequestId;

    if (!sourceType) {
      resetTemplateState();
      return;
    }

      templateLoading.value = true;
      syncingTemplateType.value = true;
      templateSchema.value = null;
      templateInitialValues.value = {};
      templateValues.value = {};
    try {
      const templateName = unwrapSingleResult(
        await api.getTemplateName2({
          sourceType: sourceType as GetTemplateName2Params["sourceType"],
        }),
      );
      if (requestId !== templateRequestId) return;

      templateNamePreview.value = templateName ?? "";
      if (!templateName) {
        resetTemplateState();
        templateNamePreview.value = "";
        return;
      }

      const template = unwrapSingleResult(await api.getTemplate(templateName));
      if (requestId !== templateRequestId) return;
      if (!template) {
        resetTemplateState();
        return;
      }
      applyTemplate(template);
    } catch (error) {
      if (requestId === templateRequestId) {
        resetTemplateState();
      }
      console.error("加载来源模板失败:", error);
    } finally {
      if (requestId === templateRequestId) {
        templateLoading.value = false;
        syncingTemplateType.value = false;
      }
    }
  };

  const pageSize = computed(() => query.limit);
  const tableData = computed(() => {
    const current = pagination.value.current || 1;
    const pageSizeValue = pagination.value.pageSize || 10;
    const start = (current - 1) * pageSizeValue;
    return rows.value.slice(start, start + pageSizeValue);
  });
  const enabledCount = computed(
    () => rows.value.filter((row) => row.enabled === true).length,
  );
  const hasEnabledRoutes = (row?: TransferSourceViewDTO | null) => {
    if (!row) {
      return false;
    }
    return Number(row.enabledRouteCount ?? 0) > 0;
  };
  const templateBoundCount = computed(
    () => rows.value.filter((row) => Boolean(row.formTemplateName)).length,
  );
  const sourceTypeCount = computed(
    () => new Set(rows.value.map((row) => row.sourceType).filter(Boolean)).size,
  );

  const mapQuery = (): ListSourcesParams => ({
    sourceType: query.sourceType || undefined,
    sourceCode: query.sourceCode || undefined,
    sourceName: query.sourceName || undefined,
    enabled:
      query.enabled === "true"
        ? true
        : query.enabled === "false"
          ? false
          : undefined,
    limit: query.limit || undefined,
  });

  const loadList = async () => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const res = await api.listSources(mapQuery());
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
      console.error("加载来源列表失败:", error);
      message.error("加载来源列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const fillForm = (row: TransferSourceViewDTO) => {
    formState.sourceId = row.sourceId;
    formState.sourceCode = row.sourceCode || "";
    formState.sourceName = row.sourceName || "";
    formState.sourceType =
      row.sourceType || GetTemplateName1SourceType.LOCAL_DIR;
    formState.enabled = row.enabled ?? true;
    formState.connectionConfigText = stringifyJson(row.connectionConfig);
    formState.sourceMetaText = stringifyJson(row.sourceMeta);
  };

  const resetForm = () => {
    Object.assign(formState, defaultForm());
    templateNamePreview.value = "";
    editingRow.value = null;
  };

  const openCreateDialog = () => {
    formMode.value = "create";
    editingRow.value = null;
    resetForm();
    formVisible.value = true;
    void syncTemplateBySourceType(formState.sourceType);
  };

  const openEditDialog = async (row: TransferSourceViewDTO) => {
    formMode.value = "edit";
    try {
      const detail = row.sourceId
        ? unwrapSingleResult(await api.getSource(row.sourceId))
        : row;
      if (detail) {
        fillForm(detail);
      }
      editingRow.value = detail ?? row;
      formVisible.value = true;
      await syncTemplateBySourceType(formState.sourceType);
      if (detail) {
        hydrateTemplateValuesFromRow(detail);
      }
    } catch (error) {
      console.error("加载来源详情失败:", error);
      message.error("加载来源详情失败");
    }
  };

  const openDetailDrawer = async (row: TransferSourceViewDTO) => {
    const requestId = ++detailRequestId;
    try {
      checkpointLoading.value = true;
      selectedRow.value = row.sourceId
        ? unwrapSingleResult(await api.getSource(row.sourceId)) ?? null
        : row;
      if (requestId !== detailRequestId) {
        return;
      }
      checkpointRows.value = [];
      checkpointItemRows.value = [];
      if (selectedRow.value?.sourceId) {
        const [checkpoints, checkpointItems] = await Promise.all([
          api.listCheckpoints(selectedRow.value.sourceId, { limit: 20 }),
          api.listCheckpointItems(selectedRow.value.sourceId, { limit: 20 }),
        ]);
        if (requestId !== detailRequestId) {
          return;
        }
        checkpointRows.value = unwrapMultiResult(checkpoints);
        checkpointItemRows.value = unwrapMultiResult(checkpointItems);
      }
      detailVisible.value = true;
    } catch (error) {
      if (requestId !== detailRequestId) {
        return;
      }
      console.error("加载来源详情失败:", error);
      message.error("加载来源详情失败");
    } finally {
      if (requestId === detailRequestId) {
        checkpointLoading.value = false;
      }
    }
  };

  const confirmDelete = (row: TransferSourceViewDTO) => {
    Modal.confirm({
      title: "删除来源",
      content: `确认删除来源「${row.sourceName || row.sourceCode || row.sourceId}」吗？如果该来源已被分拣路由引用，将无法删除，需要先解除关联路由配置。`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: async () => {
        if (!row.sourceId) {
          message.error("来源主键缺失，无法删除");
          return;
        }
        try {
          await api.deleteSource(row.sourceId);
          message.success("删除成功");
          await loadList();
        } catch (error) {
          console.error("删除来源失败:", error);
          if (error instanceof Error) {
            message.error(error.message);
            return;
          }
          message.error("删除来源失败");
        }
      },
    });
  };

  const buildPayload = (): TransferSourceUpsertCommand => {
    const templateConfig = extractTemplateConfigValues(templateValues.value);
    const connectionConfig = normalizeConnectionConfig(templateConfig);

    return {
      sourceId: formState.sourceId,
      sourceCode: String(templateValues.value.sourceCode ?? "").trim(),
      sourceName: String(templateValues.value.sourceName ?? "").trim(),
      sourceType: String(formState.sourceType ?? "").trim(),
      enabled: Boolean(templateValues.value.enabled),
      connectionConfig:
        Object.keys(connectionConfig).length > 0 ? connectionConfig : undefined,
      sourceMeta: undefined,
    };
  };

  const buildPayloadFromRow = (
    row: TransferSourceViewDTO,
    enabled: boolean,
  ): TransferSourceUpsertCommand => ({
    sourceId: row.sourceId,
    sourceCode: String(row.sourceCode ?? "").trim(),
    sourceName: String(row.sourceName ?? "").trim(),
    sourceType: String(row.sourceType ?? "").trim(),
    enabled,
    connectionConfig: normalizeConnectionConfig(row.connectionConfig ?? {}),
    sourceMeta: row.sourceMeta,
  });

  const isEnabledUpdating = (sourceId?: string) => {
    if (!sourceId) {
      return false;
    }
    return Boolean(enabledUpdatingIds[sourceId]);
  };

  const isIngestBusy = (row?: TransferSourceViewDTO | null) => {
    return Boolean(row?.ingestBusy);
  };

  const formatIngestStatus = (value?: string) => {
    const normalized = String(value ?? "")
      .trim()
      .toUpperCase();
    if (!normalized) {
      return "-";
    }
    if (normalized === "RUNNING") {
      return "收取中";
    }
    if (normalized === "STOPPING") {
      return "停止中";
    }
    if (normalized === "STOPPED") {
      return "已停止";
    }
    if (normalized === "SUCCESS") {
      return "收取成功";
    }
    if (normalized === "FAILED") {
      return "收取失败";
    }
    if (normalized === "SKIPPED") {
      return "已跳过";
    }
    if (normalized === "REUSED") {
      return "复用";
    }
    return normalized;
  };

  const toggleEnabled = async (
    row: TransferSourceViewDTO,
    checked: boolean,
  ) => {
    if (!row.sourceId) {
      message.error("来源主键缺失，无法切换状态");
      return;
    }
    if (hasEnabledRoutes(row)) {
      message.warning("该来源已存在启用中的路由配置，无法直接切换启用状态");
      return;
    }

    const previousEnabled = Boolean(row.enabled);
    if (previousEnabled === checked) {
      return;
    }

    enabledUpdatingIds[row.sourceId] = true;
    row.enabled = checked;

    try {
      await api.updateSource(row.sourceId, buildPayloadFromRow(row, checked));
      message.success(checked ? "启用成功" : "停用成功");
      await loadList();
    } catch (error) {
      row.enabled = previousEnabled;
      console.error("切换来源启用状态失败:", error);
      message.error("切换来源启用状态失败");
    } finally {
      delete enabledUpdatingIds[row.sourceId];
    }
  };

  const validateTemplateForm = async () => {
    if (!templateSchema.value) {
      return true;
    }

    if (templateLoading.value) {
      message.warning("模板正在加载中，请稍后再保存");
      return false;
    }

    try {
      await templateFormRef.value?.submit();
      return true;
    } catch (error) {
      console.error("来源模板校验失败:", error);
      message.error("请先完善来源模板中的必填项");
      return false;
    }
  };

  const submitForm = async () => {
    formSubmitting.value = true;
    try {
      const templateValidated = await validateTemplateForm();
      if (!templateValidated) {
        return;
      }

      if (
        formMode.value === "edit" &&
        editingRow.value &&
        hasEnabledRoutes(editingRow.value) &&
        Boolean(templateValues.value.enabled) !== Boolean(formState.enabled)
      ) {
        message.warning("该来源已存在启用中的路由配置，无法修改启用状态");
        return;
      }

      const payload = buildPayload();
      if (formMode.value === "create") {
        await api.createSource(payload);
        message.success("新建来源成功");
      } else if (formState.sourceId) {
        await api.updateSource(formState.sourceId, payload);
        message.success("更新来源成功");
      } else {
        message.error("来源主键缺失，无法更新");
        return;
      }
      formVisible.value = false;
      await loadList();
    } catch (error) {
      console.error("保存来源失败:", error);
      message.error(error instanceof Error ? error.message : "保存来源失败");
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

  const formatEnabled = (value: boolean | undefined) => {
    if (value === true) return "启用";
    if (value === false) return "停用";
    return "-";
  };

  watch(
    () => formState.sourceType,
    async (value) => {
      if (!formVisible.value || syncingTemplateType.value) {
        return;
      }
      await syncTemplateBySourceType(String(value ?? ""));
    },
  );

  void loadList();

  const page = reactive({
    loading: listLoading,
    rows,
    tableData,
    total,
    pageSize,
    pagination,
    enabledCount,
    templateBoundCount,
    sourceTypeCount,
    query,
    sourceTypeOptions,
    templateNamePreview,
    templateDescription,
    templateVersion,
    templateMode,
    templateReadPretty,
    templateSchema,
    templateInitialValues,
    templateValues,
    templateScope,
    templateDetailOptions,
    templateGridDefaults,
    templateFormRef,
    setTemplateFormRef: (instance: any) => {
      templateFormRef.value = instance;
    },
    templateLoading,
    enabledUpdatingIds,
    isEnabledUpdating,
    hasEnabledRoutes,
    isIngestRunning: isIngestBusy,
    isIngestBusy,
    formatIngestStatus,
    toggleEnabled,
    formVisible,
    formMode,
    editingRow,
    formState,
    selectedRow,
    detailVisible,
    checkpointRows,
    checkpointItemRows,
    checkpointLoading,
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
    },
    closeDetail: () => {
      detailVisible.value = false;
      checkpointRows.value = [];
      checkpointItemRows.value = [];
    },
    formatEnabled,
  }) as SourcePage;

  return {
    page,
  };
};
