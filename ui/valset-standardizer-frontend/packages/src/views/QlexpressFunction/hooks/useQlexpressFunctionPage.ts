import { computed, reactive, ref } from "vue";
import { message, Modal } from "ant-design-vue";
import type { ISchema, YTablePagination } from "@yss-ui/components";
import {
  createQlexpressFunction,
  debugQlexpressFunction,
  deleteQlexpressFunction,
  disableQlexpressFunction,
  enableQlexpressFunction,
  getQlexpressFunction,
  getQlexpressFunctionUsage,
  pageQlexpressFunctions,
  updateQlexpressFunction,
  type QlexpressFunctionUpsertCommand,
  type QlexpressFunctionViewDTO,
} from "@/api/qlexpressFunction";
import type {
  QlexpressFunctionDebugState,
  QlexpressFunctionPageState,
  QlexpressFunctionQueryState,
} from "../types";

const defaultQuery = (): QlexpressFunctionQueryState => ({
  functionCnName: "",
  functionName: "",
  enabled: "",
});

const defaultScript = `function customContains(source, keyword) {
  if (source == null || keyword == null) {
    return false;
  }
  return source.indexOf(keyword) >= 0;
}`;

const defaultFormValues = (): Record<string, any> => ({
  functionCnName: "",
  functionName: "",
  remark: "",
  scriptBody: defaultScript,
  enabled: false,
  extInfo: "{}",
});

const defaultDebugState = (): QlexpressFunctionDebugState => ({
  debugExpression: "",
  contextText: "{}",
});

const parseJsonText = (value: string, fallback: Record<string, any> = {}) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return fallback;
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error("JSON 格式不合法");
  }
};

const stringifyJsonValue = (value: unknown) => {
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

const buildTemplateValues = (row?: QlexpressFunctionViewDTO) => ({
  functionId: row?.functionId,
  functionCnName: row?.functionCnName ?? "",
  functionName: row?.functionName ?? "",
  remark: row?.remark ?? "",
  scriptBody: row?.scriptBody ?? defaultScript,
  enabled: row?.enabled ?? false,
  extInfo: stringifyJsonValue(row?.extInfo),
});

const buildPayload = (
  values: Record<string, any>,
  functionId?: string,
): QlexpressFunctionUpsertCommand => ({
  functionId,
  functionCnName: String(values.functionCnName ?? "").trim(),
  functionName: String(values.functionName ?? "").trim(),
  remark: String(values.remark ?? "").trim() || undefined,
  scriptBody: String(values.scriptBody ?? "").trim(),
  enabled: Boolean(values.enabled),
  extInfo: parseJsonText(String(values.extInfo ?? ""), {}),
});

export const useQlexpressFunctionPage = (): QlexpressFunctionPageState => {
  const loading = ref(false);
  const rows = ref<QlexpressFunctionViewDTO[]>([]);
  const total = ref(0);
  const query = reactive<QlexpressFunctionQueryState>(defaultQuery());
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<QlexpressFunctionViewDTO | null>(null);
  const selectedUsage = ref(null as QlexpressFunctionPageState["selectedUsage"]);
  const usageLoading = ref(false);
  const detailVisible = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const formSubmitting = ref(false);
  const editingFunctionId = ref<string | undefined>(undefined);
  const templateValues = ref<Record<string, any>>(defaultFormValues());
  const templateInitialValues = ref<Record<string, any>>(defaultFormValues());
  const templateFormRef = ref<{ submit?: () => Promise<unknown> } | null>(null);
  const operatingIds = ref<Record<string, boolean>>({});
  const debugVisible = ref(false);
  const debugSubmitting = ref(false);
  const debugTarget = ref<QlexpressFunctionViewDTO | null>(null);
  const debugState = reactive<QlexpressFunctionDebugState>(defaultDebugState());
  const debugResult = ref(null as QlexpressFunctionPageState["debugResult"]);

  const templateSchema = computed<ISchema>(() => ({
    type: "object",
    properties: {
      layout: {
        type: "void",
        "x-component": "FormLayout",
        "x-component-props": { layout: "horizontal", labelWidth: 110 },
        properties: {
          grid: {
            type: "void",
            "x-component": "FormGrid",
            "x-component-props": {
              maxColumns: 2,
              minColumns: 1,
              minWidth: 260,
              columnGap: 16,
            },
            properties: {
              functionCnName: {
                type: "string",
                title: "函数中文名称",
                required: true,
                "x-decorator": "FormItem",
                "x-component": "Input",
                "x-component-props": { placeholder: "例如：文本包含" },
              },
              functionName: {
                type: "string",
                title: "函数名称",
                required: true,
                "x-decorator": "FormItem",
                "x-component": "Input",
                "x-component-props": { placeholder: "例如：customContains" },
              },
              enabled: {
                type: "boolean",
                title: "启用状态",
                "x-decorator": "FormItem",
                "x-component": "Switch",
                "x-component-props": {
                  checkedChildren: "启用",
                  unCheckedChildren: "停用",
                },
              },
              remark: {
                type: "string",
                title: "备注",
                "x-decorator": "FormItem",
                "x-component": "Input.TextArea",
                "x-component-props": { rows: 2, placeholder: "函数用途说明" },
                "x-decorator-props": { gridSpan: 2 },
              },
              scriptBody: {
                type: "string",
                title: "函数脚本",
                required: true,
                "x-decorator": "FormItem",
                "x-component": "Slot",
                "x-component-props": { name: "scriptBody" },
                "x-decorator-props": { gridSpan: 2 },
              },
              extInfo: {
                type: "string",
                title: "函数扩展信息",
                "x-decorator": "FormItem",
                "x-component": "Slot",
                "x-component-props": { name: "extInfo" },
                "x-decorator-props": { gridSpan: 2 },
              },
            },
          },
        },
      },
    },
  }));

  const formatEnabled = (value?: boolean) => (value ? "启用" : "停用");
  const isActive = (row?: QlexpressFunctionViewDTO | null) => Boolean(row?.enabled);
  const isOperating = (functionId?: string) =>
    Boolean(functionId && operatingIds.value[functionId]);

  const runQuery = async () => {
    loading.value = true;
    try {
      const res = await pageQlexpressFunctions({
        functionCnName: query.functionCnName || undefined,
        functionName: query.functionName || undefined,
        enabled:
          query.enabled === ""
            ? undefined
            : query.enabled === "true",
        pageIndex: (pagination.value.current || 1) - 1,
        pageSize: pagination.value.pageSize || 10,
      });
      rows.value = res?.data ?? [];
      total.value = Number(res?.totalCount ?? 0);
      pagination.value.total = total.value;
    } finally {
      loading.value = false;
    }
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    pagination.value.current = 1;
    runQuery();
  };

  const handlePageChange = (params: { current: number; pageSize: number }) => {
    pagination.value.current = params.current;
    pagination.value.pageSize = params.pageSize;
    runQuery();
  };

  const openCreateDialog = () => {
    formMode.value = "create";
    editingFunctionId.value = undefined;
    templateInitialValues.value = defaultFormValues();
    templateValues.value = defaultFormValues();
    formVisible.value = true;
  };

  const openEditDialog = async (row: QlexpressFunctionViewDTO) => {
    if (isActive(row)) {
      message.warning("已激活函数不可修改，请先停用");
      return;
    }
    const id = row.functionId;
    if (!id) {
      return;
    }
    const detail = await getQlexpressFunction(id);
    const values = buildTemplateValues(detail?.data ?? row);
    formMode.value = "edit";
    editingFunctionId.value = id;
    templateInitialValues.value = values;
    templateValues.value = { ...values };
    formVisible.value = true;
  };

  const closeForm = () => {
    formVisible.value = false;
  };

  const setTemplateFormRef = (instance: unknown) => {
    templateFormRef.value =
      instance && typeof instance === "object" && "submit" in instance
        ? (instance as { submit?: () => Promise<unknown> })
        : null;
  };

  const submitForm = async () => {
    formSubmitting.value = true;
    try {
      await templateFormRef.value?.submit?.();
      if (formMode.value === "edit" && !editingFunctionId.value) {
        throw new Error("函数ID不能为空");
      }
      const functionId = editingFunctionId.value;
      const payload = buildPayload(templateValues.value, functionId);
      if (formMode.value === "create") {
        await createQlexpressFunction(payload);
      } else {
        await updateQlexpressFunction(functionId as string, payload);
      }
      message.success("函数保存成功");
      formVisible.value = false;
      runQuery();
    } catch (error: any) {
      message.error(error?.message || "函数保存失败");
    } finally {
      formSubmitting.value = false;
    }
  };

  const loadUsage = async (row: QlexpressFunctionViewDTO) => {
    if (!row.functionId) {
      selectedUsage.value = null;
      return;
    }
    usageLoading.value = true;
    try {
      const res = await getQlexpressFunctionUsage(row.functionId);
      selectedUsage.value = res?.data ?? null;
    } catch (error: any) {
      selectedUsage.value = null;
      message.error(error?.message || "函数使用关系查询失败");
    } finally {
      usageLoading.value = false;
    }
  };

  const openDetailDrawer = (row: QlexpressFunctionViewDTO) => {
    selectedRow.value = row;
    detailVisible.value = true;
    loadUsage(row);
  };

  const openUsageDrawer = (row: QlexpressFunctionViewDTO) => {
    openDetailDrawer(row);
  };

  const closeDetail = () => {
    detailVisible.value = false;
    selectedUsage.value = null;
  };

  const setOperating = (functionId: string | undefined, value: boolean) => {
    if (!functionId) {
      return;
    }
    operatingIds.value = { ...operatingIds.value, [functionId]: value };
  };

  const toggleEnabled = async (
    row: QlexpressFunctionViewDTO,
    checked: boolean,
  ) => {
    const id = row.functionId;
    if (!id) {
      return;
    }
    setOperating(id, true);
    try {
      if (checked) {
        await enableQlexpressFunction(id);
      } else {
        await disableQlexpressFunction(id);
      }
      message.success(checked ? "函数启用成功" : "函数停用成功");
      runQuery();
    } catch (error: any) {
      message.error(error?.message || "启停函数失败");
    } finally {
      setOperating(id, false);
    }
  };

  const confirmDelete = (row: QlexpressFunctionViewDTO) => {
    if (isActive(row)) {
      message.warning("已激活函数不可删除，请先停用");
      return;
    }
    Modal.confirm({
      title: "确认删除函数",
      content: `确认删除函数 ${row.functionName || ""}？`,
      okText: "删除",
      okType: "danger",
      cancelText: "取消",
      onOk: async () => {
        if (!row.functionId) {
          return;
        }
        await deleteQlexpressFunction(row.functionId);
        message.success("函数删除成功");
        runQuery();
      },
    });
  };

  const openDebugDrawer = (row?: QlexpressFunctionViewDTO | null) => {
    debugTarget.value = row ?? null;
    debugResult.value = null;
    Object.assign(debugState, defaultDebugState(), {
      debugExpression: row?.functionName ? `${row.functionName}()` : "",
    });
    debugVisible.value = true;
  };

  const closeDebug = () => {
    debugVisible.value = false;
  };

  const submitDebug = async () => {
    debugSubmitting.value = true;
    try {
      const context = parseJsonText(debugState.contextText, {});
      const result = await debugQlexpressFunction({
        functionId: debugTarget.value?.functionId,
        functionName: debugTarget.value ? undefined : templateValues.value.functionName,
        scriptBody: debugTarget.value ? undefined : templateValues.value.scriptBody,
        debugExpression: debugState.debugExpression,
        context,
      });
      debugResult.value = result?.data ?? null;
    } catch (error: any) {
      message.error(error?.message || "函数调试失败");
    } finally {
      debugSubmitting.value = false;
    }
  };

  const resolveScriptEditorLanguage = () => "javascript";

  runQuery();

  return reactive({
    loading,
    rows,
    tableData: rows,
    total,
    pagination,
    query,
    selectedRow,
    selectedUsage,
    usageLoading,
    detailVisible,
    formVisible,
    formMode,
    formSubmitting,
    templateValues,
    templateSchema,
    templateInitialValues,
    templateMode: 1,
    templateReadPretty: false,
    templateScope: {},
    templateDetailOptions: { bordered: true, maxColumns: 2 },
    templateGridDefaults: { maxColumns: 2, minColumns: 1, minWidth: 260 },
    setTemplateFormRef,
    debugVisible,
    debugSubmitting,
    debugTarget,
    debugState,
    debugResult,
    operatingIds,
    isOperating,
    isActive,
    runQuery,
    resetQuery,
    handlePageChange,
    openCreateDialog,
    openEditDialog,
    closeForm,
    submitForm,
    openDetailDrawer,
    openUsageDrawer,
    closeDetail,
    confirmDelete,
    toggleEnabled,
    openDebugDrawer,
    closeDebug,
    submitDebug,
    formatEnabled,
    resolveScriptEditorLanguage,
  }) as QlexpressFunctionPageState;
};
