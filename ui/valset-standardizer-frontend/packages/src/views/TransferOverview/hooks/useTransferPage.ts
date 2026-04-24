import { computed, reactive, ref } from "vue";
import { transferSectionOptions } from "../schemas/transferSchemas";
import type { YTablePagination } from "@yss-ui/components";

export const useTransferPage = () => {
  const buildRecentDeliveries = () => [
    {
      id: 1,
      fileName: "2026-04-22_资金清单.xlsx",
      source: "本地目录",
      target: "财务中心",
      route: "规则-001",
      status: "SUCCESS",
      deliveredAt: "2026-04-22 09:18:12",
      snapshot: { request: { sourceType: "LOCAL_DIR" } },
    },
    {
      id: 2,
      fileName: "2026-04-22_回单.pdf",
      source: "邮件来源",
      target: "业务归档",
      route: "规则-002",
      status: "SUCCESS",
      deliveredAt: "2026-04-22 08:52:36",
      snapshot: { request: { sourceType: "EMAIL" } },
    },
    {
      id: 3,
      fileName: "2026-04-21_对账单.csv",
      source: "本地目录",
      target: "对账平台",
      route: "规则-001",
      status: "FAILED",
      deliveredAt: "2026-04-21 22:11:45",
      snapshot: { request: { sourceType: "LOCAL_DIR" } },
    },
    {
      id: 4,
      fileName: "2026-04-21_资产明细.xlsx",
      source: "邮件来源",
      target: "档案中心",
      route: "规则-003",
      status: "SUCCESS",
      deliveredAt: "2026-04-21 18:03:21",
      snapshot: { request: { sourceType: "EMAIL" } },
    },
    {
      id: 5,
      fileName: "2026-04-21_结算结果.xlsx",
      source: "本地目录",
      target: "财务中心",
      route: "规则-001",
      status: "SUCCESS",
      deliveredAt: "2026-04-21 15:41:09",
      snapshot: { request: { sourceType: "LOCAL_DIR" } },
    },
    {
      id: 6,
      fileName: "2026-04-20_费用单.pdf",
      source: "邮件来源",
      target: "业务归档",
      route: "规则-002",
      status: "SUCCESS",
      deliveredAt: "2026-04-20 11:28:57",
      snapshot: { request: { sourceType: "EMAIL" } },
    },
    {
      id: 7,
      fileName: "2026-04-20_销账清单.xlsx",
      source: "本地目录",
      target: "对账平台",
      route: "规则-001",
      status: "FAILED",
      deliveredAt: "2026-04-20 09:05:34",
      snapshot: { request: { sourceType: "LOCAL_DIR" } },
    },
    {
      id: 8,
      fileName: "2026-04-19_审批附件.zip",
      source: "邮件来源",
      target: "档案中心",
      route: "规则-003",
      status: "SUCCESS",
      deliveredAt: "2026-04-19 19:25:02",
      snapshot: { request: { sourceType: "EMAIL" } },
    },
    {
      id: 9,
      fileName: "2026-04-19_明细回传.xlsx",
      source: "本地目录",
      target: "财务中心",
      route: "规则-002",
      status: "SUCCESS",
      deliveredAt: "2026-04-19 13:54:44",
      snapshot: { request: { sourceType: "LOCAL_DIR" } },
    },
    {
      id: 10,
      fileName: "2026-04-18_合同扫描件.pdf",
      source: "邮件来源",
      target: "业务归档",
      route: "规则-003",
      status: "SUCCESS",
      deliveredAt: "2026-04-18 10:07:18",
      snapshot: { request: { sourceType: "EMAIL" } },
    },
  ];

  const buildTrendSeries = () => {
    const series = Array.from({ length: 30 }, (_, index) => {
      const date = new Date();
      date.setDate(date.getDate() - (29 - index));
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return {
        label: `${month}-${day}`,
        value: 8 + ((index * 7 + 11) % 19),
      };
    });
    return series;
  };

  const activeSection =
    ref<(typeof transferSectionOptions)[number]["value"]>("overview");
  const loading = ref(false);
  const sourceCards = ref([
    {
      title: "本地目录",
      description: "本地临时抽取目录作为输入源。",
      template: "LOCAL_DIR",
    },
    {
      title: "邮件来源",
      description: "解析邮件附件并进入传输流程。",
      template: "EMAIL",
    },
  ]);
  const targets = ref([{ id: 1 }, { id: 2 }]);
  const rules = ref([{ id: 1 }, { id: 2 }, { id: 3 }]);
  const logs = ref(buildRecentDeliveries());
  const trendWindow = ref<3 | 7 | 30>(7);
  const trendSeries = ref(buildTrendSeries());
  const recentDeliveryPagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 10,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const searchModel = reactive({
    keyword: "",
    enabledStatus: "all",
    category: "all",
    targetType: "",
  });
  const targetDraft = reactive({ targetType: "EMAIL" });
  const ruleDraft = reactive({ matchStrategy: "SCRIPT" });

  const deliveryCount = computed(() => logs.value.length);
  const sourceCount = computed(() => sourceCards.value.length);
  const targetCount = computed(() => targets.value.length);
  const routeCount = computed(() => rules.value.length);

  const overviewMetrics = computed(() => [
    {
      key: "delivery",
      label: "文件投递个数",
      value: deliveryCount.value,
      description: "当前统计周期内累计投递的文件数量",
    },
    {
      key: "source",
      label: "来源生效个数",
      value: sourceCount.value,
      description: "已启用并参与投递的来源配置数量",
    },
    {
      key: "target",
      label: "目标生效个数",
      value: targetCount.value,
      description: "已启用并参与分拣的目标配置数量",
    },
    {
      key: "route",
      label: "路由生效个数",
      value: routeCount.value,
      description: "已启用并参与匹配的分拣规则配置数量",
    },
  ]);

  const trendData = computed(() => {
    const startIndex = Math.max(
      0,
      trendSeries.value.length - trendWindow.value,
    );
    return trendSeries.value.slice(startIndex);
  });

  const trendOptions: Array<{ label: string; value: 3 | 7 | 30 }> = [
    { label: "最近3天", value: 3 },
    { label: "最近7天", value: 7 },
    { label: "最近30天", value: 30 },
  ];

  const trendChartOption = computed(() => ({
    grid: {
      left: 44,
      right: 18,
      top: 20,
      bottom: 34,
      containLabel: true,
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "line",
      },
      backgroundColor: "rgba(15, 23, 42, 0.92)",
      borderWidth: 0,
      textStyle: {
        color: "#fff",
      },
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: trendData.value.map((item) => item.label),
      axisLine: {
        lineStyle: {
          color: "rgba(15, 23, 42, 0.14)",
        },
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: "rgba(15, 23, 42, 0.55)",
        margin: 14,
      },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      splitLine: {
        lineStyle: {
          color: "rgba(15, 23, 42, 0.08)",
        },
      },
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: "rgba(15, 23, 42, 0.55)",
      },
    },
    series: [
      {
        name: "文件投递个数",
        type: "line",
        smooth: true,
        symbol: "circle",
        symbolSize: 8,
        data: trendData.value.map((item) => item.value),
        lineStyle: {
          width: 3,
          color: "#1677ff",
        },
        itemStyle: {
          color: "#1677ff",
        },
        emphasis: {
          scale: 1.1,
        },
        areaStyle: {
          color: "rgba(22, 119, 255, 0.12)",
        },
      },
    ],
  }));

  const recentDeliveryRows = computed(() => logs.value.slice(0, 10));
  const recentDeliveryTableData = computed(() => {
    const current = recentDeliveryPagination.value.current || 1;
    const pageSize = recentDeliveryPagination.value.pageSize || 10;
    const start = (current - 1) * pageSize;
    return recentDeliveryRows.value.slice(start, start + pageSize);
  });

  const handleRecentDeliveryPageChange = ({
    current,
    pageSize,
  }: {
    current: number;
    pageSize: number;
  }) => {
    recentDeliveryPagination.value.current = current;
    recentDeliveryPagination.value.pageSize = pageSize;
  };

  const setTrendWindow = (window: 3 | 7 | 30) => {
    trendWindow.value = window;
  };

  const activeSectionLabel = computed(
    () =>
      transferSectionOptions.find((item) => item.value === activeSection.value)
        ?.label || "分拣总览",
  );

  const setActiveSection = (section: any) => {
    activeSection.value = section;
  };
  const openTargetCreate = () => {
    activeSection.value = "target";
  };
  const openRuleCreate = () => {
    activeSection.value = "rule";
  };
  const selectTarget = async () => undefined;
  const selectRule = () => undefined;
  const selectLog = () => undefined;
  const applySearch = () => undefined;
  const resetSearch = () => {
    searchModel.keyword = "";
    searchModel.enabledStatus = "all";
    searchModel.category = "all";
    searchModel.targetType = "";
  };
  const saveTarget = async () => undefined;
  const saveRule = async () => undefined;
  const resolveTargetTemplateName = async () => "";
  const formatJson = (value: unknown) => JSON.stringify(value ?? {}, null, 2);
  const paged = <T>(items: T[]) => items;

  const page = reactive({
    activeSection,
    activeSectionLabel,
    loading,
    sourceCards,
    targets,
    rules,
    logs,
    overviewMetrics,
    trendOptions,
    trendWindow,
    trendChartOption,
    recentDeliveryRows,
    recentDeliveryTableData,
    recentDeliveryPagination,
    handleRecentDeliveryPageChange,
    setTrendWindow,
    searchModel,
    targetDraft,
    ruleDraft,
    setActiveSection,
    openTargetCreate,
    openRuleCreate,
    selectTarget,
    selectRule,
    selectLog,
    applySearch,
    resetSearch,
    saveTarget,
    saveRule,
    resolveTargetTemplateName,
    formatJson,
    paged,
    filteredTargets: targets,
    filteredRules: rules,
    filteredLogs: logs,
  });

  return {
    page,
  };
};
