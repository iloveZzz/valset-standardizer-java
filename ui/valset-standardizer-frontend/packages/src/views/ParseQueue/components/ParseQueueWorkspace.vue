<script setup lang="ts">
import { computed, h } from "vue";
import { Modal } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  PauseCircleOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import { YButton, YCard, YTable } from "@yss-ui/components";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useParseQueueColumns } from "../../TransferShared/hooks/useParseQueueColumns";
import type { ParseQueuePage, ParseQueueRow } from "../types";

const { page } = defineProps<{
  page: ParseQueuePage;
}>();

const columns = useParseQueueColumns();

const openGenerateConfirm = (row: ParseQueueRow, forceRebuild = false) => {
  Modal.confirm({
    title: forceRebuild ? "重建待解析任务" : "生成待解析任务",
    content: forceRebuild
      ? "将基于当前分拣对象重建待解析任务，是否继续？"
      : "将基于当前分拣对象生成待解析任务，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: forceRebuild ? "确定重建" : "确定生成",
    cancelText: "取消",
    okButtonProps: {
      danger: forceRebuild,
      loading: page.loading,
    },
    onOk: () => page.generateQueue(row, forceRebuild),
  });
};

const openBackfillConfirm = () => {
  Modal.confirm({
    title: "批量补漏待解析任务",
    content: "将基于当前筛选条件批量补生成缺失的待解析任务，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: "确定补漏",
    cancelText: "取消",
    okButtonProps: {
      loading: page.backfillLoading,
    },
    onOk: () => page.backfillCurrentScope(false),
  });
};

const actionConfig = useTableActionConfig({
  width: 180,
  fixed: "left",
  displayLimit: 3,
  buttons: [
    {
      text: "详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: any) => page.openDetailDrawer(row),
    },
    {
      text: "生命周期",
      key: "lifecycle",
      type: "link",
      clickFn: ({ row }: any) => page.openLifecyclePage(row),
    },
    {
      text: "补生成",
      key: "generate",
      type: "link",
      clickFn: ({ row }: any) =>
        openGenerateConfirm(row, row.parseStatus !== "PENDING"),
    },
    {
      text: "重试",
      key: "retry",
      type: "link",
      disabledFn: ({ row }: any) => row.parseStatus !== "FAILED",
      clickFn: ({ row }: any) => page.retryQueue(row),
    },
  ],
});

const summaryDescription = computed(
  () =>
    `当前筛选：${page.currentFilterSummary}。查询总数 ${page.total} 条；统计卡片基于当前页数据：待订阅 ${page.pendingCount} 条，解析中 ${page.parsingCount} 条，已解析 ${page.parsedCount} 条，解析失败 ${page.failedCount} 条；实时同步状态：${page.realtimeStatusText}。`,
);
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="12">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>待解析任务</h2>
          <p>
            目标投递成功后自动生成待解析任务，支持补漏、重试和结构化结果回查。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">自动生成于投递成功之后</span>
            <span class="workspace-pill">支持手工补漏和强制重建</span>
            <span class="workspace-pill">解析状态独立管理</span>
            <span class="workspace-pill">实时同步：{{ page.realtimeStatusText }}</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton
              type="primary"
              :loading="page.backfillLoading"
              @click="openBackfillConfirm"
            >
              <template #icon><SearchOutlined /></template>
              当前条件补漏
            </YButton>
            <YButton :loading="page.loading" @click="page.runQuery">
              <template #icon><ReloadOutlined /></template>
              刷新列表
            </YButton>
            <YButton @click="page.toggleRealtimeSync">
              <template #icon>
                <PauseCircleOutlined v-if="!page.realtimePaused" />
                <PlayCircleOutlined v-else />
              </template>
              {{ page.realtimePaused ? "继续实时同步" : "暂停实时同步" }}
            </YButton>
          </div>
        </div>
      </div>

      <div class="parse-queue-stat-grid">
        <div class="parse-queue-stat-card parse-queue-stat-card--total">
          <div class="parse-queue-stat-label">筛选总数</div>
          <div class="parse-queue-stat-value">{{ page.total }}</div>
          <div class="parse-queue-stat-desc">{{ summaryDescription }}</div>
        </div>
        <div class="parse-queue-stat-card parse-queue-stat-card--pending">
          <div class="parse-queue-stat-label">待订阅</div>
          <div class="parse-queue-stat-value">{{ page.pendingCount }}</div>
          <div class="parse-queue-stat-desc">
            当前页等待消费者订阅的任务数量
          </div>
        </div>
        <div class="parse-queue-stat-card parse-queue-stat-card--parsing">
          <div class="parse-queue-stat-label">解析中</div>
          <div class="parse-queue-stat-value">{{ page.parsingCount }}</div>
          <div class="parse-queue-stat-desc">
            当前页已经被消费者接管的任务数量
          </div>
        </div>
        <div class="parse-queue-stat-card parse-queue-stat-card--failed">
          <div class="parse-queue-stat-label">失败任务</div>
          <div class="parse-queue-stat-value">{{ page.failedCount }}</div>
          <div class="parse-queue-stat-desc">
            当前页可通过重试或补漏重新生成
          </div>
        </div>
        <div class="parse-queue-stat-card parse-queue-stat-card--realtime">
          <div class="parse-queue-stat-label">实时同步状态</div>
          <div class="parse-queue-stat-value parse-queue-stat-value--status">
            {{ page.realtimeStatusText }}
          </div>
          <div class="parse-queue-stat-desc">
            后端生命周期事件到达后自动同步当前筛选下的列表数据
          </div>
        </div>
      </div>
    </YCard>

    <div class="workspace-body">
      <YTable
        :columns="columns"
        :border="false"
        :action-config="actionConfig"
        :data="page.tableData"
        :loading="page.listLoading || page.loading || page.backfillLoading"
        :row-config="{ keyField: 'queueId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        :autoFlexColumn="false"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="待解析任务列表"
            :description="summaryDescription"
            :meta="`当前筛选：${page.currentFilterSummary} · 当前页 ${page.tableData.length} 条 · ${page.realtimeStatusText}`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="分拣ID">
                <a-input
                  v-model:value="page.query.transferId"
                  style="width: 150px"
                  placeholder="输入分拣ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="业务键">
                <a-input
                  v-model:value="page.query.businessKey"
                  style="width: 200px"
                  placeholder="输入业务键"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="来源编码">
                <a-input
                  v-model:value="page.query.sourceCode"
                  style="width: 160px"
                  placeholder="输入来源编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="路由ID">
                <a-input
                  v-model:value="page.query.routeId"
                  style="width: 160px"
                  placeholder="输入路由ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="标签编码">
                <a-input
                  v-model:value="page.query.tagCode"
                  style="width: 180px"
                  placeholder="例如 VALUATION_TABLE"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="文件状态">
                <a-input
                  v-model:value="page.query.fileStatus"
                  style="width: 140px"
                  placeholder="例如 已识别"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="投递状态">
                <a-input
                  v-model:value="page.query.deliveryStatus"
                  style="width: 140px"
                  placeholder="例如 已投递"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="解析状态">
                <a-input
                  v-model:value="page.query.parseStatus"
                  style="width: 140px"
                  placeholder="例如 PENDING"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="触发方式">
                <a-input
                  v-model:value="page.query.triggerMode"
                  style="width: 140px"
                  placeholder="例如 AUTO"
                  allow-clear
                />
              </a-form-item>
              <a-form-item class="workspace-table-toolbar-actions">
                <YButton type="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>

        <template #parseStatus="{ row }">
          <a-tag
            :color="
              row.parseStatus === 'PARSED'
                ? 'green'
                : row.parseStatus === 'PARSING'
                  ? 'processing'
                  : row.parseStatus === 'FAILED'
                    ? 'red'
                    : 'blue'
            "
          >
            {{ page.formatParseStatus(row.parseStatus) }}
          </a-tag>
        </template>

        <template #triggerMode="{ row }">
          <a-tag :color="row.triggerMode === 'AUTO' ? 'cyan' : 'orange'">
            {{ page.formatTriggerMode(row.triggerMode) }}
          </a-tag>
        </template>
      </YTable>
    </div>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="待解析任务详情"
      :width="840"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{ page.selectedRow.originalName || page.selectedRow.queueId }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.formatParseStatus(page.selectedRow.parseStatus) }} ·
            {{ page.formatTriggerMode(page.selectedRow.triggerMode) }} ·
            {{ page.selectedRow.createdAt || "-" }}
          </div>
        </div>

        <div class="parse-queue-detail-actions">
          <YButton
            type="primary"
            @click="page.openLifecyclePage(page.selectedRow)"
          >
            查看生命周期
          </YButton>
          <YButton
            :loading="page.loading"
            :disabled="
              !['PENDING', 'FAILED'].includes(page.selectedRow.parseStatus)
            "
            @click="page.subscribeQueue(page.selectedRow)"
          >
            订阅任务
          </YButton>
          <YButton
            :loading="page.loading"
            :disabled="page.selectedRow.parseStatus !== 'PARSING'"
            @click="page.completeQueue(page.selectedRow)"
          >
            标记已解析
          </YButton>
          <YButton
            :loading="page.loading"
            :disabled="
              !['PENDING', 'PARSING'].includes(page.selectedRow.parseStatus)
            "
            danger
            @click="page.failQueue(page.selectedRow)"
          >
            标记失败
          </YButton>
          <YButton
            :loading="page.loading"
            @click="
              openGenerateConfirm(
                page.selectedRow,
                page.selectedRow.parseStatus !== 'PENDING',
              )
            "
          >
            补生成
          </YButton>
          <YButton
            :loading="page.loading"
            :disabled="page.selectedRow.parseStatus !== 'FAILED'"
            @click="page.retryQueue(page.selectedRow)"
          >
            重试
          </YButton>
        </div>

        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="队列ID">
            {{ page.selectedRow.queueId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="业务键">
            {{ page.selectedRow.businessKey || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="分拣ID">
            {{ page.selectedRow.transferId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="原始文件名">
            {{ page.selectedRow.originalName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="来源编码">
            {{ page.selectedRow.sourceCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="路由ID">
            {{ page.selectedRow.routeId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="标签">
            {{ page.selectedRow.tagName || page.selectedRow.tagCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="文件状态">
            {{ page.formatStatus(page.selectedRow.fileStatus) }}
          </a-descriptions-item>
          <a-descriptions-item label="投递状态">
            {{ page.formatStatus(page.selectedRow.deliveryStatus) }}
          </a-descriptions-item>
          <a-descriptions-item label="解析状态">
            {{ page.formatParseStatus(page.selectedRow.parseStatus) }}
          </a-descriptions-item>
          <a-descriptions-item label="触发方式">
            {{ page.formatTriggerMode(page.selectedRow.triggerMode) }}
          </a-descriptions-item>
          <a-descriptions-item label="重试次数">
            {{ page.selectedRow.retryCount ?? 0 }}
          </a-descriptions-item>
          <a-descriptions-item label="订阅人">
            {{ page.selectedRow.subscribedBy || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="订阅时间">
            {{ page.selectedRow.subscribedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="解析完成时间">
            {{ page.selectedRow.parsedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ page.selectedRow.createdAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ page.selectedRow.updatedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="错误信息" :span="2">
            {{ page.selectedRow.lastErrorMessage || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="parse-queue-json-block">
          <h4>对象快照</h4>
          <pre>{{ page.safeJson(page.selectedRow.objectSnapshotJson) }}</pre>
        </div>
        <div class="parse-queue-json-block">
          <h4>投递快照</h4>
          <pre>{{ page.safeJson(page.selectedRow.deliverySnapshotJson) }}</pre>
        </div>
        <div class="parse-queue-json-block">
          <h4>解析请求</h4>
          <pre>{{ page.safeJson(page.selectedRow.parseRequestJson) }}</pre>
        </div>
        <div class="parse-queue-json-block">
          <h4>结构化结果</h4>
          <pre>{{ page.safeJson(page.selectedRow.parseResultJson) }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
