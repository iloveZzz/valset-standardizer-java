<script setup lang="ts">
import { h } from "vue";
import { Modal } from "ant-design-vue";
import { YButton, YCard } from "@yss-ui/components";
import {
  ExclamationCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import OverviewRunLogConsole from "../../TransferOverview/components/OverviewRunLogConsole.vue";
import type { RunLogPage } from "../types";

const { page } = defineProps<{
  page: RunLogPage;
}>();

const confirmCleanupLogs = () => {
  Modal.confirm({
    title: "清理日志",
    content: "将清理前一天产生的运行日志，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: "确定清理",
    cancelText: "取消",
    okButtonProps: {
      danger: true,
      loading: page.cleanupLoading,
    },
    onOk: () => page.cleanupLogs(),
  });
};
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="12">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>运行日志</h2>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton type="primary" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询日志
            </YButton>
            <YButton @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置条件
            </YButton>
            <YButton
              danger
              :loading="page.cleanupLoading"
              @click="confirmCleanupLogs"
            >
              清理日志
            </YButton>
          </div>
        </div>
      </div>

      <div class="workspace-summary">
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
      <div class="run-log-console-slot">
        <OverviewRunLogConsole :items="page.consoleItems" />
      </div>
    </div>
  </div>
</template>
