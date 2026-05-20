<script setup lang="ts">
import { computed, h, ref } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { Modal } from "ant-design-vue";
import { ExclamationCircleOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import {
  YButton,
  YCard,
  YTable,
  type YTableColumn,
} from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import type {
  WorkflowTaskInstancePage,
  WorkflowTaskInstanceRow,
} from "../types";

defineOptions({ name: "WorkflowTaskInstanceWorkspace" });

const { page } = defineProps<{
  page: WorkflowTaskInstancePage;
}>();

const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: true,
});

const TIME_RANGE_FORMAT = "YYYY-MM-DD HH:mm:ss";

const statusOptions = [
  { value: "", label: "全部状态" },
  { value: "SUBMITTED_SUCCESS", label: "已提交" },
  { value: "RUNNING_EXECUTION", label: "运行中" },
  { value: "PAUSE", label: "暂停" },
  { value: "FAILURE", label: "失败" },
  { value: "SUCCESS", label: "成功" },
];

const parseDateTime = (value?: string | null) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return null;
  }
  const parsed = dayjs(text);
  return parsed.isValid() ? parsed : null;
};

const queryTimeRange = computed<[Dayjs, Dayjs] | undefined>({
  get: () => {
    const startTime = parseDateTime(page.query.startTimeFrom);
    const endTime = parseDateTime(page.query.endTimeTo);
    if (!startTime || !endTime) {
      return undefined;
    }
    return [startTime, endTime] as [Dayjs, Dayjs];
  },
  set: (value: [Dayjs, Dayjs] | undefined) => {
    if (!value || value.length !== 2) {
      page.query.startTimeFrom = "";
      page.query.endTimeTo = "";
      return;
    }
    page.query.startTimeFrom = value[0].format(TIME_RANGE_FORMAT);
    page.query.endTimeTo = value[1].format(TIME_RANGE_FORMAT);
  },
});

const columns = computed<YTableColumn[]>(() => [
  {
    field: "taskInstanceId",
    title: "任务实例 ID",
    width: 120,
    align: "center",
  },
  { field: "taskName", title: "任务名称", minWidth: 200, ellipsis: true },
  {
    field: "workflowInstanceLabel",
    title: "所属工作流实例",
    minWidth: 240,
    ellipsis: true,
  },
  { field: "taskTypeLabel", title: "任务类型", width: 140, ellipsis: true },
  { field: "status", title: "状态", width: 110 },
  {
    field: "hostLabel",
    title: "执行主机",
    minWidth: 180,
    ellipsis: true,
  },
  { field: "startTimeLabel", title: "开始时间", width: 170 },
  { field: "endTimeLabel", title: "结束时间", width: 170 },
]);

const confirmForceSuccess = (row: WorkflowTaskInstanceRow) => {
  Modal.confirm({
    title: "强制成功任务实例",
    content: `确认将任务实例 ${row.taskInstanceId} 标记为强制成功吗？此操作仅对失败、终止或容错中的任务实例有效。`,
    icon: h(ExclamationCircleOutlined),
    okText: "强制成功",
    cancelText: "取消",
    okButtonProps: {
      danger: true,
      loading: page.actionLoading,
    },
    onOk: () => page.forceSuccessTask(row),
  });
};

const actionConfig = useTableActionConfig({
  width: 260,
  fixed: "right",
  displayLimit: 3,
  moreRenderType: "ellipsis",
  buttons: [
    {
      text: "强制成功",
      key: "forceSuccess",
      type: "link",
      hideFn: ({ row }: { row: WorkflowTaskInstanceRow }) =>
        !["FAILURE", "NEED_FAULT_TOLERANCE", "KILL"].includes(
          String(row.state ?? "").trim().toUpperCase(),
        ),
      clickFn: ({ row }: { row: WorkflowTaskInstanceRow }) =>
        confirmForceSuccess(row),
    },
    {
      text: "查看日志",
      key: "viewLog",
      type: "link",
      clickFn: ({ row }: { row: WorkflowTaskInstanceRow }) =>
        page.openTaskLog(row),
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

</script>

<template>
  <div class="workflow-task-instance-page">
    <YCard class="workflow-task-instance-header" :bordered="false" :padding="12">
      <div class="workflow-task-instance-header__top">
        <div class="workflow-task-instance-header__copy">
          <h2>任务实例</h2>
          <p>
            按工作流定义查看 DolphinScheduler 任务实例，支持按任务名称、阶段实例名称、状态和开始结束时间筛选，并可强制成功或查看日志。
          </p>
        </div>
        <div class="workflow-task-instance-header__actions">
          <a-select
            v-model:value="page.query.workflowCode"
            class="workflow-task-instance-workflow-select"
            size="small"
            :options="page.workflowOptions"
            placeholder="选择工作流定义"
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

      <div class="workflow-task-instance-stat-area">
        <a-spin :spinning="page.taskStateLoading">
          <div v-if="page.taskStateCards.length" class="workflow-task-instance-stat-grid">
            <div
              v-for="card in page.taskStateCards"
              :key="card.statKey"
              :class="[
                'workflow-task-instance-stat-card',
                `workflow-task-instance-stat-card--${card.tone}`,
                {
                  'is-active':
                    (card.statKey === 'TOTAL' && !String(page.query.status ?? '').trim()) ||
                    String(page.query.status ?? '').trim().toUpperCase() === card.statKey,
                },
              ]"
              role="button"
              tabindex="0"
              :aria-pressed="
                card.statKey !== 'TOTAL' && String(page.query.status ?? '').trim().toUpperCase() === card.statKey
              "
              :aria-label="`按${card.label}筛选任务实例`"
              @click="page.handleStatusSelect(card.statKey === 'TOTAL' ? '' : card.statKey)"
              @keydown.enter.prevent="page.handleStatusSelect(card.statKey === 'TOTAL' ? '' : card.statKey)"
              @keydown.space.prevent="page.handleStatusSelect(card.statKey === 'TOTAL' ? '' : card.statKey)"
            >
              <div class="workflow-task-instance-stat-card__head">
                <div class="workflow-task-instance-stat-label">
                  {{ card.label }}
                </div>
                <div class="workflow-task-instance-stat-value">
                  {{ card.value }}
                </div>
              </div>
              <div class="workflow-task-instance-stat-desc">
                {{ card.desc }}
              </div>
            </div>
          </div>
        </a-spin>
      </div>

      <div v-if="page.loading && !page.stageRows.length" class="workflow-task-instance-stage-chain workflow-task-instance-stage-chain--skeleton">
        <div
          v-for="item in 4"
          :key="item"
          class="workflow-task-instance-stage workflow-task-instance-stage--skeleton"
        >
          <span class="workflow-task-instance-skeleton workflow-task-instance-skeleton--pill"></span>
          <span class="workflow-task-instance-skeleton workflow-task-instance-skeleton--title"></span>
          <span class="workflow-task-instance-skeleton workflow-task-instance-skeleton--desc"></span>
        </div>
      </div>

      <div v-else-if="page.stageRows.length" class="workflow-task-instance-stage-chain">
        <div
          v-for="(stage, index) in page.stageRows"
          :key="`${stage.stageCode || index}-${stage.stageOrder ?? index}`"
          class="workflow-task-instance-stage"
          :class="{
            'is-active':
              String(page.query.taskName ?? '').trim() ===
              String(stage.stageName || stage.stageCode || '').trim(),
          }"
          @click="page.selectStageName(stage.stageName || stage.stageCode || '')"
        >
          <div class="workflow-task-instance-stage__main">
            <span class="workflow-task-instance-stage__index">{{ index + 1 }}</span>
            <strong class="workflow-task-instance-stage__title">{{ stage.stageName || stage.stageCode || "未命名阶段" }}</strong>
          </div>
          <div class="workflow-task-instance-stage__desc">
            {{ stage.description || "阶段定义未填写描述" }}
          </div>
        </div>
      </div>

    </YCard>

    <YCard class="workflow-task-instance-filter" :bordered="false" :padding="12">
      <a-form layout="inline" class="workflow-task-instance-filter__form">
        <a-form-item label="任务名称">
          <a-input
            v-model:value="page.query.taskName"
            style="width: 180px"
            size="small"
            allow-clear
            placeholder="输入任务名称"
          />
        </a-form-item>
        <a-form-item label="阶段实例名称">
          <a-input
            v-model:value="page.query.workflowInstanceName"
            style="width: 220px"
            size="small"
            allow-clear
            placeholder="输入阶段实例名称"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="page.query.status"
            size="small"
            style="width: 150px"
            placeholder="选择状态"
            allow-clear
            @change="page.handleStatusSelect"
          >
            <a-select-option
              v-for="item in statusOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker
            v-model:value="queryTimeRange"
            size="small"
            style="width: 360px"
            allow-clear
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            :placeholder="['开始时间', '结束时间']"
          />
        </a-form-item>
        <a-form-item class="workflow-task-instance-filter__actions">
          <YButton size="small" type="primary" @click="page.runQuery">查询</YButton>
          <YButton size="small" @click="page.resetQuery">重置</YButton>
        </a-form-item>
      </a-form>
      <div class="workflow-task-instance-filter__tag">
        <a-tag color="blue">{{ page.currentFilterSummary }}</a-tag>
      </div>
    </YCard>

    <div ref="tableAreaRef" class="workflow-task-instance-table">
      <YTable
        :columns="columnsWithAction"
        :data="page.tableData"
        :loading="page.listLoading || page.loading || page.actionLoading"
        :max-height="tableHeight"
        :row-config="{ keyField: 'taskKey' }"
        :pageable="true"
        :autoFlexColumn="false"
        :border="false"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #status="{ row }">
          <a-tag :color="row.statusColor">
            {{ row.statusLabel }}
          </a-tag>
        </template>
      </YTable>
    </div>

    <a-modal
      class="workflow-task-instance-log-modal"
      :open="page.logVisible"
      :title="page.logTitle"
      :footer="null"
      :width="980"
      :body-style="{ padding: '12px 14px 14px' }"
      @cancel="page.closeTaskLog"
    >
      <a-spin :spinning="page.logLoading">
        <pre class="workflow-task-instance-log-pre">{{ page.logContent }}</pre>
      </a-spin>
    </a-modal>
  </div>
</template>
