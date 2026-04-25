import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { message, Modal } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import { formatDateTime } from "@/utils/format";
import { copyToClipboard } from "@/utils";
import { transferIngestProgressSse } from "@/services/transferIngestProgressSse";
import { GetTemplateName1SourceType } from "@/api/generated/valset/schemas/getTemplateName1SourceType";
import { GetTemplateName2TargetType } from "@/api/generated/valset/schemas/getTemplateName2TargetType";
import type {
  ListRulesParams,
  ListLogsParams,
  ListRoutes1Params,
  ListSourcesParams,
  ListTargetsParams,
  TransferRouteUpsertCommand,
  TransferRouteViewDTO,
  TransferRuleViewDTO,
  TransferSourceViewDTO,
  TransferTargetViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import {
  unwrapMultiResult,
  unwrapSingleResult,
} from "@/utils/api-response";
import type {
  RouteConfigPage,
  RouteFlowAnchor,
  RouteFlowChainNode,
  RouteFlowFactMessage,
  RouteFlowPreview,
  RouteFlowStats,
  SourceIngestMessage,
} from "../types";

type QueryState = {
  sourceCode: string;
  sourceType: string;
  ruleId: string;
  targetCode: string;
  targetType: string;
  limit: number;
};

type RouteFormState = RouteConfigPage["formState"];
type SourceIngestState = {
  ingestBusy?: boolean;
  ingestFinishedAt?: string;
  ingestStartedAt?: string;
  ingestTriggerType?: string;
  ingestStatus?: string;
  processedCount?: number;
  progressPercent?: number;
  statusMessage?: string;
  totalCount?: number;
};

type RouteExecutionState = {
  createdAt?: string;
  errorMessage?: string;
  logMessage?: string;
  runStage?: string;
  runStatus?: string;
};

const SELECT_LIMIT = 200;
const api = getJavaSpringBootQuartzApi();

const defaultQuery = (): QueryState => ({
  sourceCode: "",
  sourceType: "",
  ruleId: "",
  targetCode: "",
  targetType: "",
  limit: 100,
});

const TARGET_TYPE_LABELS: Record<string, string> = {
  EMAIL: "邮件",
  S3: "S3",
  SFTP: "SFTP",
  LOCAL_DIR: "本地目录",
  FILESYS: "文件服务",
};

const defaultForm = (): RouteFormState => ({
  routeId: undefined,
  sourceId: undefined,
  sourceCode: "",
  sourceType: "",
  targetCode: "",
  targetType: "",
  targetPath: "",
  ruleId: undefined,
  renamePattern: "",
});

const uniqueByKey = <T>(
  items: T[],
  keyGetter: (item: T) => string | number | undefined,
) => {
  const seen = new Set<string | number>();
  return items.filter((item) => {
    const key = keyGetter(item);
    if (key === undefined || key === null) {
      return false;
    }
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

const buildSourceOption = (item: TransferSourceViewDTO) => ({
  label: [item.sourceCode, item.sourceName, item.sourceType]
    .filter(Boolean)
    .join(" · "),
  value: item.sourceCode ?? "",
});

const describeTriggerType = (value?: string) => {
  const normalized = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!normalized) {
    return "未知";
  }
  if (normalized === "CRON") {
    return "cron 定时";
  }
  if (normalized === "MANUAL") {
    return "手动触发";
  }
  if (normalized === "SYSTEM") {
    return "系统触发";
  }
  return normalized;
};

const formatTriggerTime = (value?: string) => formatDateTime(value);

const buildTargetOption = (item: TransferTargetViewDTO) => ({
  label: [
    item.targetCode,
    item.targetName,
    item.targetType,
    item.targetPathTemplate,
  ]
    .filter(Boolean)
    .join(" · "),
  value: item.targetCode ?? "",
});

const buildRuleOption = (item: TransferRuleViewDTO) => ({
  label: [item.ruleId, item.ruleCode, item.ruleName, item.ruleVersion]
    .filter(Boolean)
    .join(" · "),
  value: item.ruleId ?? "",
});

const buildFlowPreview = (
  row: TransferRouteViewDTO | null,
  ruleLabel: string,
  sourceState?: SourceIngestState | null,
): RouteFlowPreview => {
  const sourceTitle = row?.sourceCode?.trim() || "请选择一条路由";
  const sourceTriggerSummary = sourceState?.ingestTriggerType
    ? `${describeTriggerType(sourceState.ingestTriggerType)} · ${formatTriggerTime(sourceState.ingestStartedAt)}`
    : sourceState?.ingestStartedAt
      ? `触发时间 ${formatTriggerTime(sourceState.ingestStartedAt)}`
      : "";
  const sourceMeta = [
    row?.sourceType || "来源类型待补全",
    row?.sourceId != null ? `来源ID ${row.sourceId}` : "",
    sourceTriggerSummary,
  ]
    .filter(Boolean)
    .join(" · ");
  const ruleTitle = ruleLabel || "路由规则待选择";
  const ruleMeta = [
    row?.renamePattern ? `模板 ${row.renamePattern}` : "默认命名",
    ruleLabel ? `规则 ${ruleLabel}` : "规则待选择",
  ]
    .filter(Boolean)
    .join(" · ");
  const targetTitle = row?.targetCode?.trim() || "目标待配置";
  const targetMeta = [
    row?.targetType || "目标类型待补全",
    row?.targetPath || "目标路径待配置",
  ]
    .filter(Boolean)
    .join(" · ");
  const routeMetaSummary = formatRouteMetaCount(row);

  return {
    sourceTitle,
    sourceMeta,
    ruleTitle,
    ruleMeta,
    targetTitle,
    targetMeta,
    routeMetaSummary,
  };
};

const buildFlowAnchor = (
  selectedRow: TransferRouteViewDTO | null,
  rows: TransferRouteViewDTO[],
  ruleLabel: string,
) => {
  if (selectedRow?.routeId != null) {
    return {
      label: "当前映射",
      detail:
        [selectedRow.sourceCode, ruleLabel, selectedRow.targetCode]
          .filter(Boolean)
          .join(" → ") || `路由 ${selectedRow.routeId}`,
    };
  }

  const firstRow = rows[0];
  if (firstRow?.routeId != null) {
    return {
      label: "默认映射",
      detail:
        [firstRow.sourceCode, ruleLabel, firstRow.targetCode]
          .filter(Boolean)
          .join(" → ") || `路由 ${firstRow.routeId}`,
    };
  }

  return {
    label: "暂无映射",
    detail: "请先创建或选择一条路由映射",
  } satisfies RouteFlowAnchor;
};

const formatRouteMetaCount = (row: TransferRouteViewDTO | null) => {
  if (!row?.routeMeta) {
    return "无扩展信息";
  }
  const keys = Object.keys(row.routeMeta);
  return keys.length ? `${keys.length} 个扩展字段` : "无扩展信息";
};

const normalizeIngestStatus = (value?: string) => {
  const normalized = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!normalized) {
    return "-";
  }
  if (normalized === "RUNNING") {
    return "收取中";
  }
  if (normalized === "STOPPING") {
    return "停止中";
  }
  if (normalized === "STOPPED") {
    return "已停止";
  }
  if (normalized === "SUCCESS") {
    return "收取成功";
  }
  if (
    normalized === "COMPLETED" ||
    normalized === "COMPLETE" ||
    normalized === "FINISHED"
  ) {
    return "收取成功";
  }
  if (normalized === "FAILED") {
    return "收取失败";
  }
  if (normalized === "SKIPPED") {
    return "已跳过";
  }
  if (normalized === "REUSED") {
    return "复用";
  }
  if (normalized === "IDLE") {
    return "待收取";
  }
  return normalized;
};

const normalizeIngestStatusWord = (value?: string, ingestBusy?: boolean) => {
  const normalized = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!normalized) {
    return ingestBusy ? "loading" : "pending";
  }
  if (normalized === "RUNNING" || normalized === "STOPPING") {
    return "loading";
  }
  if (
    normalized === "SUCCESS" ||
    normalized === "COMPLETED" ||
    normalized === "COMPLETE" ||
    normalized === "FINISHED" ||
    normalized === "SKIPPED" ||
    normalized === "REUSED"
  ) {
    return "success";
  }
  if (normalized === "FAILED") {
    return "error";
  }
  if (normalized === "STOPPED" || normalized === "IDLE") {
    return "pending";
  }
  return "pending";
};

const normalizeIngestBusy = (state?: SourceIngestState | null) => {
  const status = String(state?.ingestStatus ?? "")
    .trim()
    .toUpperCase();
  return Boolean(state?.ingestBusy) || status === "RUNNING" || status === "STOPPING";
};

const normalizeProgressPercent = (
  processedCount?: number,
  totalCount?: number,
) => {
  if (!Number.isFinite(processedCount) || processedCount === undefined) {
    return undefined;
  }
  if (!Number.isFinite(totalCount) || totalCount === undefined || totalCount <= 0) {
    return undefined;
  }
  return Math.max(
    0,
    Math.min(100, Math.round((Number(processedCount) / Number(totalCount)) * 100)),
  );
};

const isTerminalIngestStatus = (value?: string) => {
  const normalized = String(value ?? "")
    .trim()
    .toUpperCase();
  return [
    "SUCCESS",
    "COMPLETED",
    "COMPLETE",
    "FINISHED",
    "FAILED",
    "STOPPED",
    "SKIPPED",
    "REUSED",
  ].includes(normalized);
};

const getCurrentTimeText = () => formatDateTime(new Date().toISOString());

const getTriggerSummaryText = (state?: SourceIngestState | null) => {
  if (!state) {
    return "";
  }
  if (!state.ingestTriggerType && !state.ingestStartedAt) {
    return "";
  }
  if (!state.ingestTriggerType) {
    return `触发时间 ${formatTriggerTime(state.ingestStartedAt)}`;
  }
  if (!state.ingestStartedAt) {
    return `触发方式 ${describeTriggerType(state.ingestTriggerType)}`;
  }
  return `触发方式 ${describeTriggerType(state.ingestTriggerType)}，触发时间 ${formatTriggerTime(state.ingestStartedAt)}`;
};

const buildRouteFlowFacts = (
  row: TransferRouteViewDTO,
  sourceState?: SourceIngestState | null,
  ruleLabel?: string,
  routeIssue?: RouteExecutionState | null,
): RouteFlowFactMessage[] => {
  const statusText = normalizeIngestStatus(sourceState?.ingestStatus);
  const isBusy = normalizeIngestBusy(sourceState);
  const sourceLabel = row.sourceCode || row.sourceId || "来源";
  const routeRuleLabel = ruleLabel || (row.ruleId ? `规则 ${row.ruleId}` : "规则待补全");
  const targetLabel = row.targetCode || "目标";
  const targetPath = row.targetPath || "路径待配置";
  const routeIssueText = routeIssue
    ? [
        routeIssue.runStage ? `阶段 ${routeIssue.runStage}` : "",
        routeIssue.runStatus ? `状态 ${routeIssue.runStatus}` : "",
        routeIssue.errorMessage || routeIssue.logMessage || "",
      ]
        .filter(Boolean)
        .join("，")
    : "";
  const routeIssuePrefix = routeIssueText ? `，最近异常 ${routeIssueText}` : "";
  const progressText =
    sourceState?.processedCount !== undefined &&
    sourceState?.totalCount !== undefined &&
    sourceState.totalCount > 0
      ? `，已处理 ${sourceState.processedCount}/${sourceState.totalCount}，进度 ${sourceState.progressPercent ?? normalizeProgressPercent(sourceState.processedCount, sourceState.totalCount) ?? 0}%`
      : "";

  return [
    {
      stage: "source",
      title: "来源",
      content: isBusy
        ? `来源 ${sourceLabel} 正在收取${getTriggerSummaryText(sourceState) ? `，${getTriggerSummaryText(sourceState)}` : ""}${progressText}。`
        : `来源 ${sourceLabel} ${statusText === "-" ? "已就绪，等待收取。" : `当前状态 ${statusText}`}${getTriggerSummaryText(sourceState) ? `，最近${getTriggerSummaryText(sourceState)}` : ""}`,
      timeText: sourceState?.ingestStartedAt
        ? formatTriggerTime(sourceState.ingestStartedAt)
        : getCurrentTimeText(),
    },
    {
      stage: "route",
      title: "路由",
      content: routeIssueText
        ? `路由 ${routeRuleLabel} 已完成匹配，重命名模板 ${row.renamePattern || "默认命名"}。${routeIssuePrefix}`
        : `路由 ${routeRuleLabel} 已完成匹配，重命名模板 ${row.renamePattern || "默认命名"}。`,
      timeText: getCurrentTimeText(),
    },
    {
      stage: "target",
      title: "目标",
      content: routeIssueText
        ? `目标 ${targetLabel} 正在等待投递，目标路径 ${targetPath}。${routeIssuePrefix}`
        : `目标 ${targetLabel} 正在等待投递，目标路径 ${targetPath}。`,
      timeText: getCurrentTimeText(),
    },
  ];
};

export const useTransferPage = (): { page: RouteConfigPage } => {
  const query = reactive<QueryState>(defaultQuery());
  const rows = ref<TransferRouteViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["10", "20", "50", "100"],
  });
  const selectedRow = ref<TransferRouteViewDTO | null>(null);
  const detailVisible = ref(false);
  const formVisible = ref(false);
  const formMode = ref<"create" | "edit">("create");
  const formState = reactive<RouteFormState>(defaultForm());
  const sourceRows = ref<TransferSourceViewDTO[]>([]);
  const targetRows = ref<TransferTargetViewDTO[]>([]);
  const ruleRows = ref<TransferRuleViewDTO[]>([]);
  const selectLoading = ref(false);
  const listLoading = ref(false);
  const formSubmitting = ref(false);
  const formRef = ref<{ validate: () => Promise<void> } | null>(null);
  const sourceActionLoadingIds = reactive<Record<string, boolean>>({});
  const sourceIngestStates = reactive<Record<string, SourceIngestState>>({});
  const routeExecutionStates = reactive<Record<string, RouteExecutionState>>({});
  const routeFlowFactMessages = reactive<
    Record<string, RouteFlowFactMessage[]>
  >({});
  const sourceIngestMessages = reactive<Record<string, SourceIngestMessage[]>>({});
  const sourceTypeOptions = Object.values(GetTemplateName1SourceType).map(
    (value) => ({
      label: value,
      value,
    }),
  );
  const targetTypeOptions = Object.values(GetTemplateName2TargetType).map(
    (value) => ({
      label: TARGET_TYPE_LABELS[value] ?? value,
      value,
    }),
  );

  let listRequestId = 0;
  let selectRequestId = 0;

  const sourceOptions = computed(() => sourceRows.value.map(buildSourceOption));
  const targetOptions = computed(() => targetRows.value.map(buildTargetOption));
  const ruleOptions = computed(() => ruleRows.value.map(buildRuleOption));
  const sourceProgressUnsubscribers = new Map<string, () => void>();
  const visibleRows = computed(() => {
    const current = pagination.value.current || 1;
    const pageSizeValue = pagination.value.pageSize || 10;
    const start = (current - 1) * pageSizeValue;
    return rows.value.slice(start, start + pageSizeValue);
  });
  const visibleSourceIds = computed(() =>
    Array.from(
      new Set(
        visibleRows.value
          .map((row) => row.sourceId?.trim())
          .filter((value): value is string => Boolean(value)),
      ),
    ),
  );

  const getRuleDisplayName = (ruleId?: string | number | null) => {
    const normalizedRuleId = String(ruleId ?? "").trim();
    if (!normalizedRuleId) {
      return "-";
    }

    const rule = ruleRows.value.find((item) => item.ruleId === normalizedRuleId);
    return rule?.ruleName?.trim() || rule?.ruleCode?.trim() || normalizedRuleId;
  };

  const resetForm = () => {
    Object.assign(formState, defaultForm());
  };

  const clearTrackedSourceState = (sourceId: string) => {
    delete sourceIngestStates[sourceId];
    delete routeFlowFactMessages[sourceId];
    delete sourceIngestMessages[sourceId];
  };

  const updateRouteFlowFactsForSource = (sourceId: string) => {
    const row = rows.value.find((item) => item.sourceId === sourceId);
    if (!row) {
      delete routeFlowFactMessages[sourceId];
      return;
    }
    const routeIssue = row.routeId ? routeExecutionStates[row.routeId] ?? null : null;
    routeFlowFactMessages[sourceId] = buildRouteFlowFacts(
      row,
      sourceIngestStates[sourceId] ?? null,
      getRuleDisplayName(row.ruleId),
      routeIssue,
    );
  };

  const appendSourceIngestMessage = (sourceId: string, content: string) => {
    if (!sourceId || !content) {
      return;
    }
    const history = sourceIngestMessages[sourceId] ?? [];
    sourceIngestMessages[sourceId] = [
      ...history,
      {
        title: "邮件收取消息",
        content,
        timeText: getCurrentTimeText(),
      },
    ].slice(-20);
  };

  const updateRouteFlowFactsForRoute = (routeId: string) => {
    const row = rows.value.find((item) => item.routeId === routeId);
    if (!row?.sourceId) {
      return;
    }
    updateRouteFlowFactsForSource(row.sourceId);
  };

  const upsertSourceIngestState = (
    sourceId: string,
    nextState: SourceIngestState,
  ) => {
    sourceIngestStates[sourceId] = nextState;
    updateRouteFlowFactsForSource(sourceId);
  };

  const patchSourceIngestState = (
    sourceId: string,
    patch: Partial<SourceIngestState>,
  ) => {
    const current = sourceIngestStates[sourceId] ?? {};
    upsertSourceIngestState(sourceId, {
      ...current,
      ...patch,
    });
  };

  const upsertRouteExecutionState = (
    routeId: string,
    nextState: RouteExecutionState,
  ) => {
    routeExecutionStates[routeId] = nextState;
    updateRouteFlowFactsForRoute(routeId);
  };

  const clearRouteExecutionStates = (routeIds: string[]) => {
    const nextRouteIdSet = new Set(routeIds);
    Object.keys(routeExecutionStates).forEach((routeId) => {
      if (!nextRouteIdSet.has(routeId)) {
        delete routeExecutionStates[routeId];
      }
    });
  };

  const syncSourceProgressSubscriptions = (sourceIds: string[]) => {
    const nextSourceIds = new Set(sourceIds);

    Array.from(sourceProgressUnsubscribers.keys()).forEach((sourceId) => {
      if (nextSourceIds.has(sourceId)) {
        return;
      }
      const unsubscribe = sourceProgressUnsubscribers.get(sourceId);
      unsubscribe?.();
      sourceProgressUnsubscribers.delete(sourceId);
      clearTrackedSourceState(sourceId);
    });

    sourceIds.forEach((sourceId) => {
      if (sourceProgressUnsubscribers.has(sourceId)) {
        return;
      }

      const unsubscribe = transferIngestProgressSse.subscribe(
        sourceId,
        (message) => {
          if (message.sourceId !== sourceId) {
            return;
          }

          if (message.type === "status") {
            const normalizedStatus = String(message.data.status ?? "")
              .trim()
              .toUpperCase();
            const busy = ["RUNNING", "STOPPING"].includes(normalizedStatus);
            const currentState = sourceIngestStates[sourceId];
            const nextStartedAt =
              message.data.triggeredAt ??
              currentState?.ingestStartedAt ??
              (normalizedStatus === "RUNNING" ? getCurrentTimeText() : undefined);
            const nextBusy = busy
              ? true
              : isTerminalIngestStatus(normalizedStatus)
                ? false
                : Boolean(currentState?.ingestBusy);
            patchSourceIngestState(sourceId, {
              ingestBusy: nextBusy,
              ingestStatus: normalizedStatus || currentState?.ingestStatus,
              statusMessage: message.data.message,
              ingestTriggerType:
                message.data.triggerType ?? currentState?.ingestTriggerType,
              ingestStartedAt: nextStartedAt,
              ingestFinishedAt: isTerminalIngestStatus(normalizedStatus)
                ? getCurrentTimeText()
                : currentState?.ingestFinishedAt,
              progressPercent:
                normalizedStatus === "RUNNING" || normalizedStatus === "STOPPING"
                  ? currentState?.progressPercent ?? 0
                  : normalizedStatus === "SUCCESS" ||
                      normalizedStatus === "COMPLETED" ||
                      normalizedStatus === "COMPLETE" ||
                      normalizedStatus === "FINISHED" ||
                      normalizedStatus === "STOPPED"
                    ? 100
                    : currentState?.progressPercent,
            });
            return;
          }

          if (message.type === "progress") {
            const progressPercent = normalizeProgressPercent(
              message.data.processedCount,
              message.data.totalCount,
            );
            patchSourceIngestState(sourceId, {
              ingestBusy: true,
              ingestStatus:
                sourceIngestStates[sourceId]?.ingestStatus || "RUNNING",
              processedCount: message.data.processedCount,
              totalCount: message.data.totalCount,
              progressPercent:
                progressPercent ?? sourceIngestStates[sourceId]?.progressPercent,
              statusMessage: message.data.message,
              ingestStartedAt:
                sourceIngestStates[sourceId]?.ingestStartedAt ??
                getCurrentTimeText(),
            });
            return;
          }

          if (message.type === "message") {
            const text = String(message.data.message ?? "").trim();
            if (!text) {
              return;
            }
            appendSourceIngestMessage(sourceId, text);
            patchSourceIngestState(sourceId, {
              ingestBusy: true,
              ingestStatus:
                sourceIngestStates[sourceId]?.ingestStatus || "RUNNING",
              statusMessage: text,
              ingestStartedAt:
                sourceIngestStates[sourceId]?.ingestStartedAt ??
                getCurrentTimeText(),
            });
            updateRouteFlowFactsForSource(sourceId);
            return;
          }

          if (message.type === "complete") {
            patchSourceIngestState(sourceId, {
              ingestBusy: false,
              ingestStatus: "SUCCESS",
              progressPercent: 100,
              ingestFinishedAt: getCurrentTimeText(),
              statusMessage: message.data.message,
            });
            return;
          }

          if (message.type === "error") {
            patchSourceIngestState(sourceId, {
              ingestBusy: false,
              ingestStatus: "FAILED",
              ingestFinishedAt: getCurrentTimeText(),
              statusMessage: message.data.message,
            });
          }
        },
      );

      sourceProgressUnsubscribers.set(sourceId, unsubscribe);
    });
  };

  onBeforeUnmount(() => {
    sourceProgressUnsubscribers.forEach((unsubscribe) => unsubscribe());
    sourceProgressUnsubscribers.clear();
  });

  watch(
    visibleSourceIds,
    (sourceIds) => {
      syncSourceProgressSubscriptions(sourceIds);
    },
    { immediate: true },
  );

  const hydrateFormFromRow = (row: TransferRouteViewDTO) => {
    Object.assign(formState, defaultForm(), {
      routeId: row.routeId,
      sourceId: row.sourceId,
      sourceCode: row.sourceCode ?? "",
      sourceType: row.sourceType ?? "",
      targetCode: row.targetCode ?? "",
      targetType: row.targetType ?? "",
      targetPath: row.targetPath ?? "",
      ruleId: row.ruleId,
      renamePattern: row.renamePattern ?? "",
    });
  };

  const ensureCurrentOptionRows = (row: TransferRouteViewDTO) => {
    if (
      row.sourceCode &&
      !sourceRows.value.some((item) => item.sourceCode === row.sourceCode)
    ) {
      sourceRows.value = uniqueByKey(
        [
          {
            sourceId: row.sourceId,
            sourceCode: row.sourceCode,
            sourceType: row.sourceType,
            sourceName: row.sourceCode,
          } as TransferSourceViewDTO,
          ...sourceRows.value,
        ],
        (item) => item.sourceCode,
      );
    }

    if (
      row.targetCode &&
      !targetRows.value.some((item) => item.targetCode === row.targetCode)
    ) {
      targetRows.value = uniqueByKey(
        [
          {
            targetCode: row.targetCode,
            targetType: row.targetType,
            targetPathTemplate: row.targetPath,
            targetName: row.targetCode,
          } as TransferTargetViewDTO,
          ...targetRows.value,
        ],
        (item) => item.targetCode,
      );
    }

    if (
      row.ruleId &&
      !ruleRows.value.some((item) => item.ruleId === row.ruleId)
    ) {
      ruleRows.value = uniqueByKey(
        [
          {
            ruleId: row.ruleId,
            ruleCode: String(row.ruleId),
            ruleName: `规则 ${row.ruleId}`,
          } as TransferRuleViewDTO,
          ...ruleRows.value,
        ],
        (item) => item.ruleId,
      );
    }
  };

  const loadSelectRows = async () => {
    const requestId = ++selectRequestId;
    selectLoading.value = true;
    try {
      const [sources, targets, rules] = await Promise.all([
        api.listSources({
          limit: SELECT_LIMIT,
          enabled: true,
        } as ListSourcesParams),
        api.listTargets({
          limit: SELECT_LIMIT,
          enabled: true,
        } as ListTargetsParams),
        api.listRules({
          limit: SELECT_LIMIT,
          enabled: true,
        } as ListRulesParams),
      ]);

      if (requestId !== selectRequestId) {
        return;
      }
      sourceRows.value = unwrapMultiResult(sources);
      targetRows.value = unwrapMultiResult(targets);
      ruleRows.value = unwrapMultiResult(rules);
    } catch (error) {
      if (requestId !== selectRequestId) {
        return;
      }
      console.error("加载下拉数据失败:", error);
      message.error("加载下拉数据失败");
    } finally {
      if (requestId === selectRequestId) {
        selectLoading.value = false;
      }
    }
  };

  const mapQuery = (): ListRoutes1Params => {
    const next: ListRoutes1Params = {};
    if (query.sourceCode) next.sourceCode = query.sourceCode;
    if (query.sourceType) next.sourceType = query.sourceType;
    if (query.ruleId) next.ruleId = query.ruleId;
    if (query.targetCode) next.targetCode = query.targetCode;
    if (query.targetType) next.targetType = query.targetType;
    next.limit = query.limit;
    return next;
  };

  const loadList = async () => {
    const requestId = ++listRequestId;
    listLoading.value = true;
    try {
      const res = await api.listRoutes1(mapQuery());
      if (requestId !== listRequestId) {
        return;
      }
      const records = unwrapMultiResult(res);
      rows.value = records;
      total.value = records.length;
      pagination.value.total = total.value;
      const sourceIds = Array.from(
        new Set(
          records
            .map((row) => row.sourceId?.trim())
            .filter((value): value is string => Boolean(value)),
        ),
      );
      const routeIds = Array.from(
        new Set(
          records
            .map((row) => row.routeId?.trim())
            .filter((value): value is string => Boolean(value)),
        ),
      );
      const entries = await Promise.all(
        sourceIds.map(async (sourceId) => {
          try {
            const source = unwrapSingleResult(await api.getSource(sourceId));
            if (!source) {
              return null;
            }
            return [
              sourceId,
              {
                ingestBusy: source.ingestBusy,
                ingestFinishedAt: source.ingestFinishedAt,
                ingestStartedAt: source.ingestStartedAt,
                ingestTriggerType: source.ingestTriggerType,
                ingestStatus: source.ingestStatus,
                progressPercent: source.ingestBusy
                  ? sourceIngestStates[sourceId]?.progressPercent ?? 0
                  : [
                        "SUCCESS",
                        "COMPLETED",
                        "COMPLETE",
                        "FINISHED",
                        "STOPPED",
                        "FAILED",
                        "SKIPPED",
                        "REUSED",
                      ].includes(String(source.ingestStatus ?? "").toUpperCase())
                    ? 100
                    : sourceIngestStates[sourceId]?.progressPercent,
              } satisfies SourceIngestState,
            ] as const;
          } catch (error) {
            console.error("加载来源收取状态失败:", error);
            return null;
          }
        }),
      );
      const routeExecutionEntries = await Promise.all(
        routeIds.map(async (routeId) => {
          try {
            const logs = unwrapMultiResult(
              await api.listLogs({
                routeId,
                runStatus: "FAILED",
                limit: 5,
              } as ListLogsParams),
            );
            const latest = logs[0];
            if (!latest) {
              return null;
            }
            return [
              routeId,
              {
                createdAt: latest.createdAt,
                errorMessage: latest.errorMessage,
                logMessage: latest.logMessage,
                runStage: latest.runStage,
                runStatus: latest.runStatus,
              } satisfies RouteExecutionState,
            ] as const;
          } catch (error) {
            console.error("加载路由运行日志失败:", error);
            return null;
          }
        }),
      );

      if (requestId !== listRequestId) {
        return;
      }

      const nextSourceIdSet = new Set(sourceIds);
      Array.from(sourceProgressUnsubscribers.keys()).forEach((sourceId) => {
        if (nextSourceIdSet.has(sourceId)) {
          return;
        }
        const unsubscribe = sourceProgressUnsubscribers.get(sourceId);
        unsubscribe?.();
        sourceProgressUnsubscribers.delete(sourceId);
        clearTrackedSourceState(sourceId);
      });

      Object.keys(sourceIngestStates).forEach((key) => {
        if (!nextSourceIdSet.has(key)) {
          delete sourceIngestStates[key];
        }
      });
      Object.keys(routeFlowFactMessages).forEach((key) => {
        if (!nextSourceIdSet.has(key)) {
          delete routeFlowFactMessages[key];
        }
      });
      clearRouteExecutionStates(routeIds);
      entries.forEach((entry) => {
        if (!entry) {
          return;
        }
        const [sourceId, state] = entry;
        sourceIngestStates[sourceId] = {
          ...sourceIngestStates[sourceId],
          ...state,
        };
      });
      routeExecutionEntries.forEach((entry) => {
        if (!entry) {
          return;
        }
        const [routeId, state] = entry;
        upsertRouteExecutionState(routeId, {
          ...routeExecutionStates[routeId],
          ...state,
        });
      });

      records.forEach((row) => {
        if (row.sourceId) {
          updateRouteFlowFactsForSource(row.sourceId);
        }
      });
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      console.error("加载路由列表失败:", error);
      message.error("加载路由列表失败");
      rows.value = [];
      total.value = 0;
      pagination.value.total = 0;
    } finally {
      if (requestId === listRequestId) {
        listLoading.value = false;
      }
    }
  };

  const loadFormContext = async (row?: TransferRouteViewDTO) => {
    await loadSelectRows();
    if (row) {
      ensureCurrentOptionRows(row);
    }
  };

  const openCreateDialog = async () => {
    formMode.value = "create";
    resetForm();
    await loadFormContext();
    formVisible.value = true;
  };

  const openEditDialog = async (row: TransferRouteViewDTO) => {
    formMode.value = "edit";
    try {
      const detail = row.routeId
        ? unwrapSingleResult(await api.getRoute1(row.routeId))
        : row;
      if (!detail) {
        message.error("加载路由详情失败");
        return;
      }

      hydrateFormFromRow(detail);
      await loadFormContext(detail);
      formVisible.value = true;
    } catch (error) {
      console.error("加载路由详情失败:", error);
      message.error("加载路由详情失败");
    }
  };

  const openDetailDrawer = async (row: TransferRouteViewDTO) => {
    try {
      selectedRow.value = row.routeId
        ? unwrapSingleResult(await api.getRoute1(row.routeId)) ?? null
        : row;
      detailVisible.value = true;
    } catch (error) {
      console.error("加载路由详情失败:", error);
      message.error("加载路由详情失败");
    }
  };

  const confirmDelete = (row: TransferRouteViewDTO) => {
    Modal.confirm({
      title: "删除路由配置",
      content: `确认删除路由配置「${row.sourceCode || row.targetCode || row.routeId}」吗？`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: async () => {
        if (!row.routeId) {
          message.error("路由主键缺失，无法删除");
          return;
        }
        try {
          await api.deleteRoute(row.routeId);
          message.success("删除成功");
          await loadList();
        } catch (error) {
          console.error("删除路由失败:", error);
          message.error("删除路由失败");
        }
      },
    });
  };

  const getSourceIngestState = (row: TransferRouteViewDTO | null) => {
    if (!row?.sourceId) {
      return null;
    }
    return sourceIngestStates[row.sourceId] ?? null;
  };

  const getSourceIngestStatusText = (row: TransferRouteViewDTO | null) =>
    normalizeIngestStatus(getSourceIngestState(row)?.ingestStatus);

  const canTriggerSource = (row: TransferRouteViewDTO | null) =>
    Boolean(row?.sourceId) && !normalizeIngestBusy(getSourceIngestState(row));

  const canStopSource = (row: TransferRouteViewDTO | null) =>
    Boolean(row?.sourceId) && normalizeIngestBusy(getSourceIngestState(row));

  const getSourceIngestProgressPercent = (
    row: TransferRouteViewDTO | null,
  ) => {
    const state = getSourceIngestState(row);
    if (!state) {
      return 0;
    }
    if (typeof state.progressPercent === "number") {
      return state.progressPercent;
    }
    const normalized = String(state.ingestStatus ?? "")
      .trim()
      .toUpperCase();
    if (!normalized) {
      return state.ingestBusy ? 60 : 0;
    }
    if (normalized === "RUNNING") {
      return 60;
    }
    if (normalized === "STOPPING") {
      return 80;
    }
    if (
      normalized === "SUCCESS" ||
      normalized === "COMPLETED" ||
      normalized === "COMPLETE" ||
      normalized === "FINISHED" ||
      normalized === "FAILED" ||
      normalized === "STOPPED" ||
      normalized === "SKIPPED" ||
      normalized === "REUSED"
    ) {
      return 100;
    }
    return 0;
  };

  const getSourceIngestProgressText = (row: TransferRouteViewDTO | null) => {
    const state = getSourceIngestState(row);
    if (!state) {
      return "暂无进度";
    }

    if (typeof state.processedCount === "number" && typeof state.totalCount === "number") {
      return `已处理 ${state.processedCount}/${state.totalCount}`;
    }

    if (state.ingestBusy) {
      return "正在收取";
    }

    const percent = getSourceIngestProgressPercent(row);
    if (percent > 0) {
      return `进度 ${percent}%`;
    }

    return "暂无进度";
  };

  const getRouteFlowFactMessages = (row: TransferRouteViewDTO | null) => {
    if (!row?.sourceId) {
      return [];
    }
    return routeFlowFactMessages[row.sourceId] ?? [];
  };

  const getSourceIngestMessages = (row: TransferRouteViewDTO | null) => {
    if (!row?.sourceId) {
      return [];
    }
    return sourceIngestMessages[row.sourceId] ?? [];
  };

  const getSourceIngestChainItems = (
    row: TransferRouteViewDTO | null,
  ): RouteFlowChainNode[] => {
    const sourceState = getSourceIngestState(row);
    const sourceTone = normalizeIngestStatusWord(
      sourceState?.ingestStatus,
      sourceState?.ingestBusy,
    );
    const ruleLabel = getRuleDisplayName(row?.ruleId);
    const progressText = getSourceIngestProgressText(row);
    const routeIssue = row?.routeId ? routeExecutionStates[row.routeId] ?? null : null;
    const routeIssueText = routeIssue
      ? [
          routeIssue.runStage ? `阶段 ${routeIssue.runStage}` : "",
          routeIssue.runStatus ? `状态 ${routeIssue.runStatus}` : "",
          routeIssue.errorMessage || routeIssue.logMessage || "",
        ]
          .filter(Boolean)
          .join("，")
      : "";
    const routeTone =
      sourceTone === "error"
        ? "error"
        : sourceTone === "loading"
          ? "loading"
          : routeIssue
            ? "error"
            : row?.ruleId
              ? "success"
              : "pending";
    const targetTone =
      sourceTone === "error"
        ? "error"
        : sourceTone === "loading"
          ? "loading"
          : routeIssue?.runStage === "DELIVER"
            ? "error"
            : row?.targetCode
              ? "success"
              : "pending";

    return [
      {
        key: "source-status",
        title: "当前收取",
        statusKey: sourceTone,
        statusLabel: sourceTone,
        content: [
          getTriggerSummaryText(sourceState)
            ? `本轮${getTriggerSummaryText(sourceState)}`
            : "暂无开始时间",
          progressText !== "暂无进度" ? progressText : "",
          sourceState?.statusMessage ? sourceState.statusMessage : "",
        ]
          .filter(Boolean)
          .join(" · "),
        timeText: sourceState?.ingestStartedAt
          ? formatTriggerTime(sourceState.ingestStartedAt)
          : getCurrentTimeText(),
      },
      {
        key: "route-match",
        title: "路由匹配",
        statusKey: routeTone,
        statusLabel: routeTone,
        content: [
          row?.ruleId
            ? `规则 ${ruleLabel} 已完成匹配，重命名模板 ${row.renamePattern || "默认命名"}。`
            : "路由规则待运行。",
          routeIssueText ? `最近异常 ${routeIssueText}` : "",
        ]
          .filter(Boolean)
          .join(" "),
        timeText: getCurrentTimeText(),
      },
      {
        key: "target-delivery",
        title: "目标投递",
        statusKey: targetTone,
        statusLabel: targetTone,
        content: [
          row?.targetCode
            ? `目标 ${row.targetCode} 正在等待投递，目标路径 ${row.targetPath || "待配置"}。`
            : "目标待运行。",
          routeIssue?.runStage === "DELIVER" && routeIssueText
            ? `投递异常 ${routeIssueText}`
            : "",
        ]
          .filter(Boolean)
          .join(" "),
        timeText: getCurrentTimeText(),
      },
    ];
  };

  const isSourceTriggering = (sourceId?: string) => {
    if (!sourceId) {
      return false;
    }
    return Boolean(sourceActionLoadingIds[`trigger:${sourceId}`]);
  };

  const isSourceStopping = (sourceId?: string) => {
    if (!sourceId) {
      return false;
    }
    return Boolean(sourceActionLoadingIds[`stop:${sourceId}`]);
  };

  const loadSourceDetailByRoute = async (row: TransferRouteViewDTO) => {
    if (!row.sourceId) {
      throw new Error("来源主键缺失，无法执行收取操作");
    }

    return unwrapSingleResult(await api.getSource(row.sourceId));
  };

  const triggerSource = async (row: TransferRouteViewDTO) => {
    const sourceId = row.sourceId;
    if (!sourceId) {
      message.error("来源主键缺失，无法收取");
      return;
    }
    if (!canTriggerSource(row)) {
      message.warning("当前收取状态未停止，无法手动收取");
      return;
    }

    const loadingKey = `trigger:${sourceId}`;
    if (sourceActionLoadingIds[loadingKey]) {
      message.warning("该来源正在收取中，请等待当前操作完成后再触发");
      return;
    }

    try {
      const source = await loadSourceDetailByRoute(row);
      if (!source) {
        message.error("加载来源详情失败");
        return;
      }
      if (source.ingestBusy) {
        message.warning("该来源正在收取中，请等待当前收取完成后再触发");
        return;
      }

      sourceActionLoadingIds[loadingKey] = true;
      if (row.sourceId) {
        upsertSourceIngestState(row.sourceId, {
          ...sourceIngestStates[row.sourceId],
          ingestBusy: true,
          ingestStartedAt: getCurrentTimeText(),
          ingestTriggerType: "MANUAL",
          ingestStatus: "RUNNING",
          progressPercent: sourceIngestStates[row.sourceId]?.progressPercent ?? 0,
          statusMessage: "正在发起收取请求",
        });
      }
      await api.triggerSource(sourceId);
      message.success("已触发来源收取");
      await loadList();
    } catch (error) {
      console.error("触发来源收取失败:", error);
      message.error(
        error instanceof Error ? error.message : "触发来源收取失败",
      );
      await loadList();
    } finally {
      delete sourceActionLoadingIds[loadingKey];
    }
  };

  const stopSource = async (row: TransferRouteViewDTO) => {
    const sourceId = row.sourceId;
    if (!sourceId) {
      message.error("来源主键缺失，无法停止");
      return;
    }
    if (!canStopSource(row)) {
      message.warning("当前没有运行中的收取任务，无法停止");
      return;
    }

    const loadingKey = `stop:${sourceId}`;
    if (sourceActionLoadingIds[loadingKey]) {
      message.warning("该来源正在提交停止请求，请勿重复操作");
      return;
    }

    try {
      const source = await loadSourceDetailByRoute(row);
      if (!source) {
        message.error("加载来源详情失败");
        return;
      }
      if (!source.ingestBusy) {
        message.warning("该来源当前未在收取中");
        return;
      }

      Modal.confirm({
        title: "停止收取",
        content: `确认停止来源「${row.sourceCode || row.sourceId}」当前正在进行的收取吗？停止后本轮收取会在下一个检查点退出。`,
        okText: "停止",
        okButtonProps: { danger: true },
        cancelText: "取消",
        onOk: async () => {
          sourceActionLoadingIds[loadingKey] = true;
          try {
            if (row.sourceId) {
              upsertSourceIngestState(row.sourceId, {
                ...sourceIngestStates[row.sourceId],
                ingestBusy: true,
                ingestStatus: "STOPPING",
                progressPercent: sourceIngestStates[row.sourceId]?.progressPercent ?? 0,
                statusMessage: "正在提交停止请求",
              });
            }
            await api.stopSource(sourceId);
            message.success("已提交停止请求");
            await loadList();
          } catch (error) {
            console.error("停止来源收取失败:", error);
            message.error(
              error instanceof Error ? error.message : "停止来源收取失败",
            );
            await loadList();
          } finally {
            delete sourceActionLoadingIds[loadingKey];
          }
        },
      });
    } catch (error) {
      console.error("停止来源收取失败:", error);
      message.error(
        error instanceof Error ? error.message : "停止来源收取失败",
      );
      await loadList();
    }
  };

  const handleSourceChange = (value: unknown) => {
    const nextValue = String(value ?? "");
    if (!nextValue) {
      formState.sourceId = undefined;
      formState.sourceCode = "";
      formState.sourceType = "";
      return;
    }

    const source = sourceRows.value.find(
      (item) => item.sourceCode === nextValue,
    );
    formState.sourceId = source?.sourceId;
    formState.sourceCode = source?.sourceCode ?? nextValue;
    formState.sourceType = source?.sourceType ?? "";
  };

  const handleTargetChange = (value: unknown) => {
    const nextValue = String(value ?? "");
    if (!nextValue) {
      formState.targetCode = "";
      formState.targetType = "";
      formState.targetPath = "";
      return;
    }

    const target = targetRows.value.find(
      (item) => item.targetCode === nextValue,
    );
    formState.targetCode = target?.targetCode ?? nextValue;
    formState.targetType = target?.targetType ?? "";
    formState.targetPath = target?.targetPathTemplate ?? "";
  };

  const handleRuleChange = (value: unknown) => {
    const nextValue = String(value ?? "").trim();
    if (!nextValue) {
      formState.ruleId = undefined;
      return;
    }

    const rule = ruleRows.value.find((item) => item.ruleId === nextValue);
    formState.ruleId = rule?.ruleId ?? nextValue;
  };

  const buildPayload = (): TransferRouteUpsertCommand => {
    const sourceCode = String(formState.sourceCode ?? "").trim();
    const sourceType = String(formState.sourceType ?? "").trim();
    const targetCode = String(formState.targetCode ?? "").trim();
    const targetType = String(formState.targetType ?? "").trim();
    const targetPath = String(formState.targetPath ?? "").trim();
    const renamePattern = String(formState.renamePattern ?? "").trim();
    const ruleId = String(formState.ruleId ?? "").trim();

    if (!sourceCode) {
      throw new Error("请选择来源规则");
    }
    if (!sourceType) {
      throw new Error("来源类型不能为空");
    }
    if (!targetCode) {
      throw new Error("请选择目标规则");
    }
    if (!targetType) {
      throw new Error("目标类型不能为空");
    }
    if (!targetPath) {
      throw new Error("目标路径不能为空");
    }
    if (!ruleId) {
      throw new Error("请选择分拣规则");
    }

    return {
      routeId: formState.routeId,
      sourceId: formState.sourceId,
      sourceCode,
      sourceType,
      targetCode,
      targetType,
      targetPath,
      ruleId,
      renamePattern: renamePattern || undefined,
    };
  };

  const submitForm = async () => {
    formSubmitting.value = true;
    try {
      if (formRef.value) {
        try {
          await formRef.value.validate();
        } catch {
          return;
        }
      }
      const payload = buildPayload();

      if (formMode.value === "create") {
        await api.createRoute(payload);
        message.success("新建路由配置成功");
      } else if (formState.routeId) {
        await api.updateRoute(formState.routeId, payload);
        message.success("更新路由配置成功");
      } else {
        message.error("路由主键缺失，无法更新");
        return;
      }

      formVisible.value = false;
      await loadList();
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
        return;
      }
      console.error("保存路由配置失败:", error);
      message.error("保存路由配置失败");
    } finally {
      formSubmitting.value = false;
    }
  };

  const runQuery = async () => {
    pagination.value.current = 1;
    await loadList();
  };

  const resetQuery = async () => {
    Object.assign(query, defaultQuery());
    pagination.value.current = 1;
    await loadList();
  };

  const handlePageChange = ({
    current,
    pageSize: nextPageSize,
  }: {
    current: number;
    pageSize: number;
  }) => {
    pagination.value.current = current;
    pagination.value.pageSize = nextPageSize;
  };

  const formatRouteMetaSummary = (row: TransferRouteViewDTO | null) =>
    formatRouteMetaCount(row);

  const copyRouteField = async (
    value: string | number | undefined | null,
    label: string,
  ) => {
    const text =
      value === null || value === undefined ? "" : String(value).trim();
    if (!text) {
      message.warning(`${label} 为空，无法复制`);
      return;
    }

    const copied = await copyToClipboard(text);
    if (copied) {
      message.success(`${label} 已复制`);
      return;
    }

    message.error(`${label} 复制失败`);
  };

  const flowStats = computed<RouteFlowStats>(() => {
    const sourceSet = new Set<string>();
    const targetSet = new Set<string>();
    let routedCount = 0;
    let pendingCount = 0;

    rows.value.forEach((row) => {
      if (row.sourceCode) {
        sourceSet.add(row.sourceCode);
      }
      if (row.targetCode) {
        targetSet.add(row.targetCode);
      }
      routedCount += 1;
    });

    return {
      sourceCount: sourceSet.size,
      targetCount: targetSet.size,
      routedCount,
      pendingCount,
    };
  });

  const flowPreview = computed(() =>
    buildFlowPreview(
      selectedRow.value ?? rows.value[0] ?? null,
      getRuleDisplayName(selectedRow.value?.ruleId ?? rows.value[0]?.ruleId),
      selectedRow.value?.sourceId
        ? sourceIngestStates[selectedRow.value.sourceId] ?? null
        : rows.value[0]?.sourceId
          ? sourceIngestStates[rows.value[0].sourceId] ?? null
          : null,
    ),
  );

  const flowAnchor = computed(() =>
    buildFlowAnchor(
      selectedRow.value,
      rows.value,
      getRuleDisplayName(selectedRow.value?.ruleId ?? rows.value[0]?.ruleId),
    ),
  );

  const flowTableLegend = computed(() => [
    {
      label: "来源",
      hint: "来源编码 / 来源类型",
      tone: "source",
    },
    {
      label: "路由 + 规则",
      hint: "规则名称 / 模板 / 扩展信息",
      tone: "route",
    },
    {
      label: "目标",
      hint: "目标编码 / 目标类型 / 路径",
      tone: "target",
    },
  ]);

  void loadSelectRows();
  void loadList();

  const page = reactive({
    loading: listLoading,
    rows,
    flowPreview,
    flowAnchor,
    flowTableLegend,
    flowStats,
    tableData: computed(() => {
      return visibleRows.value;
    }),
    total,
    pageSize: computed(() => query.limit),
    pagination,
    query,
    sourceOptions,
    targetOptions,
    ruleOptions,
    sourceTypeOptions,
    targetTypeOptions,
    selectLoading,
    sourceActionLoadingIds,
    sourceIngestStates,
    routeFlowFactMessages,
    sourceIngestMessages,
    formVisible,
    formMode,
    formState,
    detailVisible,
    selectedRow,
    formSubmitting,
    handlePageChange,
    openCreateDialog,
    openEditDialog,
    openDetailDrawer,
    confirmDelete,
    getSourceIngestState,
    getSourceIngestStatusText,
    canTriggerSource,
    canStopSource,
    getSourceIngestProgressPercent,
    getSourceIngestProgressText,
    getRouteFlowFactMessages,
    getSourceIngestMessages,
    getSourceIngestChainItems,
    getRuleDisplayName,
    triggerSource,
    stopSource,
    isSourceTriggering,
    isSourceStopping,
    runQuery,
    resetQuery,
    submitForm,
    closeForm: () => {
      formVisible.value = false;
      resetForm();
    },
    closeDetail: () => {
      detailVisible.value = false;
    },
    handleSourceChange,
    handleTargetChange,
    handleRuleChange,
    setFormRef: (instance: unknown) => {
      formRef.value = instance as { validate: () => Promise<void> } | null;
    },
    copyRouteField,
    formatRouteMetaSummary,
  }) as RouteConfigPage;

  return {
    page,
  };
};
