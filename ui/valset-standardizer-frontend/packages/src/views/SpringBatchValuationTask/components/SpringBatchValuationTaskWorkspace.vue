<script setup lang="ts">
import { computed } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons-vue";
import { YButton, YCard, YTable, type YTableColumn } from "@yss-ui/components";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import {
  springBatchValuationTaskPageText,
  springBatchValuationTaskStageCatalog,
  springBatchValuationTaskStatusCatalog,
} from "../constants";
import type {
  SpringBatchValuationTaskBatchRow,
  SpringBatchValuationTaskPageState,
} from "../types";

const { page } = defineProps<{
  page: SpringBatchValuationTaskPageState;
}>();

const summaryCards = computed(() => [
  {
    key: "total",
    label: springBatchValuationTaskPageText.summary.total,
    value: page.summary.totalCount ?? 0,
    color: "blue",
  },
  {
    key: "running",
    label: springBatchValuationTaskPageText.summary.running,
    value: page.summary.runningCount ?? 0,
    color: "cyan",
  },
  {
    key: "success",
    label: springBatchValuationTaskPageText.summary.success,
    value: page.summary.successCount ?? 0,
    color: "green",
  },
  {
    key: "failed",
    label: springBatchValuationTaskPageText.summary.failed,
    value: page.summary.failedCount ?? 0,
    color: "red",
  },
]);

const stageCards = computed(() => {
  const summaryMap = new Map(
    (page.summary.stepSummaries ?? []).map((item) => [String(item.stage ?? "").trim().toUpperCase(), item]),
  );
  return springBatchValuationTaskStageCatalog.map((item) => {
    const summary = summaryMap.get(item.stage);
    return {
      key: item.stage,
      label: item.label,
      description: item.description,
      active: page.query.stage === item.stage,
      totalCount: summary?.totalCount ?? 0,
      pendingCount: summary?.pendingCount ?? 0,
      runningCount: summary?.runningCount ?? 0,
      doneCount:
        Math.max(
          0,
          (summary?.totalCount ?? 0) -
            (summary?.pendingCount ?? 0) -
            (summary?.runningCount ?? 0) -
            (summary?.failedCount ?? 0),
        ),
      failedCount: summary?.failedCount ?? 0,
    };
  });
});

const columns = computed<YTableColumn[]>(() => [
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
    field: "sourceType",
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
  width: 120,
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

const tableSummary = computed(() => {
  const total = page.totalCount ?? 0;
  const stageCount = stageCards.value.reduce(
    (sum, item) => sum + item.totalCount,
    0,
  );
  return `当前共 ${total} 条批次记录，阶段统计覆盖 ${stageCount} 条明细。`;
});

const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
  page.handlePageChange({
    current: pagination.current ?? page.pagination.current ?? 1,
    pageSize: pagination.pageSize ?? page.pagination.pageSize ?? 10,
  });
};
const stepColumns = computed<YTableColumn[]>(() => [
  {
    field: "stepName",
    title: "阶段",
    width: 140,
  },
  {
    field: "statusName",
    title: "状态",
    width: 100,
  },
  {
    field: "startedAt",
    title: "开始时间",
    width: 160,
  },
  {
    field: "endedAt",
    title: "结束时间",
    width: 160,
  },
  {
    field: "durationText",
    title: "耗时",
    width: 90,
  },
  {
    field: "errorMessage",
    title: "错误摘要",
    ellipsis: true,
  },
]);

const selectedDetail = computed(() => page.detail?.batch ?? page.selectedRow);
const detailSteps = computed(() => page.detail?.steps ?? page.selectedRow?.steps ?? []);

const getStatusColor = (status?: string) => page.formatStatusColor(status);
</script>

<template>
  <div class="spring-batch-task-page">
    <YCard class="spring-batch-task-header" :bordered="false" :padding="12">
      <div class="spring-batch-task-header__top">
        <div class="spring-batch-task-header__copy">
          <h2>{{ springBatchValuationTaskPageText.title }}</h2>
        </div>
      </div>

      <div class="spring-batch-task-metrics">
        <div
          v-for="card in summaryCards"
          :key="card.key"
          class="spring-batch-task-metric"
          :class="`spring-batch-task-metric--${card.color}`"
        >
          <div class="spring-batch-task-metric__label">
            {{ card.label }}
          </div>
          <div class="spring-batch-task-metric__value">
            {{ card.value }}
          </div>
        </div>
      </div>

      <div class="spring-batch-task-stage-area">
        <div class="spring-batch-task-stage-grid">
          <button
            v-for="card in stageCards"
            :key="card.key"
            type="button"
            class="spring-batch-task-stage-card"
            :class="[
              `spring-batch-task-stage-card--${stageSummaryTone(card.key)}`,
              card.active ? 'spring-batch-task-stage-card--active' : '',
            ]"
            @click="handleStageCardClick(card.key)"
          >
            <div class="spring-batch-task-stage-card__head">
              <strong>{{ card.label }}</strong>
              <span>{{ card.totalCount }} 条</span>
            </div>
            <p>{{ card.description }}</p>
          </button>
        </div>
      </div>
    </YCard>

    <YCard class="spring-batch-task-list-card" :bordered="false" :padding="12">
      <div class="spring-batch-task-panel-title">
        <div>
          <h3>批次列表</h3>
          <p>{{ tableSummary }}</p>
        </div>
      </div>

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
              v-model:value="page.query.stage"
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
          <YButton size="small" @click="page.resetQuery">
            <template #icon><ReloadOutlined /></template>
            重置
          </YButton>
        </div>
      </div>

      <div class="spring-batch-task-table">
        <YTable
          :columns="columnsWithAction"
          :action-config="actionConfig"
          :data="page.rows"
          :loading="page.loading || page.detailLoading"
          :row-config="{ keyField: 'batchId' }"
          :pageable="true"
          :autoFlexColumn="false"
          :border="false"
          v-model:pagination="page.pagination"
          :toolbar-config="{ custom: false }"
          @page-change="handleTableChange"
        >
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

        <div class="spring-batch-task-detail__steps">
          <div class="spring-batch-task-detail__title">{{ springBatchValuationTaskPageText.detail.stepInfo }}</div>
          <YTable
            :columns="stepColumns"
            :data="detailSteps"
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

        <a-descriptions :column="1" bordered size="small" class="spring-batch-task-detail__summary">
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.errorMessage">
            {{ selectedDetail?.lastErrorMessage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="springBatchValuationTaskPageText.detail.logRef">
            {{ selectedDetail?.batchId || "-" }}
          </a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>
  </div>
</template>
