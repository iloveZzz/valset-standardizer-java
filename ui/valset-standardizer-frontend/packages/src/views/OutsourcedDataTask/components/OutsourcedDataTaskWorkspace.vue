<script setup lang="ts">
import { computed, h, ref } from "vue";
import { Modal } from "ant-design-vue";
import {
  DatabaseOutlined,
  ExclamationCircleOutlined,
  FileSearchOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  StopOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard, YTable, type YTableColumn } from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import {
  outsourcedDataTaskActionTexts,
  outsourcedDataTaskMetricCardText,
  outsourcedDataTaskPreviewText,
  outsourcedDataTaskTableTexts,
  outsourcedDataTaskStatusCatalog,
} from "../constants";
import type {
  OutsourcedDataTaskBatchRow,
  OutsourcedDataTaskPage,
  OutsourcedDataTaskStepRow,
} from "../types";

const { page } = defineProps<{
  page: OutsourcedDataTaskPage;
}>();

const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  extraOffset: 24,
  defaultHeight: 480,
  minHeight: 260,
});

const columns = computed<YTableColumn[]>(() => [
  { type: "checkbox", width: 48, align: "center", fixed: "left" as const },
  { type: "expand", width: 46, fixed: "left" as const },
  {
    field: "batchName",
    title: outsourcedDataTaskTableTexts.batchColumns.batchName,
    minWidth: 260,
    fixed: "left" as const,
    ellipsis: true,
  },
  { field: "productCode", title: outsourcedDataTaskTableTexts.batchColumns.productCode, width: 120 },
  { field: "productName", title: outsourcedDataTaskTableTexts.batchColumns.productName, width: 140 },
  { field: "managerName", title: outsourcedDataTaskTableTexts.batchColumns.managerName, width: 130 },
  { field: "valuationDate", title: outsourcedDataTaskTableTexts.batchColumns.valuationDate, width: 120 },
  {
    field: "originalFileName",
    title: outsourcedDataTaskTableTexts.batchColumns.originalFileName,
    minWidth: 220,
    ellipsis: true,
  },
  { field: "currentStepName", title: outsourcedDataTaskTableTexts.batchColumns.currentStepName, width: 140 },
  { field: "status", title: outsourcedDataTaskTableTexts.batchColumns.status, width: 110 },
  { field: "progress", title: outsourcedDataTaskTableTexts.batchColumns.progress, width: 180 },
  { field: "startedAt", title: outsourcedDataTaskTableTexts.batchColumns.startedAt, width: 180 },
  { field: "durationText", title: outsourcedDataTaskTableTexts.batchColumns.durationText, width: 90 },
  {
    field: "lastErrorMessage",
    title: outsourcedDataTaskTableTexts.batchColumns.lastErrorMessage,
    minWidth: 240,
    ellipsis: true,
  },
  { field: "action", title: outsourcedDataTaskTableTexts.batchColumns.action, width: 220, fixed: "right" as const },
]);

const historyColumns = computed<YTableColumn[]>(() =>
  columns.value.filter((column) => Boolean(column.field) && column.field !== "action"),
);

const stepColumns = computed<YTableColumn[]>(() => [
  { field: "stepName", title: outsourcedDataTaskTableTexts.stepColumns.stepName, width: 150 },
  { field: "startedAt", title: outsourcedDataTaskTableTexts.stepColumns.startedAt, width: 180 },
  { field: "durationText", title: outsourcedDataTaskTableTexts.stepColumns.durationText, width: 100 },
  { field: "runNo", title: outsourcedDataTaskTableTexts.stepColumns.runNo, width: 100 },
  { field: "triggerModeName", title: outsourcedDataTaskTableTexts.stepColumns.triggerModeName, width: 120 },
  { field: "status", title: outsourcedDataTaskTableTexts.stepColumns.status, width: 110 },
  { field: "errorMessage", title: outsourcedDataTaskTableTexts.stepColumns.errorMessage, minWidth: 240, ellipsis: true },
  { field: "action", title: outsourcedDataTaskTableTexts.stepColumns.action, width: 190, fixed: "right" as const },
]);

const dataEntryColumns = computed<YTableColumn[]>(() => [
  { field: "name", title: outsourcedDataTaskTableTexts.dataEntryColumns.name, width: 130 },
  {
    field: "description",
    title: outsourcedDataTaskTableTexts.dataEntryColumns.description,
    minWidth: 280,
    ellipsis: true,
  },
  { field: "status", title: outsourcedDataTaskTableTexts.dataEntryColumns.status, width: 110 },
  { field: "action", title: outsourcedDataTaskTableTexts.dataEntryColumns.action, width: 120, fixed: "right" as const },
]);

const logColumns = computed<YTableColumn[]>(() => [
  { field: "stepName", title: outsourcedDataTaskTableTexts.logColumns.stepName, width: 140 },
  { field: "logLevel", title: outsourcedDataTaskTableTexts.logColumns.logLevel, width: 90 },
  { field: "occurredAt", title: outsourcedDataTaskTableTexts.logColumns.occurredAt, width: 180 },
  { field: "startedAt", title: outsourcedDataTaskTableTexts.logColumns.startedAt, width: 180 },
  { field: "durationText", title: outsourcedDataTaskTableTexts.logColumns.durationText, width: 90 },
  { field: "status", title: outsourcedDataTaskTableTexts.logColumns.status, width: 100 },
  { field: "message", title: outsourcedDataTaskTableTexts.logColumns.message, minWidth: 260, ellipsis: true },
  { field: "errorStack", title: outsourcedDataTaskTableTexts.logColumns.errorStack, minWidth: 320 },
]);

const detailActiveKey = ref("overview");
const manualExceptionConfirmed = ref(false);
const manualRemark = ref("");

const dataStatusColor = (status: string) => {
  if (status === "READY") return "green";
  if (status === "ERROR") return "red";
  return "default";
};

const statusLabelMap = Object.fromEntries(
  outsourcedDataTaskStatusCatalog.map((item) => [item.status, item.label]),
);
const statusOptions = outsourcedDataTaskStatusCatalog;

const formatStepLabel = (stage: string) =>
  page.stepSummaries.find((item) => item.stage === stage)?.stepName || stage;

const formatStatusLabel = (status: string) => statusLabelMap[status] || status;

const openActionConfirm = (
  title: string,
  content: string,
  onOk: () => void,
  danger = false,
) => {
  Modal.confirm({
    title,
    content,
    icon: h(ExclamationCircleOutlined),
    okText: "确定",
    cancelText: "取消",
    okButtonProps: { danger },
    onOk,
  });
};

const confirmExecute = (row: OutsourcedDataTaskBatchRow) => {
  openActionConfirm(
    outsourcedDataTaskActionTexts.executeBatchConfirmTitle,
    outsourcedDataTaskActionTexts.executeBatchConfirmContent,
    () => page.executeBatch(row),
  );
};

const confirmRetry = (row: OutsourcedDataTaskBatchRow) => {
  openActionConfirm(
    outsourcedDataTaskActionTexts.retryBatchConfirmTitle,
    outsourcedDataTaskActionTexts.retryBatchConfirmContent,
    () => page.retryBatch(row),
  );
};

const confirmStop = (row: OutsourcedDataTaskBatchRow) => {
  openActionConfirm(
    outsourcedDataTaskActionTexts.stopBatchConfirmTitle,
    outsourcedDataTaskActionTexts.stopBatchConfirmContent,
    () => page.stopBatch(row),
  );
};

const confirmRetryStep = (row: OutsourcedDataTaskStepRow) => {
  openActionConfirm(
    outsourcedDataTaskActionTexts.retryStepConfirmTitle,
    `${outsourcedDataTaskActionTexts.retryStepConfirmContentPrefix}${row.stepName}${outsourcedDataTaskActionTexts.retryStepConfirmContentSuffix}`,
    () => page.retryStep(row),
  );
};

const activeFilterText = computed(() => {
  const filters = [
    "当前任务",
    page.query.businessDate && `任务日期：${page.query.businessDate}`,
    page.query.managerName && `管理人：${page.query.managerName}`,
    page.query.productKeyword && `产品：${page.query.productKeyword}`,
    page.query.step && `步骤：${formatStepLabel(page.query.step)}`,
    page.query.status && `状态：${formatStatusLabel(page.query.status)}`,
    page.query.sourceType && `来源：${page.query.sourceType}`,
    page.query.errorType && `异常：${page.query.errorType}`,
  ].filter(Boolean);
  return filters.length ? filters.join(" / ") : "全部任务";
});

const taskMetricCards = computed(() => [
  {
    key: "total",
    label: outsourcedDataTaskMetricCardText.total.label,
    value: page.totalCount,
    description: outsourcedDataTaskMetricCardText.total.description,
    tone: "primary",
    active: !page.query.status,
    onClick: () => page.selectStatus(""),
  },
  {
    key: "running",
    label: outsourcedDataTaskMetricCardText.running.label,
    value: page.runningCount,
    description: outsourcedDataTaskMetricCardText.running.description,
    tone: "warning",
    active: page.query.status === "RUNNING",
    onClick: () => page.selectStatus("RUNNING"),
  },
  {
    key: "success",
    label: outsourcedDataTaskMetricCardText.success.label,
    value: page.successCount,
    description: outsourcedDataTaskMetricCardText.success.description,
    tone: "success",
    active: page.query.status === "SUCCESS",
    onClick: () => page.selectStatus("SUCCESS"),
  },
  {
    key: "failed",
    label: outsourcedDataTaskMetricCardText.failed.label,
    value: page.failedCount,
    description: outsourcedDataTaskMetricCardText.failed.description,
    tone: "danger",
    active: page.query.status === "FAILED",
    onClick: () => page.selectStatus("FAILED"),
  },
]);
</script>

<template>
  <div class="outsourced-task-page">
    <YCard class="outsourced-task-header" :bordered="false" :padding="12">
      <div class="outsourced-task-header__top">
        <div>
          <h2>{{ outsourcedDataTaskActionTexts.pageHeaderTitle }}</h2>
          <p>{{ outsourcedDataTaskActionTexts.pageHeaderDescription }}</p>
        </div>
        <div class="outsourced-task-actions">
          <YButton type="primary" @click="page.batchExecute">
            <template #icon><PlayCircleOutlined /></template>
            {{ outsourcedDataTaskActionTexts.batchExecuteButtonText }}
          </YButton>
          <YButton @click="page.batchRetry">
            <template #icon><ReloadOutlined /></template>
            {{ outsourcedDataTaskActionTexts.batchRetryButtonText }}
          </YButton>
          <YButton danger @click="page.batchStop">
            <template #icon><StopOutlined /></template>
            {{ outsourcedDataTaskActionTexts.batchStopButtonText }}
          </YButton>
          <YButton @click="page.openHistoryDrawer">
            {{ outsourcedDataTaskActionTexts.historyButtonText }}
          </YButton>
        </div>
      </div>

      <div class="outsourced-task-metrics">
        <button
          v-for="item in taskMetricCards"
          :key="item.key"
          class="outsourced-task-metric"
          :class="[
            `outsourced-task-metric--${item.tone}`,
            { 'is-active': item.active },
          ]"
          @click="item.onClick"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.description }}</em>
        </button>
      </div>

      <div class="outsourced-task-stage-chain">
        <button
          v-for="(item, index) in page.stepSummaries"
          :key="item.stage"
          class="outsourced-task-stage"
          :class="{ 'is-active': page.query.step === item.stage }"
          @click="page.selectStep(item.step)"
        >
          <span class="outsourced-task-stage__title"
            >{{ index + 1 }}.{{ item.stepName }}</span
          >
          <span class="outsourced-task-stage__desc">{{
            item.stepDescription
          }}</span>
        </button>
      </div>
    </YCard>

    <YCard class="outsourced-task-filter" :bordered="false" :padding="12">
      <a-form layout="inline" size="small" class="outsourced-task-filter__form">
        <a-form-item label="任务日期">
          <a-date-picker
            v-model:value="page.query.businessDate"
            size="small"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="管理人">
          <a-input
            v-model:value="page.query.managerName"
            size="small"
            style="width: 140px"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="产品">
          <a-input
            v-model:value="page.query.productKeyword"
            size="small"
            style="width: 180px"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="page.query.status"
            style="width: 130px"
            size="small"
            allow-clear
          >
            <a-select-option
              v-for="item in statusOptions"
              :key="item.status"
              :value="item.status"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="来源">
          <a-input
            v-model:value="page.query.sourceType"
            size="small"
            style="width: 140px"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="异常">
          <a-input
            v-model:value="page.query.errorType"
            size="small"
            style="width: 160px"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <div class="outsourced-task-filter__actions">
            <YButton type="primary" size="small" @click="page.runQuery"
              >查询</YButton
            >
            <YButton size="small" @click="page.resetQuery">重置</YButton>
          </div>
        </a-form-item>
      </a-form>
      <div class="outsourced-task-filter__tags">
        <a-tag color="blue">{{ activeFilterText }}</a-tag>
      </div>
    </YCard>

    <div ref="tableAreaRef" class="outsourced-task-table">
      <YTable
        :columns="columns"
        :data="page.tableData"
        :loading="page.loading"
        :height="tableHeight"
        :row-config="{ keyField: 'batchId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        :autoFlexColumn="false"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
        @toggle-row-expand="page.handleExpandChange"
      >
        <template #batchName="{ row }">
          <a @click="page.openDetailDrawer(row)">{{ row.batchName }}</a>
        </template>
        <template #status="{ row }">
          <a-tag :color="page.formatStatusColor(row.status)">{{
            row.statusName
          }}</a-tag>
        </template>
        <template #progress="{ row }">
          <a-progress
            :percent="row.progress"
            size="small"
            :status="row.status === 'FAILED' ? 'exception' : undefined"
          />
        </template>
        <template #expand-row="{ row }">
          <div class="outsourced-task-expand">
          <div class="outsourced-task-expand__header">
              <strong>{{ outsourcedDataTaskActionTexts.detailStepSectionTitle }}</strong>
              <span>{{ outsourcedDataTaskActionTexts.detailStepSectionDescription }}</span>
            </div>
            <YTable
              :columns="stepColumns"
              :data="page.getOrderedSteps(row)"
              :row-config="{ keyField: 'stepId' }"
              :row-class-name="page.getStepRowClassName"
              :pageable="false"
              :autoFlexColumn="false"
            >
              <template #status="{ row: stepRow }">
                <a-tag :color="page.formatStatusColor(stepRow.status)">{{
                  stepRow.statusName
                }}</a-tag>
              </template>
              <template #errorMessage="{ row: stepRow }">
                <span
                  class="outsourced-task-step-error"
                  :class="{
                    'is-highlight':
                      stepRow.status === 'FAILED' ||
                      stepRow.status === 'BLOCKED',
                  }"
                >
                  {{ stepRow.errorMessage || "-" }}
                </span>
              </template>
              <template #action="{ row: stepRow }">
                <a-space>
                  <a @click="page.openStepLogs(stepRow)">
                    {{ outsourcedDataTaskActionTexts.stepLogButtonText }}
                  </a>
                  <a @click="page.openStepData(stepRow)">
                    {{ outsourcedDataTaskActionTexts.stepDataButtonText }}
                  </a>
                  <a
                    :class="{ 'is-disabled': stepRow.status === 'PENDING' }"
                    @click="confirmRetryStep(stepRow)"
                  >
                    {{ outsourcedDataTaskActionTexts.stepRetryButtonText }}
                  </a>
                </a-space>
              </template>
            </YTable>
          </div>
        </template>
        <template #action="{ row }">
          <a-space>
            <a @click="page.openDetailDrawer(row)">
              {{ outsourcedDataTaskActionTexts.viewButtonText }}
            </a>
            <a @click="confirmExecute(row)">
              {{ outsourcedDataTaskActionTexts.executeButtonText }}
            </a>
            <a @click="confirmRetry(row)">
              {{ outsourcedDataTaskActionTexts.retryButtonText }}
            </a>
            <a v-if="row.status === 'RUNNING'" @click="confirmStop(row)">
              {{ outsourcedDataTaskActionTexts.stopButtonText }}
            </a>
          </a-space>
        </template>
      </YTable>
    </div>

    <a-drawer
      class="outsourced-task-drawer"
      :open="page.detailVisible"
      :title="outsourcedDataTaskActionTexts.detailHeaderTitle"
      :width="960"
      @close="page.closeDetailDrawer"
    >
      <template v-if="page.selectedRow">
        <div class="outsourced-task-detail-banner">
          <div>
            <h3>{{ page.selectedRow.batchName }}</h3>
            <p>
              {{ page.selectedRow.productCode }} ·
              {{ page.selectedRow.productName }} ·
              {{ page.selectedRow.valuationDate }}
            </p>
          </div>
          <a-tag :color="page.formatStatusColor(page.selectedRow.status)">
            {{ page.selectedRow.statusName }}
          </a-tag>
        </div>

        <a-tabs
          v-model:active-key="detailActiveKey"
          class="outsourced-task-detail-tabs"
        >
          <a-tab-pane
            key="overview"
            :tab="outsourcedDataTaskActionTexts.detailOverviewTabTitle"
          >
            <a-alert
              v-if="page.manualState.currentBlockPoint"
              class="outsourced-task-detail-alert"
              type="info"
              show-icon
              :message="`${outsourcedDataTaskPreviewText.currentBlockPointPrefix}${page.manualState.currentBlockPoint}`"
            />

            <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.batchId">{{
                page.selectedRow.batchId
              }}</a-descriptions-item>
              <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.currentStepName">{{
                page.selectedRow.currentStepName
              }}</a-descriptions-item>
              <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.originalFileName">{{
                page.selectedRow.originalFileName
              }}</a-descriptions-item>
              <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.filesysFileId">{{
                page.selectedRow.filesysFileId
              }}</a-descriptions-item>
              <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.startedAt">{{
                page.selectedRow.startedAt
              }}</a-descriptions-item>
              <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.durationText">{{
                page.selectedRow.durationText
              }}</a-descriptions-item>
              <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.lastErrorMessage" :span="2">
                {{ page.selectedRow.lastErrorMessage || "-" }}
              </a-descriptions-item>
            </a-descriptions>

            <div class="outsourced-task-step-table">
              <YTable
                :columns="stepColumns"
                :data="page.getOrderedSteps(page.selectedRow)"
                :row-config="{ keyField: 'stepId' }"
                :row-class-name="page.getStepRowClassName"
                :pageable="false"
                :autoFlexColumn="false"
              >
                <template #status="{ row }">
                  <a-tag :color="page.formatStatusColor(row.status)">{{
                    row.statusName
                  }}</a-tag>
                </template>
                <template #errorMessage="{ row }">
                  <span
                    class="outsourced-task-step-error"
                    :class="{
                      'is-highlight':
                        row.status === 'FAILED' || row.status === 'BLOCKED',
                    }"
                  >
                    {{ row.errorMessage || "-" }}
                  </span>
                </template>
                <template #action="{ row }">
                  <a-space>
                    <a @click="page.openStepLogs(row)">
                      {{ outsourcedDataTaskActionTexts.stepLogButtonText }}
                    </a>
                    <a @click="page.openStepData(row)">
                      {{ outsourcedDataTaskActionTexts.stepDataButtonText }}
                    </a>
                    <a
                      :class="{ 'is-disabled': row.status === 'PENDING' }"
                      @click="confirmRetryStep(row)"
                    >
                      {{ outsourcedDataTaskActionTexts.stepRetryButtonText }}
                    </a>
                  </a-space>
                </template>
              </YTable>
            </div>
          </a-tab-pane>

          <a-tab-pane
            key="data"
            :tab="outsourcedDataTaskActionTexts.detailDataTabTitle"
          >
            <div class="outsourced-task-data-summary">
              <div>
                <span>{{ outsourcedDataTaskTableTexts.detailFields.originalFileName }}</span>
                <strong>{{ page.selectedRow.originalFileName || "-" }}</strong>
              </div>
              <div>
                <span>{{ outsourcedDataTaskTableTexts.detailFields.filesysFileId }}</span>
                <strong>{{
                  page.selectedRow.filesysFileId ||
                  page.selectedRow.fileId ||
                  "-"
                }}</strong>
              </div>
              <div>
                <span>{{ outsourcedDataTaskTableTexts.batchColumns.valuationDate }}</span>
                <strong>{{ page.selectedRow.valuationDate || "-" }}</strong>
              </div>
            </div>
            <YTable
              :columns="dataEntryColumns"
              :data="page.detailDataEntries"
              :row-config="{ keyField: 'key' }"
              :pageable="false"
              :autoFlexColumn="false"
            >
              <template #name="{ row }">
                <a-space>
                  <FileSearchOutlined
                    v-if="
                      row.key === 'source-file' || row.key === 'parse-result'
                    "
                  />
                  <DatabaseOutlined v-else />
                  <span>{{ row.name }}</span>
                </a-space>
              </template>
              <template #status="{ row }">
                <a-tag :color="dataStatusColor(row.status)">{{
                  row.statusName
                }}</a-tag>
              </template>
              <template #action="{ row }">
                <a-button
                  v-if="row.href"
                  type="link"
                  size="small"
                  :href="row.href"
                  target="_blank"
                >
                  {{ outsourcedDataTaskActionTexts.openEntryButtonText }}
                </a-button>
                <span v-else class="outsourced-task-muted">
                  {{ outsourcedDataTaskActionTexts.pendingEntryText }}
                </span>
              </template>
            </YTable>
          </a-tab-pane>

          <a-tab-pane
            key="logs"
            :tab="outsourcedDataTaskActionTexts.detailLogsTabTitle"
          >
            <YTable
              :columns="logColumns"
              :data="page.detailLogRows"
              :row-config="{ keyField: 'key' }"
              :pageable="false"
              :autoFlexColumn="false"
            >
              <template #status="{ row }">
                <a-tag :color="page.formatStatusColor(row.status)">{{
                  row.statusName
                }}</a-tag>
              </template>
              <template #errorStack="{ row }">
                <pre v-if="row.errorStack" class="outsourced-task-stack">{{
                  row.errorStack
                }}</pre>
                <span v-else class="outsourced-task-muted">
                  {{ outsourcedDataTaskActionTexts.noErrorStackText }}
                </span>
              </template>
            </YTable>
          </a-tab-pane>

          <a-tab-pane
            key="manual"
            :tab="outsourcedDataTaskActionTexts.detailManualTabTitle"
          >
            <div class="outsourced-task-manual">
              <a-alert
                type="warning"
                show-icon
                :message="page.manualState.currentBlockPoint"
                :description="page.manualState.exceptionConfirmText"
              />
              <a-checkbox v-model:checked="manualExceptionConfirmed">
                {{ outsourcedDataTaskActionTexts.manualExceptionCheckboxText }}
              </a-checkbox>
              <a-textarea
                v-model:value="manualRemark"
                :rows="5"
                :placeholder="
                  outsourcedDataTaskActionTexts.manualRemarkPlaceholder
                "
              />
              <div class="outsourced-task-prerequisites">
                <h4>
                  {{ outsourcedDataTaskActionTexts.manualPrerequisitesTitle }}
                </h4>
                <ul>
                  <li
                    v-for="item in page.manualState.rerunPrerequisites"
                    :key="item"
                  >
                    {{ item }}
                  </li>
                </ul>
              </div>
              <a-space>
                <YButton type="primary" :disabled="!manualExceptionConfirmed">
                  {{ outsourcedDataTaskActionTexts.manualConfirmButtonText }}
                </YButton>
                <YButton
                  :disabled="!manualExceptionConfirmed"
                  @click="confirmRetry(page.selectedRow)"
                >
                  {{ outsourcedDataTaskActionTexts.manualRerunButtonText }}
                </YButton>
              </a-space>
            </div>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-drawer>

    <a-drawer
      class="outsourced-task-drawer"
      :open="page.stepLogVisible"
      :title="
        page.activeStep
          ? `${page.activeStep.stepName}${outsourcedDataTaskActionTexts.detailStepLogSuffix}`
          : outsourcedDataTaskActionTexts.detailStepLogFallbackTitle
      "
      :width="880"
      @close="page.closeStepLogs"
    >
      <YTable
        :columns="logColumns"
        :data="page.stepLogRows"
        :loading="page.stepLogLoading"
        :row-config="{ keyField: 'key' }"
        :pageable="false"
        :autoFlexColumn="false"
      >
        <template #logLevel="{ row }">
          <a-tag
            :color="
              row.logLevel === 'ERROR'
                ? 'red'
                : row.logLevel === 'WARN'
                  ? 'orange'
                  : 'blue'
            "
          >
            {{ row.logLevel || "INFO" }}
          </a-tag>
        </template>
        <template #status="{ row }">
          <a-tag :color="page.formatStatusColor(row.status)">{{
            row.statusName
          }}</a-tag>
        </template>
        <template #errorStack="{ row }">
          <pre v-if="row.errorStack" class="outsourced-task-stack">{{
            row.errorStack
          }}</pre>
          <span v-else class="outsourced-task-muted">
            {{ outsourcedDataTaskActionTexts.noErrorStackText }}
          </span>
        </template>
      </YTable>
    </a-drawer>

    <a-drawer
      class="outsourced-task-drawer"
      :open="page.stepDataVisible"
      :title="
        page.activeStep
          ? `${page.activeStep.stepName}${outsourcedDataTaskActionTexts.detailStepDataSuffix}`
          : outsourcedDataTaskActionTexts.detailStepDataFallbackTitle
      "
      :width="720"
      @close="page.closeStepData"
    >
      <template v-if="page.activeStep">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.step">{{
            page.activeStep.stepName
          }}</a-descriptions-item>
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.taskId">{{
            page.activeStep.taskId || "-"
          }}</a-descriptions-item>
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.taskType">{{
            page.activeStep.taskType || "-"
          }}</a-descriptions-item>
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.inputSummary">{{
            page.activeStep.inputSummary || "-"
          }}</a-descriptions-item>
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.outputSummary">{{
            page.activeStep.outputSummary || "-"
          }}</a-descriptions-item>
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.errorMessage">
            <span
              class="outsourced-task-step-error"
              :class="{
                'is-highlight':
                  page.activeStep.status === 'FAILED' ||
                  page.activeStep.status === 'BLOCKED',
              }"
            >
              {{ page.activeStep.errorMessage || "-" }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item :label="outsourcedDataTaskTableTexts.detailFields.logRef">{{
            page.activeStep.logRef || "-"
          }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>

    <a-drawer
      class="outsourced-task-drawer"
      :open="page.historyVisible"
      :title="outsourcedDataTaskActionTexts.historyDrawerTitle"
      :width="1200"
      @close="page.closeHistoryDrawer"
    >
      <div class="outsourced-task-history">
        <div class="outsourced-task-history__summary">
          <span>{{ outsourcedDataTaskActionTexts.historyDrawerDescription }}</span>
          <a-tag color="blue"
            >{{ outsourcedDataTaskActionTexts.historyTotalPrefix
            }}{{ page.historyPagination.total }}</a-tag
          >
        </div>
        <YTable
          :columns="historyColumns"
          :data="page.historyRows"
          :loading="page.historyLoading"
          :height="720"
          :row-config="{ keyField: 'batchId' }"
          :checkbox-config="{ highlight: true }"
          :pageable="true"
          :autoFlexColumn="false"
          v-model:pagination="page.historyPagination"
          :toolbar-config="{ custom: false }"
          @page-change="page.handleHistoryPageChange"
        >
          <template #batchName="{ row }">
            <a @click="page.openDetailDrawer(row)">{{ row.batchName }}</a>
          </template>
          <template #status="{ row }">
            <a-tag :color="page.formatStatusColor(row.status)">{{
              row.statusName
            }}</a-tag>
          </template>
          <template #progress="{ row }">
            <a-progress
              :percent="row.progress"
              size="small"
              :status="row.status === 'FAILED' ? 'exception' : undefined"
            />
          </template>
        </YTable>
      </div>
    </a-drawer>
  </div>
</template>
