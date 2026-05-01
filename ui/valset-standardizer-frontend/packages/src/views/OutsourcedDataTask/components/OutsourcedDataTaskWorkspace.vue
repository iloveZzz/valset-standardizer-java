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
import type {
  OutsourcedDataTaskBatchRow,
  OutsourcedDataTaskPage,
  OutsourcedDataTaskStepRow,
} from "../types";

const { page } = defineProps<{
  page: OutsourcedDataTaskPage;
}>();

const columns = computed<YTableColumn[]>(() => [
  { type: "checkbox", width: 48, align: "center", fixed: "left" as const },
  { type: "expand", width: 46, fixed: "left" as const },
  {
    field: "batchName",
    title: "数据批次名称",
    minWidth: 260,
    fixed: "left" as const,
    ellipsis: true,
  },
  { field: "productCode", title: "产品代码", width: 120 },
  { field: "productName", title: "产品名称", width: 140 },
  { field: "managerName", title: "管理人", width: 130 },
  { field: "valuationDate", title: "估值日期", width: 120 },
  {
    field: "originalFileName",
    title: "文件名称",
    minWidth: 220,
    ellipsis: true,
  },
  { field: "currentStageName", title: "当前阶段", width: 140 },
  { field: "status", title: "状态", width: 110 },
  { field: "progress", title: "进度", width: 180 },
  { field: "startedAt", title: "开始时间", width: 180 },
  { field: "durationText", title: "耗时", width: 90 },
  {
    field: "lastErrorMessage",
    title: "异常原因",
    minWidth: 240,
    ellipsis: true,
  },
  { field: "action", title: "操作", width: 220, fixed: "right" as const },
]);

const stepColumns = computed<YTableColumn[]>(() => [
  { field: "stageName", title: "阶段名称", width: 150 },
  { field: "startedAt", title: "任务开始时间", width: 180 },
  { field: "durationText", title: "执行耗时", width: 100 },
  { field: "runNo", title: "执行次数", width: 100 },
  { field: "triggerModeName", title: "触发方式", width: 120 },
  { field: "status", title: "状态", width: 110 },
  { field: "errorMessage", title: "错误摘要", minWidth: 240, ellipsis: true },
  { field: "action", title: "操作", width: 190, fixed: "right" as const },
]);

const dataEntryColumns = computed<YTableColumn[]>(() => [
  { field: "name", title: "入口名称", width: 130 },
  { field: "description", title: "说明", minWidth: 280, ellipsis: true },
  { field: "status", title: "状态", width: 110 },
  { field: "action", title: "操作", width: 120, fixed: "right" as const },
]);

const logColumns = computed<YTableColumn[]>(() => [
  { field: "stageName", title: "阶段", width: 140 },
  { field: "logLevel", title: "级别", width: 90 },
  { field: "occurredAt", title: "日志时间", width: 180 },
  { field: "startedAt", title: "开始时间", width: 180 },
  { field: "durationText", title: "耗时", width: 90 },
  { field: "status", title: "状态", width: 100 },
  { field: "message", title: "阶段日志", minWidth: 260, ellipsis: true },
  { field: "errorStack", title: "错误堆栈", minWidth: 320 },
]);

const detailActiveKey = ref("overview");
const manualExceptionConfirmed = ref(false);
const manualRemark = ref("");

const dataStatusColor = (status: string) => {
  if (status === "READY") return "green";
  if (status === "ERROR") return "red";
  return "default";
};

const statusLabelMap: Record<string, string> = {
  PENDING: "待处理",
  RUNNING: "处理中",
  SUCCESS: "已完成",
  FAILED: "失败",
  STOPPED: "已停止",
  BLOCKED: "阻塞",
};

const formatStageLabel = (stage: string) =>
  page.stageSummaries.find((item) => item.stage === stage)?.stageName || stage;

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
    "执行委外数据任务",
    "将提交该批次的数据处理任务，是否继续？",
    () => page.executeBatch(row),
  );
};

const confirmRetry = (row: OutsourcedDataTaskBatchRow) => {
  openActionConfirm(
    "重跑委外数据任务",
    "将从失败或当前阶段重新执行该批次，是否继续？",
    () => page.retryBatch(row),
  );
};

const confirmStop = (row: OutsourcedDataTaskBatchRow) => {
  openActionConfirm(
    "停止委外数据任务",
    "将停止该批次当前运行阶段，是否继续？",
    () => page.stopBatch(row),
  );
};

const confirmRetryStep = (row: OutsourcedDataTaskStepRow) => {
  openActionConfirm(
    "重跑任务阶段",
    `将重新执行 ${row.stageName} 阶段，是否继续？`,
    () => page.retryStep(row),
  );
};

const activeFilterText = computed(() => {
  const filters = [
    page.query.businessDate && `任务日期：${page.query.businessDate}`,
    page.query.managerName && `管理人：${page.query.managerName}`,
    page.query.productKeyword && `产品：${page.query.productKeyword}`,
    page.query.stage && `阶段：${formatStageLabel(page.query.stage)}`,
    page.query.status && `状态：${formatStatusLabel(page.query.status)}`,
    page.query.sourceType && `来源：${page.query.sourceType}`,
    page.query.errorType && `异常：${page.query.errorType}`,
  ].filter(Boolean);
  return filters.length ? filters.join(" / ") : "全部任务";
});

const taskMetricCards = computed(() => [
  {
    key: "total",
    label: "今日数据批次",
    value: page.totalCount,
    description: "当前任务日期内累计进入处理链路的数据批次",
    tone: "primary",
    active: !page.query.status,
    onClick: () => page.selectStatus(""),
  },
  {
    key: "running",
    label: "处理中",
    value: page.runningCount,
    description: "正在解析、标准化、落地或加工的任务批次",
    tone: "warning",
    active: page.query.status === "RUNNING",
    onClick: () => page.selectStatus("RUNNING"),
  },
  {
    key: "success",
    label: "处理完成",
    value: page.successCount,
    description: "已完成校验归档并进入可用状态的批次",
    tone: "success",
    active: page.query.status === "SUCCESS",
    onClick: () => page.selectStatus("SUCCESS"),
  },
  {
    key: "failed",
    label: "异常待处理",
    value: page.failedCount,
    description: "失败或阻塞后等待定位、修复和重跑的批次",
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
          <h2>委外数据任务管理</h2>
          <p>
            覆盖估值文件解析、结构标准化、标准表落地和后续数据加工的任务链路。
          </p>
        </div>
        <div class="outsourced-task-actions">
          <YButton type="primary" @click="page.batchExecute">
            <template #icon><PlayCircleOutlined /></template>
            批量执行
          </YButton>
          <YButton @click="page.batchRetry">
            <template #icon><ReloadOutlined /></template>
            批量重跑
          </YButton>
          <YButton danger @click="page.batchStop">
            <template #icon><StopOutlined /></template>
            批量停止
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
          v-for="(item, index) in page.stageSummaries"
          :key="item.stage"
          class="outsourced-task-stage"
          :class="{ 'is-active': page.query.stage === item.stage }"
          @click="page.selectStage(item.stage)"
        >
          <span class="outsourced-task-stage__title"
            >{{ index + 1 }}.{{ item.stageName }}</span
          >
          <span class="outsourced-task-stage__desc">{{
            item.stageDescription
          }}</span>
          <span v-if="item.failedCount" class="outsourced-task-stage__badge">{{
            item.failedCount
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
            <a-select-option value="PENDING">待处理</a-select-option>
            <a-select-option value="RUNNING">处理中</a-select-option>
            <a-select-option value="SUCCESS">已完成</a-select-option>
            <a-select-option value="FAILED">失败</a-select-option>
            <a-select-option value="STOPPED">已停止</a-select-option>
            <a-select-option value="BLOCKED">阻塞</a-select-option>
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

    <div class="outsourced-task-table">
      <YTable
        :columns="columns"
        :data="page.tableData"
        :loading="page.loading"
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
              <strong>阶段执行明细</strong>
              <span
                >按文件解析、结构标准化、科目识别、标准表落地、加工任务、校验归档的业务阶段顺序展示。</span
              >
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
                  <a @click="page.openStepLogs(stepRow)">日志</a>
                  <a @click="page.openStepData(stepRow)">数据</a>
                  <a
                    :class="{ 'is-disabled': stepRow.status === 'PENDING' }"
                    @click="confirmRetryStep(stepRow)"
                  >
                    重跑
                  </a>
                </a-space>
              </template>
            </YTable>
          </div>
        </template>
        <template #action="{ row }">
          <a-space>
            <a @click="page.openDetailDrawer(row)">查看</a>
            <a @click="confirmExecute(row)">执行</a>
            <a @click="confirmRetry(row)">重跑</a>
            <a v-if="row.status === 'RUNNING'" @click="confirmStop(row)"
              >停止</a
            >
          </a-space>
        </template>
      </YTable>
    </div>

    <a-drawer
      class="outsourced-task-drawer"
      :open="page.detailVisible"
      title="委外数据任务详情"
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
          <a-tab-pane key="overview" tab="链路概览">
            <a-alert
              v-if="page.manualState.currentBlockPoint"
              class="outsourced-task-detail-alert"
              type="info"
              show-icon
              :message="`当前阻塞点：${page.manualState.currentBlockPoint}`"
            />

            <a-descriptions bordered size="small" :column="2">
              <a-descriptions-item label="批次ID">{{
                page.selectedRow.batchId
              }}</a-descriptions-item>
              <a-descriptions-item label="当前阶段">{{
                page.selectedRow.currentStageName
              }}</a-descriptions-item>
              <a-descriptions-item label="原始文件">{{
                page.selectedRow.originalFileName
              }}</a-descriptions-item>
              <a-descriptions-item label="文件服务ID">{{
                page.selectedRow.filesysFileId
              }}</a-descriptions-item>
              <a-descriptions-item label="开始时间">{{
                page.selectedRow.startedAt
              }}</a-descriptions-item>
              <a-descriptions-item label="执行耗时">{{
                page.selectedRow.durationText
              }}</a-descriptions-item>
              <a-descriptions-item label="异常原因" :span="2">
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
                    <a @click="page.openStepLogs(row)">日志</a>
                    <a @click="page.openStepData(row)">数据</a>
                    <a
                      :class="{ 'is-disabled': row.status === 'PENDING' }"
                      @click="confirmRetryStep(row)"
                      >重跑</a
                    >
                  </a-space>
                </template>
              </YTable>
            </div>
          </a-tab-pane>

          <a-tab-pane key="data" tab="文件与数据">
            <div class="outsourced-task-data-summary">
              <div>
                <span>原始文件</span>
                <strong>{{ page.selectedRow.originalFileName || "-" }}</strong>
              </div>
              <div>
                <span>文件服务ID</span>
                <strong>{{
                  page.selectedRow.filesysFileId ||
                  page.selectedRow.fileId ||
                  "-"
                }}</strong>
              </div>
              <div>
                <span>估值日期</span>
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
                  >打开</a-button
                >
                <span v-else class="outsourced-task-muted">待接入</span>
              </template>
            </YTable>
          </a-tab-pane>

          <a-tab-pane key="logs" tab="执行日志">
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
                <span v-else class="outsourced-task-muted">暂无错误堆栈</span>
              </template>
            </YTable>
          </a-tab-pane>

          <a-tab-pane key="manual" tab="人工处理">
            <div class="outsourced-task-manual">
              <a-alert
                type="warning"
                show-icon
                :message="page.manualState.currentBlockPoint"
                :description="page.manualState.exceptionConfirmText"
              />
              <a-checkbox v-model:checked="manualExceptionConfirmed"
                >异常已确认，允许记录人工处理结论</a-checkbox
              >
              <a-textarea
                v-model:value="manualRemark"
                :rows="5"
                placeholder="填写处理备注、数据修正说明、重跑范围或外部确认结果"
              />
              <div class="outsourced-task-prerequisites">
                <h4>重跑前置提示</h4>
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
                <YButton type="primary" :disabled="!manualExceptionConfirmed"
                  >确认异常</YButton
                >
                <YButton
                  :disabled="!manualExceptionConfirmed"
                  @click="confirmRetry(page.selectedRow)"
                  >按前置检查重跑</YButton
                >
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
          ? `${page.activeStep.stageName}执行日志`
          : '阶段执行日志'
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
          <span v-else class="outsourced-task-muted">暂无错误堆栈</span>
        </template>
      </YTable>
    </a-drawer>

    <a-drawer
      class="outsourced-task-drawer"
      :open="page.stepDataVisible"
      :title="
        page.activeStep ? `${page.activeStep.stageName}阶段数据` : '阶段数据'
      "
      :width="720"
      @close="page.closeStepData"
    >
      <template v-if="page.activeStep">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item label="阶段">{{
            page.activeStep.stageName
          }}</a-descriptions-item>
          <a-descriptions-item label="任务ID">{{
            page.activeStep.taskId || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="任务类型">{{
            page.activeStep.taskType || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="输入摘要">{{
            page.activeStep.inputSummary || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="输出摘要">{{
            page.activeStep.outputSummary || "-"
          }}</a-descriptions-item>
          <a-descriptions-item label="错误摘要">
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
          <a-descriptions-item label="日志定位">{{
            page.activeStep.logRef || "-"
          }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>
