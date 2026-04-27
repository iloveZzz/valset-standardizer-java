import { computed, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import type { ISchema, YTablePagination } from "@yss-ui/components";
import type { Object as JsonObject } from "@/api/generated/valset/schemas/object";
import type {
  ListRulesParams,
  TransferFormTemplateViewDTO,
  TransferRuleUpsertCommand,
  TransferRuleViewDTO,
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
import type { RulePage } from "../types";

type QueryState = {
  ruleCode: string;
  enabled: string;
  limit: number;
};

type RuleFormState = {
  ruleId?: string;
  ruleCode: string;
  ruleName: string;
  ruleVersion: string;
  enabled: boolean;
  priority: number | undefined;
  matchStrategy: string;
  scriptLanguage: string;
  scriptBody: string;
};

const RULE_TEMPLATE_NAME = "transfer_rule";
const RULE_TEMPLATE_BASE_KEYS = [
  "ruleCode",
  "ruleName",
  "ruleVersion",
  "enabled",
  "priority",
  "matchStrategy",
  "scriptLanguage",
  "scriptBody",
  "effectiveFrom",
  "effectiveTo",
] as const;
const RULE_TEMPLATE_DEPRECATED_KEYS = [
  "groupStrategy",
  "groupField",
  "groupTargetMapping",
  "groupExpression",
  "regGroup",
] as const;

const api = getJavaSpringBootQuartzApi();

const defaultQuery = (): QueryState => ({
  ruleCode: "",
  enabled: "",
  limit: 100,
});

const defaultForm = (): RuleFormState => ({
  ruleCode: "",
  ruleName: "",
  ruleVersion: "1.0.0",
  enabled: true,
  priority: 10,
  matchStrategy: "SCRIPT_RULE",
  scriptLanguage: "qlexpress4",
  scriptBody: "",
});

const extractTemplateConfigValues = (values: Record<string, any>) => {
  const next = { ...values };
  RULE_TEMPLATE_BASE_KEYS.forEach((key) => {
    delete next[key];
  });
  RULE_TEMPLATE_DEPRECATED_KEYS.forEach((key) => {
    delete next[key];
  });
  return next;
};

const stripDeprecatedTemplateValues = (values: Record<string, any>) => {
  const next = { ...values };
  RULE_TEMPLATE_DEPRECATED_KEYS.forEach((key) => {
    delete next[key];
  });
  return next;
};

export const useTransferPage = (): { page: RulePage } => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TransferRuleViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferRuleViewDTO | null>(null);
  const detailVisible = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const formState = reactive<RuleFormState>(defaultForm());
  const templateNamePreview = ref(RULE_TEMPLATE_NAME);
  const templateDescription = ref("");
  const templateVersion = ref("");
  const templateMode = ref<0 | 1 | 2>(1);
  const templateReadPretty = ref(false);
  const templateSchema = ref<ISchema | null>(null);
  const templateInitialValues = ref<Record<string, any>>({});
  const templateValues = ref<Record<string, any>>({});
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
    templateNamePreview.value = RULE_TEMPLATE_NAME;
    templateDescription.value = "";
    templateVersion.value = "";
    templateMode.value = 1;
    templateReadPretty.value = false;
    templateSchema.value = null;
    templateInitialValues.value = {};
    templateValues.value = {};
    templateDetailOptions.value = {
      bordered: true,
      maxColumns: 2,
    };
    templateGridDefaults.value = {
      maxColumns: 2,
      minColumns: 1,
      minWidth: 260,
      columnGap: 16,
      rowGap: 0,
    };
  };

  const hydrateTemplateValuesFromRow = (row?: TransferRuleViewDTO) => {
    templateValues.value = {
      ...templateInitialValues.value,
      ...extractTemplateConfigValues(templateValues.value),
      ...stripDeprecatedTemplateValues(normalizeFormilyValues(row?.ruleMeta)),
      ruleCode: row?.ruleCode ?? templateValues.value.ruleCode ?? "",
      ruleName: row?.ruleName ?? templateValues.value.ruleName ?? "",
      ruleVersion:
        row?.ruleVersion ?? templateValues.value.ruleVersion ?? "1.0.0",
      enabled: row?.enabled ?? templateValues.value.enabled ?? true,
      priority: row?.priority ?? templateValues.value.priority ?? 10,
      matchStrategy:
        row?.matchStrategy ??
        templateValues.value.matchStrategy ??
        "SCRIPT_RULE",
      scriptLanguage:
        row?.scriptLanguage ??
        templateValues.value.scriptLanguage ??
        "qlexpress4",
      scriptBody: row?.scriptBody ?? templateValues.value.scriptBody ?? "",
      effectiveFrom:
        row?.effectiveFrom ?? templateValues.value.effectiveFrom ?? "",
      effectiveTo: row?.effectiveTo ?? templateValues.value.effectiveTo ?? "",
    };
  };

  const applyTemplate = (template: TransferFormTemplateViewDTO) => {
    const formDefinition = template.formDefinition;
    const initialValues = normalizeFormilyValues(
      formDefinition?.initialValues ?? template.initialValues,
    );
    const detailOptions = normalizeFormilyValues(formDefinition?.detailOptions);
    const gridDefaults = normalizeFormilyValues(formDefinition?.gridDefaults);

    templateNamePreview.value = template.name ?? RULE_TEMPLATE_NAME;
    templateDescription.value = template.description ?? "";
    templateVersion.value = template.version ?? "";
    templateMode.value = (formDefinition?.mode as 0 | 1 | 2 | undefined) ?? 1;
    templateReadPretty.value = formDefinition?.readPretty ?? false;
    templateSchema.value =
      injectFormilyAsyncValidator(
        normalizeFormilySchema(formDefinition?.schema),
        "ruleCode",
        "asyncRuleCodeValidator",
        "规则编码已存在",
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

  const loadTemplate = async () => {
    const requestId = ++templateRequestId;
    templateLoading.value = true;
    try {
      const template = unwrapSingleResult(
        await api.getTemplate(RULE_TEMPLATE_NAME),
      );
      if (requestId !== templateRequestId) {
        return;
      }
      if (!template) {
        resetTemplateState();
        return;
      }
      applyTemplate(template);
    } catch (error) {
      if (requestId === templateRequestId) {
        resetTemplateState();
      }
      console.error("加载规则模板失败:", error);
    } finally {
      if (requestId === templateRequestId) {
        templateLoading.value = false;
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
  const scriptBoundCount = computed(
    () => rows.value.filter((row) => Boolean(row.scriptBody)).length,
  );
  const strategyCount = computed(
    () =>
      new Set(rows.value.map((row) => row.matchStrategy).filter(Boolean)).size,
  );

  const templateScope = {
    asyncRuleCodeValidator: async (value: string | undefined) => {
      const nextValue = String(value ?? "").trim();
      if (!nextValue) {
        return undefined;
      }

      const result = await api.listRules({
        ruleCode: nextValue,
        limit: 100,
      });
      const duplicated = unwrapMultiResult(result).some(
        (item) =>
          item.ruleCode === nextValue && item.ruleId !== formState.ruleId,
      );

      if (duplicated) {
        return "规则编码已存在";
      }

      return undefined;
    },
  };

  const mapQuery = (): ListRulesParams => ({
    ruleCode: query.ruleCode || undefined,
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
      const res = await api.listRules(mapQuery());
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
      console.error("加载规则列表失败:", error);
      message.error("加载规则列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const fillForm = (row: TransferRuleViewDTO) => {
    formState.ruleId = row.ruleId;
    formState.ruleCode = row.ruleCode || "";
    formState.ruleName = row.ruleName || "";
    formState.ruleVersion = row.ruleVersion || "1.0.0";
    formState.enabled = row.enabled ?? true;
    formState.priority = row.priority ?? 10;
    formState.matchStrategy = row.matchStrategy || "SCRIPT_RULE";
    formState.scriptLanguage = row.scriptLanguage || "qlexpress4";
    formState.scriptBody = row.scriptBody || "";
  };

  const loadRuleDetail = async (row: TransferRuleViewDTO) => {
    if (!row.ruleId) {
      return row;
    }
    return unwrapSingleResult(await api.getRule(row.ruleId)) ?? row;
  };

  const resetForm = () => {
    Object.assign(formState, defaultForm());
  };

  const openCreateDialog = () => {
    formMode.value = "create";
    resetForm();
    resetTemplateState();
    void (async () => {
      await loadTemplate();
      formVisible.value = true;
    })();
  };

  const openEditDialog = (row: TransferRuleViewDTO) => {
    formMode.value = "edit";
    void (async () => {
      try {
        const detail = await loadRuleDetail(row);
        if (detail) {
          fillForm(detail);
        }
        resetTemplateState();
        await loadTemplate();
        if (detail) {
          hydrateTemplateValuesFromRow(detail);
        }
        formVisible.value = true;
      } catch (error) {
        console.error("加载规则详情失败:", error);
        message.error("加载规则详情失败");
      }
    })();
  };

  const openDetailDrawer = async (row: TransferRuleViewDTO) => {
    try {
      selectedRow.value = await loadRuleDetail(row);
      detailVisible.value = true;
    } catch (error) {
      console.error("加载规则详情失败:", error);
      message.error("加载规则详情失败");
    }
  };

  const confirmDelete = (row: TransferRuleViewDTO) => {
    Modal.confirm({
      title: "删除规则",
      content: `确认删除规则「${row.ruleName || row.ruleCode || row.ruleId}」吗？`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: async () => {
        if (!row.ruleId) {
          message.error("规则主键缺失，无法删除");
          return;
        }
        try {
          await api.deleteRule(row.ruleId);
          message.success("删除成功");
          await loadList();
        } catch (error) {
          console.error("删除规则失败:", error);
          message.error("删除规则失败");
        }
      },
    });
  };

  const buildPayload = (): TransferRuleUpsertCommand => {
    const normalizedTemplateValues = stripDeprecatedTemplateValues(
      templateValues.value,
    );
    const templateConfig = extractTemplateConfigValues(normalizedTemplateValues);
    return {
      ruleId: formState.ruleId,
      ruleCode: String(normalizedTemplateValues.ruleCode ?? "").trim(),
      ruleName: String(normalizedTemplateValues.ruleName ?? "").trim(),
      ruleVersion:
        String(normalizedTemplateValues.ruleVersion ?? "").trim() || undefined,
      enabled: Boolean(normalizedTemplateValues.enabled),
      priority:
        normalizedTemplateValues.priority === undefined ||
        normalizedTemplateValues.priority === null
          ? undefined
          : Number(normalizedTemplateValues.priority),
      matchStrategy:
        String(normalizedTemplateValues.matchStrategy ?? "").trim() || undefined,
      scriptLanguage:
        String(normalizedTemplateValues.scriptLanguage ?? "").trim() || undefined,
      effectiveFrom:
        String(normalizedTemplateValues.effectiveFrom ?? "").trim() || undefined,
      effectiveTo:
        String(normalizedTemplateValues.effectiveTo ?? "").trim() || undefined,
      scriptBody: String(normalizedTemplateValues.scriptBody ?? "").trim(),
      ruleMeta:
        Object.keys(templateConfig).length > 0
          ? (templateConfig as JsonObject)
          : undefined,
    };
  };

  const buildPayloadFromRow = (
    row: TransferRuleViewDTO,
    enabled: boolean,
  ): TransferRuleUpsertCommand => ({
    ruleId: row.ruleId,
    ruleCode: String(row.ruleCode ?? "").trim(),
    ruleName: String(row.ruleName ?? "").trim(),
    ruleVersion: String(row.ruleVersion ?? "").trim() || undefined,
    enabled,
    priority:
      row.priority === undefined || row.priority === null
        ? undefined
        : Number(row.priority),
    matchStrategy: String(row.matchStrategy ?? "").trim() || undefined,
    scriptLanguage: String(row.scriptLanguage ?? "").trim() || undefined,
    effectiveFrom: String(row.effectiveFrom ?? "").trim() || undefined,
    effectiveTo: String(row.effectiveTo ?? "").trim() || undefined,
    scriptBody: String(row.scriptBody ?? "").trim(),
    ruleMeta: stripDeprecatedTemplateValues(
      (row.ruleMeta ?? {}) as Record<string, any>,
    ),
  });

  const isEnabledUpdating = (ruleId?: number) => {
    if (!ruleId) {
      return false;
    }
    return Boolean(enabledUpdatingIds[ruleId]);
  };

  const toggleEnabled = async (row: TransferRuleViewDTO, checked: boolean) => {
    if (!row.ruleId) {
      message.error("规则主键缺失，无法切换状态");
      return;
    }

    const previousEnabled = Boolean(row.enabled);
    if (previousEnabled === checked) {
      return;
    }

    enabledUpdatingIds[row.ruleId] = true;
    row.enabled = checked;

    try {
      await api.updateRule(row.ruleId, buildPayloadFromRow(row, checked));
      message.success(checked ? "启用成功" : "停用成功");
      await loadList();
    } catch (error) {
      row.enabled = previousEnabled;
      console.error("切换规则启用状态失败:", error);
      message.error("切换规则启用状态失败");
    } finally {
      delete enabledUpdatingIds[row.ruleId];
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
      console.error("规则模板校验失败:", error);
      message.error("请先完善规则模板中的必填项");
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
        await api.createRule(payload);
        message.success("新建规则成功");
      } else if (formState.ruleId) {
        await api.updateRule(formState.ruleId, payload);
        message.success("更新规则成功");
      } else {
        message.error("规则主键缺失，无法更新");
        return;
      }
      formVisible.value = false;
      await loadList();
    } catch (error) {
      console.error("保存规则失败:", error);
      message.error(error instanceof Error ? error.message : "保存规则失败");
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

  const resolveScriptEditorLanguage = (value: string | undefined) => {
    const normalized = String(value ?? "").trim().toLowerCase();
    if (!normalized) {
      return "javascript";
    }

    if (
      normalized === "qlexpress4" ||
      normalized === "qlexpress" ||
      normalized === "javascript" ||
      normalized === "js"
    ) {
      return "javascript";
    }

    return normalized;
  };

  watch(
    () => formVisible.value,
    async (visible) => {
      if (visible && !templateSchema.value && !templateLoading.value) {
        await loadTemplate();
      }
    },
  );

  void loadList();
  void loadTemplate();

  const page = reactive({
    loading: listLoading,
    rows,
    tableData,
    total,
    pageSize,
    pagination,
    enabledCount,
    scriptBoundCount,
    strategyCount,
    query,
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
    resolveScriptEditorLanguage,
  }) as RulePage;

  return {
    page,
  };
};
