import { computed, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import type { ISchema, YTablePagination } from "@yss-ui/components";
import { getJavaSpringBootQuartzApi } from "@/api/generated/valset";
import { unwrapSingleResult } from "@/utils/api-response";
import {
  normalizeFormilySchema,
  normalizeFormilyValues,
} from "@/utils/formily";
import type {
  PageResultTransferTagViewDTO,
  TransferFormTemplateViewDTO,
  TransferTagUpsertCommand,
} from "@/api/generated/valset/schemas";
import type { TagPage, TagTestResultDTO, TagViewDTO } from "../types";

type QueryState = {
  tagCode: string;
  enabled: string;
  matchStrategy: string;
};

type TagTestState = TagPage["testState"];

const TEMPLATE_NAME = "transfer_tag";
const DEFAULT_SCRIPT_BODY = `String source = filePath != null && filePath.trim() != "" ? filePath : path;
if (source == null || source.trim() == "") {
  return false;
}
if (!(isExcelFile(source) || isCsvFile(source))) {
  return false;
}
return fn.isValuationTableByMeta(source, tagMeta);
`;
const api = getJavaSpringBootQuartzApi();
const defaultQuery = (): QueryState => ({
  tagCode: "",
  enabled: "",
  matchStrategy: "",
});

const defaultTestState = (): TagTestState => ({
  sourceType: "",
  sourceCode: "",
  fileName: "",
  mimeType: "",
  fileSize: "",
  sender: "",
  subject: "",
  path: "",
  mailFolder: "",
  body: "",
  attributesText: "{}",
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

const buildTemplateValues = (
  initialValues: Record<string, any>,
  row?: TagViewDTO,
) => ({
  ...initialValues,
  tagId: row?.tagId,
  tagCode: row?.tagCode ?? "",
  tagName: row?.tagName ?? "",
  tagValue: row?.tagValue ?? "",
  enabled: row?.enabled ?? true,
  priority: row?.priority ?? 10,
  matchStrategy: row?.matchStrategy ?? "SCRIPT_AND_REGEX",
  scriptLanguage: row?.scriptLanguage ?? "qlexpress4",
  scriptBody: row?.scriptBody ?? "",
  regexPattern: row?.regexPattern ?? "",
  tagMeta: stringifyJsonValue(row?.tagMeta),
});

const buildPayloadFromTemplate = (
  templateValues: Record<string, any>,
  tagId?: string,
): TransferTagUpsertCommand => ({
  tagId,
  tagCode: String(templateValues.tagCode ?? "").trim(),
  tagName: String(templateValues.tagName ?? "").trim(),
  tagValue: String(templateValues.tagValue ?? "").trim(),
  enabled:
    templateValues.enabled === undefined
      ? true
      : Boolean(templateValues.enabled),
  priority:
    templateValues.priority === undefined || templateValues.priority === null
      ? undefined
      : Number(templateValues.priority),
  matchStrategy:
    String(templateValues.matchStrategy ?? "").trim() || "SCRIPT_AND_REGEX",
  scriptLanguage:
    String(templateValues.scriptLanguage ?? "").trim() || "qlexpress4",
  scriptBody: String(templateValues.scriptBody ?? "").trim() || undefined,
  regexPattern: String(templateValues.regexPattern ?? "").trim() || undefined,
  tagMeta: parseJsonText(String(templateValues.tagMeta ?? ""), {}),
});

const buildPayloadFromRow = (
  row: TagViewDTO,
  enabled: boolean,
): TransferTagUpsertCommand => ({
  tagId: row.tagId,
  tagCode: String(row.tagCode ?? "").trim(),
  tagName: String(row.tagName ?? "").trim(),
  tagValue: String(row.tagValue ?? "").trim(),
  enabled,
  priority:
    row.priority === undefined || row.priority === null
      ? undefined
      : Number(row.priority),
  matchStrategy: String(row.matchStrategy ?? "").trim() || "SCRIPT_AND_REGEX",
  scriptLanguage: String(row.scriptLanguage ?? "").trim() || "qlexpress4",
  scriptBody: String(row.scriptBody ?? "").trim() || undefined,
  regexPattern: String(row.regexPattern ?? "").trim() || undefined,
  tagMeta: parseJsonText(stringifyJsonValue(row.tagMeta), {}),
});

const buildTestPayload = (state: TagTestState) => ({
  sourceType: state.sourceType || undefined,
  sourceCode: state.sourceCode || undefined,
  fileName: state.fileName || undefined,
  mimeType: state.mimeType || undefined,
  fileSize: state.fileSize ? Number(state.fileSize) : undefined,
  sender: state.sender || undefined,
  subject: state.subject || undefined,
  path: state.path || undefined,
  mailFolder: state.mailFolder || undefined,
  body: state.body || undefined,
  attributes: parseJsonText(state.attributesText, {}),
});

export const useTransferPage = (): { page: TagPage } => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TagViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TagViewDTO | null>(null);
  const detailVisible = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const editingTagId = ref<string | undefined>(undefined);
  const templateNamePreview = ref(TEMPLATE_NAME);
  const templateDescription = ref("");
  const templateVersion = ref("");
  const templateMode = ref<0 | 1 | 2>(1);
  const templateReadPretty = ref(false);
  const templateSchema = ref<ISchema | null>(null);
  const templateInitialValues = ref<Record<string, any>>({});
  const templateValues = ref<Record<string, any>>({});
  const templateScope = ref<Record<string, any>>({});
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
  const testVisible = ref(false);
  const testSubmitting = ref(false);
  const testTag = ref<TagViewDTO | null>(null);
  const testState = reactive<TagTestState>(defaultTestState());
  const testResult = ref<TagTestResultDTO | null>(null);
  const enabledUpdatingIds = reactive<Record<string, boolean>>({});
  let listRequestId = 0;
  let templateRequestId = 0;

  const resetTemplateState = () => {
    templateNamePreview.value = TEMPLATE_NAME;
    templateDescription.value = "";
    templateVersion.value = "";
    templateMode.value = 1;
    templateReadPretty.value = false;
    templateSchema.value = null;
    templateInitialValues.value = {};
    templateValues.value = {};
    templateScope.value = {};
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

  const applyTemplate = (template: TransferFormTemplateViewDTO) => {
    const formDefinition = template.formDefinition;
    const initialValues = normalizeFormilyValues(
      formDefinition?.initialValues ?? template.initialValues,
    );
    templateNamePreview.value = template.name ?? TEMPLATE_NAME;
    templateDescription.value = template.description ?? "";
    templateVersion.value = template.version ?? "";
    templateMode.value = (formDefinition?.mode as 0 | 1 | 2 | undefined) ?? 1;
    templateReadPretty.value = formDefinition?.readPretty ?? false;
    templateSchema.value =
      normalizeFormilySchema(formDefinition?.schema) ?? null;
    templateInitialValues.value = {
      ...initialValues,
      tagMeta: stringifyJsonValue(initialValues.tagMeta),
    };
    templateValues.value = { ...templateInitialValues.value };
    templateDetailOptions.value = {
      bordered: true,
      ...(normalizeFormilyValues(formDefinition?.detailOptions) || {}),
    };
    templateGridDefaults.value = {
      maxColumns: 2,
      minColumns: 1,
      minWidth: 260,
      columnGap: 16,
      rowGap: 0,
      ...(normalizeFormilyValues(formDefinition?.gridDefaults) || {}),
    };
  };

  const loadTemplate = async () => {
    const requestId = ++templateRequestId;
    templateLoading.value = true;
    try {
      const name =
        unwrapSingleResult(await api.getTemplateName3()) || TEMPLATE_NAME;
      const template = unwrapSingleResult(await api.getTemplate(name));
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
      console.error("加载标签模板失败:", error);
    } finally {
      if (requestId === templateRequestId) {
        templateLoading.value = false;
      }
    }
  };

  const pageSize = computed(() => pagination.value.pageSize || 10);
  const tableData = computed(() => rows.value);

  const formatEnabled = (value: boolean | undefined) =>
    value ? "启用" : "停用";
  const formatMatchStrategy = (value?: string) => {
    const normalized = String(value ?? "")
      .trim()
      .toUpperCase();
    if (!normalized) return "-";
    const map: Record<string, string> = {
      SCRIPT_RULE: "脚本规则",
      REGEX_RULE: "正则规则",
      SCRIPT_AND_REGEX: "脚本且正则",
      SCRIPT_OR_REGEX: "脚本或正则",
    };
    return map[normalized] || normalized;
  };
  const resolveScriptEditorLanguage = () => "javascript";

  const resetScriptBody = () => {
    templateValues.value = {
      ...templateValues.value,
      scriptBody:
        String(templateInitialValues.value.scriptBody ?? "").trim() ||
        DEFAULT_SCRIPT_BODY,
    };
  };

  const mapQuery = (
    pageIndex = 0,
    pageSizeValue = pagination.value.pageSize || 10,
  ) => ({
    tagCode: query.tagCode || undefined,
    matchStrategy: query.matchStrategy || undefined,
    enabled:
      query.enabled === "true"
        ? true
        : query.enabled === "false"
          ? false
          : undefined,
    pageIndex,
    pageSize: pageSizeValue,
  });

  const loadRows = async (
    pageIndex = pagination.value.current
      ? Math.max(pagination.value.current - 1, 0)
      : 0,
    pageSizeValue = pagination.value.pageSize || 10,
  ) => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const result = (await api.pageTags(
        mapQuery(pageIndex, pageSizeValue),
      )) as PageResultTransferTagViewDTO | undefined;
      if (requestId !== listRequestId) {
        return;
      }
      const records = result?.data ?? [];
      rows.value = records;
      total.value = Number(result?.totalCount ?? records.length);
      pagination.value.total = total.value;
      // 从0开始索引
      pagination.value.current = Number(
        result?.pageIndex ?? Math.max(pageIndex - 1, 0),
      );
      pagination.value.pageSize = Number(result?.pageSize ?? pageSizeValue);
    } catch (error) {
      if (requestId === listRequestId) {
        rows.value = [];
        total.value = 0;
        pagination.value.total = 0;
      }
      console.error("加载标签列表失败:", error);
      message.error("加载标签列表失败");
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const runQuery = async () => {
    await loadRows(0, pagination.value.pageSize || 10);
  };

  const resetQuery = async () => {
    Object.assign(query, defaultQuery());
    await loadRows(0, pagination.value.pageSize || 10);
  };

  const openCreateDialog = () => {
    formMode.value = "create";
    editingTagId.value = undefined;
    templateValues.value = buildTemplateValues(templateInitialValues.value);
    formVisible.value = true;
    void loadTemplate();
  };

  const openEditDialog = async (row: TagViewDTO) => {
    formMode.value = "edit";
    editingTagId.value = row.tagId;
    formVisible.value = true;
    await loadTemplate();
    templateValues.value = buildTemplateValues(
      templateInitialValues.value,
      row,
    );
  };

  const openDetailDrawer = (row: TagViewDTO) => {
    selectedRow.value = row;
    detailVisible.value = true;
  };

  const openTestDrawer = (row: TagViewDTO) => {
    testTag.value = row;
    Object.assign(testState, defaultTestState());
    testResult.value = null;
    testVisible.value = true;
  };

  const closeForm = () => {
    formVisible.value = false;
  };

  const closeDetail = () => {
    detailVisible.value = false;
  };

  const closeTest = () => {
    testVisible.value = false;
  };

  const submitForm = async () => {
    formSubmitting.value = true;
    try {
      await templateFormRef.value?.submit();
      const tagId = editingTagId.value;
      if (formMode.value === "edit" && !tagId) {
        throw new Error("标签ID不能为空");
      }
      const payload = buildPayloadFromTemplate(templateValues.value, tagId);
      const response =
        formMode.value === "create"
          ? await api.createTag(payload)
          : await api.updateTag(tagId as string, payload);
      const result = unwrapSingleResult(response);
      message.success(result?.message || "标签保存成功");
      formVisible.value = false;
      await loadRows();
    } catch (error) {
      console.error("保存标签失败:", error);
      message.error(error instanceof Error ? error.message : "保存标签失败");
    } finally {
      formSubmitting.value = false;
    }
  };

  const submitTest = async () => {
    if (!testTag.value?.tagId) {
      message.warning("请先选择标签");
      return;
    }
    testSubmitting.value = true;
    try {
      const payload = buildTestPayload(testState);
      const response = await api.testTag(testTag.value.tagId, payload);
      testResult.value = unwrapSingleResult(response) || null;
      message.success(testResult.value?.matched ? "标签命中" : "标签未命中");
    } catch (error) {
      console.error("试跑标签失败:", error);
      message.error(error instanceof Error ? error.message : "试跑标签失败");
    } finally {
      testSubmitting.value = false;
    }
  };

  const confirmDelete = (row: TagViewDTO) => {
    Modal.confirm({
      title: "删除标签",
      content: `确认删除标签「${row.tagName || row.tagCode || "未命名"}」吗？`,
      okText: "删除",
      okType: "danger",
      cancelText: "取消",
      onOk: async () => {
        try {
          const response = await api.deleteTag(row.tagId as string);
          const result = unwrapSingleResult(response);
          message.success(result?.message || "标签删除成功");
          await loadRows();
        } catch (error) {
          console.error("删除标签失败:", error);
          message.error(
            error instanceof Error ? error.message : "删除标签失败",
          );
        }
      },
    });
  };

  const isEnabledUpdating = (tagId?: string) => {
    if (!tagId) {
      return false;
    }
    return Boolean(enabledUpdatingIds[tagId]);
  };

  const toggleEnabled = async (row: TagViewDTO, checked: boolean) => {
    if (!row.tagId) {
      message.error("标签主键缺失，无法切换状态");
      return;
    }

    const previousEnabled = Boolean(row.enabled);
    if (previousEnabled === checked) {
      return;
    }

    enabledUpdatingIds[row.tagId] = true;
    row.enabled = checked;

    try {
      const payload = buildPayloadFromRow(row, checked);
      const response = checked
        ? await api.updateTag(row.tagId, payload)
        : await api.updateTag(row.tagId, payload);
      const result = unwrapSingleResult(response);
      message.success(result?.message || (checked ? "启用成功" : "停用成功"));
      await loadRows(
        pagination.value.current ? pagination.value.current - 1 : 0,
        pagination.value.pageSize || 10,
      );
    } catch (error) {
      row.enabled = previousEnabled;
      console.error("切换标签启用状态失败:", error);
      message.error(
        error instanceof Error ? error.message : "切换标签启用状态失败",
      );
    } finally {
      delete enabledUpdatingIds[row.tagId];
    }
  };

  const handlePageChange = (params: { current: number; pageSize: number }) => {
    pagination.value.current = params.current;
    pagination.value.pageSize = params.pageSize;
    void loadRows(Math.max(params.current - 1, 0), params.pageSize);
  };

  watch(
    () => pagination.value.current,
    () => {
      pagination.value.total = total.value;
    },
  );

  void loadRows();
  void loadTemplate();

  const page = reactive({
    loading: listLoading,
    rows,
    tableData,
    total,
    pageSize,
    pagination,
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
    templateLoading,
    enabledUpdatingIds,
    isEnabledUpdating,
    toggleEnabled,
    templateDetailOptions,
    templateGridDefaults,
    templateFormRef,
    setTemplateFormRef: (instance: any) => {
      templateFormRef.value = instance;
    },
    formVisible,
    formMode,
    formSubmitting,
    detailVisible,
    selectedRow,
    testVisible,
    testSubmitting,
    testState,
    testResult,
    testTag,
    openCreateDialog,
    openEditDialog,
    openDetailDrawer,
    openTestDrawer,
    confirmDelete,
    runQuery,
    resetQuery,
    submitForm,
    closeForm,
    closeDetail,
    submitTest,
    closeTest,
    formatEnabled,
    formatMatchStrategy,
    resolveScriptEditorLanguage,
    resetScriptBody,
    handlePageChange,
  }) as TagPage;

  return {
    page,
  };
};
