<script setup lang="ts">
import {
  h,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { Modal } from "ant-design-vue";
import { ExclamationCircleOutlined } from "@ant-design/icons-vue";
import * as echarts from "echarts";
import { YButton, YCard, YTable } from "@yss-ui/components";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import {
  useTransferOverviewRecentDeliveryColumns,
} from "../../TransferShared/hooks/useTransferTableColumns";

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
let trendChartInstance: echarts.ECharts | null = null;
let trendChartObserver: ResizeObserver | null = null;

const openLogSnapshot = (row: any) => {
  Modal.info({
    title: "运行快照",
    width: 920,
    icon: h(ExclamationCircleOutlined),
    content: h(
      "pre",
      { class: "snapshot-code" },
      page.formatJson(row.snapshot),
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
  () => page.activeSection,
  (section) => {
    if (section === "overview") {
      void renderTrendChart();
    }
  },
  {
    immediate: true,
  },
);

onMounted(() => {
  if (trendChartRef.value) {
    trendChartObserver = new ResizeObserver(() => {
      trendChartInstance?.resize();
    });
    trendChartObserver.observe(trendChartRef.value);
  }
  void renderTrendChart();
});

onBeforeUnmount(() => {
  trendChartObserver?.disconnect();
  trendChartInstance?.dispose();
  trendChartObserver = null;
  trendChartInstance = null;
});
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-body" :bordered="false" :padding="18">
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
          <YCard class="overview-metric-panel" :bordered="false" :padding="18">
            <div class="section-title">
              <div>
                <h3>核心统计</h3>
                <p>汇总当前文件分拣链路的关键运行数据。</p>
              </div>
            </div>
            <div class="overview-metric-grid">
              <div
                v-for="item in page.overviewMetrics"
                :key="item.key"
                class="overview-metric-card"
              >
                <div class="overview-metric-label">{{ item.label }}</div>
                <div class="overview-metric-value">{{ item.value }}</div>
                <div class="overview-metric-desc">{{ item.description }}</div>
              </div>
            </div>
          </YCard>

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

          <YCard class="overview-table-panel" :bordered="false" :padding="18">
            <div class="section-title">
              <div>
                <h3>最近 10 个投递文件结果</h3>
                <p>展示最近投递记录，支持快速查看每条结果快照。</p>
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
              <template #status="{ row }">
                <a-tag :color="row.status === 'SUCCESS' ? 'green' : 'red'">
                  {{ row.status === "SUCCESS" ? "成功" : "失败" }}
                </a-tag>
              </template>
            </YTable>
          </YCard>
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
            <h3>分拣规则配置</h3>
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
    </YCard>
  </div>
</template>
