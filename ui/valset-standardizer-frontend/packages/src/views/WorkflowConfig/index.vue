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
  getWorkflowConfig,
  pageWorkflowConfigs,
  publishWorkflowConfig,
  saveWorkflowConfigDraft,
  type WorkflowDefinitionDTO,
  type WorkflowStageDTO,
  type WorkflowStatusMappingDTO,
} from "@/api/workflowConfig";
import "./index.less";

defineOptions({ name: "WorkflowConfigPage" });

const loading = ref(false);
const detailLoading = ref(false);
const saving = ref(false);
const rows = ref<WorkflowDefinitionDTO[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const activeTab = ref("base");
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
  workflowFallbackStage: "DATA_PROCESSING",
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

const smallFieldProps = {
  size: "small",
};

const columns: YTableColumn[] = [
  { type: "seq", title: "序号", width: 70, align: "center" },
  { field: "workflowCode", title: "工作流编码", minWidth: 180 },
  { field: "workflowName", title: "工作流名称", minWidth: 200 },
  { field: "businessType", title: "业务类型", width: 120 },
  { field: "engineType", title: "执行平台", width: 150 },
  { field: "versionNo", title: "版本", width: 90, align: "center" },
  { field: "status", title: "状态", width: 120, align: "center" },
  { field: "updatedAt", title: "更新时间", width: 190 },
  { field: "action", title: "操作", width: 220, fixed: "right" as const },
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

const defaultStages = (): WorkflowStageDTO[] => [
  {
    stageCode: "FILE_PARSE",
    stepCode: "FILE_PARSE",
    stageName: "文件解析",
    stepName: "文件解析",
    stageDescription: "文件识别、Sheet 解析、结构化解析",
    stepDescription: "文件识别、Sheet 解析、结构化解析",
    sortOrder: 1,
    retryable: true,
    skippable: false,
    enabled: true,
    taskTypes: ["EXTRACT_DATA"],
    taskStages: ["EXTRACT"],
    parseLifecycleStages: [],
  },
  {
    stageCode: "STRUCTURE_STANDARDIZE",
    stepCode: "STRUCTURE_STANDARDIZE",
    stageName: "结构标准化",
    stepName: "结构标准化",
    stageDescription: "字段映射、数据清洗、STG 结构转换",
    stepDescription: "字段映射、数据清洗、STG 结构转换",
    sortOrder: 2,
    retryable: true,
    skippable: false,
    enabled: true,
    taskTypes: [],
    taskStages: ["STANDARDIZE"],
    parseLifecycleStages: ["TASK_STANDARDIZED"],
  },
  {
    stageCode: "SUBJECT_RECOGNIZE",
    stepCode: "SUBJECT_RECOGNIZE",
    stageName: "科目识别",
    stepName: "科目识别",
    stageDescription: "科目匹配、属性识别、标签补全",
    stepDescription: "科目匹配、属性识别、标签补全",
    sortOrder: 3,
    retryable: true,
    skippable: false,
    enabled: true,
    taskTypes: ["MATCH_SUBJECT"],
    taskStages: ["MATCH"],
    parseLifecycleStages: [],
  },
  {
    stageCode: "STANDARD_LANDING",
    stepCode: "STANDARD_LANDING",
    stageName: "标准表落地",
    stepName: "标准表落地",
    stageDescription: "STG/DWD/标准持仓/估值数据写入",
    stepDescription: "STG/DWD/标准持仓/估值数据写入",
    sortOrder: 4,
    retryable: true,
    skippable: false,
    enabled: true,
    taskTypes: [],
    taskStages: [],
    parseLifecycleStages: ["TASK_PERSISTED"],
  },
  {
    stageCode: "VERIFY_ARCHIVE",
    stepCode: "VERIFY_ARCHIVE",
    stageName: "校验归档",
    stepName: "校验归档",
    stageDescription: "一致性校验、结果确认、归档完成",
    stepDescription: "一致性校验、结果确认、归档完成",
    sortOrder: 5,
    retryable: true,
    skippable: false,
    enabled: true,
    taskTypes: ["EXPORT_RESULT"],
    taskStages: [],
    parseLifecycleStages: ["TASK_SUCCEEDED", "QUEUE_COMPLETED"],
  },
];

const defaultStatusMappings = (): WorkflowStatusMappingDTO[] =>
  [
    ["WORKFLOW_TASK", "SUCCESS", "SUCCESS", "已完成"],
    ["WORKFLOW_TASK", "FAILED", "FAILED", "失败"],
    ["WORKFLOW_TASK", "CANCELED", "STOPPED", "已停止"],
    ["WORKFLOW_TASK", "RUNNING", "RUNNING", "处理中"],
    ["WORKFLOW_TASK", "RETRYING", "RUNNING", "处理中"],
    ["PARSE_LIFECYCLE", "TASK_EXECUTION_STARTED", "RUNNING", "处理中"],
    ["PARSE_LIFECYCLE", "TASK_CREATED", "RUNNING", "处理中"],
    ["PARSE_LIFECYCLE", "TASK_DISPATCHED", "RUNNING", "处理中"],
    ["PARSE_LIFECYCLE", "QUEUE_SUBSCRIBED", "RUNNING", "处理中"],
    ["PARSE_LIFECYCLE", "TASK_RAW_PARSED", "SUCCESS", "已完成"],
    ["PARSE_LIFECYCLE", "TASK_STANDARDIZED", "SUCCESS", "已完成"],
    ["PARSE_LIFECYCLE", "TASK_PERSISTED", "SUCCESS", "已完成"],
    ["PARSE_LIFECYCLE", "TASK_SUCCEEDED", "SUCCESS", "已完成"],
    ["PARSE_LIFECYCLE", "QUEUE_COMPLETED", "SUCCESS", "已完成"],
    ["PARSE_LIFECYCLE", "TASK_FAILED", "FAILED", "失败"],
    ["PARSE_LIFECYCLE", "QUEUE_FAILED", "FAILED", "失败"],
    ["PARSE_LIFECYCLE", "QUEUE_SKIPPED", "STOPPED", "已停止"],
  ].map(([sourceType, sourceStatus, targetStatus, statusLabel]) => ({
    sourceType,
    sourceStatus,
    targetStatus,
    statusLabel,
  }));

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
            workflowCode: {
              type: "string",
              title: "工作流编码",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": smallFieldProps,
            },
            workflowName: {
              type: "string",
              title: "工作流名称",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": smallFieldProps,
            },
            businessType: {
              type: "string",
              title: "业务类型",
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": smallFieldProps,
            },
            engineType: {
              type: "string",
              title: "执行平台",
              enum: engineOptions,
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": smallFieldProps,
            },
            versionNo: {
              type: "number",
              title: "版本号",
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
              enum: stageOptions.value,
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-component-props": smallFieldProps,
            },
            workflowFallbackStage: {
              type: "string",
              title: "工作流任务默认阶段",
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
  const index = (form.stages || []).indexOf(row);
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
  const index = (form.statusMappings || []).indexOf(row);
  if (index >= 0) {
    removeStatusMapping(index);
  }
};

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
    workflowFallbackStage: next.workflowFallbackStage || "DATA_PROCESSING",
    versionNo: next.versionNo || 1,
    enabled: next.enabled,
    status: next.status,
    description: next.description || "",
    stages: next.stages?.length ? next.stages : defaultStages(),
    statusMappings: next.statusMappings?.length
      ? next.statusMappings
      : defaultStatusMappings(),
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
  activeTab.value = "base";
  detailVisible.value = true;
};

const openEdit = async (row: WorkflowDefinitionDTO) => {
  if (!row.workflowId) {
    return;
  }
  detailLoading.value = true;
  detailVisible.value = true;
  activeTab.value = "base";
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
    syncEngineFormToDefinition();
    normalizeWorkflowForm();
    const res = await saveWorkflowConfigDraft({ ...form });
    resetForm(res.data);
    message.success("草稿已保存");
    await loadData();
  } finally {
    saving.value = false;
  }
};

const publish = async (row?: WorkflowDefinitionDTO) => {
  const workflowId = row?.workflowId || form.workflowId;
  if (!workflowId) {
    message.warning("请先保存草稿");
    return;
  }
  syncEngineFormToDefinition();
  const res = await publishWorkflowConfig(workflowId);
  resetForm(res.data);
  message.success("工作流配置已发布");
  await loadData();
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
          {{ row.status || "DRAFT" }}
        </a-tag>
      </template>
      <template #action="{ row }">
        <a-space>
          <a-button type="link" size="small" @click="openEdit(row)"
            >编辑</a-button
          >
          <a-button type="link" size="small" @click="publish(row)"
            >发布</a-button
          >
          <a-button type="link" size="small" danger @click="disable(row)"
            >停用</a-button
          >
        </a-space>
      </template>
    </YTable>

    <a-drawer
      v-model:open="detailVisible"
      width="920"
      :title="form.workflowName || '工作流配置'"
      class="workflow-config-drawer"
    >
      <a-spin :spinning="detailLoading">
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="base" tab="基础信息">
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
          </a-tab-pane>

          <a-tab-pane key="stages" tab="阶段编排">
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
                    <a-button type="primary" size="small" @click="addStage"
                      >新增阶段</a-button
                    >
                  </div>
                </div>
              </template>
            </YssFormily>
          </a-tab-pane>

          <a-tab-pane key="mapping" tab="事件映射">
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
          </a-tab-pane>

          <a-tab-pane key="engine" tab="平台适配">
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
          </a-tab-pane>
        </a-tabs>
      </a-spin>

      <template #footer>
        <a-space>
          <a-button size="small" @click="detailVisible = false">关闭</a-button>
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
    </a-drawer>
  </div>
</template>
