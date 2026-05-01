<script setup lang="ts">
import { computed, ref, watch } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { message } from "ant-design-vue";
import { YButton, YCard } from "@yss-ui/components";
import {
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import OverviewRunLogConsole from "../../TransferOverview/components/OverviewRunLogConsole.vue";
import type { RunLogPage } from "../types";

const { page } = defineProps<{
  page: RunLogPage;
}>();

type CleanupMode = "1" | "3" | "5" | "15" | "30" | "custom";

const cleanupModalVisible = ref(false);
const cleanupMode = ref<CleanupMode>("1");
const cleanupRange = ref<[Dayjs, Dayjs] | null>(null);

const cleanupModeOptions = [
  { label: "近1天", value: "1" },
  { label: "近3天", value: "3" },
  { label: "近5天", value: "5" },
  { label: "近15天", value: "15" },
  { label: "近30天", value: "30" },
  { label: "自定义", value: "custom" },
] as const;

const buildPresetRange = (days: number): [Dayjs, Dayjs] => [
  dayjs().subtract(days, "day"),
  dayjs(),
];

const formatRangeLabel = (range: [Dayjs, Dayjs]) =>
  `${range[0].format("YYYY-MM-DD HH:mm:ss")} 至 ${range[1].format(
    "YYYY-MM-DD HH:mm:ss",
  )}`;

const syncPresetRange = () => {
  const days = Number(cleanupMode.value);
  if (Number.isFinite(days) && days > 0) {
    cleanupRange.value = buildPresetRange(days);
  }
};

const openCleanupDialog = () => {
  cleanupMode.value = "1";
  cleanupRange.value = buildPresetRange(1);
  cleanupModalVisible.value = true;
};

const closeCleanupDialog = () => {
  cleanupModalVisible.value = false;
};

const handleCleanupModeChange = () => {
  if (cleanupMode.value === "custom") {
    cleanupRange.value ??= buildPresetRange(1);
    return;
  }
  syncPresetRange();
};

const confirmCleanupLogs = async () => {
  if (cleanupMode.value === "custom") {
    if (!cleanupRange.value || cleanupRange.value.length !== 2) {
      message.warning("请选择清理时间区间");
      return;
    }
    if (!cleanupRange.value[0].isBefore(cleanupRange.value[1])) {
      message.warning("清理开始时间必须早于结束时间");
      return;
    }
  } else {
    syncPresetRange();
  }

  const range = cleanupRange.value;
  if (!range) {
    message.warning("请选择清理时间区间");
    return;
  }

  const cleanupLabel =
    cleanupMode.value === "custom"
      ? formatRangeLabel(range)
      : `近${cleanupMode.value}天`;

  await page.cleanupLogs({
    startInclusive: range[0].format("YYYY-MM-DDTHH:mm:ss"),
    endExclusive: range[1].format("YYYY-MM-DDTHH:mm:ss"),
    cleanupLabel,
  });
  closeCleanupDialog();
};

watch(cleanupMode, handleCleanupModeChange);
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
              @click="openCleanupDialog"
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

    <a-modal
      v-model:open="cleanupModalVisible"
      title="清理日志"
      :confirm-loading="page.cleanupLoading"
      ok-text="确定清理"
      cancel-text="取消"
      centered
      @ok="confirmCleanupLogs"
      @cancel="closeCleanupDialog"
    >
      <div class="cleanup-modal">
        <div class="cleanup-modal-tip">
          请选择要清理的日志时间范围。预设项按当前时间计算，自定义区间支持精确到秒。
        </div>
        <a-radio-group
          v-model:value="cleanupMode"
          button-style="solid"
          class="cleanup-mode-group"
        >
          <a-radio-button
            v-for="item in cleanupModeOptions"
            :key="item.value"
            :value="item.value"
            class="cleanup-mode-button"
          >
            {{ item.label }}
          </a-radio-button>
        </a-radio-group>

        <div v-if="cleanupMode === 'custom'" class="cleanup-range-block">
          <a-range-picker
            v-model:value="cleanupRange"
            allow-clear
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            class="cleanup-range-picker"
          />
        </div>

        <div class="cleanup-range-preview">
          当前将清理：{{ cleanupMode === "custom" ? (cleanupRange ? formatRangeLabel(cleanupRange) : "请选择时间区间") : `近${cleanupMode}天` }}
        </div>
      </div>
    </a-modal>
  </div>
</template>
