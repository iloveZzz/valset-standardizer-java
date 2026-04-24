<script setup lang="ts">
import { YButton, YCard, YTable } from "@yss-ui/components";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons-vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferRunLogColumns } from "../../TransferShared/hooks/useTransferTableColumns";
import type { RunLogPage } from "../types";

const { page } = defineProps<{
  page: RunLogPage;
}>();

const columns = useTransferRunLogColumns();

const actionConfig = useTableActionConfig({
  width: 180,
  displayLimit: 2,
  buttons: [
    {
      text: "详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: any) => page.openDetailDrawer(row),
    },
    {
      text: "重新投递",
      key: "redeliver",
      type: "link",
      disabledFn: ({ row }: any) => !page.isFailedDeliverRow(row),
      clickFn: ({ row }: any) => page.redeliverRunLog(row),
    },
  ],
});
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="20">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h1>分拣运行日志</h1>
          <p>
            查询分拣任务的运行阶段、运行状态、来源信息和错误详情，用于定位分拣链路中的异常、失败投递和重投递过程。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持按来源、路由、阶段和状态过滤</span>
            <span class="workspace-pill">查看运行说明与错误信息</span>
            <span class="workspace-pill">失败投递支持批量重新投递</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton theme="primary" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询日志
            </YButton>
            <YButton @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置条件
            </YButton>
          </div>
        </div>
      </div>

      <div class="workspace-summary">
        <div class="workspace-summary-title">
          <strong>阶段统计</strong>
          <span>按来源 - 路由 - 目标顺序展示各阶段的状态数量</span>
        </div>
        <a-spin :spinning="page.analysisLoading">
          <div class="workspace-analysis-grid">
            <div
              v-for="stageItem in page.analysis.stageAnalyses"
              :key="stageItem.runStage"
              class="analysis-card-shell"
              @click="page.applyStageFilter(stageItem.runStage)"
            >
              <YCard class="analysis-card" :bordered="false" :padding="18">
                <div class="analysis-card-header">
                  <div>
                    <div class="analysis-card-label">
                      {{ stageItem.stageLabel }}
                    </div>
                    <div class="analysis-card-desc">
                      {{
                        page.formatStageLabel(stageItem.runStage)
                      }}阶段日志统计
                    </div>
                  </div>
                  <a-tag color="blue">{{ stageItem.totalCount }} 条</a-tag>
                </div>
                <div class="analysis-card-status-list">
                  <button
                    v-for="statusItem in stageItem.statusCounts"
                    :key="`${stageItem.runStage}-${statusItem.runStatus}`"
                    type="button"
                    class="analysis-status-chip"
                    :class="page.getStatusChipClass(statusItem.runStatus)"
                    @click.stop="
                      page.applyStageStatusFilter(
                        stageItem.runStage,
                        statusItem.runStatus,
                      )
                    "
                  >
                    <span class="analysis-status-chip-label">
                      {{ statusItem.statusLabel }}
                    </span>
                    <span class="analysis-status-chip-value">
                      {{ statusItem.count }}
                    </span>
                  </button>
                  <div
                    v-if="!stageItem.statusCounts.length"
                    class="analysis-card-empty"
                  >
                    当前筛选下暂无日志
                  </div>
                </div>
              </YCard>
            </div>
          </div>
        </a-spin>
      </div>
    </YCard>

    <div class="workspace-body">
      <YTable
        :ref="page.setTableRef"
        :columns="columns"
        :action-config="actionConfig"
        :data="page.rows"
        :loading="page.loading || page.redeliverLoading"
        :row-config="{ keyField: 'runLogId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @checkbox-change="page.refreshSelectedRows"
        @checkbox-all="page.refreshSelectedRows"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="分拣运行日志列表"
            :description="`按条件检索运行日志，查看分页结果、失败投递和单条详情。`"
            :meta="`当前共 ${page.total} 条日志`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="来源ID">
                <a-input
                  v-model:value="page.query.sourceId"
                  style="width: 160px"
                  placeholder="输入来源ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="分拣ID">
                <a-input
                  v-model:value="page.query.transferId"
                  style="width: 160px"
                  placeholder="输入分拣ID"
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
              <a-form-item label="运行阶段">
                <a-input
                  v-model:value="page.query.runStage"
                  style="width: 160px"
                  placeholder="例如 EXECUTE"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="运行状态">
                <a-input
                  v-model:value="page.query.runStatus"
                  style="width: 160px"
                  placeholder="例如 SUCCESS"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="触发类型">
                <a-input
                  v-model:value="page.query.triggerType"
                  style="width: 160px"
                  placeholder="例如 MANUAL"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="关键字">
                <a-input
                  v-model:value="page.query.keyword"
                  style="width: 180px"
                  placeholder="日志关键字"
                  allow-clear
                />
              </a-form-item>
              <a-form-item class="workspace-table-toolbar-actions">
                <YButton theme="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>
        <template #toolbar-right>
          <div class="workspace-table-toolbar-right">
            <a-tag color="red">
              已选失败日志 {{ page.selectedFailedCount }} 条
            </a-tag>
            <YButton
              theme="primary"
              :loading="page.redeliverLoading"
              @click="page.redeliverSelectedFailedLogs"
            >
              批量重新投递
            </YButton>
          </div>
        </template>

        <template #runStatus="{ row }">
          <a-tag :color="page.runStatusTagColor(row.runStatus)">
            {{ page.formatText(row.runStatus) }}
          </a-tag>
        </template>
        <template #runStage="{ row }">
          {{ page.formatText(row.runStage) }}
        </template>
        <template #originalName="{ row }">
          {{ page.formatText(row.originalName) }}
        </template>
      </YTable>
    </div>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="分拣运行日志详情"
      :width="720"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{ page.selectedRow.runLogId || "分拣运行日志详情" }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.formatText(page.selectedRow.runStatus) }} ·
            {{ page.formatText(page.selectedRow.createdAt) }}
          </div>
        </div>

        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="日志ID">
            {{ page.formatText(page.selectedRow.runLogId) }}
          </a-descriptions-item>
          <a-descriptions-item label="来源名称">
            {{ page.formatText(page.selectedRow.sourceName) }}
          </a-descriptions-item>
          <a-descriptions-item label="路由名称">
            {{ page.formatText(page.selectedRow.routeName) }}
          </a-descriptions-item>
          <a-descriptions-item label="目标名称">
            {{ page.formatText(page.selectedRow.targetName) }}
          </a-descriptions-item>
          <a-descriptions-item label="来源ID">
            {{ page.formatText(page.selectedRow.sourceId) }}
          </a-descriptions-item>
          <a-descriptions-item label="来源编码">
            {{ page.formatText(page.selectedRow.sourceCode) }}
          </a-descriptions-item>
          <a-descriptions-item label="路由ID">
            {{ page.formatText(page.selectedRow.routeId) }}
          </a-descriptions-item>
          <a-descriptions-item label="分拣ID">
            {{ page.formatText(page.selectedRow.transferId) }}
          </a-descriptions-item>
          <a-descriptions-item label="原始文件名">
            {{ page.formatText(page.selectedRow.originalName) }}
          </a-descriptions-item>
          <a-descriptions-item label="来源类型">
            {{ page.formatText(page.selectedRow.sourceType) }}
          </a-descriptions-item>
          <a-descriptions-item label="运行阶段">
            {{ page.formatText(page.selectedRow.runStage) }}
          </a-descriptions-item>
          <a-descriptions-item label="运行状态">
            <a-tag :color="page.runStatusTagColor(page.selectedRow.runStatus)">
              {{ page.formatText(page.selectedRow.runStatus) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="触发类型">
            {{ page.formatText(page.selectedRow.triggerType) }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ page.formatText(page.selectedRow.createdAt) }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-json-block">
          <h4>运行说明</h4>
          <pre>{{ page.safeJson(page.selectedRow.logMessage) }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>错误信息</h4>
          <pre>{{ page.safeJson(page.selectedRow.errorMessage) }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
