import { computed, onMounted, reactive, ref } from "vue";
import { getJavaSpringBootQuartzApi } from "@/api";
import {
  getTransferDeliveryRecordSummary,
  type TransferDeliveryRecordSummaryDTO,
} from "@/api/transferDeliveryRecord";
import {
  getOutsourcedDataTaskSummary,
  pageOutsourcedDataTasks,
  type OutsourcedDataTaskBatchDTO,
  type OutsourcedDataTaskStageSummaryDTO,
  type OutsourcedDataTaskSummaryDTO,
} from "@/api/outsourcedDataTask";
import { unwrapMultiResult, unwrapSingleResult } from "@/utils/api-response";
import { outsourcedDataTaskStageCatalog } from "../OutsourcedDataTask/constants";
import { transferSectionOptions } from "../schemas/transferSchemas";
import type {
  PageLogsParams,
  TransferObjectAnalysisViewDTO,
  TransferRuleViewDTO,
  TransferRunLogAnalysisViewDTO,
  TransferRunLogViewDTO,
  TransferSourceViewDTO,
  TransferTargetViewDTO,
} from "@/api/generated/valset/schemas";

const api = getJavaSpringBootQuartzApi();

export const useTransferPage = () => {
  const shanghaiDateFormatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Shanghai",
  });
  const formatShanghaiDateKey = (value: Date) =>
    shanghaiDateFormatter.format(value);
  const parseShanghaiDateKey = (value?: string) => {
    if (!value) {
      return "";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return String(value).slice(0, 10);
    }
    return formatShanghaiDateKey(date);
  };
  const buildTrendSeries = () => {
    const deliveryCounts = new Map<string, number>();
    runLogRows.value.forEach((item) => {
      const dateKey = parseShanghaiDateKey(item.createdAt);
      if (!dateKey) {
        return;
      }
      deliveryCounts.set(dateKey, (deliveryCounts.get(dateKey) ?? 0) + 1);
    });

    const today = new Date();
    const series = Array.from({ length: 30 }, (_, index) => {
      const date = new Date(today);
      date.setDate(today.getDate() - (29 - index));
      const label = formatShanghaiDateKey(date);
      return {
        label,
        value: deliveryCounts.get(label) ?? 0,
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
  const logs = ref<any[]>([]);
  const trendWindow = ref<3 | 7 | 30>(3);
  const runLogAnalysis = ref<TransferRunLogAnalysisViewDTO | null>(null);
  const objectAnalysis = ref<TransferObjectAnalysisViewDTO | null>(null);
  const runLogRows = ref<TransferRunLogViewDTO[]>([]);
  const deliveryRecordSummary = ref<TransferDeliveryRecordSummaryDTO | null>(
    null,
  );
  const outsourcedTaskSummary = ref<OutsourcedDataTaskSummaryDTO | null>(null);
  const outsourcedTaskRows = ref<OutsourcedDataTaskBatchDTO[]>([]);
  const searchModel = reactive({
    keyword: "",
    enabledStatus: "all",
    category: "all",
    targetType: "",
  });
  const targetDraft = reactive({ targetType: "EMAIL" });
  const ruleDraft = reactive({ matchStrategy: "SCRIPT" });

  const deliveryCount = computed(() =>
    Number(deliveryRecordSummary.value?.todayDeliveryCount ?? 0),
  );
  const successCount = computed(() =>
    Number(deliveryRecordSummary.value?.todaySuccessCount ?? 0),
  );
  const successRate = computed(() => {
    const rawRate = deliveryRecordSummary.value?.successRate;
    if (rawRate !== undefined && rawRate !== null) {
      return Number(rawRate);
    }
    return deliveryCount.value === 0
      ? 0
      : Math.round((successCount.value / deliveryCount.value) * 1000) / 10;
  });
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
  const formatBytes = (value: number | undefined) => {
    const size = Number(value ?? 0);
    if (!Number.isFinite(size) || size <= 0) {
      return "0 B";
    }
    const units = ["B", "KB", "MB", "GB", "TB"];
    let current = size;
    let unitIndex = 0;
    while (current >= 1024 && unitIndex < units.length - 1) {
      current /= 1024;
      unitIndex += 1;
    }
    const precision = unitIndex === 0 ? 0 : current >= 10 ? 1 : 2;
    return `${current.toFixed(precision)} ${units[unitIndex]}`;
  };

  const parseTaskStageOrder = outsourcedDataTaskStageCatalog.map(
    (item) => item.stage,
  );
  const normalizeSummaryStage = (
    item: OutsourcedDataTaskStageSummaryDTO,
    fallbackStage: string,
  ) => {
    const stage = String(item.stage ?? item.step ?? fallbackStage)
      .trim()
      .toUpperCase();
    const stageMeta = outsourcedDataTaskStageCatalog.find(
      (catalogItem) =>
        catalogItem.stage === stage ||
        catalogItem.step === stage ||
        catalogItem.stageName === item.stageName ||
        catalogItem.stepName === item.stepName,
    );
    const totalCount = Number(item.totalCount ?? 0);
    const runningCount = Number(item.runningCount ?? 0);
    const failedCount = Number(item.failedCount ?? 0);
    const pendingCount = Number(item.pendingCount ?? 0);
    const successCount = Math.max(
      0,
      totalCount - runningCount - failedCount - pendingCount,
    );
    return {
      key: stage || fallbackStage,
      stage,
      label:
        stageMeta?.stageName ||
        stageMeta?.stepName ||
        item.stageName ||
        item.stepName ||
        fallbackStage,
      description:
        stageMeta?.stageDescription ||
        stageMeta?.stepDescription ||
        item.stageDescription ||
        item.stepDescription ||
        "",
      totalCount,
      successCount,
      failedCount,
      pendingCount,
    };
  };
  const stageChartRows = computed(() => {
    const sourceSummaries =
      outsourcedTaskSummary.value?.stageCatalog?.length
        ? outsourcedTaskSummary.value.stageCatalog
        : outsourcedTaskSummary.value?.stepSummaries ?? [];
    return parseTaskStageOrder.map((stage, index) => {
      const matched = sourceSummaries.find((item) => {
        const normalizedStage = String(item.stage ?? item.step ?? "")
          .trim()
          .toUpperCase();
        return (
          normalizedStage === stage ||
          String(item.stageName ?? "").trim() ===
            outsourcedDataTaskStageCatalog[index]?.stageName ||
          String(item.stepName ?? "").trim() ===
            outsourcedDataTaskStageCatalog[index]?.stepName
        );
      });
      return normalizeSummaryStage(
        matched ?? {
          stage,
          step: stage,
          stageName: outsourcedDataTaskStageCatalog[index]?.stageName,
          stepName: outsourcedDataTaskStageCatalog[index]?.stepName,
          stageDescription: outsourcedDataTaskStageCatalog[index]?.stageDescription,
          stepDescription: outsourcedDataTaskStageCatalog[index]?.stepDescription,
        },
        stage,
      );
    });
  });
  const stageChartMax = computed(() =>
    Math.max(
      ...stageChartRows.value.map(
        (item) => item.successCount + item.failedCount + item.pendingCount,
      ),
      1,
    ),
  );
  const hasStageChartData = computed(() =>
    stageChartRows.value.some(
      (item) =>
        item.totalCount > 0 ||
        item.successCount > 0 ||
        item.failedCount > 0 ||
        item.pendingCount > 0,
    ),
  );

  const sourceAnalysisCards = computed(() =>
    (() => {
      const analyses = objectAnalysis.value?.sourceAnalyses ?? [];
      const findAnalysis = (matcher: (sourceType: string) => boolean) =>
        analyses.find((item) => matcher(String(item.sourceType ?? "")));
      const createCard = (
        sourceLabel: string,
        analysis?: (typeof analyses)[number] | null,
      ) => {
        const statusCounts = analysis?.statusCounts ?? [];
        const mailFolderCounts = analysis?.mailFolderCounts ?? [];
        const countStatus = (status: string) =>
          Number(
            statusCounts.find(
              (item) =>
                String(item.status ?? "")
                  .trim()
                  .toUpperCase() === status,
            )?.count ?? 0,
          );
        const countMailFolder = (mailFolder: string) =>
          Number(
            mailFolderCounts.find(
              (item) =>
                String(item.mailFolder ?? "")
                  .trim()
                  .toUpperCase() === mailFolder,
            )?.count ?? 0,
          );
        return {
          key: sourceLabel,
          sourceLabel,
          totalCount: Number(analysis?.totalCount ?? 0),
          statusItems: [
            {
              key: `${sourceLabel}-identified`,
              label: "已识别",
              value: countStatus("IDENTIFIED"),
              color: "#1677ff",
            },
            {
              key: `${sourceLabel}-inbox`,
              label: "已收取",
              value: countMailFolder("INBOX"),
              color: "#52c41a",
            },
            {
              key: `${sourceLabel}-skipped`,
              label: "已跳过",
              value: countStatus("SKIPPED"),
              color: "#ff4d4f",
            },
          ],
        };
      };
      const emailAnalysis = findAnalysis(
        (sourceType) => sourceType.trim().toUpperCase() === "EMAIL",
      );
      const httpAnalysis =
        findAnalysis((sourceType) => {
          const normalized = sourceType.trim().toUpperCase();
          return normalized === "HTTP" || normalized.includes("HTTP");
        }) ?? null;
      return [
        createCard("EMAIL", emailAnalysis ?? null),
        createCard("transfer_source_http", httpAnalysis),
      ];
    })(),
  );
  const overviewStatusChartItems = computed(() => {
    const totals = sourceAnalysisCards.value.reduce(
      (acc, card) => {
        card.statusItems.forEach((item) => {
          if (item.label === "已识别") {
            acc.identified += Number(item.value ?? 0);
          } else if (item.label === "已收取") {
            acc.inbox += Number(item.value ?? 0);
          } else if (item.label === "已跳过") {
            acc.skipped += Number(item.value ?? 0);
          }
        });
        return acc;
      },
      { identified: 0, inbox: 0, skipped: 0 },
    );
    const values = [
      { name: "已识别", value: totals.identified, color: "#1677ff" },
      { name: "已收取", value: totals.inbox, color: "#52c41a" },
      { name: "已跳过", value: totals.skipped, color: "#ff4d4f" },
    ];
    return values.some((item) => item.value > 0)
      ? values
      : [{ name: "暂无数据", value: 1, color: "rgba(15, 23, 42, 0.12)" }];
  });

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
      description: "今日统计的文件投递数量",
      tone: "primary",
    },
    {
      key: "success-rate",
      label: "成功率",
      value: `${successRate.value.toFixed(1)}%`,
      description: "今日成功投递占总投递比例",
      tone: "success",
    },
    {
      key: "receive-size",
      label: "收取文件大小统计",
      value: formatBytes(page.objectSizeAnalysis.totalSizeBytes)+"",
      description: `共 ${page.objectSizeAnalysis.totalCount} 个文件， 按后缀统计展示`,
      tone: "success",
    }
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
      description: "今日成功投递与总投递的比例关系",
      tone: successRate.value >= 95 ? "success" : "warning",
    },
  ]);

  const isAbnormalOutsourcedTask = (status?: string) => {
    const normalizedStatus = String(status ?? "")
      .trim()
      .toUpperCase();
    return ["FAILED", "BLOCKED"].includes(normalizedStatus);
  };
  const anomalyItems = computed(() =>
    outsourcedTaskRows.value
      .filter((item) => isAbnormalOutsourcedTask(item.status))
      .slice(0, 4)
      .map((item, index) => ({
        key: `${item.batchId ?? index}`,
        batchId: item.batchId,
        batchName: item.batchName || item.originalFileName || item.batchId,
        productCode: item.productCode,
        productName: item.productName,
        managerName: item.managerName,
        status: item.status,
        statusName: item.statusName || item.status || "异常",
        currentStageName: item.currentStageName || item.currentStepName,
        currentStepName: item.currentStepName || item.currentStageName,
        originalFileName: item.originalFileName,
        startedAt: item.startedAt,
        lastErrorMessage: item.lastErrorMessage || "未提供错误信息",
      })),
  );
  const anomalyCount = computed(
    () =>
      Number(
        outsourcedTaskSummary.value?.failedCount ??
          outsourcedTaskRows.value.filter((item) =>
            isAbnormalOutsourcedTask(item.status),
          ).length ??
          0,
      ),
  );

  const overviewMetrics = computed(() => [
    {
      key: "delivery",
      label: "文件投递个数",
      value: deliveryCount.value,
      description: "今日统计的文件投递数量",
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

  const overviewStatusTotal = computed(() =>
    Number(
      sourceAnalysisCards.value.reduce(
        (sum, item) => sum + Number(item.totalCount ?? 0),
        0,
      ),
    ),
  );

  const objectSizeAnalysis = computed(() => ({
    totalCount: Number(objectAnalysis.value?.sizeAnalysis?.totalCount ?? 0),
    totalSizeBytes: Number(
      objectAnalysis.value?.sizeAnalysis?.totalSizeBytes ?? 0,
    ),
    extensionCounts: objectAnalysis.value?.sizeAnalysis?.extensionCounts ?? [],
  }));

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

  const trendSeries = computed(() => buildTrendSeries());
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
        name: "来源状态",
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
        data: overviewStatusChartItems.value.map((item) => ({
          name: item.name,
          value: item.value,
          itemStyle: {
            color: item.color,
          },
        })),
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
              text: "总计",
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
              text: String(overviewStatusTotal.value),
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
  const overviewStageHighlights = computed(() =>
    stageChartRows.value.map((item) => ({
      key: item.key,
      label: item.label,
      description: item.description,
      totalCount: item.totalCount,
      successCount: item.successCount,
      failedCount: item.failedCount,
      pendingCount: item.pendingCount,
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
            String(
              stageChartRows.value[params.dataIndex]?.successCount ?? "",
            ),
        },
        data: stageChartRows.value.map((item) => item.successCount),
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
        data: stageChartRows.value.map((item) => item.failedCount),
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
        data: stageChartRows.value.map((item) => item.pendingCount),
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
              text: "暂无解析任务统计",
              fill: "rgba(15, 23, 42, 0.46)",
              fontSize: 13,
              textAlign: "center",
            },
          },
        ],
  }));

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

  const mapRunLogPageQuery = (): PageLogsParams => ({
    pageIndex: 0,
    pageSize: 1000,
    runStage: "DELIVER",
  });

  const loadOverviewRunLogs = async () => {
    try {
      const res = await api.pageLogs(mapRunLogPageQuery());
      runLogRows.value = res?.data ?? [];
      logs.value = runLogRows.value.map((item, index) => ({
        id: item.runLogId || `${index}`,
        fileName: item.originalName || item.transferId || item.runLogId || "-",
        source: item.sourceName || item.sourceCode || item.sourceType || "-",
        target: item.targetName || item.routeName || "-",
        route: item.routeName || item.routeId || "-",
        status: item.runStatus || "UNKNOWN",
        deliveredAt: item.createdAt || "",
        snapshot: {
          request: {
            sourceId: item.sourceId,
            sourceCode: item.sourceCode,
            sourceName: item.sourceName,
            sourceType: item.sourceType,
            transferId: item.transferId,
            routeId: item.routeId,
            triggerType: item.triggerType,
          },
        },
        deliveryId: item.runLogId,
        transferId: item.transferId,
        routeId: item.routeId,
        targetCode: item.routeName || item.routeId,
        targetType: item.sourceType,
        executeStatus: item.runStatus,
        executeStatusLabel: item.runStatusLabel || item.runStatus || "未知",
        errorMessage: item.errorMessage,
        createdAt: item.createdAt,
      }));
    } catch (error) {
      console.error("加载总览运行日志失败:", error);
      runLogRows.value = [];
      logs.value = [];
    }
  };

  const loadOverviewSnapshot = async () => {
    try {
      const [
        sources,
        targets,
        rules,
        analysisResult,
        objectAnalysisResult,
        runLogsPage,
        outsourcedTaskSummaryResult,
        outsourcedTaskFailedPageResult,
        outsourcedTaskBlockedPageResult,
        deliverySummaryResult,
      ] = await Promise.all([
        api.listSources(),
        api.listTargets(),
        api.listRules(),
        api.analyzeLogs(),
        api.analyzeObjects(),
        api.pageLogs(mapRunLogPageQuery()),
        getOutsourcedDataTaskSummary(),
        pageOutsourcedDataTasks({
          status: "FAILED",
          pageIndex: 1,
          pageSize: 10,
        }),
        pageOutsourcedDataTasks({
          status: "BLOCKED",
          pageIndex: 1,
          pageSize: 10,
        }),
        getTransferDeliveryRecordSummary().catch(() => null),
      ]);

      sourceRows.value = unwrapMultiResult(sources);
      targetRows.value = unwrapMultiResult(targets);
      ruleRows.value = unwrapMultiResult(rules);

      const analysis = unwrapSingleResult(analysisResult);
      runLogAnalysis.value = analysis ?? null;
      objectAnalysis.value = unwrapSingleResult(objectAnalysisResult) ?? null;
      runLogRows.value = runLogsPage?.data ?? [];
      outsourcedTaskSummary.value =
        unwrapSingleResult(outsourcedTaskSummaryResult) ?? null;
      outsourcedTaskRows.value = [
        ...(outsourcedTaskFailedPageResult?.data ?? []),
        ...(outsourcedTaskBlockedPageResult?.data ?? []),
      ].sort((left, right) => {
        const leftTime = new Date(left.startedAt ?? "").getTime();
        const rightTime = new Date(right.startedAt ?? "").getTime();
        return (Number.isNaN(rightTime) ? 0 : rightTime) -
          (Number.isNaN(leftTime) ? 0 : leftTime);
      });
      deliveryRecordSummary.value =
        unwrapSingleResult(deliverySummaryResult) ?? null;
      logs.value = runLogRows.value.map((item, index) => ({
        id: item.runLogId || `${index}`,
        fileName: item.originalName || item.transferId || item.runLogId || "-",
        source: item.sourceName || item.sourceCode || item.sourceType || "-",
        target: item.targetName || item.routeName || "-",
        route: item.routeName || item.routeId || "-",
        status: item.runStatus || "UNKNOWN",
        deliveredAt: item.createdAt || "",
        snapshot: {
          request: {
            sourceId: item.sourceId,
            sourceCode: item.sourceCode,
            sourceName: item.sourceName,
            sourceType: item.sourceType,
            transferId: item.transferId,
            routeId: item.routeId,
            triggerType: item.triggerType,
          },
        },
        deliveryId: item.runLogId,
        transferId: item.transferId,
        routeId: item.routeId,
        targetCode: item.routeName || item.routeId,
        targetType: item.sourceType,
        executeStatus: item.runStatus,
        executeStatusLabel: item.runStatusLabel || item.runStatus || "未知",
        errorMessage: item.errorMessage,
        createdAt: item.createdAt,
      }));
    } catch (error) {
      console.error("加载分拣总览数据失败:", error);
      logs.value = [];
      runLogAnalysis.value = null;
      objectAnalysis.value = null;
      deliveryRecordSummary.value = null;
      outsourcedTaskSummary.value = null;
      outsourcedTaskRows.value = [];
      await loadOverviewRunLogs();
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
    overviewStatusTotal,
    sourceAnalysisCards,
    overviewStageHighlights,
    anomalyCount,
    objectSizeAnalysis,
    formatBytes,
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
