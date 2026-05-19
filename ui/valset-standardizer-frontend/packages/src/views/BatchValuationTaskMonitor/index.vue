<script setup lang="ts">
import "./index.less";
import "@vue-flow/core/dist/style.css";
import "@vue-flow/core/dist/theme-default.css";
import "@vue-flow/controls/dist/style.css";
import { computed, h } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { Modal } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard, YTable, type YTableColumn } from "@yss-ui/components";
import ValuationTraceFlowCanvas from "./components/ValuationTraceFlowCanvas.vue";
import { useBatchValuationTaskMonitorPage } from "./hooks/useBatchValuationTaskMonitorPage";
import {
  batchValuationTaskStatusCatalog,
} from "@/views/BatchValuationTask/constants";
import type {
  BatchValuationTaskBatchRow,
  BatchValuationTaskStepRow,
} from "@/views/BatchValuationTask/types";

defineOptions({ name: "BatchValuationTaskMonitorPage" });

const { page } = useBatchValuationTaskMonitorPage();

const DATE_FORMAT = "YYYY-MM-DD";

const taskDateValue = computed<Dayjs | undefined>({
  get: () => {
    const text = String(page.query.taskDate ?? "").trim();
    const parsed = dayjs(text);
    return parsed.isValid() ? parsed : undefined;
  },
  set: (value) => {
    page.query.taskDate = value ? value.format(DATE_FORMAT) : "";
  },
});

const stepColumns = computed<YTableColumn[]>(() => [
  { field: "stepName", title: "步骤", minWidth: 150 },
  { field: "statusName", title: "状态", width: 90 },
  { field: "startedAt", title: "开始时间", width: 155 },
  { field: "endedAt", title: "结束时间", width: 155 },
  { field: "durationText", title: "耗时", width: 90 },
  { field: "errorMessage", title: "错误摘要", minWidth: 220, ellipsis: true },
  { type: "action", title: "操作", width: 90, fixed: "right", align: "center" },
]);

const emptyText = computed(() =>
  page.selectedRow
    ? "当前任务暂无可展示的链路数据"
    : "请选择左侧估值解析任务查看链路",
);

const selectRow = (row: BatchValuationTaskBatchRow) => {
  void page.selectTask(row);
};

const confirmAction = (
  title: string,
  content: string,
  onOk: () => Promise<unknown>,
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

const retryStep = (step: BatchValuationTaskStepRow) => {
  confirmAction(
    "重跑 Batch Step",
    `将重跑步骤 ${step.stepName || step.stepId}，是否继续？`,
    () => page.retryStep(step),
  );
};
</script>

<template>
  <div class="valuation-monitor-page">
    <div class="valuation-monitor-layout">
      <YCard class="valuation-monitor-sidebar" :bordered="false" :padding="12">
        <div class="valuation-monitor-panel-head">
          <h3>解析任务</h3>
          <div class="valuation-monitor-panel-actions">
            <a-space>
              <a-date-picker
                  v-model:value="taskDateValue"
                  format="YYYY-MM-DD"
                  allow-clear
                  size="small"
                  placeholder="任务日期"
              />
              <a-select
                  v-model:value="page.query.status"
                  allow-clear
                  size="small"
                  placeholder="状态"
              >
                <a-select-option
                    v-for="item in batchValuationTaskStatusCatalog"
                :key="item.status"
                :value="item.status"
                >
                {{ item.label }}
              </a-select-option>
            </a-select>
            <a-tag color="blue">{{ page.totalCount }} 条</a-tag>
            </a-space>
          </div>
        </div>

        <div class="valuation-monitor-query">
          <a-form :model="page.query">
            <a-space class="valuation-monitor-query__inline" :size="8">
              <a-input
                v-model:value="page.query.batchId"
                allow-clear
                size="small"
                placeholder="批次号"
                @press-enter="page.runQuery"
              />
              <YButton size="small" type="primary" @click="page.runQuery">
                <template #icon><SearchOutlined /></template>
                查询
              </YButton>
            </a-space>
          </a-form>
        </div>

        <a-list
          :ref="page.setListContainerRef"
          class="valuation-monitor-task-list"
          :loading="page.loading"
          item-layout="horizontal"
          :data-source="page.rows"
          :locale="{ emptyText: '当前没有匹配的解析任务' }"
          @scroll.passive="page.handleListScroll"
        >
          <template #loadMore>
            <div v-if="page.loadingMore" class="valuation-monitor-list-footer">
              正在加载更多任务...
            </div>
            <div
              v-else-if="page.hasMoreRows"
              class="valuation-monitor-list-footer valuation-monitor-list-footer--hint"
            >
              <YButton size="small" type="link" @click="page.loadMoreRows">
                下滑继续加载更多任务
              </YButton>
            </div>
          </template>
          <template #renderItem="{ item: row }">
            <a-list-item :key="row.batchId || row.batchName">
              <button
                type="button"
                class="valuation-monitor-task-card"
                :class="{
                  'valuation-monitor-task-card--selected':
                    page.selectedBatchId === row.batchId,
                }"
                @click="selectRow(row)"
              >
                <div class="valuation-monitor-task-card__avatar">
                  {{ page.getTaskInitial(row) }}
                </div>
                <div class="valuation-monitor-task-card__body">
                  <div class="valuation-monitor-task-card__head">
                    <strong>{{ row.batchName || row.batchId || "-" }}</strong>
                    <a-tag :color="page.formatStatusColor(row.status)">
                      {{ row.statusName || row.status }}
                    </a-tag>
                  </div>
                  <div class="valuation-monitor-task-card__meta">
                    <span>业务日期：{{ row.businessDate || "-" }}</span>
                    <span>开始：{{ row.startedAt || "-" }}</span>
                  </div>
                  <div class="valuation-monitor-task-card__subject">
                    {{ row.productName || row.productCode || "未关联产品" }}
                  </div>
                  <div class="valuation-monitor-task-card__preview">
                    管理人：{{ row.managerName || "-" }} · 当前阶段：{{
                      row.currentStepName || row.currentStageName || "-"
                    }}
                  </div>
                  <div class="valuation-monitor-task-card__footer">
                    <span>{{ row.sourceTypeName || row.sourceType || "默认来源" }}</span>
                    <span>{{ row.durationText || "-" }}</span>
                  </div>
                </div>
              </button>
            </a-list-item>
          </template>
        </a-list>
      </YCard>

      <YCard class="valuation-monitor-main" :bordered="false" :padding="0">
        <div class="valuation-monitor-toolbar">
          <div class="valuation-monitor-toolbar__copy">
            <h2>解析链路监控</h2>
            <p>{{ page.graphSummary }}</p>
          </div>
          <div class="valuation-monitor-toolbar__actions">
            <a-switch
              size="small"
              :checked="page.autoRefresh"
              :disabled="!page.canAutoRefresh"
              checked-children="自动"
              un-checked-children="手动"
              @change="(checked) => page.toggleAutoRefresh(Boolean(checked))"
            />
            <YButton
              size="small"
              :loading="page.graphLoading"
              :disabled="!page.selectedBatchId"
              @click="page.refreshGraph"
            >
              刷新链路
            </YButton>
            <YButton
              v-if="page.canExecute()"
              size="small"
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '执行估值解析任务',
                  `将执行 ${page.selectedBatchId}，是否继续？`,
                  page.executeSelected,
                )
              "
            >
              执行
            </YButton>
            <YButton
              v-if="page.canRetry()"
              size="small"
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '全流程重跑',
                  `将全流程重跑 ${page.selectedBatchId}，是否继续？`,
                  page.retrySelected,
                )
              "
            >
              全流程重跑
            </YButton>
            <YButton
              v-if="page.canStop()"
              size="small"
              danger
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '停止估值解析任务',
                  `将停止 ${page.selectedBatchId}，是否继续？`,
                  page.stopSelected,
                  true,
                )
              "
            >
              停止
            </YButton>
          </div>
        </div>

        <ValuationTraceFlowCanvas
          :nodes="page.nodes"
          :edges="page.edges"
          :loading="page.graphLoading"
          :empty-text="emptyText"
          @node-click="page.openNodeDetail"
        />
      </YCard>
    </div>

    <a-drawer
      class="valuation-monitor-detail-drawer"
      :open="page.detailVisible"
      title="链路节点详情"
      :width="820"
      :destroyOnClose="true"
      :bodyStyle="{ padding: '14px' }"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedDetail">
        <div class="valuation-monitor-detail">
          <div class="valuation-monitor-detail__banner">
            <div>
              <h3>{{ page.selectedDetail.node.title }}</h3>
              <p>{{ page.selectedDetail.node.subtitle || "-" }}</p>
            </div>
            <a-tag :color="page.formatStatusColor(page.selectedDetail.node.status)">
              {{ page.selectedDetail.node.statusLabel }}
            </a-tag>
          </div>

          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="节点类型">
              {{ page.selectedDetail.node.kind }}
            </a-descriptions-item>
            <a-descriptions-item label="关联 ID">
              {{ page.selectedDetail.node.subtitle || "-" }}
            </a-descriptions-item>
            <a-descriptions-item label="执行时间">
              {{ page.selectedDetail.node.timeLabel || "-" }}
            </a-descriptions-item>
            <a-descriptions-item label="日志引用">
              {{ page.selectedDetail.node.meta || "-" }}
            </a-descriptions-item>
            <a-descriptions-item label="错误消息" :span="2">
              {{ page.selectedDetail.node.message || "-" }}
            </a-descriptions-item>
          </a-descriptions>

          <div class="valuation-monitor-detail__block">
            <h4>输入输出摘要</h4>
            <pre>{{ page.formatJson(page.selectedDetail.node.payload) }}</pre>
          </div>

          <div class="valuation-monitor-detail__block">
            <h4>Batch Step</h4>
            <YTable
              :columns="stepColumns"
              :data="page.selectedDetail.steps"
              :pageable="false"
              :border="false"
              :autoFlexColumn="false"
              :row-config="{ keyField: 'stepId' }"
            >
              <template #statusName="{ row }">
                <a-tag :color="page.formatStatusColor(row.status)">
                  {{ row.statusName || row.status }}
                </a-tag>
              </template>
              <template #action="{ row }">
                <YButton
                  size="small"
                  type="link"
                  :disabled="!page.canRetryStep(row)"
                  @click="retryStep(row)"
                >
                  重跑
                </YButton>
              </template>
            </YTable>
          </div>

          <div class="valuation-monitor-detail__block">
            <h4>日志摘要</h4>
            <pre>{{ page.formatJson(page.selectedTrace?.logs ?? []) }}</pre>
          </div>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
