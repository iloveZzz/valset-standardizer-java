<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import { YButton, YCard } from "@yss-ui/components";
import OverviewRunLogConsole from "./OverviewRunLogConsole.vue";

defineOptions({ name: "TransferWorkspace" });

const props = defineProps<{
  page: any;
}>();

const page = props.page;

const trendChartRef = ref<HTMLDivElement | null>(null);
const statusChartRef = ref<HTMLDivElement | null>(null);
const tagChartRef = ref<HTMLDivElement | null>(null);
let trendChartInstance: echarts.ECharts | null = null;
let statusChartInstance: echarts.ECharts | null = null;
let tagChartInstance: echarts.ECharts | null = null;
let trendChartObserver: ResizeObserver | null = null;
let statusChartObserver: ResizeObserver | null = null;
let tagChartObserver: ResizeObserver | null = null;

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
  if (tagChartRef.value && !tagChartObserver) {
    tagChartObserver = new ResizeObserver(() => {
      tagChartInstance?.resize();
    });
    tagChartObserver.observe(tagChartRef.value);
  }
};

const renderOverviewCharts = async () => {
  await nextTick();
  bindChartObservers();
  await Promise.all([
    renderTrendChart(),
    renderStatusChart(),
    renderTagChart(),
  ]);
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

const renderTagChart = async () => {
  if (page.activeSection !== "overview") {
    return;
  }

  await nextTick();
  const element = tagChartRef.value;
  if (!element) {
    return;
  }

  if (!tagChartInstance) {
    tagChartInstance = echarts.init(element);
  }

  tagChartInstance.setOption(page.overviewTagChartOption, true);
  tagChartInstance.resize();
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
  () => page.overviewTagChartOption,
  () => {
    void renderTagChart();
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
  tagChartObserver?.disconnect();
  trendChartInstance?.dispose();
  statusChartInstance?.dispose();
  tagChartInstance?.dispose();
  trendChartObserver = null;
  statusChartObserver = null;
  tagChartObserver = null;
  trendChartInstance = null;
  statusChartInstance = null;
  tagChartInstance = null;
});
</script>

<template>
  <div class="transfer-workspace">
    <div class="workspace-body">
      <section
        v-if="page.activeSection === 'overview'"
        class="workspace-section workspace-section--overview"
      >
        <div class="overview-hero-panel">
          <div class="overview-hero">
            <template v-if="page.loading">
              <div class="overview-hero-head">
                <span class="overview-skeleton overview-skeleton--title"></span>
                <span class="overview-skeleton overview-skeleton--badge"></span>
              </div>
              <div class="overview-hero-summary-row">
                <div class="overview-hero-summary-group">
                  <div class="overview-hero-board-main">
                    <div
                      v-for="item in 3"
                      :key="`hero-stat-${item}`"
                      class="overview-hero-stat-card overview-hero-stat-card--skeleton"
                    >
                      <span class="overview-skeleton overview-skeleton--label"></span>
                      <span class="overview-skeleton overview-skeleton--value"></span>
                      <span class="overview-skeleton overview-skeleton--desc"></span>
                    </div>
                  </div>
                </div>

                <div class="overview-hero-summary-group">
                  <div class="overview-object-summary-stack">
                    <div
                      v-for="item in 2"
                      :key="`object-stat-${item}`"
                      class="overview-hero-stat-card overview-hero-stat-card--skeleton"
                    >
                      <span class="overview-skeleton overview-skeleton--label"></span>
                      <span class="overview-skeleton overview-skeleton--value"></span>
                      <span class="overview-skeleton overview-skeleton--desc"></span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="overview-hero-columns">
                <div class="overview-hero-charts-grid">
                  <div class="overview-status-card overview-status-card--skeleton">
                    <div class="overview-status-head">
                      <div class="overview-status-head-copy">
                        <span class="overview-skeleton overview-skeleton--section"></span>
                        <span class="overview-skeleton overview-skeleton--line"></span>
                      </div>
                      <span class="overview-skeleton overview-skeleton--badge"></span>
                    </div>
                    <div class="overview-status-body">
                      <div class="overview-status-source-grid">
                        <div
                          v-for="item in 2"
                          :key="`source-stat-${item}`"
                          class="overview-status-source-card overview-status-source-card--skeleton"
                        >
                          <div class="overview-status-source-head">
                            <span class="overview-skeleton overview-skeleton--label"></span>
                            <span class="overview-skeleton overview-skeleton--badge"></span>
                          </div>
                          <div class="overview-status-chip-grid">
                            <div
                              v-for="chip in 3"
                              :key="`source-chip-${item}-${chip}`"
                              class="overview-status-chip overview-status-chip--skeleton"
                            >
                              <span class="overview-skeleton overview-skeleton--chip-label"></span>
                              <span class="overview-skeleton overview-skeleton--chip-value"></span>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div class="overview-status-chart-shell">
                        <div class="overview-status-chart overview-chart-placeholder"></div>
                      </div>
                    </div>
                  </div>
                  <div class="overview-mini-chart-card overview-mini-chart-card--stage overview-mini-chart-card--skeleton">
                    <div class="section-title section-title-inline">
                      <div>
                        <span class="overview-skeleton overview-skeleton--section"></span>
                        <span class="overview-skeleton overview-skeleton--line"></span>
                      </div>
                    </div>
                    <div class="overview-mini-chart overview-chart-placeholder"></div>
                    <div class="overview-mini-summary-grid overview-mini-summary-grid--stage">
                      <div
                        v-for="item in 4"
                        :key="`stage-stat-${item}`"
                        class="overview-mini-summary-card overview-mini-summary-card--skeleton"
                      >
                        <span class="overview-skeleton overview-skeleton--label"></span>
                        <span class="overview-skeleton overview-skeleton--value"></span>
                        <span class="overview-skeleton overview-skeleton--desc"></span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="overview-hero-head">
                <div class="workspace-kicker workspace-kicker--overview">
                  {{ page.overviewHero.title }}
                </div>
                <div class="overview-hero-head-right">
                  <span class="overview-task-date-label">任务日期</span>
                  <a-date-picker
                    v-model:value="page.taskDateValue"
                    :allowClear="false"
                    format="YYYY-MM-DD"
                  />
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
                      <div
                        class="overview-hero-stat-value"
                        :class="item.valueClass"
                      >
                        {{ item.value }}
                      </div>
                      <div
                        v-if="item.descriptionParts?.length"
                        class="overview-hero-stat-desc"
                      >
                        <template
                          v-for="(part, index) in item.descriptionParts"
                          :key="part.key"
                        >
                          <span>{{ part.label }}</span>
                          <strong :class="part.valueClass">
                            {{ part.value }}
                          </strong>
                          <span>个</span>
                          <span v-if="index < item.descriptionParts.length - 1">
                            ，
                          </span>
                        </template>
                      </div>
                      <div v-else class="overview-hero-stat-desc">
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
                    class="overview-status-card"
                    :bordered="false"
                    :padding="18"
                  >
                    <div class="overview-status-head">
                      <div>
                        <h3>来源统计</h3>
                        <p>按来源类型展示已识别、已收取与已跳过统计。</p>
                      </div>
                      <a-tag color="blue">
                        {{ page.overviewStatusTotal }} 条
                      </a-tag>
                    </div>
                    <div class="overview-status-body">
                      <div class="overview-status-source-grid">
                        <div
                          v-for="item in page.sourceAnalysisCards"
                          :key="item.key"
                          class="overview-status-source-card"
                        >
                          <div class="overview-status-source-head">
                            <span class="overview-status-source-label">
                              {{ item.sourceLabel }}
                            </span>
                            <a-tag color="blue">{{ item.totalCount }} 条</a-tag>
                          </div>
                          <div class="overview-status-chip-grid">
                            <div
                              v-for="statusItem in item.statusItems"
                              :key="statusItem.key"
                              class="overview-status-chip"
                            >
                              <span class="overview-status-chip-label">
                                {{ statusItem.label }}
                              </span>
                              <strong :style="{ color: statusItem.color }">{{
                                statusItem.value
                              }}</strong>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div class="overview-status-chart-shell">
                        <div
                          ref="statusChartRef"
                          class="overview-status-chart"
                        ></div>
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
                        <h3>标签识别结果</h3>
                        <p>按任务日期展示标签识别命中数量。</p>
                      </div>
                    </div>
                    <div ref="tagChartRef" class="overview-mini-chart"></div>
                    <div
                      class="overview-mini-summary-grid overview-mini-summary-grid--stage"
                    >
                      <div
                        v-for="item in page.overviewTagHighlights"
                        :key="item.key"
                        class="overview-mini-summary-card"
                      >
                        <span>{{ item.label }}  : {{ item.tagCount }}</span>

                      </div>
                    </div>
                  </YCard>
                </div>
              </div>
            </template>
          </div>

          <div class="overview-surface-grid">
            <YCard class="overview-trend-panel" :bordered="false" :padding="18">
              <template v-if="page.loading">
                <div class="section-title">
                  <div>
                    <span class="overview-skeleton overview-skeleton--section"></span>
                    <span class="overview-skeleton overview-skeleton--line"></span>
                  </div>
                  <div class="trend-window-switch">
                    <span
                      v-for="item in 3"
                      :key="`trend-window-${item}`"
                      class="overview-skeleton overview-skeleton--switch"
                    ></span>
                  </div>
                </div>
                <div class="trend-chart-shell trend-chart-shell--echarts">
                  <div class="trend-chart-canvas overview-chart-placeholder"></div>
                </div>
              </template>
              <template v-else>
                <div class="section-title">
                  <div>
                    <h3>分拣对象趋势图</h3>
                    <p>支持查看最近 3 天、7 天和 30 天的已投递与未投递趋势。</p>
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
              </template>
            </YCard>
          </div>

          <div class="overview-lower-grid">
            <YCard
              class="overview-anomaly-panel"
              :bordered="false"
              :padding="14"
            >
              <template v-if="page.loading">
                <div class="section-title">
                  <div>
                    <span class="overview-skeleton overview-skeleton--section"></span>
                    <span class="overview-skeleton overview-skeleton--line"></span>
                  </div>
                  <span class="overview-skeleton overview-skeleton--badge"></span>
                </div>
                <div class="overview-anomaly-list">
                  <div
                    v-for="item in 4"
                    :key="`anomaly-${item}`"
                    class="overview-anomaly-card overview-anomaly-card--skeleton"
                  >
                    <div class="overview-anomaly-title">
                      <span class="overview-skeleton overview-skeleton--label"></span>
                      <span class="overview-skeleton overview-skeleton--badge"></span>
                    </div>
                    <div class="overview-anomaly-meta">
                      <span class="overview-skeleton overview-skeleton--line"></span>
                      <span class="overview-skeleton overview-skeleton--line"></span>
                    </div>
                    <div class="overview-anomaly-meta">
                      <span class="overview-skeleton overview-skeleton--line"></span>
                      <span class="overview-skeleton overview-skeleton--line"></span>
                    </div>
                    <div class="overview-skeleton overview-skeleton--desc overview-skeleton--desc-wide"></div>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="section-title">
                  <div>
                    <h3>异常聚焦</h3>
                    <p>优先展示估值表解析任务中的异常批次，便于快速定位问题。</p>
                  </div>
                  <a-tag color="red">{{ page.anomalyCount }} 条</a-tag>
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
                      <strong>{{ item.batchName || item.batchId }}</strong>
                      <a-tag color="red">{{ item.statusName }}</a-tag>
                    </div>
                    <div class="overview-anomaly-meta">
                      <span>批次：{{ item.batchId || "-" }}</span>
                      <span
                        >产品：{{
                          item.productName || item.productCode || "-"
                        }}</span
                      >
                    </div>
                    <div class="overview-anomaly-meta">
                      <span>责任人：{{ item.managerName || "-" }}</span>
                      <span
                        >阶段：{{
                          item.currentStageName || item.currentStepName || "-"
                        }}</span
                      >
                    </div>
                    <div class="overview-anomaly-desc">
                      {{ item.lastErrorMessage || "未提供错误信息" }}
                    </div>
                  </div>
                </div>
                <a-empty v-else description="暂无异常记录" />
              </template>
            </YCard>
          </div>
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
          v-else-if="page.activeSection === 'run-log'"
          class="workspace-section"
        >
          <OverviewRunLogConsole />
        </section>
    </div>
  </div>
</template>
