import { computed, onMounted, reactive, ref } from "vue";
import { getJavaSpringBootQuartzApi } from "@/api";
import { unwrapMultiResult, unwrapSingleResult } from "@/utils/api-response";
import { transferSectionOptions } from "../schemas/transferSchemas";
import type { YTablePagination } from "@yss-ui/components";
import type {
  PageRecordsParams,
  PageLogsParams,
  TransferObjectAnalysisViewDTO,
  TransferDeliveryRecordViewDTO,
  TransferRuleViewDTO,
  TransferRunLogAnalysisViewDTO,
  TransferRunLogStageAnalysisViewDTO,
  TransferRunLogViewDTO,
  TransferSourceViewDTO,
  TransferTargetViewDTO,
} from "@/api/generated/valset/schemas";

const api = getJavaSpringBootQuartzApi();

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
  const loading = ref(true);
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
  const sourceRows = ref<TransferSourceViewDTO[]>([]);
  const targetRows = ref<TransferTargetViewDTO[]>([]);
  const ruleRows = ref<TransferRuleViewDTO[]>([]);
  const targets = targetRows;
  const rules = ruleRows;
  const logs = ref<TransferDeliveryRecordViewDTO[]>(buildRecentDeliveries());
  const recentDeliveryRows = ref<TransferDeliveryRecordViewDTO[]>([]);
  const deliveryTotal = ref(logs.value.length);
  const trendWindow = ref<3 | 7 | 30>(3);
  const trendSeries = ref(buildTrendSeries());
  const runLogAnalysis = ref<TransferRunLogAnalysisViewDTO | null>(null);
  const objectAnalysis = ref<TransferObjectAnalysisViewDTO | null>(null);
  const runLogRows = ref<TransferRunLogViewDTO[]>([]);
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

  const deliveryCount = computed(() => deliveryTotal.value);
  const sourceCount = computed(
    () =>
      sourceRows.value.filter((item) => item.enabled !== false).length ||
      sourceCards.value.length,
  );
  const targetCount = computed(
    () =>
      targetRows.value.filter((item) => item.enabled !== false).length ||
      targetRows.value.length ||
      0,
  );
  const routeCount = computed(
    () =>
      ruleRows.value.filter((item) => item.enabled !== false).length ||
      ruleRows.value.length ||
      0,
  );

  const getDeliverStage = () =>
    runLogAnalysis.value?.stageAnalyses?.find(
      (stage) =>
        String(stage.runStage ?? "")
          .trim()
          .toUpperCase() === "DELIVER",
    ) || null;

  const getStageCount = (
    stage: TransferRunLogStageAnalysisViewDTO | null | undefined,
    keys: string[],
  ) =>
    Number(
      (stage?.statusCounts ?? [])
        .filter((item) =>
          keys.includes(
            String(item.runStatus ?? "")
              .trim()
              .toUpperCase(),
          ),
        )
        .reduce((sum, item) => sum + Number(item.count ?? 0), 0),
    );

  const getStageAnalysis = (stageName: string) =>
    runLogAnalysis.value?.stageAnalyses?.find(
      (stage) =>
        String(stage.runStage ?? "")
          .trim()
          .toUpperCase() === stageName,
    ) || null;

  const stageAxisNames = computed(() => [
    "RECEIVE",
    "INGEST",
    "ROUTE",
    "DELIVER",
  ]);
  const resolveStageAxisLabel = (stageName: string) => {
    switch (stageName) {
      case "RECEIVE":
        return "收取";
      case "INGEST":
        return "识别";
      case "ROUTE":
        return "路由";
      case "DELIVER":
        return "投递";
      default:
        return stageName;
    }
  };
  const stageChartRows = computed(() =>
    stageAxisNames.value.map((stageName) => {
      if (stageName === "RECEIVE") {
        const total = Number(
          runLogAnalysis.value?.totalCount ??
            deliveryTotal.value ??
            logs.value.length,
        );
        return {
          stageName,
          label: resolveStageAxisLabel(stageName),
          total,
          success: total,
          failed: 0,
          pending: 0,
          processing: 0,
        };
      }

      const stage = getStageAnalysis(stageName);
      const statusCounts = stage?.statusCounts ?? [];
      const countOf = (keys: string[]) =>
        Number(
          statusCounts
            .filter((item) =>
              keys.includes(
                String(item.runStatus ?? "")
                  .trim()
                  .toUpperCase(),
              ),
            )
            .reduce((sum, item) => sum + Number(item.count ?? 0), 0),
        );

      return {
        stageName,
        label: stage?.stageLabel ?? resolveStageAxisLabel(stageName),
        total: Number(stage?.totalCount ?? 0),
        success: countOf(["SUCCESS", "SUCCEEDED", "DONE", "COMPLETED"]),
        failed: countOf(["FAILED", "FAIL", "ERROR"]),
        pending: countOf(["PENDING"]),
        processing: countOf(["RUNNING", "PROCESSING", "RETRYING"]),
      };
    }),
  );
  const stageChartMax = computed(() =>
    Math.max(...stageChartRows.value.map((item) => item.total), 1),
  );
  const hasStageChartData = computed(() =>
    stageChartRows.value.some((item) => item.total > 0),
  );

  const successCount = computed(() =>
    getStageCount(getDeliverStage(), [
      "SUCCESS",
      "SUCCEEDED",
      "DONE",
      "COMPLETED",
    ]),
  );
  const failureCount = computed(() =>
    getStageCount(getDeliverStage(), ["FAILED", "FAIL", "ERROR"]),
  );
  const pendingCount = computed(() =>
    getStageCount(getDeliverStage(), ["PENDING"]),
  );
  const processingCount = computed(() =>
    getStageCount(getDeliverStage(), ["RUNNING", "PROCESSING", "RETRYING"]),
  );
  const successRate = computed(() =>
    deliveryCount.value === 0
      ? 0
      : Math.round((successCount.value / deliveryCount.value) * 1000) / 10,
  );
  const abnormalCount = computed(
    () => failureCount.value + pendingCount.value + processingCount.value,
  );

  const overviewHero = computed(() => ({
    title: "分拣总览",
    subtitle: "让文件分拣态势一眼可见",
    description:
      "关注当前投递健康度、趋势、异常和快捷入口，快速判断系统是否正常。",
    lastRefresh: "2026-04-25 19:55",
    healthLabel: successRate.value >= 95 ? "稳定运行" : "需要关注",
    healthTone: successRate.value >= 95 ? "green" : "gold",
  }));

  const overviewHeroStats = computed(() => [
    {
      key: "delivery-total",
      label: "今日投递",
      value: deliveryCount.value,
      description: "当前统计周期内累计投递的文件数量",
      tone: "primary",
    },
    {
      key: "success-rate",
      label: "成功率",
      value: `${successRate.value.toFixed(1)}%`,
      description: "成功投递占总投递比例",
      tone: "success",
    },
    {
      key: "processing",
      label: "处理中",
      value: processingCount.value,
      description: "当前仍在识别或路由中的对象",
      tone: "warning",
    },
    {
      key: "abnormal",
      label: "异常",
      value: abnormalCount.value,
      description: "失败、待处理和重试中的对象数",
      tone: "danger",
    },
  ]);

  const pipelineCards = computed(() => [
    {
      key: "source",
      label: "来源健康",
      value: `${sourceCount.value} 个来源`,
      description: "启用中的来源配置已纳入定时收取",
      tone: "success",
    },
    {
      key: "target",
      label: "目标健康",
      value: `${targetCount.value} 个目标`,
      description: "本地目录、邮件等目标已进入投递链路",
      tone: "primary",
    },
    {
      key: "rule",
      label: "规则健康",
      value: `${routeCount.value} 条规则`,
      description: "启用规则会决定对象分流到哪些目标",
      tone: "primary",
    },
    {
      key: "delivery",
      label: "投递健康",
      value: `${successCount.value}/${deliveryCount.value}`,
      description: "成功投递与总投递的比例关系",
      tone: successRate.value >= 95 ? "success" : "warning",
    },
  ]);

  const anomalyItems = computed(() =>
    logs.value
      .filter((item) => {
        const status = String(item.executeStatus ?? "")
          .trim()
          .toUpperCase();
        return (
          status === "FAILED" ||
          status === "FAIL" ||
          status === "ERROR" ||
          Boolean(item.errorMessage)
        );
      })
      .slice(0, 4)
      .map((item, index) => ({
        key: `${item.deliveryId ?? item.transferId ?? index}`,
        title: item.executeStatusLabel || item.executeStatus || "失败",
        deliveryId: item.deliveryId,
        transferId: item.transferId,
        targetCode: item.targetCode,
        targetType: item.targetType,
        errorMessage: item.errorMessage,
        deliveredAt: item.deliveredAt,
      })),
  );

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
      description: "已启用并参与匹配的分拣规则数量",
    },
  ]);

  const objectSummaryCards = computed(() => [
    {
      key: "object-total",
      label: "分拣对象个数",
      value: Number(objectAnalysis.value?.totalCount ?? 0),
      description: "分拣对象的总数量",
      tone: "primary",
    },
    {
      key: "object-tagged",
      label: "分拣对象打标个数",
      value: Number(objectAnalysis.value?.taggedCount ?? 0),
      description: "已匹配到标签的分拣对象数量",
      tone: "success",
    },
    {
      key: "object-untagged",
      label: "未打标分拣对象个数",
      value: Number(objectAnalysis.value?.untaggedCount ?? 0),
      description: "尚未匹配到标签的分拣数量",
      tone: "warning",
    },
  ]);

  const trendData = computed(() => {
    const startIndex = Math.max(
      0,
      trendSeries.value.length - trendWindow.value,
    );
    return trendSeries.value.slice(startIndex);
  });
  const hasTrendChartData = computed(() =>
    trendData.value.some((item) => Number(item.value ?? 0) > 0),
  );

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
    graphic: hasTrendChartData.value
      ? []
      : [
          {
            type: "text",
            left: "center",
            top: "middle",
            style: {
              text: "暂无趋势数据",
              fill: "rgba(15, 23, 42, 0.46)",
              fontSize: 13,
              textAlign: "center",
            },
          },
        ],
  }));

  const deliveryStatusItems = computed(() => {
    const counts = new Map<string, number>();
    logs.value.forEach((item) => {
      const status = String(item.executeStatus ?? "")
        .trim()
        .toUpperCase();
      if (!status) {
        return;
      }
      counts.set(status, (counts.get(status) ?? 0) + 1);
    });
    const resolveCount = (keys: string[]) =>
      keys.reduce((sum, key) => sum + Number(counts.get(key) ?? 0), 0);
    return [
      {
        name: "成功",
        value: resolveCount(["SUCCESS", "SUCCEEDED", "DONE", "COMPLETED"]),
        color: "#52c41a",
      },
      {
        name: "失败",
        value: resolveCount(["FAILED", "FAIL", "ERROR"]),
        color: "#ff4d4f",
      },
      {
        name: "待处理",
        value: resolveCount(["PENDING"]),
        color: "#faad14",
      },
      {
        name: "处理中",
        value: resolveCount(["RUNNING", "PROCESSING", "RETRYING"]),
        color: "#1677ff",
      },
    ];
  });
  const hasDeliveryStatusData = computed(() =>
    deliveryStatusItems.value.some((item) => item.value > 0),
  );

  const overviewStatusChartOption = computed(() => ({
    tooltip: {
      trigger: "item",
      formatter: "{b}<br/>{c} 条 ({d}%)",
    },
    legend: {
      bottom: 0,
      left: "center",
      icon: "circle",
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: {
        color: "rgba(15, 23, 42, 0.62)",
        fontSize: 12,
      },
    },
    series: [
      {
        name: "投递结果",
        type: "pie",
        radius: ["62%", "78%"],
        center: ["50%", "43%"],
        minAngle: 10,
        avoidLabelOverlap: false,
        label: {
          show: false,
        },
        labelLine: {
          show: false,
        },
        itemStyle: {
          borderColor: "#fff",
          borderWidth: 2,
        },
        data: hasDeliveryStatusData.value
          ? deliveryStatusItems.value.map((item) => ({
              name: item.name,
              value: item.value,
              itemStyle: {
                color: item.color,
              },
            }))
          : [
              {
                name: "暂无数据",
                value: 1,
                itemStyle: {
                  color: "rgba(15, 23, 42, 0.12)",
                },
              },
            ],
      },
    ],
    graphic: [
      {
        type: "group",
        left: "center",
        top: "38%",
        children: [
          {
            type: "text",
            style: {
              text: "总投递",
              x: 0,
              y: -16,
              fill: "rgba(15, 23, 42, 0.68)",
              fontSize: 12,
              textAlign: "center",
            },
          },
          {
            type: "text",
            style: {
              text: String(deliveryCount.value),
              x: 0,
              y: 16,
              fill: "#0f172a",
              fontSize: 28,
              fontWeight: 800,
              textAlign: "center",
            },
          },
        ],
      },
    ],
  }));
  const overviewStatusHighlights = computed(() =>
    deliveryStatusItems.value.map((item) => ({
      key: item.name,
      label: item.name,
      value: item.value,
      color: item.color,
    })),
  );

  const overviewStageHighlights = computed(() =>
    stageChartRows.value.map((item) => ({
      key: item.stageName,
      label: item.label,
      value: item.total,
    })),
  );

  const overviewStageChartOption = computed(() => ({
    grid: {
      left: 60,
      right: 34,
      top: 34,
      bottom: 12,
      containLabel: true,
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "shadow",
      },
      backgroundColor: "rgba(15, 23, 42, 0.92)",
      borderWidth: 0,
      textStyle: {
        color: "#fff",
      },
    },
    legend: {
      top: 0,
      right: 0,
      icon: "roundRect",
      itemWidth: 12,
      itemHeight: 8,
      itemGap: 14,
      textStyle: {
        color: "rgba(15, 23, 42, 0.62)",
        fontSize: 12,
      },
    },
    xAxis: {
      type: "value",
      minInterval: 1,
      max: Math.ceil(stageChartMax.value * 1.2),
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: "rgba(15, 23, 42, 0.55)",
      },
      splitLine: {
        lineStyle: {
          color: "rgba(15, 23, 42, 0.08)",
        },
      },
    },
    yAxis: {
      type: "category",
      data: stageChartRows.value.map((item) => item.label),
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: "rgba(15, 23, 42, 0.68)",
      },
    },
    series: [
      {
        name: "成功",
        type: "bar",
        stack: "total",
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: "#52c41a",
        },
        label: {
          show: true,
          position: "right",
          color: "#0f172a",
          fontWeight: 700,
          formatter: (params: { dataIndex: number }) =>
            String(stageChartRows.value[params.dataIndex]?.total ?? ""),
        },
        data: stageChartRows.value.map((item) => item.success),
      },
      {
        name: "失败",
        type: "bar",
        stack: "total",
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: "#ff4d4f",
        },
        data: stageChartRows.value.map((item) => item.failed),
      },
      {
        name: "待处理",
        type: "bar",
        stack: "total",
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: "#faad14",
        },
        data: stageChartRows.value.map((item) => item.pending),
      },
      {
        name: "处理中",
        type: "bar",
        stack: "total",
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: "#1677ff",
        },
        label: {
          show: false,
          position: "right",
          color: "rgba(15, 23, 42, 0.56)",
          formatter: (params: { dataIndex: number }) =>
            String(stageChartRows.value[params.dataIndex]?.total ?? ""),
        },
        data: stageChartRows.value.map((item) => item.processing),
      },
    ],
    graphic: hasStageChartData.value
      ? []
      : [
          {
            type: "text",
            left: "center",
            top: "middle",
            style: {
              text: "暂无阶段日志",
              fill: "rgba(15, 23, 42, 0.46)",
              fontSize: 13,
              textAlign: "center",
            },
          },
        ],
  }));

  const recentDeliveryTableData = computed(() => recentDeliveryRows.value);

  const handleRecentDeliveryPageChange = ({
    current,
    pageSize,
  }: {
    current: number;
    pageSize: number;
  }) => {
    recentDeliveryPagination.value.current = current;
    recentDeliveryPagination.value.pageSize = pageSize;
    void loadRecentDeliveries(current, pageSize);
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

  const mapRecentDeliveryPageQuery = (
    current = recentDeliveryPagination.value.current || 1,
    pageSize = recentDeliveryPagination.value.pageSize || 10,
  ): PageRecordsParams => ({
    pageIndex: Math.max(current - 1, 0),
    pageSize,
  });

  const loadRecentDeliveries = async (
    current = recentDeliveryPagination.value.current || 1,
    pageSize = recentDeliveryPagination.value.pageSize || 10,
  ) => {
    try {
      const res = await api.pageRecords(
        mapRecentDeliveryPageQuery(current, pageSize),
      );
      const records = res?.data ?? [];
      recentDeliveryRows.value = records;
      recentDeliveryPagination.value.total = Number(
        res?.totalCount ?? records.length,
      );
      recentDeliveryPagination.value.current =
        Number(res?.pageIndex ?? Math.max(current - 1, 0)) + 1;
      recentDeliveryPagination.value.pageSize = Number(
        res?.pageSize ?? pageSize,
      );
    } catch (error) {
      console.error("加载总览最近投递失败:", error);
      recentDeliveryRows.value = [];
    }
  };

  const mapRunLogPageQuery = (): PageLogsParams => ({
    pageIndex: 0,
    pageSize: 6,
  });

  const loadOverviewRunLogs = async () => {
    try {
      const res = await api.pageLogs(mapRunLogPageQuery());
      runLogRows.value = res?.data ?? [];
    } catch (error) {
      console.error("加载总览运行日志失败:", error);
      runLogRows.value = [];
    }
  };

  const loadOverviewSnapshot = async () => {
    try {
      const [
        sources,
        targets,
        rules,
        logsPage,
        analysisResult,
        objectAnalysisResult,
        runLogsPage,
      ] = await Promise.all([
        api.listSources(),
        api.listTargets(),
        api.listRules(),
        api.pageRecords({ pageIndex: 0, pageSize: 200 }),
        api.analyzeLogs(),
        api.analyzeObjects(),
        api.pageLogs(mapRunLogPageQuery()),
      ]);

      sourceRows.value = unwrapMultiResult(sources);
      targetRows.value = unwrapMultiResult(targets);
      ruleRows.value = unwrapMultiResult(rules);

      const page = logsPage as
        | { data?: TransferDeliveryRecordViewDTO[]; totalCount?: number }
        | undefined;
      const snapshotRows = page?.data ?? [];
      logs.value = snapshotRows;
      deliveryTotal.value = Number(page?.totalCount ?? snapshotRows.length);
      trendSeries.value = snapshotRows.length
        ? [
            ...snapshotRows
              .reduce<Map<string, number>>((series, item) => {
                const deliveredAt = String(item.deliveredAt ?? "");
                const datePart = deliveredAt.slice(5, 10);
                if (!datePart) {
                  return series;
                }
                series.set(datePart, (series.get(datePart) ?? 0) + 1);
                return series;
              }, new Map())
              .entries(),
          ]
            .sort(([left], [right]) => left.localeCompare(right))
            .map(([label, value]) => ({ label, value }))
        : buildTrendSeries();

      const analysis = unwrapSingleResult(analysisResult);
      runLogAnalysis.value = analysis ?? null;
      objectAnalysis.value = unwrapSingleResult(objectAnalysisResult) ?? null;
      runLogRows.value = runLogsPage?.data ?? [];

      recentDeliveryPagination.value.total = deliveryTotal.value;
      await loadRecentDeliveries(
        1,
        recentDeliveryPagination.value.pageSize || 10,
      );
    } catch (error) {
      console.error("加载分拣总览数据失败:", error);
      logs.value = buildRecentDeliveries();
      trendSeries.value = buildTrendSeries();
      runLogAnalysis.value = null;
      objectAnalysis.value = null;
      await loadOverviewRunLogs();
      await loadRecentDeliveries(
        1,
        recentDeliveryPagination.value.pageSize || 10,
      );
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    void loadOverviewSnapshot();
  });

  const page = reactive({
    activeSection,
    activeSectionLabel,
    loading,
    sourceCards,
    targets,
    rules,
    logs,
    overviewHero,
    overviewHeroStats,
    pipelineCards,
    anomalyItems,
    overviewMetrics,
    objectSummaryCards,
    trendOptions,
    trendWindow,
    trendChartOption,
    overviewStatusChartOption,
    overviewStageChartOption,
    overviewStatusHighlights,
    overviewStageHighlights,
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
    formatDeliveryStatus: (status: string) => {
      const labels: Record<string, { text: string; color: string }> = {
        SUCCESS: { text: "成功", color: "green" },
        FAILED: { text: "失败", color: "red" },
        PENDING: { text: "待处理", color: "gold" },
        PROCESSING: { text: "投递中", color: "blue" },
        RETRYING: { text: "重试中", color: "orange" },
        SKIPPED: { text: "已跳过", color: "default" },
      };
      return labels[status] || { text: status, color: "default" };
    },
    filteredTargets: targets,
    filteredRules: rules,
    filteredLogs: logs,
  });

  return {
    page,
  };
};
