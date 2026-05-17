<script setup lang="ts">
import { computed, h, ref, watch } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { Modal } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard, YTable, type YTableColumn } from "@yss-ui/components";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import {
  springBatchValuationTaskPageText,
  springBatchValuationTaskStageCatalog,
  springBatchValuationTaskStatusCatalog,
  resolveSpringBatchValuationTaskSourceTypeLabel,
} from "../constants";
import type {
  SpringBatchValuationTaskBatchRow,
  SpringBatchValuationTaskPageState,
} from "../types";

const { page } = defineProps<{
  page: SpringBatchValuationTaskPageState;
}>();
const yTableRef = ref<InstanceType<typeof YTable> | null>(null);

watch(
  yTableRef,
  (instance) => {
    page.setTableRef(instance);
  },
  { immediate: true },
);

const summaryCards = computed(() => [
  {
    key: "total",
    label: "批次总数",
    value: page.summary.totalCount ?? 0,
    color: "blue",
    clickable: false,
  },
  {
    key: "running",
    label: "处理中",
    value: page.summary.runningCount ?? 0,
    color: "cyan",
    clickable: true,
    status: "RUNNING",
  },
  {
    key: "success",
    label: "已完成",
    value: page.summary.successCount ?? 0,
    color: "green",
    clickable: true,
    status: "SUCCESS",
  },
  {
    key: "failed",
    label: "失败或停止",
    value: page.summary.failedCount ?? 0,
    color: "red",
    clickable: true,
    status: "FAILED",
  },
]);

const isSummaryCardActive = (status?: string) => {
  const normalized = String(page.query.status ?? "").trim().toUpperCase();
  if (!normalized) {
    return false;
  }
  if (status === "FAILED") {
    return normalized === "FAILED" || normalized === "STOPPED";
  }
  return normalized === String(status ?? "").trim().toUpperCase();
};

const summaryDescription = computed(
  () =>
    `当前筛选：${page.currentFilterSummary}。总批次 ${page.summary.totalCount ?? 0} 条；处理中 ${page.summary.runningCount ?? 0} 条，已完成 ${page.summary.successCount ?? 0} 条，失败或停止 ${page.summary.failedCount ?? 0} 条。`,
);

const stageCards = computed(() => {
  const summaryMap = new Map(
    (page.summary.stepSummaries ?? []).map((item) => [String(item.stage ?? "").trim().toUpperCase(), item]),
  );
  const activeStageCode = String(page.query.taskStage ?? "").trim().toUpperCase();
  return springBatchValuationTaskStageCatalog.map((item, index) => {
    const summary = summaryMap.get(item.stage);
    const pendingCount = summary?.pendingCount ?? 0;
    const runningCount = summary?.runningCount ?? 0;
    const failedCount = summary?.failedCount ?? 0;
    const totalCount = summary?.totalCount ?? 0;
    return {
      index: index + 1,
      key: item.stage,
      label: item.label,
      description: item.description,
      active: activeStageCode === item.stage,
      totalCount,
      pendingCount,
      runningCount,
      doneCount:
        Math.max(
          0,
          totalCount - pendingCount - runningCount - failedCount,
        ),
      failedCount,
    };
  });
});

const columns = computed<YTableColumn[]>(() => [
  {
    type: "checkbox",
    width: 50,
    align: "center",
    fixed: "left",
  },
  {
    field: "batchName",
    title: springBatchValuationTaskPageText.table.batchName,
    width: 240,
    ellipsis: true,
  },
  {
    field: "businessDate",
    title: springBatchValuationTaskPageText.table.businessDate,
    width: 120,
  },
  {
    field: "productCode",
    title: springBatchValuationTaskPageText.table.productCode,
    width: 120,
  },
  {
    field: "productName",
    title: springBatchValuationTaskPageText.table.productName,
    width: 160,
    ellipsis: true,
  },
  {
    field: "managerName",
    title: springBatchValuationTaskPageText.table.managerName,
    width: 140,
    ellipsis: true,
  },
  {
    field: "sourceTypeName",
    title: springBatchValuationTaskPageText.table.sourceType,
    width: 120,
    ellipsis: true,
  },
  {
    field: "currentStageName",
    title: springBatchValuationTaskPageText.table.currentStageName,
    width: 140,
  },
  {
    field: "statusName",
    title: springBatchValuationTaskPageText.table.statusName,
    width: 100,
  },
  {
    field: "startedAt",
    title: springBatchValuationTaskPageText.table.startedAt,
    width: 160,
  },
  {
    field: "endedAt",
    title: springBatchValuationTaskPageText.table.endedAt,
    width: 160,
  },
  {
    field: "durationText",
    title: springBatchValuationTaskPageText.table.durationText,
    width: 90,
  },
  {
    field: "lastErrorMessage",
    title: springBatchValuationTaskPageText.table.lastErrorMessage,
    width: 240,
    ellipsis: true,
  },
  {
    type: "action",
    title: springBatchValuationTaskPageText.table.action,
    width: 120,
    fixed: "right",
    align: "center",
  },
]);

const TASK_DATE_FORMAT = "YYYY-MM-DD";

const taskDateValue = computed<Dayjs | undefined>({
  get: () => {
    const text = String(page.query.taskDate ?? "").trim();
    const parsed = dayjs(text);
    return parsed.isValid() ? parsed : undefined;
  },
  set: (value) => {
    page.query.taskDate = value ? value.format(TASK_DATE_FORMAT) : "";
  },
});

const actionConfig = useTableActionConfig({
  width: 180,
  fixed: "right",
  displayLimit: 1,
  moreRenderType: "moreButton",
  buttons: [
    {
      text: "查看详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: { row: SpringBatchValuationTaskBatchRow }) =>
        page.openDetailDrawer(row),
    },
    {
      text: "重新解析",
      key: "retry",
      type: "link",
      disabledFn: ({ row }: { row: SpringBatchValuationTaskBatchRow }) =>
        !page.canRetryBatch(row),
      clickFn: ({ row }: { row: SpringBatchValuationTaskBatchRow }) => {
        Modal.confirm({
          title: "确认重新解析",
          icon: () => h(ExclamationCircleOutlined),
          content: `确认重新解析批次 ${row.batchId || "-"} 吗？`,
          okText: "确认",
          cancelText: "取消",
          onOk: () => page.retryBatchRow(row),
        });
      },
    },
  ],
});

const stageSummaryTone = (key: string) => {
  switch (key) {
    case "FILE_PARSE":
      return "blue";
    case "STRUCTURE_STANDARDIZE":
      return "cyan";
    case "STANDARD_LANDING":
      return "green";
    default:
      return "default";
  }
};

const columnsWithAction = computed<YTableColumn[]>(() => columns.value);

const handleStageCardClick = (stage: string) => {
  page.handleStageSelect(stage);
};

const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
  page.handlePageChange({
    current: pagination.current ?? page.pagination.current ?? 1,
    pageSize: pagination.pageSize ?? page.pagination.pageSize ?? 10,
  });
};

const confirmBatchRetry = () => {
  const selectedBatchIds = page.selectedBatchIds ?? [];
  if (!selectedBatchIds.length) {
    Modal.warning({
      title: "请先选择批次",
      content: "请选择至少一个已完成、失败或已停止的批次后再执行批量重新解析。",
    });
    return;
  }
  Modal.confirm({
    title: "确认批量重新解析",
    icon: () => h(ExclamationCircleOutlined),
    content: `确认对 ${selectedBatchIds.length} 个批次执行批量重新解析吗？\n${selectedBatchIds
      .slice(0, 8)
      .join("、")}${selectedBatchIds.length > 8 ? "…" : ""}`,
    okText: "确认",
    cancelText: "取消",
    onOk: () => page.batchRetrySelected(),
  });
};

const selectCurrentPageRows = async () => {
  await page.selectCurrentPageRows();
};

const selectedDetail = computed(() => page.detail?.batch ?? page.selectedRow);

const getStatusColor = (status?: string) => page.formatStatusColor(status);

const handleSummaryCardClick = (status?: string) => {
  if (!status) {
    return;
  }
  page.handleStatusSelect(status);
};

</script>

<template>
  <div class="spring-batch-task-page">
    <YCard class="spring-batch-task-header" :bordered="false" :padding="12">
      <div class="spring-batch-task-header__top">
        <div class="spring-batch-task-header__copy">
          <h2>{{ springBatchValuationTaskPageText.title }}</h2>
        </div>
      </div>

      <div class="spring-batch-task-header__meta">
        <span class="spring-batch-task-pill">
          {{ summaryDescription }}
        </span>
      </div>

      <div class="spring-batch-task-metrics">
        <div
          v-for="card in summaryCards"
          :key="card.key"
          class="spring-batch-task-metric"
          :role="card.clickable ? 'button' : undefined"
          :tabindex="card.clickable ? 0 : undefined"
          :aria-pressed="card.clickable ? isSummaryCardActive(card.status) : undefined"
          :aria-label="card.clickable ? `${card.label}，点击筛选` : undefined"
          :class="[
            `spring-batch-task-metric--${card.color}`,
            card.clickable ? 'spring-batch-task-metric--clickable' : '',
            card.status && isSummaryCardActive(card.status) ? 'spring-batch-task-metric--active' : '',
          ]"
          @click="handleSummaryCardClick(card.status)"
          @keydown.enter.prevent="handleSummaryCardClick(card.status)"
          @keydown.space.prevent="handleSummaryCardClick(card.status)"
        >
          <div class="spring-batch-task-metric__head">
            <div class="spring-batch-task-metric__label">
              {{ card.label }}
            </div>
            <div class="spring-batch-task-metric__value">
              {{ card.value }}
            </div>
          </div>
        </div>
      </div>

      <div class="spring-batch-task-stage-area">
        <div class="spring-batch-task-stage-chain">
          <button
            v-for="card in stageCards"
            :key="card.key"
            type="button"
            class="spring-batch-task-stage-card"
            :class="[
              `spring-batch-task-stage-card--${stageSummaryTone(card.key)}`,
              card.active ? 'spring-batch-task-stage-card--active' : '',
            ]"
            :role="'button'"
            :tabindex="0"
            :aria-pressed="card.active"
            :aria-label="`按${card.label}筛选批量估值解析任务`"
            @click="handleStageCardClick(card.key)"
            @keydown.enter.prevent="handleStageCardClick(card.key)"
            @keydown.space.prevent="handleStageCardClick(card.key)"
          >
            <div class="spring-batch-task-stage-card__main">
              <span class="spring-batch-task-stage-card__index">{{ card.index }}</span>
              <strong class="spring-batch-task-stage-card__title">{{ card.label }}</strong>
            </div>
            <div class="spring-batch-task-stage-card__desc">
              {{ card.description }}
            </div>
          </button>
        </div>
      </div>
    </YCard>

    <YCard class="spring-batch-task-list-card" :bordered="false" :padding="12">
      <div class="spring-batch-task-query-bar">
        <a-form layout="inline" class="spring-batch-task-query-form" :model="page.query">
          <a-form-item :label="springBatchValuationTaskPageText.query.batchId">
            <a-input
              v-model:value="page.query.batchId"
              placeholder="批次ID"
              allow-clear
              style="width: 180px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="springBatchValuationTaskPageText.query.taskDate">
            <a-date-picker
              v-model:value="taskDateValue"
              format="YYYY-MM-DD"
              allow-clear
              style="width: 150px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="springBatchValuationTaskPageText.query.productKeyword">
            <a-input
              v-model:value="page.query.productKeyword"
              placeholder="产品名称或代码"
              allow-clear
              style="width: 180px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="springBatchValuationTaskPageText.query.stage">
            <a-select
              v-model:value="page.query.taskStage"
              allow-clear
              placeholder="全部"
              style="width: 150px"
              size="small"
            >
              <a-select-option
                v-for="item in springBatchValuationTaskStageCatalog"
                :key="item.stage"
                :value="item.stage"
              >
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="springBatchValuationTaskPageText.query.status">
            <a-select
              v-model:value="page.query.status"
              allow-clear
              placeholder="全部"
              style="width: 130px"
              size="small"
            >
              <a-select-option
                v-for="item in springBatchValuationTaskStatusCatalog"
                :key="item.status"
                :value="item.status"
              >
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <div class="spring-batch-task-query-actions">
          <YButton size="small" type="primary" @click="page.runQuery">
            <template #icon><SearchOutlined /></template>
            查询
          </YButton>
          <YButton
            size="small"
            type="primary"
            ghost
            :loading="page.batchRetryLoading"
            :disabled="!page.selectedBatchIds.length"
            @click="confirmBatchRetry"
          >
            批量重新解析
          </YButton>
          <YButton size="small" @click="page.resetQuery">
            <template #icon><ReloadOutlined /></template>
            重置
          </YButton>
        </div>
      </div>
      <div class="spring-batch-task-table">
        <YTable
          ref="yTableRef"
          :columns="columnsWithAction"
          :action-config="actionConfig"
          :data="page.rows"
          :loading="page.loading || page.detailLoading"
          :row-config="{ keyField: 'batchId' }"
          :checkbox-config="{ highlight: true }"
          :pageable="true"
          :autoFlexColumn="false"
          :border="false"
          v-model:pagination="page.pagination"
          :toolbar-config="{ custom: false }"
          @page-change="handleTableChange"
          @checkbox-change="page.syncSelectionFromTable"
          @checkbox-all="page.syncSelectionFromTable"
        >
          <template #toolbar-left>
            <WorkspaceTableToolbar
              title="批次列表"
              :description="`总数 ${page.totalCount} 条，已选 ${page.selectedBatchIds.length} 条。仅允许已完成、失败或已停止的批次重新解析。`"
              :meta="`当前页 ${page.rows.length} 条`"
            >
              <a-space>
                <YButton size="small" :disabled="!page.rows.length" @click="selectCurrentPageRows">
                  选中当前页
                </YButton>
                <YButton
                  size="small"
                  :disabled="!page.selectedBatchIds.length"
                  @click="page.clearSelection"
                >
                  清除选择
                </YButton>
              </a-space>
            </WorkspaceTableToolbar>
          </template>
          <template #sourceTypeName="{ row }">
            {{ row.sourceTypeName || resolveSpringBatchValuationTaskSourceTypeLabel(row.sourceType) }}
          </template>
          <template #statusName="{ row }">
            <a-tag :color="getStatusColor(row.status)">
              {{ row.statusName || row.status }}
            </a-tag>
          </template>
          <template #lastErrorMessage="{ row }">
            <span class="spring-batch-task-table__ellipsis">
              {{ row.lastErrorMessage || "-" }}
            </span>
          </template>
        </YTable>
      </div>
    </YCard>

    <a-drawer
      :open="page.detailVisible"
      width="920"
      :title="springBatchValuationTaskPageText.detail.title"
      :destroy-on-close="true"
      @close="page.closeDetailDrawer"
    >
        <a-spin :spinning="page.detailLoading">
          <a-descriptions :column="2" bordered size="small" class="spring-batch-task-detail">
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.batchId">
            {{ selectedDetail?.batchId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.workflowCode">
            {{ page.summary.workflowCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.workflowId">
            {{ page.summary.workflowId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.versionNo">
            {{ page.summary.versionNo || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.currentStage">
            {{ selectedDetail?.currentStageName || selectedDetail?.currentStage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.status">
            <a-tag :color="getStatusColor(selectedDetail?.status)">{{ selectedDetail?.statusName || selectedDetail?.status || "-" }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.progress">
            {{ selectedDetail?.progress ?? "-" }}%
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.startedAt">
            {{ selectedDetail?.startedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.endedAt">
            {{ selectedDetail?.endedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.durationText">
            {{ selectedDetail?.durationText || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.fileId">
            {{ selectedDetail?.fileId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.filesysFileId">
            {{ selectedDetail?.filesysFileId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.originalFileName" :span="2">
            {{ selectedDetail?.originalFileName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.lastErrorMessage" :span="2">
            {{ selectedDetail?.lastErrorMessage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.currentBlockPoint" :span="2">
            {{ page.detail?.currentBlockPoint || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions :column="1" bordered size="small" class="spring-batch-task-detail__summary">
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.errorMessage">
            {{ selectedDetail?.lastErrorMessage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.logRef">
            {{ selectedDetail?.batchId || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="spring-batch-task-detail__steps">
          <div class="spring-batch-task-detail__title">{{ springBatchValuationTaskPageText.detail.stepInfo }}</div>
          <YTable
            :columns="[
              {
                field: 'stepName',
                title: '阶段',
                width: 140,
              },
              {
                field: 'statusName',
                title: '状态',
                width: 100,
              },
              {
                field: 'startedAt',
                title: '开始时间',
                width: 160,
              },
              {
                field: 'endedAt',
                title: '结束时间',
                width: 160,
              },
              {
                field: 'durationText',
                title: '耗时',
                width: 90,
              },
              {
                field: 'errorMessage',
                title: '错误摘要',
                showOverflow: 'tooltip',
              },
            ]"
            :data="page.detail?.steps ?? []"
            :pageable="false"
            :row-config="{ keyField: 'stepId' }"
            :border="false"
            :autoFlexColumn="false"
          >
            <template #statusName="{ row }">
              <a-tag :color="getStatusColor(row.status)">
                {{ row.statusName || row.status }}
              </a-tag>
            </template>
            <template #errorMessage="{ row }">
              <span class="spring-batch-task-table__ellipsis">
                {{ row.errorMessage || "-" }}
              </span>
            </template>
          </YTable>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>
