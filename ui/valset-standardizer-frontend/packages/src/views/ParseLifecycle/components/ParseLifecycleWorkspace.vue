<script setup lang="ts">
import { computed, h } from "vue";
import { Modal } from "ant-design-vue";
import {
  CheckCircleOutlined,
  DisconnectOutlined,
  ExclamationCircleOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  DeleteOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard, YTable } from "@yss-ui/components";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import type {
  ParseLifecycleEventRow,
  ParseLifecyclePage,
  ParseLifecycleTaskRow,
} from "../types";

const { page } = defineProps<{
  page: ParseLifecyclePage;
}>();

const columns = computed(() => [
  {
    field: "title",
    title: "任务对象",
    minWidth: 260,
    fixed: "left" as const,
    ellipsis: true,
  },
  {
    field: "statusLabel",
    title: "状态",
    width: 110,
  },
  {
    field: "currentStageLabel",
    title: "当前阶段",
    width: 150,
  },
  {
    field: "queueId",
    title: "队列ID",
    width: 180,
    ellipsis: true,
  },
  {
    field: "transferId",
    title: "transferId",
    width: 180,
    ellipsis: true,
  },
  {
    field: "taskId",
    title: "taskId",
    width: 110,
  },
  {
    field: "message",
    title: "最近消息",
    minWidth: 260,
    ellipsis: true,
  },
  {
    field: "updatedAt",
    title: "更新时间",
    width: 170,
  },
  {
    field: "eventCount",
    title: "事件数",
    width: 90,
  },
  {
    field: "durationText",
    title: "耗时",
    width: 100,
  },
]);

const rawColumns = computed(() => [
  {
    field: "displayTime",
    title: "发生时间",
    width: 170,
  },
  {
    field: "stageLabel",
    title: "阶段",
    width: 140,
  },
  {
    field: "source",
    title: "来源",
    width: 170,
    formatter: (params: any) => params?.cellValue || "-",
  },
  {
    field: "queueId",
    title: "队列ID",
    width: 180,
    ellipsis: true,
  },
  {
    field: "message",
    title: "消息",
    minWidth: 260,
    ellipsis: true,
  },
  {
    field: "repeatCount",
    title: "次数",
    width: 80,
  },
  {
    field: "errorMessage",
    title: "错误",
    minWidth: 240,
    ellipsis: true,
  },
]);

const openDisconnectConfirm = () => {
  Modal.confirm({
    title: "断开生命周期事件流",
    content: "将停止当前 SSE 订阅，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: "确定断开",
    cancelText: "取消",
    onOk: () => page.disconnect(),
  });
};

const openClearConfirm = () => {
  Modal.confirm({
    title: "清空事件记录",
    content: "将清空当前已接收的生命周期事件，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: "确定清空",
    cancelText: "取消",
    okButtonProps: { danger: true },
    onOk: () => page.clear(),
  });
};

const openDetail = (row: ParseLifecycleTaskRow) => {
  page.openDetailDrawer(row);
};

const openRawDetail = (row: ParseLifecycleEventRow) => {
  page.openRawDetailDrawer(row);
};
</script>

<template>
  <div class="transfer-workspace parse-lifecycle-page">
    <YCard class="workspace-header parse-lifecycle-hero" :bordered="false" :padding="12">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>解析生命周期事件</h2>
          <p>
            以任务当前态为主视图，观察者循环和批次空闲事件只更新运行状态，原始事件保留为排障日志。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">任务聚合</span>
            <span class="workspace-pill">事件去重压缩</span>
            <span class="workspace-pill">异常优先定位</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton @click="page.openQueuePage">返回待解析任务</YButton>
            <YButton
              type="primary"
              :loading="page.connecting"
              :disabled="page.connected"
              @click="page.connect"
            >
              <template #icon><CheckCircleOutlined /></template>
              连接流
            </YButton>
            <YButton :disabled="!page.connected" @click="openDisconnectConfirm">
              <template #icon><DisconnectOutlined /></template>
              断开流
            </YButton>
            <YButton @click="page.togglePause">
              <template #icon>
                <PlayCircleOutlined v-if="page.paused" />
                <PauseCircleOutlined v-else />
              </template>
              {{ page.paused ? "继续接收" : "暂停接收" }}
            </YButton>
            <YButton @click="openClearConfirm">
              <template #icon><DeleteOutlined /></template>
              清空记录
            </YButton>
            <YButton :loading="page.connecting" @click="page.connect">
              <template #icon><ReloadOutlined /></template>
              重新连接
            </YButton>
          </div>
        </div>
      </div>

      <div class="parse-lifecycle-overview">
        <div class="parse-lifecycle-metric parse-lifecycle-metric--connection">
          <div class="parse-lifecycle-metric-label">连接状态</div>
          <div class="parse-lifecycle-metric-value">
            {{ page.connected ? "已连接" : page.connecting ? "连接中" : "未连接" }}
          </div>
          <div class="parse-lifecycle-metric-desc">
            {{ page.paused ? "当前已暂停接收事件" : "SSE 实时接收生命周期事件" }}
          </div>
        </div>
        <div class="parse-lifecycle-metric">
          <div class="parse-lifecycle-metric-label">观察者状态</div>
          <div class="parse-lifecycle-metric-value">{{ page.observerStatus }}</div>
          <div class="parse-lifecycle-metric-desc">{{ page.latestMessage }}</div>
        </div>
        <div class="parse-lifecycle-metric">
          <div class="parse-lifecycle-metric-label">任务聚合</div>
          <div class="parse-lifecycle-metric-value">{{ page.taskTotal }}</div>
          <div class="parse-lifecycle-metric-desc">已归并到任务维度的对象数</div>
        </div>
        <div class="parse-lifecycle-metric parse-lifecycle-metric--active">
          <div class="parse-lifecycle-metric-label">处理中</div>
          <div class="parse-lifecycle-metric-value">{{ page.activeTaskCount }}</div>
          <div class="parse-lifecycle-metric-desc">当前仍在推进的任务</div>
        </div>
        <div class="parse-lifecycle-metric parse-lifecycle-metric--failed">
          <div class="parse-lifecycle-metric-label">异常任务</div>
          <div class="parse-lifecycle-metric-value">{{ page.failedTaskCount }}</div>
          <div class="parse-lifecycle-metric-desc">失败和冲突事件自动置顶</div>
        </div>
      </div>
    </YCard>

    <div class="workspace-body parse-lifecycle-body">
      <YTable
        :columns="columns"
        :border="false"
        :data="page.tableData"
        :loading="page.connecting"
        :row-config="{ keyField: 'identity' }"
        :pageable="false"
        :autoFlexColumn="false"
        :toolbar-config="{ custom: false }"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="任务当前态"
            description="同一队列或任务的生命周期事件已聚合为一行，点击最近消息查看完整链路"
            :meta="`原始事件 ${page.rawTotal} 条，压缩后任务 ${page.taskTotal} 个`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="来源">
                <a-input
                  v-model:value="page.query.source"
                  style="width: 140px"
                  placeholder="parse-queue-management"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="队列ID">
                <a-input
                  v-model:value="page.query.queueId"
                  style="width: 170px"
                  placeholder="输入 queueId"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="transferId">
                <a-input
                  v-model:value="page.query.transferId"
                  style="width: 170px"
                  placeholder="输入 transferId"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="阶段">
                <a-input
                  v-model:value="page.query.stage"
                  style="width: 180px"
                  placeholder="例如 TASK_FAILED"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="关键字">
                <a-input
                  v-model:value="page.query.keyword"
                  style="width: 190px"
                  placeholder="文件名/消息/错误"
                  allow-clear
                />
              </a-form-item>
              <a-form-item>
                <a-checkbox v-model:checked="page.query.onlyError">
                  只看异常
                </a-checkbox>
              </a-form-item>
              <a-form-item class="workspace-table-toolbar-actions">
                <YButton type="primary" @click="page.connect">应用并连接</YButton>
                <YButton @click="page.toggleRawEvents">
                  {{ page.showRawEvents ? "隐藏原始事件" : "展开原始事件" }}
                </YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>

        <template #title="{ row }">
          <a-button type="link" class="parse-lifecycle-title-link" @click="openDetail(row)">
            {{ row.title }}
          </a-button>
        </template>

        <template #statusLabel="{ row }">
          <a-tag :color="row.statusColor">{{ row.statusLabel }}</a-tag>
        </template>

        <template #currentStageLabel="{ row }">
          <a-tag :color="row.status === 'FAILED' ? 'red' : 'blue'">
            {{ row.currentStageLabel }}
          </a-tag>
        </template>

        <template #message="{ row }">
          <a-button type="link" style="padding: 0" @click="openDetail(row)">
            {{ row.message || "-" }}
          </a-button>
        </template>
      </YTable>

      <div v-if="page.showRawEvents" class="parse-lifecycle-raw-panel">
        <div class="parse-lifecycle-raw-header">
          <div>
            <div class="parse-lifecycle-raw-title">原始事件日志</div>
            <div class="parse-lifecycle-raw-desc">
              默认仅显示最近 200 条压缩事件，重复事件通过“次数”字段体现。
            </div>
          </div>
          <div class="parse-lifecycle-raw-actions">
            <a-checkbox :checked="page.autoScroll" @change="page.toggleAutoScroll">
              自动滚动
            </a-checkbox>
            <YButton @click="page.toggleRawEvents">收起</YButton>
          </div>
        </div>
        <YTable
          :columns="rawColumns"
          :border="false"
          :data="page.rawTableData"
          :loading="page.connecting"
          :row-config="{ keyField: 'identity' }"
          :pageable="false"
          :autoFlexColumn="false"
          :toolbar-config="{ custom: false }"
        >
          <template #stageLabel="{ row }">
            <a-tag
              :color="
                row.severity === 'FAILED'
                  ? 'red'
                  : row.severity === 'SUCCESS'
                    ? 'green'
                    : row.severity === 'IDLE'
                      ? 'default'
                      : 'blue'
              "
            >
              {{ row.stageLabel }}
            </a-tag>
          </template>
          <template #message="{ row }">
            <a-button type="link" style="padding: 0" @click="openRawDetail(row)">
              {{ row.message || row.errorMessage || "-" }}
            </a-button>
          </template>
        </YTable>
      </div>
    </div>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="任务生命周期详情"
      :width="920"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{ page.selectedRow.title }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.selectedRow.statusLabel }} ·
            {{ page.selectedRow.currentStageLabel }} ·
            {{ page.selectedRow.updatedAt || "-" }}
          </div>
        </div>

        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="队列ID">
            {{ page.selectedRow.queueId }}
          </a-descriptions-item>
          <a-descriptions-item label="transferId">
            {{ page.selectedRow.transferId }}
          </a-descriptions-item>
          <a-descriptions-item label="taskId">
            {{ page.selectedRow.taskId }}
          </a-descriptions-item>
          <a-descriptions-item label="businessKey">
            {{ page.selectedRow.businessKey }}
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">
            {{ page.selectedRow.startedAt }}
          </a-descriptions-item>
          <a-descriptions-item label="最近更新时间">
            {{ page.selectedRow.updatedAt }}
          </a-descriptions-item>
          <a-descriptions-item label="耗时">
            {{ page.selectedRow.durationText }}
          </a-descriptions-item>
          <a-descriptions-item label="事件数">
            {{ page.selectedRow.eventCount }}
          </a-descriptions-item>
          <a-descriptions-item label="错误信息" :span="2">
            {{ page.selectedRow.errorMessage || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <a-timeline class="parse-lifecycle-timeline">
          <a-timeline-item
            v-for="event in page.selectedRow.rawEvents.slice().reverse()"
            :key="event.identity"
            :color="
              event.severity === 'FAILED'
                ? 'red'
                : event.severity === 'SUCCESS'
                  ? 'green'
                  : event.severity === 'IDLE'
                    ? 'gray'
                    : 'blue'
            "
          >
            <div class="parse-lifecycle-timeline-title">
              {{ event.stageLabel }}
              <a-tag v-if="event.repeatCount > 1" color="blue">
                x{{ event.repeatCount }}
              </a-tag>
            </div>
            <div class="parse-lifecycle-timeline-meta">
              {{ event.displayTime }} · {{ event.source || "-" }}
            </div>
            <div class="parse-lifecycle-timeline-message">
              {{ event.message || event.errorMessage || "-" }}
            </div>
          </a-timeline-item>
        </a-timeline>
      </template>
    </a-drawer>

    <a-drawer
      class="source-detail-drawer"
      :open="page.rawDetailVisible"
      title="原始事件详情"
      :width="880"
      @close="page.closeRawDetail"
    >
      <template v-if="page.selectedEvent">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{ page.selectedEvent.stageLabel }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.selectedEvent.displayTime }} ·
            {{ page.selectedEvent.source || "-" }} ·
            重复 {{ page.selectedEvent.repeatCount }} 次
          </div>
        </div>

        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="事件ID">
            {{ page.selectedEvent.eventId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="阶段">
            {{ page.selectedEvent.stage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="队列ID">
            {{ page.selectedEvent.queueId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="transferId">
            {{ page.selectedEvent.transferId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="taskId">
            {{ page.selectedEvent.taskId == null ? "-" : page.selectedEvent.taskId }}
          </a-descriptions-item>
          <a-descriptions-item label="businessKey">
            {{ page.selectedEvent.businessKey || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="消息" :span="2">
            {{ page.selectedEvent.message || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="错误信息" :span="2">
            {{ page.selectedEvent.errorMessage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="属性" :span="2">
            <pre class="parse-lifecycle-json">{{ page.selectedEvent.attributesText }}</pre>
          </a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>
