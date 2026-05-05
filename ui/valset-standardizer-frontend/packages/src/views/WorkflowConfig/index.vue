<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message, Modal } from "ant-design-vue";
import {
  YTable,
  YssFormily,
  type ISchema,
  type YTableColumn,
  type YTablePagination,
} from "@yss-ui/components";
import {
  disableWorkflowConfig,
  copyWorkflowConfigVersion,
  compareWorkflowConfigs,
  exportWorkflowConfig,
  getWorkflowConfig,
  pageWorkflowConfigs,
  rollbackWorkflowConfigVersion,
  publishWorkflowConfig,
  importWorkflowConfig,
  saveWorkflowConfigDraft,
  validateWorkflowConfig,
  type WorkflowDefinitionDTO,
  type WorkflowStageDTO,
  type WorkflowStatusMappingDTO,
  type WorkflowVersionDiffDTO,
} from "@/api/workflowConfig";
import "./index.less";

defineOptions({ name: "WorkflowConfigPage" });

const loading = ref(false);
const workflowcConfTbRef = ref<any>(null);
const mappingTableRef = ref<any>(null);
const detailLoading = ref(false);
const saving = ref(false);
const rows = ref<WorkflowDefinitionDTO[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const compareVisible = ref(false);
const rollbackVisible = ref(false);
const compareSourceRow = ref<WorkflowDefinitionDTO | null>(null);
const compareTargetWorkflowId = ref("");
const compareLoading = ref(false);
const compareResult = ref<WorkflowVersionDiffDTO | null>(null);
const rollbackRow = ref<WorkflowDefinitionDTO | null>(null);
const rollbackSourceWorkflowId = ref("");
const rollbackLoading = ref(false);
const pagination = ref<YTablePagination>({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
});
const query = reactive({
  workflowCode: "",
  workflowName: "",
  engineType: "",
  status: "",
  pageIndex: 1,
  pageSize: 20,
});

const form = reactive<WorkflowDefinitionDTO>({
  workflowCode: "VALUATION_PARSE",
  workflowName: "估值表解析工作流",
  businessType: "VALUATION",
  engineType: "INTERNAL",
  parseFallbackStage: "FILE_PARSE",
  workflowFallbackStage: "STANDARD_LANDING",
  versionNo: 1,
  description: "",
  stages: [],
  statusMappings: [],
  executorBindings: [],
  ignoredParseLifecycleStages: [],
  ignoredWorkflowTaskTypes: [],
});

const engineForm = reactive({
  engineType: "INTERNAL",
  externalRef: "DefaultTaskDispatcher",
  configJson: "{}",
});

const engineOptions = [
  { label: "内部任务", value: "INTERNAL" },
  { label: "XXL-JOB", value: "XXL_JOB" },
  { label: "LiteFlow", value: "LITEFLOW" },
  { label: "DolphinScheduler", value: "DOLPHIN_SCHEDULER" },
];

const statusOptions = [
  { label: "草稿", value: "DRAFT" },
  { label: "已发布", value: "PUBLISHED" },
  { label: "已停用", value: "DISABLED" },
];

const businessTypeOptions = [
  { label: "估值业务", value: "VALUATION" },
];

const smallFieldProps = {
  size: "small",
};

const columns: YTableColumn[] = [
  { type: "seq", title: "序号", width: 70, align: "center" },
  { field: "workflowCode", title: "工作流编码", minWidth: 180 },
  { field: "workflowName", title: "工作流名称", minWidth: 200 },
  {
    field: "businessType",
    title: "业务类型",
    width: 140,
    formatter: ({ cellValue }: any) =>
      resolveOptionLabel(businessTypeOptions, cellValue),
  },
  {
    field: "engineType",
    title: "执行平台",
    width: 150,
    formatter: ({ cellValue }: any) =>
      resolveOptionLabel(engineOptions, cellValue),
  },
  { field: "versionNo", title: "版本", width: 90, align: "center" },
  {
    field: "status",
    title: "状态",
    width: 120,
    align: "center",
    formatter: ({ cellValue }: any) =>
      resolveOptionLabel(statusOptions, cellValue) || cellValue || "",
  },
  { field: "updatedAt", title: "更新时间", width: 190 },
  { field: "action", title: "操作", width: 420, fixed: "right" as const },
];

const diffColumns: YTableColumn[] = [
  { field: "path", title: "字段", minWidth: 240 },
  { field: "leftValue", title: "左侧版本", minWidth: 240 },
  { field: "rightValue", title: "右侧版本", minWidth: 240 },
  { field: "changeType", title: "类型", width: 120 },
];

const sourceTypeOptions = [
  { label: "工作流任务", value: "WORKFLOW_TASK" },
  { label: "解析生命周期", value: "PARSE_LIFECYCLE" },
];

const targetStatusOptions = [
  { label: "待处理", value: "PENDING" },
  { label: "处理中", value: "RUNNING" },
  { label: "已完成", value: "SUCCESS" },
  { label: "失败", value: "FAILED" },
  { label: "已停止", value: "STOPPED" },
  { label: "阻塞", value: "BLOCKED" },
];

const resolveOptionLabel = (
  options: { label: string; value: string }[],
  value?: string,
) => options.find((item) => item.value === value)?.label || value || "";

const formatArrayCell = ({ cellValue }: any) =>
  Array.isArray(cellValue) ? cellValue.join(",") : cellValue || "";

const stageTableColumns: YTableColumn[] = [
  { type: "seq", title: "序号", width: 60, align: "center" },
  {
    field: "stageCode",
    title: "阶段编码",
    width: 170,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "stageName",
    title: "阶段名称",
    width: 150,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "stepCode",
    title: "步骤编码",
    width: 170,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "stepName",
    title: "步骤名称",
    width: 150,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "sortOrder",
    title: "排序",
    width: 90,
    align: "center",
    editRender: {
      name: "VxeNumberInput",
      props: { size: "small", type: "integer", min: 1 },
    },
  },
  {
    field: "retryable",
    title: "可重跑",
    width: 100,
    align: "center",
    formatter: ({ cellValue }: any) => (cellValue ? "是" : "否"),
    editRender: { name: "VxeSwitch", props: { size: "small" } },
  },
  {
    field: "skippable",
    title: "可跳过",
    width: 100,
    align: "center",
    formatter: ({ cellValue }: any) => (cellValue ? "是" : "否"),
    editRender: { name: "VxeSwitch", props: { size: "small" } },
  },
  {
    field: "enabled",
    title: "启用",
    width: 90,
    align: "center",
    formatter: ({ cellValue }: any) => (cellValue ? "启用" : "停用"),
    editRender: { name: "VxeSwitch", props: { size: "small" } },
  },
  {
    field: "taskTypes",
    title: "任务类型",
    minWidth: 220,
    formatter: formatArrayCell,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "taskStages",
    title: "任务阶段",
    minWidth: 200,
    formatter: formatArrayCell,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "parseLifecycleStages",
    title: "解析生命周期",
    minWidth: 240,
    formatter: formatArrayCell,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "stageDescription",
    title: "阶段说明",
    minWidth: 260,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  { field: "action", title: "操作", width: 90, fixed: "right" as const },
];

const mappingTableColumns: YTableColumn[] = [
  { type: "seq", title: "序号", width: 60, align: "center" },
  {
    field: "sourceType",
    title: "来源",
    width: 160,
    formatter: ({ cellValue }: any) =>
      resolveOptionLabel(sourceTypeOptions, cellValue),
    editRender: {
      name: "VxeSelect",
      options: sourceTypeOptions,
      props: { size: "small" },
    },
  },
  {
    field: "sourceStatus",
    title: "来源状态",
    minWidth: 220,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  {
    field: "targetStatus",
    title: "页面状态",
    width: 150,
    formatter: ({ cellValue }: any) =>
      resolveOptionLabel(targetStatusOptions, cellValue),
    editRender: {
      name: "VxeSelect",
      options: targetStatusOptions,
      props: { size: "small" },
    },
  },
  {
    field: "statusLabel",
    title: "显示名称",
    width: 160,
    editRender: { name: "VxeInput", props: { size: "small" } },
  },
  { field: "action", title: "操作", width: 90, fixed: "right" as const },
];

const stageOptions = computed(() =>
  (form.stages || []).map((item) => ({
    label: item.stageName || item.stageCode || "",
    value: item.stageCode || "",
  })),
);

const baseFormSchema = computed<ISchema>(() => ({
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": {
        layout: "horizontal",
        labelWidth: 128,
        maxColumns: 2,
        minColumns: 1,
      },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          properties: {
            workflowCode: {
              type: "string",
              title: "工作流编码",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-decorator-props": {
                gridSpan: 1,
              },
              "x-component-props": smallFieldProps,
            },
            workflowName: {
              type: "string",
              title: "工作流名称",
              "x-decorator-props": {
                gridSpan: 1,
              },
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": smallFieldProps,
            },
            businessType: {
              type: "string",
              title: "业务类型",
              "x-decorator-props": {
                gridSpan: 1,
              },
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": smallFieldProps,
            },
            engineType: {
              type: "string",
              title: "执行平台",
              enum: engineOptions,
              "x-decorator-props": {
                gridSpan: 1,
              },
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": smallFieldProps,
            },
            versionNo: {
              type: "number",
              title: "版本号",
              "x-decorator-props": {
                gridSpan: 1,
              },
              "x-decorator": "FormItem",
              "x-component": "NumberPicker",
              "x-component-props": {
                ...smallFieldProps,
                min: 1,
              },
            },
            parseFallbackStage: {
              type: "string",
              title: "解析事件默认阶段",
              "x-decorator-props": {
                gridSpan: 1,
              },
              enum: stageOptions.value,
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": smallFieldProps,
            },
            workflowFallbackStage: {
              type: "string",
              title: "工作流任务默认阶段",
              "x-decorator-props": {
                gridSpan: 1,
              },
              enum: stageOptions.value,
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": smallFieldProps,
            },
            description: {
              type: "string",
              title: "说明",
              "x-decorator": "FormItem",
              "x-component": "Slot",
              "x-component-props": {
                name: "description",
              },
              "x-grid-span": 2,
            },
          },
        },
      },
    },
  },
}));

const engineFormSchema = computed<ISchema>(() => ({
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": {
        layout: "vertical",
      },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          "x-component-props": {
            maxColumns: 2,
          },
          properties: {
            engineType: {
              type: "string",
              title: "平台类型",
              enum: engineOptions,
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": smallFieldProps,
            },
            externalRef: {
              type: "string",
              title: "外部任务标识",
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": {
                ...smallFieldProps,
                placeholder: "jobId / chainId / processDefinitionCode",
              },
            },
            configJson: {
              type: "string",
              title: "平台扩展参数 JSON",
              "x-decorator": "FormItem",
              "x-component": "Slot",
              "x-component-props": {
                name: "configJson",
              },
              "x-grid-span": 2,
            },
          },
        },
      },
    },
  },
}));

const stageFormSchema = computed<ISchema>(() => ({
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": {
        layout: "vertical",
      },
      properties: {
        stages: {
          type: "void",
          title: "阶段编排",
          "x-decorator": "FormItem",
          "x-component": "Slot",
          "x-component-props": {
            name: "stageTable",
          },
        },
      },
    },
  },
}));

const mappingFormSchema = computed<ISchema>(() => ({
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": {
        layout: "vertical",
      },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          "x-component-props": {
            maxColumns: 2,
          },
          properties: {
            ignoredParseLifecycleStages: {
              type: "array",
              title: "忽略的解析生命周期",
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": {
                ...smallFieldProps,
                mode: "tags",
                tokenSeparators: [",", "\n"],
                placeholder: "输入后回车添加",
              },
            },
            ignoredWorkflowTaskTypes: {
              type: "array",
              title: "忽略的工作流任务类型",
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": {
                ...smallFieldProps,
                mode: "tags",
                tokenSeparators: [",", "\n"],
                placeholder: "输入后回车添加",
              },
            },
          },
        },
        statusMappings: {
          type: "void",
          title: "状态映射",
          "x-decorator": "FormItem",
          "x-component": "Slot",
          "x-component-props": {
            name: "mappingTable",
          },
        },
      },
    },
  },
}));

const syncEngineFormToDefinition = () => {
  form.engineType = engineForm.engineType;
  if (!form.executorBindings?.length) {
    form.executorBindings = [{}];
  }
  form.executorBindings[0] = {
    ...(form.executorBindings[0] || {}),
    engineType: engineForm.engineType,
    externalRef: engineForm.externalRef,
    configJson: engineForm.configJson,
    enabled: true,
  };
};

const normalizeArrayValue = (value?: string[] | string) => {
  if (Array.isArray(value)) {
    return value.filter(Boolean);
  }
  return String(value || "")
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
};

const normalizeLegacyFallbackStage = (value?: string) => {
  if (!value) {
    return value;
  }
  if (value === "DATA_PROCESSING") {
    return "STANDARD_LANDING";
  }
  if (value === "RAW_DATA_EXTRACT") {
    return "FILE_PARSE";
  }
  return value;
};

const normalizeWorkflowForm = () => {
  form.stages = (form.stages || []).map((stage, index) => {
    const sortOrder = stage.sortOrder || index + 1;
    const stageCode = stage.stageCode || `CUSTOM_STAGE_${sortOrder}`;
    const stageName = stage.stageName || "新增阶段";
    return {
      ...stage,
      stageCode,
      stageName,
      stepCode: stage.stepCode || stageCode,
      stepName: stage.stepName || stageName,
      sortOrder,
      retryable: stage.retryable !== false,
      skippable: stage.skippable === true,
      enabled: stage.enabled !== false,
      taskTypes: normalizeArrayValue(stage.taskTypes),
      taskStages: normalizeArrayValue(stage.taskStages),
      parseLifecycleStages: normalizeArrayValue(stage.parseLifecycleStages),
    };
  });
  form.statusMappings = (form.statusMappings || []).map((mapping) => ({
    ...mapping,
    sourceType: mapping.sourceType || "WORKFLOW_TASK",
    sourceStatus: mapping.sourceStatus || "",
    targetStatus: mapping.targetStatus || "PENDING",
    statusLabel: mapping.statusLabel || "待处理",
  }));
  form.ignoredParseLifecycleStages = normalizeArrayValue(
    form.ignoredParseLifecycleStages,
  );
  form.ignoredWorkflowTaskTypes = normalizeArrayValue(
    form.ignoredWorkflowTaskTypes,
  );
};

const addStage = () => {
  const nextOrder = (form.stages?.length || 0) + 1;
  form.stages = [
    ...(form.stages || []),
    {
      stageCode: `CUSTOM_STAGE_${nextOrder}`,
      stepCode: `CUSTOM_STAGE_${nextOrder}`,
      stageName: "新增阶段",
      stepName: "新增阶段",
      sortOrder: nextOrder,
      retryable: true,
      skippable: false,
      enabled: true,
      taskTypes: [],
      taskStages: [],
      parseLifecycleStages: [],
    },
  ];
};

const removeStage = (index: number) => {
  form.stages = (form.stages || []).filter(
    (_, itemIndex) => itemIndex !== index,
  );
};

const removeStageRow = (row: WorkflowStageDTO) => {
  const index = (form.stages || []).findIndex(
    (item) => item.stageCode === row.stageCode,
  );
  if (index >= 0) {
    removeStage(index);
  }
};

const addStatusMapping = () => {
  form.statusMappings = [
    ...(form.statusMappings || []),
    {
      sourceType: "WORKFLOW_TASK",
      sourceStatus: "",
      targetStatus: "PENDING",
      statusLabel: "待处理",
    },
  ];
};

const removeStatusMapping = (index: number) => {
  form.statusMappings = (form.statusMappings || []).filter(
    (_, itemIndex) => itemIndex !== index,
  );
};

const removeStatusMappingRow = (row: WorkflowStatusMappingDTO) => {
  const index = (form.statusMappings || []).findIndex((item) =>
    row.mappingId
      ? item.mappingId === row.mappingId
      : item.sourceType === row.sourceType &&
        item.sourceStatus === row.sourceStatus &&
        item.targetStatus === row.targetStatus,
  );
  if (index >= 0) {
    removeStatusMapping(index);
  }
};

const sameWorkflowVersionOptions = computed(() =>
  rows.value
    .filter(
      (item) =>
        item.workflowCode &&
        compareSourceRow.value?.workflowCode &&
        item.workflowCode === compareSourceRow.value.workflowCode &&
        item.workflowId !== compareSourceRow.value.workflowId,
    )
    .map((item) => ({
      label: `${item.versionNo || ""} / ${item.workflowName || item.workflowCode || ""}`,
      value: item.workflowId || "",
    })),
);

const rollbackSourceOptions = computed(() =>
  rows.value
    .filter(
      (item) =>
        item.workflowCode &&
        rollbackRow.value?.workflowCode &&
        item.workflowCode === rollbackRow.value.workflowCode &&
        item.workflowId !== rollbackRow.value.workflowId,
    )
    .map((item) => ({
      label: `${item.versionNo || ""} / ${item.workflowName || item.workflowCode || ""}`,
      value: item.workflowId || "",
    })),
);

const resetForm = (data?: WorkflowDefinitionDTO) => {
  const next = data || {};
  const binding = next.executorBindings?.[0];
  Object.assign(form, {
    workflowId: next.workflowId,
    workflowCode: next.workflowCode || "VALUATION_PARSE",
    workflowName: next.workflowName || "估值表解析工作流",
    businessType: next.businessType || "VALUATION",
    engineType: next.engineType || "INTERNAL",
    parseFallbackStage: next.parseFallbackStage || "FILE_PARSE",
    workflowFallbackStage:
      normalizeLegacyFallbackStage(next.workflowFallbackStage) ||
      "STANDARD_LANDING",
    versionNo: next.versionNo || 1,
    enabled: next.enabled,
    status: next.status,
    description: next.description || "",
    stages: next.stages || [],
    statusMappings: next.statusMappings || [],
    executorBindings: next.executorBindings?.length
      ? next.executorBindings
      : [
          {
            engineType: next.engineType || "INTERNAL",
            externalRef: "DefaultTaskDispatcher",
            configJson: "{}",
            enabled: true,
          },
        ],
    ignoredParseLifecycleStages: next.ignoredParseLifecycleStages?.length
      ? next.ignoredParseLifecycleStages
      : [
          "CYCLE_STARTED",
          "CYCLE_FINISHED",
          "BATCH_STARTED",
          "BATCH_EMPTY",
          "BATCH_FINISHED",
        ],
    ignoredWorkflowTaskTypes: next.ignoredWorkflowTaskTypes?.length
      ? next.ignoredWorkflowTaskTypes
      : ["PARSE_WORKBOOK"],
  });
  Object.assign(engineForm, {
    engineType: next.engineType || binding?.engineType || "INTERNAL",
    externalRef: binding?.externalRef || "DefaultTaskDispatcher",
    configJson: binding?.configJson || "{}",
  });
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await pageWorkflowConfigs({
      workflowCode: query.workflowCode || undefined,
      workflowName: query.workflowName || undefined,
      engineType: query.engineType || undefined,
      status: query.status || undefined,
      pageIndex: query.pageIndex,
      pageSize: query.pageSize,
    });
    rows.value = res.data || [];
    total.value = Number(res.totalCount || 0);
    pagination.value.current = query.pageIndex;
    pagination.value.pageSize = query.pageSize;
    pagination.value.total = total.value;
  } finally {
    loading.value = false;
  }
};

const openCreate = () => {
  resetForm();
  detailVisible.value = true;
};

const openEdit = async (row: WorkflowDefinitionDTO) => {
  if (!row.workflowId) {
    return;
  }
  detailLoading.value = true;
  detailVisible.value = true;
  try {
    const res = await getWorkflowConfig(row.workflowId);
    resetForm(res.data);
  } finally {
    detailLoading.value = false;
  }
};

const saveDraft = async () => {
  saving.value = true;
  try {
    const res = await saveDraftFromCurrentForm();
    resetForm(res.data);
    detailVisible.value = false;
    message.success("草稿已保存");
    await loadData();
  } finally {
    saving.value = false;
  }
};

const importDraft = async () => {
  saving.value = true;
  try {
    syncEditableTableDataFromInstances();
    syncEngineFormToDefinition();
    normalizeWorkflowForm();
    const res = await importWorkflowConfig({ ...form });
    resetForm(res.data);
    detailVisible.value = false;
    message.success("工作流配置已导入并保存草稿");
    await loadData();
  } finally {
    saving.value = false;
  }
};

const validateCurrent = async () => {
  try {
    syncEngineFormToDefinition();
    normalizeWorkflowForm();
    await validateWorkflowConfig({ ...form });
    message.success("工作流配置校验通过");
  } catch {
    message.error("工作流配置校验失败");
  }
};

const copyVersion = async (row: WorkflowDefinitionDTO) => {
  if (!row.workflowId) {
    message.warning("请先保存草稿");
    return;
  }
  const res = await copyWorkflowConfigVersion(row.workflowId);
  resetForm(res.data);
  detailVisible.value = true;
  message.success("已复制为新版本草稿");
  await loadData();
};

const publish = async (row?: WorkflowDefinitionDTO) => {
  const workflowId = row?.workflowId || form.workflowId;
  if (!workflowId) {
    message.warning("请先保存草稿");
    return;
  }
  if (row?.workflowId) {
    const res = await publishWorkflowConfig(row.workflowId);
    message.success("工作流配置已发布");
    await loadData();
    if (form.workflowId === row.workflowId) {
      resetForm(res.data);
      detailVisible.value = false;
    }
    return;
  }
  saving.value = true;
  try {
    const draftRes = await saveDraftFromCurrentForm();
    const draftWorkflowId = draftRes.data?.workflowId || workflowId;
    const res = await publishWorkflowConfig(draftWorkflowId);
    resetForm(res.data);
    detailVisible.value = false;
    message.success("工作流配置已保存并发布");
    await loadData();
  } finally {
    saving.value = false;
  }
};

const exportRow = async (row: WorkflowDefinitionDTO) => {
  if (!row.workflowId) {
    return;
  }
  const res = await exportWorkflowConfig(row.workflowId);
  const payload = JSON.stringify(res.data || {}, null, 2);
  const blob = new Blob([payload], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${row.workflowCode || "workflow"}-v${row.versionNo || 1}.json`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  message.success("已导出工作流配置");
};

const openCompare = (row: WorkflowDefinitionDTO) => {
  compareSourceRow.value = row;
  compareTargetWorkflowId.value = "";
  compareResult.value = null;
  compareVisible.value = true;
  const candidates = rows.value.filter(
    (item) =>
      item.workflowCode &&
      row.workflowCode &&
      item.workflowCode === row.workflowCode &&
      item.workflowId !== row.workflowId,
  );
  compareTargetWorkflowId.value = candidates[0]?.workflowId || "";
};

const runCompare = async () => {
  if (!compareSourceRow.value?.workflowId || !compareTargetWorkflowId.value) {
    message.warning("请选择要对比的版本");
    return;
  }
  compareLoading.value = true;
  try {
    const res = await compareWorkflowConfigs(
      compareSourceRow.value.workflowId,
      compareTargetWorkflowId.value,
    );
    compareResult.value = res.data || null;
  } finally {
    compareLoading.value = false;
  }
};

const openRollback = (row: WorkflowDefinitionDTO) => {
  rollbackRow.value = row;
  rollbackSourceWorkflowId.value = "";
  rollbackVisible.value = true;
  const candidates = rows.value.filter(
    (item) =>
      item.workflowCode &&
      row.workflowCode &&
      item.workflowCode === row.workflowCode &&
      item.workflowId !== row.workflowId,
  );
  rollbackSourceWorkflowId.value = candidates[0]?.workflowId || "";
};

const runRollback = async () => {
  if (!rollbackRow.value?.workflowId || !rollbackSourceWorkflowId.value) {
    message.warning("请选择回滚来源版本");
    return;
  }
  rollbackLoading.value = true;
  try {
    await rollbackWorkflowConfigVersion(
      rollbackRow.value.workflowId,
      rollbackSourceWorkflowId.value,
    );
    message.success("已生成回滚草稿");
    rollbackVisible.value = false;
    await loadData();
  } finally {
    rollbackLoading.value = false;
  }
};

const disable = async (row: WorkflowDefinitionDTO) => {
  if (!row.workflowId) {
    return;
  }
  Modal.confirm({
    title: "确认停用该工作流配置？",
    content: "停用后运行期会回退到其他已发布版本或内置默认配置。",
    onOk: async () => {
      await disableWorkflowConfig(row.workflowId!);
      message.success("工作流配置已停用");
      await loadData();
    },
  });
};

const handlePageChange = (paginationInfo: any) => {
  query.pageIndex = paginationInfo.current || 1;
  query.pageSize = paginationInfo.pageSize || 20;
  pagination.value.current = query.pageIndex;
  pagination.value.pageSize = query.pageSize;
  loadData();
};

const saveDraftFromCurrentForm = async () => {
  syncEditableTableDataFromInstances();
  syncEngineFormToDefinition();
  normalizeWorkflowForm();
  return saveWorkflowConfigDraft({ ...form });
};

const syncEditableTableDataFromInstances = () => {
  const stageTableData =
    workflowcConfTbRef.value?.getTableInstance?.()?.getTableData?.()
      ?.tableData || [];
  const mappingTableData =
    mappingTableRef.value?.getTableInstance?.()?.getTableData?.()?.tableData ||
    [];
  form.stages = Array.isArray(stageTableData) ? stageTableData : [];
  form.statusMappings = Array.isArray(mappingTableData) ? mappingTableData : [];
};

onMounted(loadData);
</script>

<template>
  <div class="workflow-config-page">
    <div class="workflow-config-toolbar">
      <a-space>
        <a-input
          v-model:value="query.workflowCode"
          size="small"
          allow-clear
          placeholder="工作流编码"
          style="width: 180px"
          @press-enter="loadData"
        />
        <a-input
          v-model:value="query.workflowName"
          size="small"
          allow-clear
          placeholder="工作流名称"
          style="width: 180px"
          @press-enter="loadData"
        />
        <a-select
          v-model:value="query.engineType"
          size="small"
          allow-clear
          placeholder="执行平台"
          style="width: 160px"
          :options="engineOptions"
        />
        <a-select
          v-model:value="query.status"
          size="small"
          allow-clear
          placeholder="状态"
          style="width: 140px"
          :options="statusOptions"
        />
        <a-button type="primary" size="small" @click="loadData">查询</a-button>
      </a-space>
      <a-button type="primary" size="small" @click="openCreate"
        >新增工作流</a-button
      >
    </div>

    <YTable
      class="workflow-config-table"
      size="small"
      :columns="columns"
      :data="rows"
      :loading="loading"
      :row-config="{ keyField: 'workflowId' }"
      :pageable="true"
      v-model:pagination="pagination"
      :toolbar-config="{ custom: false }"
      @page-change="handlePageChange"
    >
      <template #toolbar-left>
        <span class="workflow-config-table-title">
          工作流列表
          <em>共 {{ total }} 条</em>
        </span>
      </template>
      <template #status="{ row }">
        <a-tag :color="row.enabled ? 'green' : 'default'">
          {{ resolveOptionLabel(statusOptions, row.status) || row.status || "草稿" }}
        </a-tag>
      </template>
      <template #action="{ row }">
        <a-space>
          <a-button type="link" size="small" @click="openEdit(row)"
            >编辑</a-button
          >
          <a-button type="link" size="small" @click="copyVersion(row)"
            >复制</a-button
          >
          <a-button type="link" size="small" @click="publish(row)"
            >发布</a-button
          >
          <a-button type="link" size="small" @click="exportRow(row)"
            >导出</a-button
          >
          <a-button type="link" size="small" @click="openCompare(row)"
            >对比</a-button
          >
          <a-button type="link" size="small" @click="openRollback(row)"
            >回滚</a-button
          >
          <a-button type="link" size="small" danger @click="disable(row)"
            >停用</a-button
          >
        </a-space>
      </template>
    </YTable>

    <a-modal
      v-model:open="detailVisible"
      :width="'80vw'"
      :title="form.workflowName || '工作流配置'"
      centered
      class="workflow-config-modal"
      wrap-class-name="workflow-config-modal-wrap"
    >
      <a-spin :spinning="detailLoading">
        <div class="workflow-config-modal-body">
          <section class="workflow-config-section">
            <div class="workflow-config-section__title">基础信息</div>
            <YssFormily
              v-model="form"
              :schema="baseFormSchema"
              :mode="1"
              :read-pretty="false"
            >
              <template #description>
                <a-textarea
                  v-model:value="form.description"
                  size="small"
                  :rows="3"
                  placeholder="工作流用途、适用业务和配置说明"
                />
              </template>
            </YssFormily>
          </section>

          <section class="workflow-config-section">
            <div class="workflow-config-section__title">阶段编排</div>
            <YssFormily
              v-model="form"
              :schema="stageFormSchema"
              :mode="1"
              :read-pretty="false"
            >
              <template #stageTable>
                <div class="workflow-config-edit-table">
                  <YTable
                    :columns="stageTableColumns"
                    ref="workflowcConfTbRef"
                    :data="form.stages"
                    size="small"
                    :pageable="false"
                    :edit-config="{
                      mode: 'cell',
                      trigger: 'dblclick',
                      showStatus: true,
                    }"
                    :mouse-config="{ selected: true }"
                    :keyboard-config="{
                      isEdit: true,
                      isArrow: true,
                      isEnter: true,
                      isBack: true,
                      isDel: true,
                      isEsc: true,
                    }"
                    :toolbar-config="{ custom: false }"
                  >
                    <template #action="{ row }">
                      <a-button
                        type="link"
                        size="small"
                        danger
                        @click="removeStageRow(row)"
                      >
                        删除
                      </a-button>
                    </template>
                  </YTable>
                  <div class="workflow-config-edit-table__actions">
                    <a-button type="primary" size="small" @click="addStage">
                      新增阶段
                    </a-button>
                  </div>
                </div>
              </template>
            </YssFormily>
          </section>

          <section class="workflow-config-section">
            <div class="workflow-config-section__title">事件映射</div>
            <YssFormily
              v-model="form"
              :schema="mappingFormSchema"
              :mode="1"
              :read-pretty="false"
            >
              <template #mappingTable>
                <div class="workflow-config-edit-table">
                  <YTable
                    :columns="mappingTableColumns"
                    :data="form.statusMappings"
                    size="small"
                    :pageable="false"
                    :edit-config="{
                      mode: 'cell',
                      trigger: 'dblclick',
                      showStatus: true,
                    }"
                    :mouse-config="{ selected: true }"
                    :keyboard-config="{
                      isEdit: true,
                      isArrow: true,
                      isEnter: true,
                      isBack: true,
                      isDel: true,
                      isEsc: true,
                    }"
                    :toolbar-config="{ custom: false }"
                    ref="mappingTableRef"
                  >
                    <template #action="{ row }">
                      <a-button
                        type="link"
                        size="small"
                        danger
                        @click="removeStatusMappingRow(row)"
                      >
                        删除
                      </a-button>
                    </template>
                  </YTable>
                  <div class="workflow-config-edit-table__actions">
                    <a-button
                      type="primary"
                      size="small"
                      @click="addStatusMapping"
                    >
                      新增状态映射
                    </a-button>
                  </div>
                </div>
              </template>
            </YssFormily>
          </section>

          <section class="workflow-config-section">
            <div class="workflow-config-section__title">平台适配</div>
            <YssFormily
              v-model="engineForm"
              class="workflow-engine-form"
              :schema="engineFormSchema"
              :mode="1"
              :read-pretty="false"
            >
              <template #configJson>
                <a-textarea
                  v-model:value="engineForm.configJson"
                  size="small"
                  :rows="8"
                  placeholder='例如：{"jobId":"10001"}'
                />
              </template>
            </YssFormily>
          </section>
        </div>
      </a-spin>

      <template #footer>
        <a-space>
          <a-button size="small" @click="detailVisible = false">关闭</a-button>
          <a-button size="small" @click="validateCurrent">校验</a-button>
          <a-button size="small" :loading="saving" @click="importDraft"
            >导入</a-button
          >
          <a-button size="small" :loading="saving" @click="saveDraft"
            >保存草稿</a-button
          >
          <a-button
            type="primary"
            size="small"
            :loading="saving"
            @click="publish()"
            >发布</a-button
          >
        </a-space>
      </template>
    </a-modal>

    <a-modal
      v-model:open="compareVisible"
      :width="'72vw'"
      title="版本对比"
      centered
      class="workflow-config-modal"
      wrap-class-name="workflow-config-modal-wrap"
      @ok="runCompare"
      :confirm-loading="compareLoading"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-select
          v-model:value="compareTargetWorkflowId"
          size="small"
          allow-clear
          placeholder="选择对比版本"
          :options="sameWorkflowVersionOptions"
        />
        <YTable
          v-if="compareResult?.items?.length"
          size="small"
          :columns="diffColumns"
          :data="compareResult.items"
          :pageable="false"
          :toolbar-config="{ custom: false }"
        />
        <a-empty v-else description="请选择版本并点击对比" />
      </a-space>
    </a-modal>

    <a-modal
      v-model:open="rollbackVisible"
      :width="'56vw'"
      title="回滚为草稿"
      centered
      class="workflow-config-modal"
      wrap-class-name="workflow-config-modal-wrap"
      @ok="runRollback"
      :confirm-loading="rollbackLoading"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-select
          v-model:value="rollbackSourceWorkflowId"
          size="small"
          allow-clear
          placeholder="选择回滚来源版本"
          :options="rollbackSourceOptions"
        />
        <div class="workflow-config-hint">
          选择的版本会复制为当前工作流的新草稿版本，不会直接覆盖已有发布版本。
        </div>
      </a-space>
    </a-modal>
  </div>
</template>
