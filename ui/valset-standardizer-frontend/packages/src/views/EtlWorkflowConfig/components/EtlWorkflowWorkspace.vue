<script setup lang="ts">
import { computed, h, reactive, ref, unref } from "vue";
import { message, Modal } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  SyncOutlined,
} from "@ant-design/icons-vue";
import {
  YButton,
  YCard,
  YTable,
  YssFormily,
  type ISchema,
  type YTableActionConfig,
  type YTableColumn,
} from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import type { EtlPlatformType } from "@/api/etlWorkflowConfig";
import type {
  EtlWorkflowConfigPage,
  EtlWorkflowStageFormState,
} from "../types";

defineOptions({ name: "EtlWorkflowConfigWorkspace" });

const { page, mode = "definition" } = defineProps<{
  page: EtlWorkflowConfigPage;
  mode?: "definition" | "stage";
}>();

const isDefinitionMode = computed(() => mode === "definition");
const isStageMode = computed(() => mode === "stage");
const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: true,
});

const workflowColumns = computed<YTableColumn[]>(() => [
  { field: "workflowCode", title: "工作流编码", ellipsis: true },
  { field: "workflowName", title: "工作流名称", ellipsis: true },
  { field: "workflowVersionNo", title: "版本", width: 88 },
  { field: "platformLabel", title: "平台" },
  { field: "stageCount", title: "阶段数", width: 88 },
  { field: "statusLabel", title: "状态", width: 88 },
  { field: "syncStatusLabel", title: "同步状态", width: 112 },
  { field: "externalStateLabel", title: "远端状态", width: 104 },
  { field: "syncTimeLabel", title: "最近同步", width: 160, ellipsis: true },
  { field: "bindingSummary", title: "绑定摘要", minWidth: 120, ellipsis: true },
]);

const workflowActionConfig = computed<YTableActionConfig>(() => ({
  width: 340,
  fixed: "right",
  displayLimit: 3,
  moreRenderType: "ellipsis",
  buttons: isDefinitionMode.value
    ? [
        {
          text: "运行",
          key: "run",
          type: "link",
          hideFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            !isWorkflowOnline(row),
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            confirmRunWorkflow(row),
        },
        {
          text: "编辑",
          key: "edit",
          type: "link",
          hideFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            !isWorkflowOffline(row),
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            openEditWorkflowDialog(row),
        },
        {
          text: "同步工作流",
          key: "sync",
          type: "link",
          hideFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            !isWorkflowOnline(row),
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            confirmSyncWorkflow(row),
        },
        {
          text: "上线",
          key: "online",
          type: "link",
          hideFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            !row.supportedOperations?.includes("ONLINE"),
          disabledFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            row.syncStatus !== "SYNCED" || isWorkflowOnline(row),
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            confirmOnlineWorkflow(row),
        },
        {
          text: "下线",
          key: "offline",
          type: "link",
          hideFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            !row.supportedOperations?.includes("OFFLINE"),
          disabledFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            row.syncStatus !== "SYNCED" || isWorkflowOffline(row),
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            confirmOfflineWorkflow(row),
        },
        {
          text: "删除",
          key: "delete",
          type: "link",
          danger: true,
          hideFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            !isWorkflowOffline(row),
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            confirmDeleteWorkflow(row),
        },
      ]
    : [
        {
          text: "新增阶段",
          key: "addStage",
          type: "link",
          clickFn: ({ row }: { row: (typeof tableData.value)[number] }) =>
            openStageDialog(row),
        },
      ],
}));

const columnsWithAction = computed<YTableColumn[]>(() => [
  ...workflowColumns.value,
  {
    type: "action" as const,
    title: "操作",
    align: "center" as const,
    actionConfig: workflowActionConfig.value,
  },
]);

const platformOptions = computed(() => {
  const options = unref(page.platformOptions);
  return Array.isArray(options) ? options.filter(Boolean) : [];
});

const tableData = computed(() => unref(page.tableData) ?? []);
const tableLoading = computed(() => Boolean(unref(page.loading)));
const tablePagination = computed(() => unref(page.pagination) ?? {});
const workflowLabel = computed(() =>
  String(page.formState.workflowCode ?? "").trim()
    ? `${String(page.formState.workflowCode ?? "").trim()} v${Number(
        page.formState.workflowVersionNo ?? 0,
      )}`
    : "未选择",
);
const workflowModalVisible = ref(false);
const stageModalVisible = ref(false);
const workflowModalMode = ref<"create" | "edit">("edit");
const workflowCreateFormRef = ref<{
  submit: () => Promise<unknown>;
  getValues?: () => Record<string, unknown>;
} | null>(null);
const stageFormRef = ref<{
  submit: () => Promise<unknown>;
  getValues?: () => Record<string, unknown>;
} | null>(null);

const createStageDraft = (stageOrder = 1): EtlWorkflowStageFormState => ({
  __rowKey: `stage-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  stageCode: "",
  stageName: "",
  stageOrder,
  description: "",
  retryable: true,
  timeoutSeconds: null,
});

const stageDraft = reactive<EtlWorkflowStageFormState>(createStageDraft(1));

const resetStageDraft = (stageOrder = 1) => {
  Object.assign(stageDraft, createStageDraft(stageOrder));
};

const pageTitle = computed(() =>
  isDefinitionMode.value ? "工作流定义" : "任务阶段定义",
);

const workflowModalTitle = computed(() =>
  workflowModalMode.value === "create"
    ? "新增工作流"
    : `编辑工作流 - ${workflowLabel.value}`,
);

const workflowModalConfirmText = computed(() =>
  workflowModalMode.value === "create" ? "创建工作流" : "保存工作流",
);

const workflowModalHintText = computed(() =>
  workflowModalMode.value === "create"
    ? "请先填写必输字段，再创建工作流。创建后仍可继续补充执行平台绑定信息。"
    : "修改工作流基础信息或执行平台配置后，点击保存会回写当前版本定义。",
);

const stageModalTitle = computed(() =>
  isStageMode.value ? `任务阶段定义 - ${workflowLabel.value}` : "任务阶段定义",
);

const stageFormValues = computed(() => ({
  stageCode: String(stageDraft.stageCode ?? ""),
  stageName: String(stageDraft.stageName ?? ""),
  stageOrder: Number(stageDraft.stageOrder ?? 1),
  timeoutSeconds:
    stageDraft.timeoutSeconds === null ||
    stageDraft.timeoutSeconds === undefined
      ? null
      : Number(stageDraft.timeoutSeconds),
  description: String(stageDraft.description ?? ""),
  retryable: Boolean(stageDraft.retryable),
}));

const confirmDeleteWorkflow = (row: (typeof tableData.value)[number]) => {
  Modal.confirm({
    title: "删除工作流",
    content: `确认删除 ${row.workflowCode} v${row.workflowVersionNo} 吗？删除后该版本的阶段和绑定配置会一并清理。`,
    icon: h(ExclamationCircleOutlined),
    okText: "删除",
    okButtonProps: { danger: true },
    cancelText: "取消",
    onOk: () => page.deleteDefinition(row),
  });
};

const confirmSyncWorkflow = (row: (typeof tableData.value)[number]) => {
  Modal.confirm({
    title: "同步工作流",
    content: `确认将 ${row.workflowCode} v${row.workflowVersionNo} 同步到远端平台吗？同步只会创建或更新远端任务流、任务分组和平台绑定，不会执行上线。`,
    icon: h(SyncOutlined),
    okText: "同步",
    cancelText: "取消",
    onOk: () => page.syncDefinition(row),
  });
};

const confirmRunWorkflow = (row: (typeof tableData.value)[number]) => {
  Modal.confirm({
    title: "运行工作流",
    content: `确认运行 ${row.workflowCode} v${row.workflowVersionNo} 吗？这会调用远端平台的运行接口并创建新的工作流实例。`,
    icon: h(PlayCircleOutlined),
    okText: "运行",
    cancelText: "取消",
    onOk: () => page.runDefinition(row),
  });
};

const confirmOnlineWorkflow = (row: (typeof tableData.value)[number]) => {
  Modal.confirm({
    title: "上线工作流",
    content: `确认将 ${row.workflowCode} v${row.workflowVersionNo} 统一上线吗？这会把任务流和任务一并上线到调度平台。`,
    icon: h(SyncOutlined),
    okText: "上线",
    cancelText: "取消",
    onOk: () => page.onlineDefinition(row),
  });
};

const confirmOfflineWorkflow = (row: (typeof tableData.value)[number]) => {
  Modal.confirm({
    title: "下线工作流",
    content: `确认将 ${row.workflowCode} v${row.workflowVersionNo} 统一下线吗？这会把任务流和任务一并从调度平台下线。`,
    icon: h(ExclamationCircleOutlined),
    okText: "下线",
    okButtonProps: { danger: true },
    cancelText: "取消",
    onOk: () => page.offlineDefinition(row),
  });
};

const syncStatusColor = (status?: string | null) => {
  switch (status) {
    case "SYNCED":
      return "green";
    case "SYNCING":
      return "blue";
    case "FAILED":
      return "red";
    default:
      return "default";
  }
};

const isWorkflowOnline = (row: (typeof tableData.value)[number]) => {
  const releaseState = String(row.externalReleaseState ?? "")
    .trim()
    .toUpperCase();
  return row.externalOnline === true || releaseState === "ONLINE";
};

const isWorkflowOffline = (row: (typeof tableData.value)[number]) => {
  const releaseState = String(row.externalReleaseState ?? "")
    .trim()
    .toUpperCase();
  return row.externalOnline === false || releaseState === "OFFLINE";
};

const externalStateColor = (row: (typeof tableData.value)[number]) => {
  if (isWorkflowOnline(row)) {
    return "green";
  }
  if (isWorkflowOffline(row)) {
    return "default";
  }
  return "gold";
};

const workflowCreateDefaultValues = {
  workflowCode: "",
  workflowName: "",
  workflowVersionNo: 1,
  platformType: "SPRING_BATCH" as EtlPlatformType,
  description: "",
  enabled: true,
  externalWorkflowId: "",
  externalProjectCode: "",
  externalNamespace: "default",
  externalJobGroup: "",
  externalJobHandler: "",
  configJson: "",
  attributesText: "{}",
};

const workflowCreateValues = computed(() => {
  if (workflowModalMode.value === "create") {
    return workflowCreateDefaultValues;
  }

  return {
    workflowCode: String(page.formState.workflowCode ?? ""),
    workflowName: String(page.formState.workflowName ?? ""),
    workflowVersionNo: Number(page.formState.workflowVersionNo ?? 1),
    platformType: page.formState.platformType || "SPRING_BATCH",
    description: String(page.formState.description ?? ""),
    enabled: Boolean(page.formState.enabled),
    externalWorkflowId: String(
      page.formState.engineBinding.externalWorkflowId ?? "",
    ),
    externalProjectCode: String(
      page.formState.engineBinding.externalProjectCode ?? "",
    ),
    externalNamespace: String(
      page.formState.engineBinding.externalNamespace ?? "default",
    ),
    externalJobGroup: String(
      page.formState.engineBinding.externalJobGroup ?? "",
    ),
    externalJobHandler: String(
      page.formState.engineBinding.externalJobHandler ?? "",
    ),
    configJson: String(page.formState.engineBinding.configJson ?? ""),
    attributesText: String(page.formState.engineBinding.attributesText ?? "{}"),
  };
});

const workflowCreateSchema = computed<ISchema>(() => ({
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": {
        layout: "horizontal",
        labelWidth: 120,
      },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          "x-component-props": {
            maxColumns: 2,
            minColumns: 1,
            columnGap: 16,
            rowGap: 0,
            minWidth: 260,
          },
          properties: {
            workflowCode: {
              type: "string",
              title: "工作流编码",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": {
                placeholder: "例如 valuation-etl-daily",
                readOnly: workflowModalMode.value === "edit",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            workflowName: {
              type: "string",
              title: "工作流名称",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": {
                placeholder: "例如 估值日终 ETL",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            workflowVersionNo: {
              type: "number",
              title: "版本号",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "InputNumber",
              "x-component-props": {
                min: 1,
                style: "width: 100%",
                readOnly: workflowModalMode.value === "edit",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            platformType: {
              type: "string",
              title: "执行平台",
              required: true,
              enum: platformOptions.value.map((item: any) => ({
                label: item.label,
                value: item.value,
              })),
              "x-decorator": "FormItem",
              "x-component": "Select",
              "x-decorator-props": { gridSpan: 2 },
              "x-component-props": {
                placeholder: "请选择执行平台",
                allowClear: true,
                disabled: workflowModalMode.value === "edit",
              },
            },
            description: {
              type: "string",
              title: "工作流说明",
              "x-decorator": "FormItem",
              "x-component": "Input.TextArea",
              "x-component-props": {
                placeholder: "补充通用 ETL 工作流说明",
              },
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
            externalWorkflowId: {
              type: "string",
              title: "外部工作流ID",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "InputNumber",
              "x-visible": "{{ $values.platformType === 'DOLPHIN_SCHEDULER' }}",
              "x-component-props": {
                placeholder: "外部平台工作流标识",
                readOnly: workflowModalMode.value === "edit",
              },
            },
            externalProjectCode: {
              type: "string",
              title: "外部项目编码",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "InputNumber",
              "x-visible": "{{ $values.platformType === 'DOLPHIN_SCHEDULER' }}",
              "x-component-props": {
                placeholder: "项目编码",
                readOnly: workflowModalMode.value === "edit",
              },
            },
            externalNamespace: {
              type: "string",
              title: "外部命名空间",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "InputNumber",
              "x-visible": "{{ $values.platformType === 'DOLPHIN_SCHEDULER' }}",
              "x-component-props": {
                placeholder: "命名空间",
              },
            },
            externalJobGroup: {
              type: "string",
              title: "JobGroup",
              "x-decorator": "FormItem",
              "x-component": "Input",
              required: true,
              "x-visible": "{{ $values.platformType === 'XXL_JOB' }}",
              "x-component-props": {
                placeholder: "任务组JobGroup（xxljob）",
              },
            },
            externalJobHandler: {
              type: "string",
              title: "JobHandler",
              "x-decorator": "FormItem",
              "x-component": "Input",
              required: true,
              "x-visible": "{{ $values.platformType === 'XXL_JOB' }}",
              "x-component-props": {
                placeholder: "外部JobHandler（xxljob）",
              },
            },
          },
        },
      },
    },
  },
}));

const stageSchema = computed<ISchema>(() => ({
  type: "object",
  properties: {
    layout: {
      type: "void",
      "x-component": "FormLayout",
      "x-component-props": { layout: "horizontal", labelWidth: 120 },
      properties: {
        grid: {
          type: "void",
          "x-component": "FormGrid",
          "x-component-props": {
            maxColumns: 2,
            minColumns: 1,
            columnGap: 16,
            rowGap: 0,
            minWidth: 260,
          },
          properties: {
            stageCode: {
              type: "string",
              title: "阶段编码",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": {
                placeholder: "阶段编码",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            stageName: {
              type: "string",
              title: "阶段名称",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": {
                placeholder: "阶段名称",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            stageOrder: {
              type: "number",
              title: "顺序",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "InputNumber",
              "x-component-props": {
                min: 1,
                style: "width: 100%",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            timeoutSeconds: {
              type: "number",
              title: "超时(秒)",
              "x-decorator": "FormItem",
              "x-component": "InputNumber",
              "x-component-props": {
                min: 0,
                precision: 0,
                style: "width: 100%",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
            description: {
              type: "string",
              title: "阶段说明",
              "x-decorator": "FormItem",
              "x-component": "Input.TextArea",
              "x-component-props": {
                placeholder: "阶段说明",
                autoSize: { minRows: 4, maxRows: 6 },
              },
              "x-decorator-props": { gridSpan: 2 },
            },
            retryable: {
              type: "boolean",
              title: "可重试",
              "x-decorator": "FormItem",
              "x-component": "Switch",
              "x-component-props": {
                checkedChildren: "启用",
                unCheckedChildren: "停用",
              },
              "x-decorator-props": { gridSpan: 1 },
            },
          },
        },
      },
    },
  },
}));

const openCreateWorkflowDialog = async () => {
  workflowModalMode.value = "create";
  page.resetForm();
  page.createWorkflowDraft();
  workflowModalVisible.value = true;
};

const openEditWorkflowDialog = async (
  row: (typeof tableData.value)[number],
) => {
  workflowModalMode.value = "edit";
  await page.loadDefinition(row);
  workflowModalVisible.value = true;
};

const closeWorkflowDialog = async () => {
  workflowModalVisible.value = false;
  page.resetForm();
};

const submitWorkflowDialog = async () => {
  try {
    await workflowCreateFormRef.value?.submit();
  } catch {
    return;
  }
  const values = workflowCreateFormRef.value?.getValues?.();
  if (!values) {
    message.error("未获取到工作流表单数据");
    return;
  }

  if (workflowModalMode.value === "create") {
    page.formState.stages = [];
  }

  page.formState.workflowCode = String(values.workflowCode ?? "").trim();
  page.formState.workflowName = String(values.workflowName ?? "").trim();
  page.formState.workflowVersionNo = Number(values.workflowVersionNo ?? 1);
  page.setBindingPlatformType(
    String(values.platformType ?? "")
      .trim()
      .toUpperCase() as EtlPlatformType,
  );
  page.formState.description = String(values.description ?? "").trim();
  page.formState.enabled = Boolean(values.enabled);
  page.formState.engineBinding.externalWorkflowId = String(
    values.externalWorkflowId ?? "",
  ).trim();
  page.formState.engineBinding.externalProjectCode = String(
    values.externalProjectCode ?? "",
  ).trim();
  page.formState.engineBinding.externalNamespace = String(
    values.externalNamespace ?? "",
  ).trim();
  page.formState.engineBinding.externalJobGroup = String(
    values.externalJobGroup ?? "",
  ).trim();
  page.formState.engineBinding.externalJobHandler = String(
    values.externalJobHandler ?? "",
  ).trim();
  page.formState.engineBinding.configJson = String(
    values.configJson ?? "",
  ).trim();
  page.formState.engineBinding.attributesText = String(
    values.attributesText ?? "{}",
  ).trim();

  const saved = await page.saveDefinition();
  if (saved) {
    workflowModalVisible.value = false;
  }
};

const openStageDialog = async (row: (typeof tableData.value)[number]) => {
  await page.loadDefinition(row);
  resetStageDraft((page.formState.stages?.length ?? 0) + 1);
  stageModalVisible.value = true;
};

const closeStageDialog = () => {
  stageModalVisible.value = false;
  resetStageDraft((page.formState.stages?.length ?? 0) + 1);
};

const submitStageDialog = async () => {
  try {
    await stageFormRef.value?.submit();
  } catch {
    return;
  }
  const values = stageFormRef.value?.getValues?.();
  if (!values) {
    message.error("未获取到阶段表单数据");
    return;
  }

  const insertIndex = page.formState.stages.length;
  page.addStageFromDraft({
    ...stageDraft,
    stageCode: String(values.stageCode ?? "").trim(),
    stageName: String(values.stageName ?? "").trim(),
    stageOrder: Number(values.stageOrder ?? stageDraft.stageOrder ?? 1),
    timeoutSeconds:
      values.timeoutSeconds === null || values.timeoutSeconds === undefined
        ? null
        : Number(values.timeoutSeconds),
    description: String(values.description ?? "").trim(),
    retryable: Boolean(values.retryable),
  });

  const saved = await page.saveDefinition();
  if (!saved) {
    page.removeStage(insertIndex);
    return;
  }

  closeStageDialog();
};
</script>

<template>
  <div class="etl-workflow-page">
    <YCard class="etl-workflow-table-card" :bordered="false" :padding="12">
      <div class="etl-workflow-panel-title">
        <div>
          <h3>{{ pageTitle }}</h3>
        </div>
      </div>

      <div class="etl-workflow-query-bar">
        <a-form layout="inline" class="etl-workflow-query-form">
          <a-form-item label="关键词">
            <a-input
              v-model:value="page.query.keyword"
              style="width: 240px"
              size="small"
              allow-clear
              placeholder="编码、名称、说明或平台摘要"
            />
          </a-form-item>
          <a-form-item label="执行平台">
            <a-select
              v-model:value="page.query.platformType"
              style="width: 180px"
              size="small"
              placeholder="全部平台"
              allow-clear
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option
                v-for="option in platformOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="page.query.enabled"
              style="width: 132px"
              placeholder="全部"
              size="small"
              allow-clear
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="true">启用</a-select-option>
              <a-select-option value="false">停用</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item class="etl-workflow-query-actions">
            <YButton size="small" type="primary" @click="page.applyQuery"
              >查询</YButton
            >
          </a-form-item>
        </a-form>
        <div class="etl-workflow-query-actions etl-workflow-page-actions">
          <YButton
            size="small"
            type="link"
            v-if="isDefinitionMode"
            @click="openCreateWorkflowDialog"
          >
            <template #icon><PlusOutlined /></template>
            新增工作流
          </YButton>
        </div>
      </div>

      <div ref="tableAreaRef" class="etl-workflow-table-body">
        <YTable
          :columns="columnsWithAction"
          :data="tableData"
          :loading="tableLoading"
          :max-height="tableHeight"
          :border="false"
          :pageable="true"
          :pagination="tablePagination"
          :toolbar-config="{ custom: false }"
          :row-config="{ keyField: 'workflowKey' }"
          @page-change="page.handlePageChange"
        >
        <template #statusLabel="{ row }">
          <a-tag :color="row.enabled === false ? 'red' : 'green'">
            {{ row.statusLabel }}
          </a-tag>
        </template>

        <template #syncStatusLabel="{ row }">
          <a-tooltip :title="row.syncFailureReason || row.bindingSummary">
            <a-tag :color="syncStatusColor(row.syncStatus)">
              {{ row.syncStatusLabel }}
            </a-tag>
          </a-tooltip>
        </template>

        <template #externalStateLabel="{ row }">
          <a-tag :color="externalStateColor(row)">
            {{ row.externalStateLabel }}
          </a-tag>
        </template>
        </YTable>
      </div>
    </YCard>

    <a-modal
      :open="workflowModalVisible"
      :title="workflowModalTitle"
      :width="'80vw'"
      :body-style="{ maxHeight: '80vh', overflowY: 'auto' }"
      centered
      :ok-text="workflowModalConfirmText"
      destroy-on-close
      @ok="submitWorkflowDialog"
      @cancel="closeWorkflowDialog"
    >
      <template
        v-if="workflowModalMode === 'create' || workflowModalMode === 'edit'"
      >
        <a-alert
          class="etl-workflow-binding-hint"
          size="small"
          type="info"
          show-icon
          :message="workflowModalHintText"
        />
        <YssFormily
          size="small"
          ref="workflowCreateFormRef"
          :initial-values="workflowCreateValues"
          :schema="workflowCreateSchema"
          :mode="workflowModalMode === 'create' ? 0 : 1"
        />
      </template>
    </a-modal>

    <a-modal
      :open="stageModalVisible"
      :title="stageModalTitle"
      :width="'50vw'"
      size="small"
      :style="{ top: '10vh' }"
      :body-style="{ maxHeight: '80vh', overflowY: 'auto' }"
      centered
      destroy-on-close
      @ok="submitStageDialog"
      @cancel="closeStageDialog"
    >
      <YssFormily
        ref="stageFormRef"
        size="small"
        :initial-values="stageFormValues"
        :schema="stageSchema"
        :mode="0"
      />
    </a-modal>
  </div>
</template>
