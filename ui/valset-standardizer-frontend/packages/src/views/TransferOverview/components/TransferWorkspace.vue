<script setup lang="ts">
import { h, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { Modal } from "ant-design-vue";
import { ExclamationCircleOutlined } from "@ant-design/icons-vue";
import * as echarts from "echarts";
import { YButton, YCard, YTable } from "@yss-ui/components";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferOverviewRecentDeliveryColumns } from "../../TransferShared/hooks/useTransferTableColumns";

defineOptions({ name: "TransferWorkspace" });

const props = defineProps<{
  page: any;
}>();

const page = props.page;
const recentDeliveryColumns = useTransferOverviewRecentDeliveryColumns();

const recentDeliveryActionConfig = useTableActionConfig({
  width: 120,
  displayLimit: 1,
  buttons: [
    {
      text: "查看快照",
      key: "snapshot",
      type: "link",
      clickFn: ({ row }: any) => openLogSnapshot(row),
    },
  ],
});

const trendChartRef = ref<HTMLDivElement | null>(null);
const statusChartRef = ref<HTMLDivElement | null>(null);
const stageChartRef = ref<HTMLDivElement | null>(null);
let trendChartInstance: echarts.ECharts | null = null;
let statusChartInstance: echarts.ECharts | null = null;
let stageChartInstance: echarts.ECharts | null = null;
let trendChartObserver: ResizeObserver | null = null;
let statusChartObserver: ResizeObserver | null = null;
let stageChartObserver: ResizeObserver | null = null;

const bindChartObservers = () => {
  if (trendChartRef.value && !trendChartObserver) {
    trendChartObserver = new ResizeObserver(() => {
      trendChartInstance?.resize();
    });
    trendChartObserver.observe(trendChartRef.value);
  }
  if (statusChartRef.value && !statusChartObserver) {
    statusChartObserver = new ResizeObserver(() => {
      statusChartInstance?.resize();
    });
    statusChartObserver.observe(statusChartRef.value);
  }
  if (stageChartRef.value && !stageChartObserver) {
    stageChartObserver = new ResizeObserver(() => {
      stageChartInstance?.resize();
    });
    stageChartObserver.observe(stageChartRef.value);
  }
};

const renderOverviewCharts = async () => {
  await nextTick();
  bindChartObservers();
  await Promise.all([
    renderTrendChart(),
    renderStatusChart(),
    renderStageChart(),
  ]);
};

const openLogSnapshot = (row: any) => {
  Modal.info({
    title: "运行快照",
    width: 920,
    icon: h(ExclamationCircleOutlined),
    content: h(
      "pre",
      { class: "snapshot-code" },
      page.formatJson({
        deliveryId: row.deliveryId,
        transferId: row.transferId,
        routeId: row.routeId,
        targetCode: row.targetCode,
        targetType: row.targetType,
        executeStatus: row.executeStatus,
        executeStatusLabel: row.executeStatusLabel,
        requestSnapshotJson: row.requestSnapshotJson,
        responseSnapshotJson: row.responseSnapshotJson,
        errorMessage: row.errorMessage,
        deliveredAt: row.deliveredAt,
      }),
    ),
  });
};

const renderTrendChart = async () => {
  if (page.activeSection !== "overview") {
    return;
  }

  await nextTick();
  const element = trendChartRef.value;
  if (!element) {
    return;
  }

  if (!trendChartInstance) {
    trendChartInstance = echarts.init(element);
  }

  trendChartInstance.setOption(page.trendChartOption, true);
  trendChartInstance.resize();
};

const renderStatusChart = async () => {
  if (page.activeSection !== "overview") {
    return;
  }

  await nextTick();
  const element = statusChartRef.value;
  if (!element) {
    return;
  }

  if (!statusChartInstance) {
    statusChartInstance = echarts.init(element);
  }

  statusChartInstance.setOption(page.overviewStatusChartOption, true);
  statusChartInstance.resize();
};

const renderStageChart = async () => {
  if (page.activeSection !== "overview") {
    return;
  }

  await nextTick();
  const element = stageChartRef.value;
  if (!element) {
    return;
  }

  if (!stageChartInstance) {
    stageChartInstance = echarts.init(element);
  }

  stageChartInstance.setOption(page.overviewStageChartOption, true);
  stageChartInstance.resize();
};

watch(
  () => page.trendChartOption,
  () => {
    void renderTrendChart();
  },
  {
    deep: true,
    immediate: true,
  },
);

watch(
  () => page.overviewStatusChartOption,
  () => {
    void renderStatusChart();
  },
  {
    deep: true,
    immediate: true,
  },
);

watch(
  () => page.overviewStageChartOption,
  () => {
    void renderStageChart();
  },
  {
    deep: true,
    immediate: true,
  },
);

watch(
  () => page.activeSection,
  (section) => {
    if (section === "overview") {
      void renderOverviewCharts();
    }
  },
  {
    immediate: true,
  },
);

watch(
  () => page.loading,
  (loading) => {
    if (!loading && page.activeSection === "overview") {
      void renderOverviewCharts();
    }
  },
  {
    flush: "post",
  },
);

onMounted(() => {
  if (!page.loading) {
    void renderOverviewCharts();
  }
});

onBeforeUnmount(() => {
  trendChartObserver?.disconnect();
  statusChartObserver?.disconnect();
  stageChartObserver?.disconnect();
  trendChartInstance?.dispose();
  statusChartInstance?.dispose();
  stageChartInstance?.dispose();
  trendChartObserver = null;
  statusChartObserver = null;
  stageChartObserver = null;
  trendChartInstance = null;
  statusChartInstance = null;
  stageChartInstance = null;
});
</script>

<template>
  <div class="transfer-workspace">
    <div class="workspace-body">
      <template v-if="page.loading">
        <a-spin
          style="display: flex; justify-content: center; padding: 72px 0"
        />
      </template>

      <template v-else>
        <section
          v-if="page.activeSection === 'overview'"
          class="workspace-section workspace-section--overview"
        >
          <div class="overview-hero-panel">
            <div class="overview-hero">
              <div class="overview-hero-head">
                <div class="workspace-kicker workspace-kicker--overview">
                  {{ page.overviewHero.title }}
                </div>
                <div class="overview-hero-head-right">
                  <strong>{{ page.overviewHero.lastRefresh }}</strong>
                  <a-tag :color="page.overviewHero.healthTone">
                    {{ page.overviewHero.healthLabel }}
                  </a-tag>
                </div>
              </div>
              <div class="overview-hero-summary-row">
                <div class="overview-hero-summary-group">
                  <div class="overview-hero-board-main">
                    <div
                      v-for="item in page.overviewHeroStats"
                      :key="item.key"
                      class="overview-hero-stat-card"
                      :class="`overview-hero-stat-card--${item.tone}`"
                    >
                      <div class="overview-hero-stat-label">
                        {{ item.label }}
                      </div>
                      <div class="overview-hero-stat-value">
                        {{ item.value }}
                      </div>
                      <div class="overview-hero-stat-desc">
                        {{ item.description }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="overview-hero-summary-group">
                  <div class="overview-object-summary-stack">
                    <div
                      v-for="item in page.objectSummaryCards"
                      :key="item.key"
                      class="overview-hero-stat-card"
                      :class="`overview-hero-stat-card--${item.tone}`"
                    >
                      <div class="overview-hero-stat-label">
                        {{ item.label }}
                      </div>
                      <div class="overview-hero-stat-value">
                        {{ item.value }}
                      </div>
                      <div class="overview-hero-stat-desc">
                        {{ item.description }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="overview-hero-columns">
                <div class="overview-hero-charts-grid">
                  <YCard
                    class="overview-mini-chart-card overview-mini-chart-card--status"
                    :bordered="false"
                    :padding="14"
                  >
                    <div class="section-title section-title-inline">
                      <div>
                        <h3>结果分布</h3>
                        <p>投递成功、失败与处理中占比。</p>
                      </div>
                    </div>
                    <div ref="statusChartRef" class="overview-mini-chart"></div>
                    <div class="overview-mini-summary-grid">
                      <div
                        v-for="item in page.overviewStatusHighlights"
                        :key="item.key"
                        class="overview-mini-summary-card"
                      >
                        <span>{{ item.label }}</span>
                        <strong :style="{ color: item.color }">{{
                          item.value
                        }}</strong>
                      </div>
                    </div>
                  </YCard>
                  <YCard
                    class="overview-mini-chart-card overview-mini-chart-card--stage"
                    :bordered="false"
                    :padding="14"
                  >
                    <div class="section-title section-title-inline">
                      <div>
                        <h3>阶段态势</h3>
                        <p>收取、识别、路由、投递阶段数量。</p>
                      </div>
                    </div>
                    <div ref="stageChartRef" class="overview-mini-chart"></div>
                    <div
                      class="overview-mini-summary-grid overview-mini-summary-grid--stage"
                    >
                      <div
                        v-for="item in page.overviewStageHighlights"
                        :key="item.key"
                        class="overview-mini-summary-card"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </YCard>
                </div>
              </div>
            </div>
          </div>

          <div class="overview-surface-grid">
            <YCard class="overview-trend-panel" :bordered="false" :padding="18">
              <div class="section-title">
                <div>
                  <h3>文件投递个数趋势图</h3>
                  <p>支持查看最近 3 天、7 天和 30 天的投递趋势。</p>
                </div>
                <div class="trend-window-switch">
                  <YButton
                    v-for="item in page.trendOptions"
                    :key="item.value"
                    class="trend-window-button"
                    :theme="
                      page.trendWindow === item.value ? 'primary' : undefined
                    "
                    @click="page.setTrendWindow(item.value)"
                  >
                    {{ item.label }}
                  </YButton>
                </div>
              </div>
              <div class="trend-chart-shell trend-chart-shell--echarts">
                <div ref="trendChartRef" class="trend-chart-canvas"></div>
              </div>
            </YCard>
          </div>

          <div class="overview-lower-grid">
            <YCard
              class="overview-anomaly-panel"
              :bordered="false"
              :padding="14"
            >
              <div class="section-title">
                <div>
                  <h3>异常聚焦</h3>
                  <p>优先展示最近失败的文件，便于快速定位问题。</p>
                </div>
              </div>
              <div
                v-if="page.anomalyItems.length"
                class="overview-anomaly-list"
              >
                <div
                  v-for="item in page.anomalyItems"
                  :key="item.key"
                  class="overview-anomaly-card"
                >
                  <div class="overview-anomaly-title">
                    <strong>{{
                      item.targetCode || item.transferId || item.deliveryId
                    }}</strong>
                    <a-tag color="red">失败</a-tag>
                  </div>
                  <div class="overview-anomaly-meta">
                    <span>投递：{{ item.deliveryId || "-" }}</span>
                    <span>分拣：{{ item.transferId || "-" }}</span>
                  </div>
                  <div class="overview-anomaly-meta">
                    <span>目标类型：{{ item.targetType || "-" }}</span>
                    <span>时间：{{ item.deliveredAt }}</span>
                  </div>
                  <div class="overview-anomaly-desc">
                    {{ item.errorMessage || "未提供错误信息" }}
                  </div>
                </div>
              </div>
              <a-empty v-else description="暂无异常记录" />
            </YCard>

            <YCard class="overview-table-panel" :bordered="false" :padding="14">
              <div class="section-title">
                <div>
                  <h3>最近投递结果</h3>
                </div>
              </div>
              <YTable
                class="overview-recent-table"
                :columns="recentDeliveryColumns"
                :action-config="recentDeliveryActionConfig"
                :data="page.recentDeliveryTableData"
                :loading="page.loading"
                :row-config="{ keyField: 'id' }"
                :checkbox-config="{ highlight: true }"
                :pageable="true"
                v-model:pagination="page.recentDeliveryPagination"
                :toolbar-config="{ custom: false }"
                @page-change="page.handleRecentDeliveryPageChange"
              >
                <template #executeStatusLabel="{ row }">
                  <a-tag
                    :color="page.formatDeliveryStatus(row.executeStatus).color"
                  >
                    {{
                      row.executeStatusLabel ||
                      page.formatDeliveryStatus(row.executeStatus).text
                    }}
                  </a-tag>
                </template>
              </YTable>
            </YCard>
          </div>
        </section>

        <section
          v-else-if="page.activeSection === 'source'"
          class="workspace-section"
        >
          <div class="section-title">
            <h3>来源管理</h3>
            <YButton @click="page.setActiveSection('overview')">返回</YButton>
          </div>
          <div class="source-card-grid">
            <YCard
              v-for="item in page.sourceCards"
              :key="item.title"
              :bordered="false"
              :padding="16"
            >
              <strong>{{ item.title }}</strong>
              <div class="source-item-template">{{ item.template }}</div>
              <div class="source-item-desc">{{ item.description }}</div>
            </YCard>
          </div>
        </section>

        <section
          v-else-if="page.activeSection === 'target'"
          class="workspace-section"
        >
          <div class="section-title">
            <h3>目标管理</h3>
            <YButton @click="page.openTargetCreate">新建目标</YButton>
          </div>
          <div class="guide-grid">
            <YCard
              v-for="item in page.targets"
              :key="item.id"
              :bordered="false"
              :padding="16"
            >
              <strong>目标 #{{ item.id }}</strong>
              <p>目标配置已迁移到顶层目录页面。</p>
            </YCard>
          </div>
        </section>

        <section
          v-else-if="page.activeSection === 'rule'"
          class="workspace-section"
        >
          <div class="section-title">
            <h3>分拣规则</h3>
            <YButton @click="page.openRuleCreate">新建规则</YButton>
          </div>
          <div class="guide-grid">
            <YCard
              v-for="item in page.rules"
              :key="item.id"
              :bordered="false"
              :padding="16"
            >
              <strong>规则 #{{ item.id }}</strong>
              <p>规则配置已迁移到顶层目录页面。</p>
            </YCard>
          </div>
        </section>

        <section
          v-else-if="page.activeSection === 'log'"
          class="workspace-section"
        >
          <div class="section-title"><h3>运行日志</h3></div>
          <div class="workspace-preview-card">
            <div v-for="row in page.logs" :key="row.id" class="summary-item">
              <span>{{ row.fileName }}</span>
              <strong @click="openLogSnapshot(row)">{{ row.status }}</strong>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>
