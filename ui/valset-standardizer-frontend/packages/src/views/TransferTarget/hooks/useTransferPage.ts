import { computed, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import type { ISchema, YTablePagination } from "@yss-ui/components";
import { GetTemplateName2TargetType } from "@/api/generated/valset/schemas/getTemplateName2TargetType";
import type {
  GetTemplateName2Params,
  TransferFormTemplateViewDTO,
  ListTargetsParams,
  TransferTargetUpsertCommand,
  TransferTargetViewDTO,
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
import type { TargetPage } from "../types";

type QueryState = {
  targetType: string;
  targetCode: string;
  enabled: string;
  limit: number;
};

type TargetFormState = {
  targetId?: string;
  targetCode: string;
  targetName: string;
  targetType: string;
  enabled: boolean;
  targetPathTemplate: string;
  connectionConfigText: string;
  targetMetaText: string;
};

const TARGET_TEMPLATE_BASE_KEYS = [
  "targetCode",
  "targetName",
  "enabled",
  "targetPathTemplate",
] as const;

const api = getJavaSpringBootQuartzApi();

const TARGET_TYPE_LABELS: Record<string, string> = {
  EMAIL: "邮件",
  S3: "S3",
  SFTP: "SFTP",
  LOCAL_DIR: "本地目录",
  FILESYS: "文件服务",
};

const targetTypeOptions = Object.values(GetTemplateName2TargetType).map(
  (value) => ({
    label: TARGET_TYPE_LABELS[value] ?? value,
    value,
  }),
);

const defaultQuery = (): QueryState => ({
  targetType: "",
  targetCode: "",
  enabled: "",
  limit: 100,
});

const defaultForm = (): TargetFormState => ({
  targetCode: "",
  targetName: "",
  targetType: GetTemplateName2TargetType.EMAIL,
  enabled: true,
  targetPathTemplate: "",
  connectionConfigText: "{}",
  targetMetaText: "{}",
});

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
  TARGET_TEMPLATE_BASE_KEYS.forEach((key) => {
    delete next[key];
  });
  return next;
};

export const useTransferPage = (): { page: TargetPage } => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TransferTargetViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferTargetViewDTO | null>(null);
  const detailVisible = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const formState = reactive<TargetFormState>(defaultForm());
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
    asyncTargetCodeValidator: async (value: string | undefined) => {
      const nextValue = String(value ?? "").trim();
      if (!nextValue) {
        return undefined;
      }

      const result = await api.listTargets({
        targetCode: nextValue,
        limit: 100,
      });
      const duplicated = unwrapMultiResult(result).some(
        (item) =>
          item.targetCode === nextValue && item.targetId !== formState.targetId,
      );

      if (duplicated) {
        return "目标编码已存在";
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
  let templateRequestId = 0;

  const resetTemplateState = () => {
    templateNamePreview.value = "";
    templateDescription.value = "";
    templateVersion.value = "";
    templateMode.value = 1;
    templateReadPretty.value = false;
    templateSchema.value = null;
    templateInitialValues.value = {};
    templateValues.value = {};
    templateDetailOptions.value = { bordered: true, maxColumns: 2 };
    templateGridDefaults.value = {
      maxColumns: 2,
      minColumns: 1,
      minWidth: 260,
      columnGap: 16,
      rowGap: 0,
    };
  };

  const hydrateTemplateValuesFromRow = (row?: TransferTargetViewDTO) => {
    templateValues.value = {
      ...templateInitialValues.value,
      ...extractTemplateConfigValues(templateValues.value),
      ...normalizeFormilyValues(row?.connectionConfig),
      ...normalizeFormilyValues(row?.targetMeta),
      targetCode: row?.targetCode ?? templateValues.value.targetCode ?? "",
      targetName: row?.targetName ?? templateValues.value.targetName ?? "",
      enabled: row?.enabled ?? templateValues.value.enabled ?? true,
      targetPathTemplate:
        row?.targetPathTemplate ??
        templateValues.value.targetPathTemplate ??
        "",
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
        "targetCode",
        "asyncTargetCodeValidator",
        "目标编码已存在",
      ) ?? null;
    templateInitialValues.value = initialValues;
    templateValues.value = { ...initialValues };
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

  const syncTemplateByTargetType = async (targetType: string) => {
    const requestId = ++templateRequestId;

    if (!targetType) {
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
          targetType: targetType as GetTemplateName2Params["targetType"],
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
      console.error("加载目标模板失败:", error);
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
  const templateBoundCount = computed(
    () => rows.value.filter((row) => Boolean(row.formTemplateName)).length,
  );
  const targetTypeCount = computed(
    () => new Set(rows.value.map((row) => row.targetType).filter(Boolean)).size,
  );

  const mapQuery = (): ListTargetsParams => ({
    targetType: query.targetType || undefined,
    targetCode: query.targetCode || undefined,
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
      const res = await api.listTargets(mapQuery());
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
      console.error("加载目标列表失败:", error);
      message.error("加载目标列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const fillForm = (row: TransferTargetViewDTO) => {
    formState.targetId = row.targetId;
    formState.targetCode = row.targetCode || "";
    formState.targetName = row.targetName || "";
    formState.targetType = row.targetType || GetTemplateName2TargetType.EMAIL;
    formState.enabled = row.enabled ?? true;
    formState.targetPathTemplate = row.targetPathTemplate || "";
    formState.connectionConfigText = stringifyJson(row.connectionConfig);
    formState.targetMetaText = stringifyJson(row.targetMeta);
  };

  const resetForm = () => {
    Object.assign(formState, defaultForm());
    templateNamePreview.value = "";
  };

  const openCreateDialog = () => {
    formMode.value = "create";
    resetForm();
    formVisible.value = true;
    void syncTemplateByTargetType(formState.targetType);
  };

  const openEditDialog = async (row: TransferTargetViewDTO) => {
    formMode.value = "edit";
    try {
      const detail = row.targetId
        ? unwrapSingleResult(await api.getTarget(row.targetId))
        : row;
      if (detail) {
        fillForm(detail);
      }
      formVisible.value = true;
      await syncTemplateByTargetType(formState.targetType);
      if (detail) {
        hydrateTemplateValuesFromRow(detail);
      }
    } catch (error) {
      console.error("加载目标详情失败:", error);
      message.error("加载目标详情失败");
    }
  };

  const openDetailDrawer = async (row: TransferTargetViewDTO) => {
    try {
      selectedRow.value = row.targetId
        ? unwrapSingleResult(await api.getTarget(row.targetId)) ?? null
        : row;
      detailVisible.value = true;
    } catch (error) {
      console.error("加载目标详情失败:", error);
      message.error("加载目标详情失败");
    }
  };

  const confirmDelete = (row: TransferTargetViewDTO) => {
    Modal.confirm({
      title: "删除目标",
      content: `确认删除目标「${row.targetName || row.targetCode || row.targetId}」吗？如果该目标已被分拣路由引用，将无法删除，需要先解除关联路由配置。`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: async () => {
        if (!row.targetId) {
          message.error("目标主键缺失，无法删除");
          return;
        }
        try {
          await api.deleteTarget(row.targetId);
          message.success("删除成功");
          await loadList();
        } catch (error) {
          console.error("删除目标失败:", error);
          if (error instanceof Error) {
            message.error(error.message);
            return;
          }
          message.error("删除目标失败");
        }
      },
    });
  };

  const buildPayload = (): TransferTargetUpsertCommand => {
    const templateConfig = extractTemplateConfigValues(templateValues.value);

    return {
      targetId: formState.targetId,
      targetCode: String(templateValues.value.targetCode ?? "").trim(),
      targetName: String(templateValues.value.targetName ?? "").trim(),
      targetType: String(formState.targetType ?? "").trim(),
      enabled: Boolean(templateValues.value.enabled),
      targetPathTemplate:
        String(templateValues.value.targetPathTemplate ?? "").trim() ||
        undefined,
      connectionConfig:
        Object.keys(templateConfig).length > 0 ? templateConfig : undefined,
      targetMeta: undefined,
    };
  };

  const buildPayloadFromRow = (
    row: TransferTargetViewDTO,
    enabled: boolean,
  ): TransferTargetUpsertCommand => ({
    targetId: row.targetId,
    targetCode: String(row.targetCode ?? "").trim(),
    targetName: String(row.targetName ?? "").trim(),
    targetType: String(row.targetType ?? "").trim(),
    enabled,
    targetPathTemplate:
      String(row.targetPathTemplate ?? "").trim() || undefined,
    connectionConfig: row.connectionConfig,
    targetMeta: row.targetMeta,
  });

  const isEnabledUpdating = (targetId?: string) => {
    if (!targetId) {
      return false;
    }
    return Boolean(enabledUpdatingIds[targetId]);
  };

  const toggleEnabled = async (
    row: TransferTargetViewDTO,
    checked: boolean,
  ) => {
    if (!row.targetId) {
      message.error("目标主键缺失，无法切换状态");
      return;
    }

    const previousEnabled = Boolean(row.enabled);
    if (previousEnabled === checked) {
      return;
    }

    enabledUpdatingIds[row.targetId] = true;
    row.enabled = checked;

    try {
      await api.updateTarget(row.targetId, buildPayloadFromRow(row, checked));
      message.success(checked ? "启用成功" : "停用成功");
      await loadList();
    } catch (error) {
      row.enabled = previousEnabled;
      console.error("切换目标启用状态失败:", error);
      message.error("切换目标启用状态失败");
    } finally {
      delete enabledUpdatingIds[row.targetId];
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
      console.error("目标模板校验失败:", error);
      message.error("请先完善目标模板中的必填项");
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

      const payload = buildPayload();
      if (formMode.value === "create") {
        await api.createTarget(payload);
        message.success("新建目标成功");
      } else if (formState.targetId) {
        await api.updateTarget(formState.targetId, payload);
        message.success("更新目标成功");
      } else {
        message.error("目标主键缺失，无法更新");
        return;
      }
      formVisible.value = false;
      await loadList();
    } catch (error) {
      console.error("保存目标失败:", error);
      message.error(error instanceof Error ? error.message : "保存目标失败");
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
    () => formState.targetType,
    async (value) => {
      if (!formVisible.value || syncingTemplateType.value) {
        return;
      }
      await syncTemplateByTargetType(String(value ?? ""));
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
    targetTypeCount,
    query,
    targetTypeOptions,
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
    toggleEnabled,
    formVisible,
    formMode,
    formState,
    selectedRow,
    detailVisible,
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
    },
    formatEnabled,
  }) as TargetPage;

  return {
    page,
  };
};
