import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import {
  subscribeParseLifecycleEventStream,
  type ParseLifecycleEventDTO,
} from "@/services/parseLifecycleEventSse";
import { formatDateTime } from "@/utils/format";
import { useRoute, useRouter } from "vue-router";
import type {
  ParseLifecycleEventRow,
  ParseLifecyclePage,
  ParseLifecycleQueryState,
  ParseLifecycleStatus,
  ParseLifecycleTaskRow,
} from "../types";

const MAX_RAW_EVENTS = 500;

const defaultQuery = (): ParseLifecycleQueryState => ({
  source: "",
  queueId: "",
  transferId: "",
  taskId: "",
  stage: "",
  onlyError: false,
  keyword: "",
});

const stageLabelMap: Record<string, string> = {
  CYCLE_STARTED: "循环开始",
  CYCLE_FINISHED: "循环结束",
  BATCH_STARTED: "批次开始",
  BATCH_EMPTY: "批次空闲",
  BATCH_FINISHED: "批次完成",
  QUEUE_DISCOVERED: "发现队列",
  QUEUE_GENERATED: "生成队列",
  QUEUE_BACKFILLED: "补漏队列",
  QUEUE_REUSED: "复用队列",
  QUEUE_UPDATED: "更新队列",
  QUEUE_RETRIED: "重试队列",
  QUEUE_SUBSCRIBE_ATTEMPTED: "尝试订阅",
  QUEUE_SUBSCRIBED: "已订阅",
  QUEUE_SUBSCRIBE_CONFLICT: "订阅冲突",
  QUEUE_SUBSCRIBE_SKIPPED: "订阅跳过",
  QUEUE_FILE_INFO_REPAIR_STARTED: "修复开始",
  QUEUE_FILE_INFO_REPAIR_COMPLETED: "修复完成",
  QUEUE_FILE_INFO_REPAIR_FAILED: "修复失败",
  TASK_REQUEST_BUILT: "构建任务参数",
  TASK_CREATED: "创建任务",
  TASK_REUSED: "复用任务",
  TASK_DISPATCHED: "派发任务",
  TASK_EXECUTION_STARTED: "执行任务",
  TASK_RAW_PARSED: "原始解析",
  TASK_STANDARDIZED: "执行标准化",
  TASK_PERSISTED: "结果落库",
  TASK_SUCCEEDED: "任务成功",
  TASK_FAILED: "任务失败",
  QUEUE_COMPLETED: "队列完成",
  QUEUE_FAILED: "队列失败",
  QUEUE_SKIPPED: "队列跳过",
};

const successStages = new Set(["TASK_SUCCEEDED", "QUEUE_COMPLETED"]);
const failedStages = new Set([
  "TASK_FAILED",
  "QUEUE_FAILED",
  "QUEUE_FILE_INFO_REPAIR_FAILED",
  "QUEUE_SUBSCRIBE_CONFLICT",
]);
const observerStages = new Set([
  "CYCLE_STARTED",
  "CYCLE_FINISHED",
  "BATCH_STARTED",
  "BATCH_EMPTY",
  "BATCH_FINISHED",
]);

const statusLabelMap: Record<ParseLifecycleStatus, string> = {
  RUNNING: "处理中",
  SUCCESS: "已完成",
  FAILED: "异常",
  IDLE: "空闲",
};

const statusColorMap: Record<ParseLifecycleStatus, string> = {
  RUNNING: "processing",
  SUCCESS: "success",
  FAILED: "error",
  IDLE: "default",
};

const formatStage = (value?: string) => {
  if (!value) {
    return "-";
  }
  return stageLabelMap[value] ?? value;
};

const stringifyAttributes = (value: unknown) => {
  if (value == null) {
    return "-";
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
};

const getAttributeText = (
  attributes: Record<string, unknown> | undefined,
  keys: string[],
) => {
  if (!attributes) {
    return "";
  }
  for (const key of keys) {
    const value = attributes[key];
    if (value != null && String(value).trim()) {
      return String(value);
    }
  }
  return "";
};

const buildIdentity = (row: ParseLifecycleEventDTO) => {
  return [
    row.eventId || "-",
    row.stage || "-",
    row.queueId || "-",
    row.taskId == null ? "-" : String(row.taskId),
    row.transferId || "-",
  ].join("::");
};

const buildEventKey = (row: ParseLifecycleEventDTO) => {
  return [
    row.source || "-",
    row.stage || "-",
    row.queueId || "-",
    row.taskId == null ? "-" : String(row.taskId),
    row.transferId || "-",
    row.message || "-",
    row.errorMessage || "-",
  ].join("::");
};

const resolveSeverity = (row: ParseLifecycleEventDTO): ParseLifecycleStatus => {
  const stage = String(row.stage || "");
  if (row.errorMessage || failedStages.has(stage) || stage.includes("FAILED")) {
    return "FAILED";
  }
  if (successStages.has(stage) || stage.includes("SUCCEEDED")) {
    return "SUCCESS";
  }
  if (stage === "BATCH_EMPTY" || stage === "CYCLE_FINISHED") {
    return "IDLE";
  }
  return "RUNNING";
};

const isObserverEvent = (row: ParseLifecycleEventDTO) => {
  const stage = String(row.stage || "");
  return (
    observerStages.has(stage) &&
    !row.queueId &&
    !row.transferId &&
    row.taskId == null &&
    !row.businessKey
  );
};

const mapRow = (row: ParseLifecycleEventDTO): ParseLifecycleEventRow => {
  const displayTime = formatDateTime(row.occurredAt);
  return {
    ...row,
    displayTime,
    stageLabel: formatStage(row.stage),
    attributesText: stringifyAttributes(row.attributes),
    identity: buildIdentity(row),
    eventKey: buildEventKey(row),
    repeatCount: 1,
    firstSeenAt: displayTime,
    lastSeenAt: displayTime,
    severity: resolveSeverity(row),
    observerEvent: isObserverEvent(row),
  };
};

const normalizeTaskId = (value: string) => {
  if (!value.trim()) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const normalizeQueryValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return String(value[0] ?? "");
  }
  return String(value ?? "");
};

const buildTaskIdentity = (row: ParseLifecycleEventRow) => {
  if (row.queueId) {
    return `queue:${row.queueId}`;
  }
  if (row.transferId) {
    return `transfer:${row.transferId}`;
  }
  if (row.taskId != null) {
    return `task:${row.taskId}`;
  }
  if (row.businessKey) {
    return `business:${row.businessKey}`;
  }
  return "";
};

const calcDurationText = (events: ParseLifecycleEventRow[]) => {
  const timestamps = events
    .map((item) => (item.occurredAt ? new Date(item.occurredAt).getTime() : 0))
    .filter((item) => Number.isFinite(item) && item > 0);
  if (timestamps.length < 2) {
    return "-";
  }
  const seconds = Math.max(...timestamps) - Math.min(...timestamps);
  const totalSeconds = Math.max(0, Math.round(seconds / 1000));
  if (totalSeconds < 60) {
    return `${totalSeconds}s`;
  }
  return `${Math.floor(totalSeconds / 60)}m ${totalSeconds % 60}s`;
};

const buildTaskRows = (events: ParseLifecycleEventRow[]) => {
  const taskMap = new Map<string, ParseLifecycleEventRow[]>();
  events.forEach((event) => {
    const identity = buildTaskIdentity(event);
    if (!identity || event.observerEvent) {
      return;
    }
    taskMap.set(identity, [...(taskMap.get(identity) || []), event]);
  });

  return Array.from(taskMap.entries())
    .map(([identity, taskEvents]): ParseLifecycleTaskRow => {
      const ordered = [...taskEvents].sort(
        (first, second) =>
          new Date(second.occurredAt || 0).getTime() -
          new Date(first.occurredAt || 0).getTime(),
      );
      const latest = ordered[0];
      const first = ordered[ordered.length - 1];
      const failed = ordered.find((item) => item.severity === "FAILED");
      const success = ordered.find((item) => item.severity === "SUCCESS");
      const status: ParseLifecycleStatus = failed
        ? "FAILED"
        : success
          ? "SUCCESS"
          : latest.severity === "IDLE"
            ? "IDLE"
            : "RUNNING";
      const title =
        getAttributeText(latest.attributes, [
          "originalName",
          "fileName",
          "filename",
          "name",
        ]) ||
        latest.businessKey ||
        latest.queueId ||
        latest.transferId ||
        (latest.taskId == null ? "" : String(latest.taskId)) ||
        "未命名任务";

      return {
        identity,
        queueId: latest.queueId || "-",
        transferId: latest.transferId || "-",
        taskId: latest.taskId == null ? "-" : String(latest.taskId),
        businessKey: latest.businessKey || "-",
        title,
        currentStage: latest.stage || "-",
        currentStageLabel: latest.stageLabel,
        status,
        statusLabel: statusLabelMap[status],
        statusColor: statusColorMap[status],
        message: latest.message || latest.errorMessage || "-",
        errorMessage: failed?.errorMessage || latest.errorMessage || "",
        startedAt: first.displayTime,
        updatedAt: latest.displayTime,
        durationText: calcDurationText(ordered),
        eventCount: ordered.reduce(
          (count, item) => count + (item.repeatCount || 1),
          0,
        ),
        rawEvents: ordered,
      };
    })
    .sort(
      (first, second) =>
        new Date(second.rawEvents[0]?.occurredAt || 0).getTime() -
        new Date(first.rawEvents[0]?.occurredAt || 0).getTime(),
    );
};

export const useParseLifecyclePage = (): { page: ParseLifecyclePage } => {
  const route = useRoute();
  const router = useRouter();
  const query = reactive<ParseLifecycleQueryState>(defaultQuery());
  const rows = ref<ParseLifecycleEventRow[]>([]);
  const connected = ref(false);
  const connecting = ref(false);
  const detailVisible = ref(false);
  const rawDetailVisible = ref(false);
  const selectedRow = ref<ParseLifecycleTaskRow | null>(null);
  const selectedEvent = ref<ParseLifecycleEventRow | null>(null);
  const eventSourceRef = ref<EventSource | null>(null);
  const paused = ref(false);
  const showRawEvents = ref(false);
  const autoScroll = ref(true);

  const taskRows = computed(() => buildTaskRows(rows.value));
  const rawTotal = computed(() =>
    rows.value.reduce((count, item) => count + (item.repeatCount || 1), 0),
  );
  const total = computed(() => rawTotal.value);
  const taskTotal = computed(() => taskRows.value.length);
  const activeTaskCount = computed(
    () => taskRows.value.filter((item) => item.status === "RUNNING").length,
  );
  const successTaskCount = computed(
    () => taskRows.value.filter((item) => item.status === "SUCCESS").length,
  );
  const failedTaskCount = computed(
    () => taskRows.value.filter((item) => item.status === "FAILED").length,
  );
  const latestEvent = computed(() => rows.value[0]);
  const latestStage = computed(() => latestEvent.value?.stageLabel || "-");
  const latestMessage = computed(
    () =>
      latestEvent.value?.message || latestEvent.value?.errorMessage || "等待事件",
  );
  const observerStatus = computed(() => {
    const observerEvent = rows.value.find((item) => item.observerEvent);
    if (!observerEvent) {
      return connected.value ? "等待观察者事件" : "未连接";
    }
    if (observerEvent.stage === "BATCH_EMPTY") {
      return "空闲";
    }
    if (observerEvent.stage === "CYCLE_STARTED") {
      return "扫描中";
    }
    if (observerEvent.stage === "BATCH_STARTED") {
      return "处理批次";
    }
    if (observerEvent.stage === "CYCLE_FINISHED") {
      return "循环结束";
    }
    return observerEvent.stageLabel;
  });

  const tableData = computed(() => {
    const keyword = query.keyword.trim().toLowerCase();
    return taskRows.value.filter((item) => {
      if (query.onlyError && item.status !== "FAILED") {
        return false;
      }
      if (!keyword) {
        return true;
      }
      return [
        item.queueId,
        item.transferId,
        item.taskId,
        item.businessKey,
        item.title,
        item.message,
        item.errorMessage,
      ]
        .join(" ")
        .toLowerCase()
        .includes(keyword);
    });
  });
  const rawTableData = computed(() => rows.value.slice(0, 200));

  const pushEvent = (event: ParseLifecycleEventDTO) => {
    if (paused.value) {
      return;
    }
    const nextRow = mapRow(event);
    const existsIndex = rows.value.findIndex(
      (item) => item.eventKey === nextRow.eventKey,
    );
    if (existsIndex >= 0) {
      const exists = rows.value[existsIndex];
      const merged = {
        ...nextRow,
        identity: exists.identity,
        repeatCount: exists.repeatCount + 1,
        firstSeenAt: exists.firstSeenAt,
        lastSeenAt: nextRow.displayTime,
      };
      rows.value = [
        merged,
        ...rows.value.slice(0, existsIndex),
        ...rows.value.slice(existsIndex + 1),
      ].slice(0, MAX_RAW_EVENTS);
      return;
    }
    rows.value = [nextRow, ...rows.value].slice(0, MAX_RAW_EVENTS);
  };

  const disconnect = () => {
    eventSourceRef.value?.close();
    eventSourceRef.value = null;
    connected.value = false;
    connecting.value = false;
  };

  const connect = () => {
    disconnect();
    connecting.value = true;
    const connection = subscribeParseLifecycleEventStream(
      {
        source: query.source || undefined,
        queueId: query.queueId || undefined,
        transferId: query.transferId || undefined,
        taskId: normalizeTaskId(query.taskId),
        stage: query.stage || undefined,
      },
      {
        onEvent: (event) => {
          pushEvent(event);
        },
        onOpen: () => {
          connected.value = true;
          connecting.value = false;
        },
        onClose: () => {
          connected.value = false;
          connecting.value = false;
        },
        onError: () => {
          connected.value = false;
        },
      },
    );
    eventSourceRef.value = connection.eventSource;
  };

  const clear = () => {
    rows.value = [];
    selectedRow.value = null;
    selectedEvent.value = null;
  };

  const togglePause = () => {
    paused.value = !paused.value;
  };

  const toggleRawEvents = () => {
    showRawEvents.value = !showRawEvents.value;
  };

  const toggleAutoScroll = () => {
    autoScroll.value = !autoScroll.value;
  };

  const openDetailDrawer = (row: ParseLifecycleTaskRow) => {
    selectedRow.value = row;
    detailVisible.value = true;
  };

  const openRawDetailDrawer = (row: ParseLifecycleEventRow) => {
    selectedEvent.value = row;
    rawDetailVisible.value = true;
  };

  const openQueuePage = () => {
    void router.push({
      path: "/transfer/parse-queue",
      query: {
        queueId: query.queueId || selectedRow.value?.queueId || undefined,
        transferId:
          query.transferId || selectedRow.value?.transferId || undefined,
      },
    });
  };

  const closeDetail = () => {
    detailVisible.value = false;
  };

  const closeRawDetail = () => {
    rawDetailVisible.value = false;
    selectedEvent.value = null;
  };

  const syncQueryFromRoute = () => {
    query.source = normalizeQueryValue(route.query.source);
    query.queueId = normalizeQueryValue(route.query.queueId);
    query.transferId = normalizeQueryValue(route.query.transferId);
    query.taskId = normalizeQueryValue(route.query.taskId);
    query.stage = normalizeQueryValue(route.query.stage);
  };

  watch(
    () => route.query,
    () => {
      syncQueryFromRoute();
      connect();
    },
    {
      deep: true,
      immediate: true,
    },
  );

  onBeforeUnmount(() => {
    disconnect();
  });

  const page = reactive({
    connected,
    connecting,
    total,
    rawTotal,
    taskTotal,
    activeTaskCount,
    successTaskCount,
    failedTaskCount,
    observerStatus,
    latestStage,
    latestMessage,
    query,
    rows,
    tableData,
    rawTableData,
    detailVisible,
    rawDetailVisible,
    selectedRow,
    selectedEvent,
    paused,
    showRawEvents,
    autoScroll,
    connect,
    disconnect,
    clear,
    togglePause,
    toggleRawEvents,
    toggleAutoScroll,
    openDetailDrawer,
    openRawDetailDrawer,
    openQueuePage,
    closeDetail,
    closeRawDetail,
    formatStage,
  });

  return { page };
};
