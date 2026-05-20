<script setup lang="ts">
import { computed, h, reactive, ref, unref } from "vue";
import { Modal, message } from "ant-design-vue";
import { ExclamationCircleOutlined } from "@ant-design/icons-vue";
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
import type {
  EtlWorkflowConfigPage,
  EtlWorkflowStageFormState,
} from "../types";

defineOptions({ name: "EtlWorkflowStageWorkspace" });

const { page } = defineProps<{
  page: EtlWorkflowConfigPage;
}>();

const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: true,
});

const workflowRows = computed(() => unref(page.tableData) ?? []);
const tableLoading = computed(() => Boolean(unref(page.loading)));
const tablePagination = computed(() => unref(page.pagination) ?? {});
const platformOptions = computed(() => unref(page.platformOptions) ?? []);

const stageModalVisible = ref(false);
const stageDialogMode = ref<"create" | "edit">("create");
const editingStageIndex = ref<number>(-1);
const editingStageBackup = ref<EtlWorkflowStageFormState | null>(null);
const stageFormRef = ref<{
  submit: () => Promise<unknown>;
  getValues?: () => Record<string, unknown>;
} | null>(null);

const createStageDraft = (stageOrder = 1): EtlWorkflowStageFormState => ({
  stageCode: "",
  stageName: "",
  stageOrder,
  description: "",
  retryable: true,
  timeoutSeconds: null as number | null,
  __rowKey: `stage-${Date.now()}-${Math.random().toString(16).slice(2)}`,
});

const stageDraft = reactive<EtlWorkflowStageFormState>(createStageDraft(1));

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
              "x-component-props": { placeholder: "阶段编码" },
              "x-decorator-props": { gridSpan: 1 },
            },
            stageName: {
              type: "string",
              title: "阶段名称",
              required: true,
              "x-decorator": "FormItem",
              "x-component": "Input",
              "x-component-props": { placeholder: "阶段名称" },
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

const workflowColumns = computed<YTableColumn[]>(() => [
  { type: "expand", width: 46, fixed: "left" as const },
  { field: "workflowCode", title: "工作流编码", width: 180, ellipsis: true },
  { field: "workflowName", title: "工作流名称", minWidth: 180, ellipsis: true },
  { field: "workflowVersionNo", title: "版本", width: 88 },
  { field: "platformLabel", title: "平台", width: 140 },
  { field: "stageCount", title: "阶段数", width: 88 },
  { field: "statusLabel", title: "状态", width: 88 },
  { field: "bindingSummary", title: "绑定摘要", minWidth: 120, ellipsis: true },
]);

const workflowActionConfig = computed<YTableActionConfig>(() => ({
  width: 140,
  fixed: "right",
  displayLimit: 3,
  moreRenderType: "ellipsis",
  buttons: [
    {
      text: "新增阶段",
      key: "addStage",
      type: "link",
      clickFn: ({ row }: { row: (typeof workflowRows.value)[number] }) =>
        openCreateStageDialog(row),
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

const stageColumns = computed<YTableColumn[]>(() => [
  {
    type: "seq" as const,
    title: "序号",
    width: 60,
    align: "center" as const,
    fixed: "left" as const,
  },
  { field: "stageCode", title: "阶段编码", width: 160, ellipsis: true },
  { field: "stageName", title: "阶段名称", minWidth: 160, ellipsis: true },
  { field: "stageOrder", title: "顺序", width: 80 },
  { field: "retryable", title: "可重试", width: 96 },
  { field: "timeoutSeconds", title: "超时(秒)", width: 96 },
  { field: "description", title: "阶段说明", minWidth: 260, ellipsis: true },
]);

const stageColumnsWithAction = computed<YTableColumn[]>(() => [
  ...stageColumns.value,
  {
    field: "action",
    title: "操作",
    width: 160,
    fixed: "right" as const,
  },
]);

const pageTitle = computed(() => "任务阶段定义");

const workflowGroupCount = computed(() => workflowRows.value.length);
const stageTotalCount = computed(() =>
  workflowRows.value.reduce(
    (total, row) => total + Number(row.stageCount ?? 0),
    0,
  ),
);

const stageCountLabel = computed(
  () =>
    `当前共有 ${workflowGroupCount.value} 个工作流分组，累计 ${stageTotalCount.value} 个阶段。`,
);

const applyStageDraft = (
  stage: Partial<EtlWorkflowStageFormState>,
  fallbackOrder = 1,
) => {
  Object.assign(stageDraft, createStageDraft(fallbackOrder), stage);
};

const openCreateStageDialog = async (
  row: (typeof workflowRows.value)[number] | null,
) => {
  const targetRow = row || workflowRows.value[0] || null;
  if (!targetRow) {
    message.warning("请先选择一个工作流");
    return;
  }
  stageDialogMode.value = "create";
  editingStageIndex.value = -1;
  editingStageBackup.value = null;
  await page.loadDefinition(targetRow);
  applyStageDraft({}, (page.formState.stages?.length ?? 0) + 1);
  stageModalVisible.value = true;
};

type WorkflowStageRow = EtlWorkflowStageFormState & { __stageIndex: number };

const openEditStageDialog = async (
  row: (typeof workflowRows.value)[number] | null,
  stageRow: WorkflowStageRow,
) => {
  const targetRow = row || workflowRows.value[0] || null;
  const stageIndex = Number(stageRow?.__stageIndex ?? -1);
  if (!targetRow) {
    message.warning("请先选择一个工作流");
    return;
  }
  if (stageIndex < 0) {
    message.warning("未找到要修改的阶段");
    return;
  }
  stageDialogMode.value = "edit";
  editingStageIndex.value = stageIndex;
  await page.loadDefinition(targetRow);
  const sourceStage = page.formState.stages[stageIndex];
  if (!sourceStage) {
    message.warning("未找到要修改的阶段");
    return;
  }
  editingStageBackup.value = { ...sourceStage };
  applyStageDraft(sourceStage, sourceStage.stageOrder ?? stageIndex + 1);
  stageModalVisible.value = true;
};

const confirmDeleteStage = async (
  row: (typeof workflowRows.value)[number] | null,
  stageRow: WorkflowStageRow,
) => {
  const targetRow = row || workflowRows.value[0] || null;
  const stageIndex = Number(stageRow?.__stageIndex ?? -1);
  const stageName = String(
    stageRow?.stageName ?? stageRow?.stageCode ?? "",
  ).trim();
  if (!targetRow) {
    message.warning("请先选择一个工作流");
    return;
  }
  if (stageIndex < 0) {
    message.warning("未找到要删除的阶段");
    return;
  }

  Modal.confirm({
    title: "删除任务阶段",
    content: `确认删除 ${stageName || "该阶段"} 吗？`,
    icon: h(ExclamationCircleOutlined),
    okText: "删除",
    okButtonProps: { danger: true },
    cancelText: "取消",
    onOk: async () => {
      await page.loadDefinition(targetRow);
      const removedStage = page.formState.stages[stageIndex];
      if (!removedStage) {
        message.warning("未找到要删除的阶段");
        return;
      }
      page.removeStage(stageIndex);
      const saved = await page.saveDefinition();
      if (!saved) {
        page.formState.stages.splice(stageIndex, 0, removedStage);
        return;
      }
    },
  });
};

const closeStageDialog = () => {
  stageModalVisible.value = false;
  stageDialogMode.value = "create";
  editingStageIndex.value = -1;
  editingStageBackup.value = null;
  applyStageDraft({}, (page.formState.stages?.length ?? 0) + 1);
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

  const stageValues = {
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
  };

  if (stageDialogMode.value === "create") {
    const insertIndex = page.formState.stages.length;
    page.addStageFromDraft(stageValues);
    const saved = await page.saveDefinition();
    if (!saved) {
      page.removeStage(insertIndex);
      return;
    }
    closeStageDialog();
    return;
  }

  const editIndex = editingStageIndex.value;
  const backup = editingStageBackup.value;
  if (editIndex < 0 || !backup) {
    message.error("未找到要修改的阶段");
    return;
  }

  page.formState.stages.splice(editIndex, 1, {
    ...backup,
    ...stageValues,
    __rowKey: backup.__rowKey,
  });

  const saved = await page.saveDefinition();
  if (!saved) {
    page.formState.stages.splice(editIndex, 1, backup);
    return;
  }

  closeStageDialog();
};

const expandStageRows = (
  row: (typeof workflowRows.value)[number],
): WorkflowStageRow[] =>
  (row.stages ?? []).map((stage, index) => ({
    ...stage,
    __rowKey: `${row.workflowKey}-${stage.stageCode || stage.stageName || index}`,
    __stageIndex: index,
  }));
const handleDragEnd = (rows: any[]) => {
  // eslint-disable-next-line no-console
  console.log(
    "拖拽后顺序：",
    rows.map((r) => r.name),
  );
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
            <YButton size="small" type="primary" @click="page.applyQuery">
              查询
            </YButton>
          </a-form-item>
        </a-form>
        <div class="etl-workflow-query-meta">{{ stageCountLabel }}</div>
      </div>

      <div ref="tableAreaRef" class="etl-workflow-table-body">
        <YTable
          :columns="columnsWithAction"
          :data="workflowRows"
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

        <template #expand-row="{ row: workflowRow }">
          <div class="etl-workflow-stage-expand">
            <div class="etl-workflow-stage-expand__header">
              <strong
                >{{ workflowRow.workflowCode }} ·
                {{ workflowRow.workflowName }}</strong
              >
              <span
                >{{ workflowRow.workflowVersionNo }} ·
                {{ workflowRow.bindingSummary }}</span
              >
            </div>
            <YTable
              class="etl-workflow-stage-table"
              :columns="stageColumnsWithAction"
              :data="expandStageRows(workflowRow)"
              :row-dragable="true"
              :row-config="{ drag: true }"
              @row-dragend="handleDragEnd"
              :pageable="false"
              :border="false"
            >
              <template #retryable="{ row: stageRow }">
                <a-tag :color="stageRow.retryable ? 'green' : 'default'">
                  {{ stageRow.retryable ? "是" : "否" }}
                </a-tag>
              </template>
              <template #timeoutSeconds="{ row: stageRow }">
                {{ stageRow.timeoutSeconds ?? "-" }}
              </template>
              <template #description="{ row: stageRow }">
                {{ stageRow.description || "-" }}
              </template>
              <template #action="{ row: stageRow }">
                <YButton
                  size="small"
                  type="link"
                  @click="openEditStageDialog(workflowRow, stageRow)"
                >
                  修改
                </YButton>
                <YButton
                  size="small"
                  type="link"
                  danger
                  @click="confirmDeleteStage(workflowRow, stageRow)"
                >
                  删除
                </YButton>
              </template>
            </YTable>
          </div>
        </template>
        </YTable>
      </div>
    </YCard>

    <a-modal
      :open="stageModalVisible"
      :title="
        stageDialogMode === 'create'
          ? `新增阶段 - ${page.formState.workflowCode || '未选择'}`
          : `修改阶段 - ${page.formState.workflowCode || '未选择'}`
      "
      :width="'50vw'"
      :style="{ top: '10vh' }"
      :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
      centered
      :confirm-loading="Boolean(unref(page.saving))"
      destroy-on-close
      @ok="submitStageDialog"
      @cancel="closeStageDialog"
    >
      <YssFormily
        size="small"
        ref="stageFormRef"
        :initial-values="stageFormValues"
        :schema="stageSchema"
        :mode="0"
      />
    </a-modal>
  </div>
</template>
