<script setup lang="ts">
import { computed, h, ref } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { Modal } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  ReloadOutlined,
} from "@ant-design/icons-vue";
import {
  YButton,
  YCard,
  YTable,
  type YTableActionConfig,
  type YTableColumn,
} from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import type {
  WorkflowInstancePage,
  WorkflowInstanceRow,
  WorkflowTaskRow,
} from "../types";

defineOptions({ name: "WorkflowInstanceWorkspace" });

const { page } = defineProps<{
  page: WorkflowInstancePage;
}>();

const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: true,
});

const TIME_RANGE_FORMAT = "YYYY-MM-DD HH:mm:ss";

const parseTriggerTime = (value?: string | null) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return null;
  }
  const parsed = dayjs(text);
  return parsed.isValid() ? parsed : null;
};

const triggerTimeRange = computed<[Dayjs, Dayjs] | undefined>({
  get: () => {
    const triggerTimeFrom = parseTriggerTime(page.query.triggerTimeFrom);
    const triggerTimeTo = parseTriggerTime(page.query.triggerTimeTo);
    if (!triggerTimeFrom || !triggerTimeTo) {
      return undefined;
    }
    return [triggerTimeFrom, triggerTimeTo] as [Dayjs, Dayjs];
  },
  set: (value: [Dayjs, Dayjs] | undefined) => {
    if (!value || value.length !== 2) {
      page.query.triggerTimeFrom = "";
      page.query.triggerTimeTo = "";
      return;
    }
    page.query.triggerTimeFrom = value[0].format(TIME_RANGE_FORMAT);
    page.query.triggerTimeTo = value[1].format(TIME_RANGE_FORMAT);
  },
});

const columns = computed<YTableColumn[]>(() => [
  { type: "expand", width: 46, fixed: "left" as const },
  { field: "workflowName", title: "工作流名称", minWidth: 200, ellipsis: true },
  { field: "workflowCode", title: "工作流编码", width: 180, ellipsis: true },
  {
    field: "workflowVersionNo",
    title: "版本",
    width: 86,
    align: "center",
  },
  { field: "platformLabel", title: "平台", width: 140 },
  { field: "produceCodes", title: "产品组合", minWidth: 160, ellipsis: true },
  { field: "status", title: "状态", width: 110 },
  {
    field: "startTime",
    title: "开始时间",
    width: 170,
  },
  { field: "duration", title: "运行时长", width: 120 },
  { field: "stageCount", title: "阶段数", width: 88, align: "center" },
]);

const taskColumns = computed<YTableColumn[]>(() => [
  {
    field: "id",
    title: "任务 ID",
    width: 90,
    align: "center",
  },
  { field: "name", title: "任务名称", minWidth: 180, ellipsis: true },
  {
    field: "workflowInstanceName",
    title: "阶段实例名称",
    minWidth: 220,
    ellipsis: true,
  },
  { field: "taskType", title: "任务类型", width: 140, ellipsis: true },
  { field: "state", title: "状态", width: 110 },
  { field: "host", title: "执行主机", minWidth: 160, ellipsis: true },
  { field: "startTime", title: "开始时间", width: 170 },
  { field: "endTime", title: "结束时间", width: 170 },
]);

const stageLogColumns = computed<YTableColumn[]>(() => [
  {
    field: "stageOrder",
    title: "顺序",
    width: 72,
    align: "center",
  },
  { field: "stageName", title: "阶段名称", minWidth: 160, ellipsis: true },
  { field: "stageCode", title: "阶段编码", minWidth: 150, ellipsis: true },
  { field: "status", title: "状态", width: 110 },
  { field: "rawStatus", title: "原始状态", minWidth: 130, ellipsis: true },
  { field: "startTime", title: "开始时间", width: 170 },
  { field: "endTime", title: "结束时间", width: 170 },
  { field: "message", title: "消息", minWidth: 260, ellipsis: true },
]);

const formatStatusColor = (value?: string) => {
  const status = String(value ?? "")
    .trim()
    .toUpperCase();
  if (status === "SUCCEEDED" || status === "SUCCESS") return "green";
  if (
    status === "RUNNING" ||
    status === "RUNNING_EXECUTION" ||
    status === "SUBMITTED"
  )
    return "processing";
  if (status === "FAILED") return "red";
  if (status === "STOPPED") return "orange";
  if (status === "RETRYING") return "gold";
  return "default";
};

const openActionConfirm = (
  title: string,
  content: string,
  onOk: () => Promise<void> | void,
  danger = false,
) => {
  Modal.confirm({
    title,
    content,
    icon: h(ExclamationCircleOutlined),
    okText: "确定",
    cancelText: "取消",
    okButtonProps: { danger, loading: page.actionLoading },
    onOk,
  });
};

const confirmRerun = (row: WorkflowInstanceRow) => {
  openActionConfirm(
    "重跑工作流实例",
    `将对 ${row.instanceId} 执行重跑，是否继续？`,
    () => page.rerunInstance(row),
  );
};

const confirmRerunFailedTasks = (row: WorkflowInstanceRow) => {
  openActionConfirm(
    "重跑失败任务",
    `将对 ${row.instanceId} 执行失败任务重跑，是否继续？`,
    () => page.rerunFailedTasks(row),
  );
};

const confirmStop = (row: WorkflowInstanceRow) => {
  openActionConfirm(
    "停止工作流实例",
    `将停止 ${row.instanceId}，是否继续？`,
    () => page.stopInstance(row),
    true,
  );
};

const confirmPause = (row: WorkflowInstanceRow) => {
  openActionConfirm(
    "暂停工作流实例",
    `将暂停 ${row.instanceId}，是否继续？`,
    () => page.pauseInstance(row),
  );
};

const confirmResume = (row: WorkflowInstanceRow) => {
  openActionConfirm(
    "恢复运行工作流实例",
    `将恢复 ${row.instanceId} 的运行，是否继续？`,
    () => page.resumeInstance(row),
  );
};

const actionConfig = useTableActionConfig({
  width: 340,
  fixed: "right",
  displayLimit: 3,
  moreRenderType: "ellipsis",
  buttons: [
    {
      text: "重跑",
      key: "rerun",
      type: "link",
      hideFn: ({ row }: { row: WorkflowInstanceRow }) =>
        !page.canRerunInstance(row),
      clickFn: ({ row }: { row: WorkflowInstanceRow }) => confirmRerun(row),
    },
    {
      text: "重跑失败任务",
      key: "rerunFailedTasks",
      type: "link",
      hideFn: ({ row }: { row: WorkflowInstanceRow }) =>
        !page.canRerunFailedTasks(row),
      clickFn: ({ row }: { row: WorkflowInstanceRow }) =>
        confirmRerunFailedTasks(row),
    },
    {
      text: "停止",
      key: "stop",
      type: "link",
      hideFn: ({ row }: { row: WorkflowInstanceRow }) =>
        !page.canStopInstance(row),
      clickFn: ({ row }: { row: WorkflowInstanceRow }) => confirmStop(row),
    },
    {
      text: "暂停",
      key: "pause",
      type: "link",
      hideFn: ({ row }: { row: WorkflowInstanceRow }) =>
        !page.canPauseInstance(row),
      clickFn: ({ row }: { row: WorkflowInstanceRow }) => confirmPause(row),
    },
    {
      text: "恢复运行",
      key: "resume",
      type: "link",
      hideFn: ({ row }: { row: WorkflowInstanceRow }) =>
        !page.canResumeInstance(row),
      clickFn: ({ row }: { row: WorkflowInstanceRow }) => confirmResume(row),
    },
  ],
});

const createTaskActionConfig = (
  workflowRow: WorkflowInstanceRow,
): YTableActionConfig => ({
  width: 260,
  fixed: "right",
  displayLimit: 3,
  moreRenderType: "ellipsis",
  buttons: [
    {
      text: "查看任务实例",
      key: "viewTaskInstance",
      type: "link",
      clickFn: ({ row }: { row: WorkflowTaskRow }) =>
        page.openTaskInstancePage(workflowRow, row),
    },
    {
      text: "查看日志",
      key: "viewLog",
      type: "link",
      clickFn: ({ row }: { row: WorkflowTaskRow }) =>
        page.openTaskLog(workflowRow, row),
    },
  ],
});

const columnsWithAction = computed<YTableColumn[]>(() => [
  ...columns.value,
  {
    type: "action" as const,
    title: "操作",
    align: "center" as const,
    actionConfig: actionConfig.value,
  },
]);

const summaryDescription = computed(
  () =>
    `当前筛选：${page.currentFilterSummary}。总数 ${page.total} 条；本页统计：运行中 ${page.runningCount} 条，成功 ${page.succeededCount} 条，失败 ${page.failedCount} 条，已提交 ${page.submittedCount} 条。`,
);

const refreshLogs = () =>
  page.refreshDetailLogs(page.selectedLogStageCode || undefined);

const stageFilterOptions = computed(() => [
  { label: "全部阶段", value: "" },
  ...page.stageOptions,
]);
</script>

<template>
  <div class="workflow-instance-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="10">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>工作流实例</h2>
          <p>查看工作流实例的运行状态、任务实例、日志等信息。</p>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-controls">
            <a-select
              v-model:value="page.query.workflowCode"
              class="workspace-header-workflow-select"
              size="small"
              :options="page.workflowOptions"
              placeholder="选择工作流"
              show-search
              option-filter-prop="label"
              @change="page.handleWorkflowCodeSelect"
            />
            <YButton
              size="small"
              :loading="page.listLoading || page.loading"
              @click="page.refreshList"
            >
              <template #icon><ReloadOutlined /></template>
              刷新列表
            </YButton>
          </div>
        </div>
      </div>

      <div class="workflow-instance-stat-area">
        <a-spin :spinning="page.workflowStateLoading">
          <div
            v-if="page.workflowStateCards.length"
            class="workflow-instance-stat-grid"
          >
            <div
              v-for="card in page.workflowStateCards"
              :key="card.statKey"
              :class="[
                'workflow-instance-stat-card',
                `workflow-instance-stat-card--${card.tone}`,
                {
                  'is-active':
                    (card.statKey === 'TOTAL' &&
                      !String(page.query.status ?? '').trim()) ||
                    String(page.query.status ?? '')
                      .trim()
                      .toUpperCase() === card.statKey,
                },
              ]"
              role="button"
              tabindex="0"
              :aria-pressed="
                (card.statKey === 'TOTAL' &&
                  !String(page.query.status ?? '').trim()) ||
                String(page.query.status ?? '')
                  .trim()
                  .toUpperCase() === card.statKey
              "
              :aria-label="`按${card.label}筛选工作流实例`"
              @click="
                page.handleStatusSelect(
                  card.statKey === 'TOTAL' ? '' : card.statKey,
                )
              "
              @keydown.enter.prevent="
                page.handleStatusSelect(
                  card.statKey === 'TOTAL' ? '' : card.statKey,
                )
              "
              @keydown.space.prevent="
                page.handleStatusSelect(
                  card.statKey === 'TOTAL' ? '' : card.statKey,
                )
              "
            >
              <div class="workflow-instance-stat-card__head">
                <div class="workflow-instance-stat-label">
                  {{ card.label }}
                </div>
                <div class="workflow-instance-stat-value">
                  {{ card.value }}
                </div>
              </div>
              <div class="workflow-instance-stat-desc">
                {{ card.desc }}
              </div>
            </div>
          </div>
        </a-spin>
      </div>
    </YCard>

    <div ref="tableAreaRef" class="workspace-body">
      <div class="workflow-instance-query-bar">
        <a-form layout="inline" class="workflow-instance-query-form">
          <a-form-item label="名称">
            <a-input
              v-model:value="page.query.workflowName"
              style="width: 180px"
              size="small"
              allow-clear
              placeholder="输入名称"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              size="small"
              v-model:value="page.query.status"
              style="width: 140px"
              :options="page.statusOptions"
              placeholder="选择状态"
              allow-clear
              @change="page.handleStatusSelect"
            />
          </a-form-item>
          <a-form-item label="时间范围">
            <a-range-picker
              v-model:value="triggerTimeRange"
              size="small"
              style="width: 360px"
              allow-clear
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              :placeholder="['开始时间', '结束时间']"
            />
          </a-form-item>
          <a-form-item class="workflow-instance-query-actions">
            <YButton size="small" type="primary" @click="page.runQuery">
              查询
            </YButton>
            <YButton size="small" @click="page.resetQuery">重置</YButton>
          </a-form-item>
        </a-form>
      </div>

      <YTable
        :columns="columnsWithAction"
        :border="false"
        :data="page.tableData"
        :loading="page.listLoading || page.loading || page.actionLoading"
        :max-height="tableHeight"
        :row-config="{ keyField: 'instanceId' }"
        :expand-config="{ expandRowKeys: page.expandedRowKeys, trigger: 'row' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        :autoFlexColumn="false"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
        @toggle-row-expand="page.handleToggleRowExpand"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="实例列表"
            :description="summaryDescription"
            :meta="`当前工作流：${page.query.workflowCode || '-'} · 当前筛选：${page.currentFilterSummary} · 当前页 ${page.tableData.length} 条`"
          />
        </template>

        <template #status="{ row }">
          <a-tag :color="formatStatusColor(row.status)">
            {{ page.formatStatusLabel(row.status) }}
          </a-tag>
        </template>

        <template #platformLabel="{ row }">
          <a-tag color="cyan">
            {{ row.platformLabel }}
          </a-tag>
        </template>

        <template #currentStageName="{ row }">
          <span>{{ row.currentStageName || row.currentStageCode || "-" }}</span>
        </template>

        <template #triggerTime="{ row }">
          <span>{{ row.triggerTime || "-" }}</span>
        </template>

        <template #expand-row="{ row }">
          <div class="workflow-instance-expand">
            <div class="workflow-instance-expand__header">
              <strong>{{ row.workflowLabel }} · {{ row.instanceId }}</strong>
              <span>
                {{ row.platformLabel }} · {{ row.statusLabel }} ·
                {{ row.currentStageName || row.currentStageCode || "-" }}
              </span>
              <span>任务实例 {{ page.getTaskRows(row).length }} 条</span>
            </div>
            <YTable
              v-if="page.getTaskRows(row).length"
              class="workflow-instance-task-table"
              :columns="[
                ...taskColumns,
                {
                  type: 'action',
                  title: '操作',
                  align: 'center',
                  actionConfig: createTaskActionConfig(row),
                },
              ]"
              :data="page.getTaskRows(row)"
              :border="false"
              :pageable="false"
              :autoFlexColumn="false"
              :row-config="{ keyField: 'taskKey' }"
            >
              <template #state="{ row: taskRow }">
                <a-tag :color="formatStatusColor(taskRow.state)">
                  {{ taskRow.stateLabel }}
                </a-tag>
              </template>
            </YTable>
            <div v-else class="workflow-instance-task-empty">
              当前实例暂无任务实例明细。
            </div>
          </div>
        </template>
      </YTable>
    </div>

    <a-drawer
      class="workflow-instance-detail-drawer"
      :open="page.detailVisible"
      title="工作流实例详情"
      :width="1160"
      :destroyOnClose="true"
      :bodyStyle="{ padding: '12px 14px 14px' }"
      @close="page.closeDetailDrawer"
    >
      <template v-if="page.selectedRow">
        <div class="workflow-instance-detail-layout">
          <div class="workflow-instance-detail-main">
            <div class="workflow-instance-detail-banner">
              <div class="workflow-instance-detail-banner-title">
                {{ page.selectedRow.workflowLabel }} ·
                {{ page.selectedRow.instanceId }}
              </div>
              <div class="workflow-instance-detail-banner-meta">
                {{ page.formatStatusLabel(page.selectedRow.status) }} ·
                {{
                  page.selectedRow.currentStageName ||
                  page.selectedRow.currentStageCode ||
                  "-"
                }}
                · {{ page.selectedRow.triggerTime || "-" }}
              </div>
            </div>

            <div class="workflow-instance-detail-actions">
              <YButton
                v-if="page.canRerunInstance(page.selectedRow)"
                size="small"
                :loading="page.actionLoading"
                @click="confirmRerun(page.selectedRow)"
              >
                重跑
              </YButton>
              <YButton
                v-if="page.canRerunFailedTasks(page.selectedRow)"
                size="small"
                :loading="page.actionLoading"
                @click="confirmRerunFailedTasks(page.selectedRow)"
              >
                重跑失败任务
              </YButton>
              <YButton
                v-if="page.canStopInstance(page.selectedRow)"
                size="small"
                :loading="page.actionLoading"
                danger
                @click="confirmStop(page.selectedRow)"
              >
                停止
              </YButton>
              <YButton
                v-if="page.canPauseInstance(page.selectedRow)"
                size="small"
                :loading="page.actionLoading"
                @click="confirmPause(page.selectedRow)"
              >
                暂停
              </YButton>
              <YButton
                v-if="page.canResumeInstance(page.selectedRow)"
                size="small"
                :loading="page.actionLoading"
                @click="confirmResume(page.selectedRow)"
              >
                恢复运行
              </YButton>
              <YButton
                size="small"
                :loading="page.detailLoading"
                @click="refreshLogs"
              >
                刷新日志
              </YButton>
            </div>

            <a-descriptions
              class="workflow-instance-detail-descriptions"
              bordered
              :column="2"
              size="small"
            >
              <a-descriptions-item label="工作流编码">
                {{ page.selectedRow.workflowCode }}
              </a-descriptions-item>
              <a-descriptions-item label="工作流名称">
                {{ page.selectedRow.workflowName }}
              </a-descriptions-item>
              <a-descriptions-item label="版本号">
                {{ page.selectedRow.workflowVersionNo }}
              </a-descriptions-item>
              <a-descriptions-item label="平台">
                {{ page.selectedRow.platformLabel }}
              </a-descriptions-item>
              <a-descriptions-item label="实例 ID">
                {{ page.selectedRow.instanceId }}
              </a-descriptions-item>
              <a-descriptions-item label="业务键">
                {{ page.selectedRow.businessKey || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="外部实例 ID">
                {{ page.selectedRow.externalInstanceId || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="外部工作流 ID">
                {{ page.selectedRow.externalWorkflowId || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="当前阶段">
                {{
                  page.selectedRow.currentStageName ||
                  page.selectedRow.currentStageCode ||
                  "-"
                }}
              </a-descriptions-item>
              <a-descriptions-item label="当前阶段编码">
                {{ page.selectedRow.currentStageCode || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                {{ page.formatStatusLabel(page.selectedRow.status) }}
              </a-descriptions-item>
              <a-descriptions-item label="原始状态">
                {{ page.selectedRow.rawStatus || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="触发时间">
                {{ page.selectedRow.triggerTime || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="开始时间">
                {{ page.selectedRow.startTime || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="结束时间">
                {{ page.selectedRow.endTime || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="阶段数量">
                {{ page.selectedRow.stageCount }}
              </a-descriptions-item>
              <a-descriptions-item label="消息" :span="2">
                {{ page.selectedRow.message || "-" }}
              </a-descriptions-item>
            </a-descriptions>

            <div class="workflow-instance-json-block">
              <h4>Context</h4>
              <pre>{{ page.formatJson(page.selectedRow.context) }}</pre>
            </div>
          </div>

          <div class="workflow-instance-detail-side">
            <div class="workflow-instance-log-shell">
              <div class="workflow-instance-log-header">
                <div>
                  <h4>阶段日志</h4>
                  <p>可按 stageCode 过滤当前实例的日志。</p>
                </div>
                <a-select
                  size="small"
                  v-model:value="page.selectedLogStageCode"
                  style="width: 240px"
                  :options="stageFilterOptions"
                  placeholder="选择阶段过滤"
                  allow-clear
                  show-search
                  @change="refreshLogs"
                />
              </div>

              <YTable
                class="workflow-instance-log-table"
                :columns="stageLogColumns"
                :border="false"
                :data="page.selectedLogs"
                :loading="page.detailLoading"
                :row-config="{ keyField: 'logKey' }"
                :pageable="false"
                :autoFlexColumn="false"
                :toolbar-config="{ custom: false }"
              >
                <template #status="{ row }">
                  <a-tag :color="formatStatusColor(row.status)">
                    {{ page.formatStatusLabel(row.status) }}
                  </a-tag>
                </template>
              </YTable>
            </div>
          </div>
        </div>
      </template>
    </a-drawer>

    <a-modal
      class="workflow-instance-task-log-modal"
      :open="page.taskLogVisible"
      :title="page.taskLogTitle"
      :footer="null"
      :width="960"
      :bodyStyle="{ padding: '12px 14px 14px' }"
      @cancel="page.closeTaskLog"
    >
      <a-spin :spinning="page.taskLogLoading">
        <pre class="workflow-instance-task-log-pre">{{
          page.taskLogContent
        }}</pre>
      </a-spin>
    </a-modal>
  </div>
</template>
