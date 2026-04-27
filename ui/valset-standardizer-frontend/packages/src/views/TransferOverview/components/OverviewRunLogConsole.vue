<script setup lang="ts">
import {
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { YButton, YCard, YMonaco } from "@yss-ui/components";
import {
  ArrowDownOutlined,
  ClearOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
} from "@ant-design/icons-vue";
import type { TransferRunLogViewDTO } from "@/api/generated/valset/schemas";

type RunLogConsoleSeedItem = {
  key: string;
  title: string;
  stageLabel: string;
  statusLabel: string;
  createdAt?: string;
  description: string;
};

type RunLogConsoleStreamItem = TransferRunLogViewDTO & {
  seedTitle?: string;
  seedStageLabel?: string;
  seedStatusLabel?: string;
  seedDescription?: string;
};

type StageFilterValue = "ALL" | "RECEIVE" | "INGEST" | "ROUTE" | "DELIVER";

const props = withDefaults(
  defineProps<{
    items: RunLogConsoleSeedItem[];
    height?: number;
    autoStart?: boolean;
    baseUrl?: string;
    streamUrl?: string;
    streamLimit?: number;
  }>(),
  {
    height: 486,
    autoStart: true,
    baseUrl: import.meta.env.VITE_API_BASE_URL || "/api",
    streamUrl: "/transfer-run-logs/stream",
    streamLimit: 2000,
  },
);

const monacoRef = ref<any>(null);
const isStreaming = ref(false);
const logCount = ref(0);
const totalLines = ref(0);
const eventSourceRef = ref<EventSource | null>(null);
const seenLogIds = new Set<string>();
const editorHeight = "100%";
const stageFilter = ref<StageFilterValue>("ALL");
const lastSeedSignature = ref("");

const formatTimestamp = (value?: string) => {
  if (!value) {
    return new Date().toISOString().slice(0, 19).replace("T", " ");
  }
  return value.replace("T", " ").slice(0, 19);
};

const stageFilterOptions: Array<{ label: string; value: StageFilterValue }> = [
  { label: "全部", value: "ALL" },
  { label: "来源收取", value: "RECEIVE" },
  { label: "规则识别", value: "INGEST" },
  { label: "路由分发", value: "ROUTE" },
  { label: "目标投递", value: "DELIVER" },
];

const resolveKey = (item: RunLogConsoleStreamItem) => {
  return (
    item.runLogId ||
    `${item.createdAt || ""}-${item.sourceId || ""}-${item.transferId || ""}-${item.routeId || ""}-${item.runStage || ""}-${item.runStatus || ""}`
  );
};

const normalizeStageFilter = (value?: string) => {
  const normalized = String(value ?? "")
    .trim()
    .toUpperCase();
  if (
    normalized === "RECEIVE" ||
    normalized === "INGEST" ||
    normalized === "ROUTE" ||
    normalized === "DELIVER"
  ) {
    return normalized as StageFilterValue;
  }
  return "ALL";
};

const matchesStageFilter = (item: RunLogConsoleStreamItem) => {
  if (stageFilter.value === "ALL") {
    return true;
  }
  return normalizeStageFilter(item.runStage) === stageFilter.value;
};

const buildLogLine = (item: RunLogConsoleStreamItem) => {
  const timestamp = formatTimestamp(item.createdAt);
  const description =
    item.errorMessage || item.logMessage || item.seedDescription || "暂无说明";

  return `[${timestamp}] ${description}`;
};

const appendLogs = async (items: RunLogConsoleStreamItem[]) => {
  await nextTick();
  if (!monacoRef.value) {
    return;
  }

  if (!items.length) {
    totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
    return;
  }

  const lines: string[] = [];
  for (const item of items) {
    if (!matchesStageFilter(item)) {
      continue;
    }
    const key = resolveKey(item);
    if (seenLogIds.has(key)) {
      continue;
    }
    seenLogIds.add(key);
    lines.push(buildLogLine(item));
  }

  if (!lines.length) {
    totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
    return;
  }

  logCount.value += lines.length;
  monacoRef.value.appendContent(lines.join("\n"));
  totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
};

const seedInitialLogs = async () => {
  if (!props.items.length || !monacoRef.value) {
    return;
  }
  const signature = `${stageFilter.value}::${props.items
    .map((item) =>
      [
        item.key,
        item.createdAt ?? "",
        item.stageLabel,
        item.statusLabel,
        item.description,
      ].join("@"),
    )
    .join("|")}`;
  if (lastSeedSignature.value === signature) {
    return;
  }
  lastSeedSignature.value = signature;
  await appendLogs(
    props.items.map((item) => ({
      runLogId: item.key,
      createdAt: item.createdAt,
      seedTitle: item.title,
      seedStageLabel: item.stageLabel,
      seedStatusLabel: item.statusLabel,
      seedDescription: item.description,
    })),
  );
};

const buildStreamUrl = () => {
  const baseUrl = props.baseUrl.endsWith("/")
    ? props.baseUrl.slice(0, -1)
    : props.baseUrl;
  const url = new URL(`${baseUrl}${props.streamUrl}`, window.location.origin);
  url.searchParams.set("limit", String(props.streamLimit));
  if (stageFilter.value !== "ALL") {
    url.searchParams.set("runStage", stageFilter.value);
  }
  return `${url.pathname}${url.search}${url.hash}`;
};

const parsePayload = (event: MessageEvent): RunLogConsoleStreamItem[] => {
  try {
    const parsed = JSON.parse(String(event.data || ""));
    const data = parsed?.data || parsed?.item || parsed;
    if (!data) {
      return [];
    }
    return Array.isArray(data) ? data : [data];
  } catch {
    return [];
  }
};

const handleStreamMessage = async (event: MessageEvent) => {
  const items = parsePayload(event);
  if (!items.length) {
    return;
  }
  await appendLogs(items);
};

const resetContent = async () => {
  await nextTick();
  if (!monacoRef.value) {
    return;
  }

  seenLogIds.clear();
  monacoRef.value.clearContent?.();
  logCount.value = 0;
  totalLines.value = 0;
};

const restartStream = () => {
  stopLogStream();
  void resetContent().then(() => {
    lastSeedSignature.value = "";
    void seedInitialLogs().then(() => {
      if (props.autoStart) {
        startLogStream();
      }
    });
  });
};

const startLogStream = () => {
  if (
    eventSourceRef.value &&
    eventSourceRef.value.readyState !== EventSource.CLOSED
  ) {
    isStreaming.value = true;
    return;
  }

  isStreaming.value = true;
  const eventSource = new EventSource(buildStreamUrl());
  eventSourceRef.value = eventSource;

  eventSource.onopen = () => {
    isStreaming.value = true;
  };

  eventSource.onmessage = (event) => {
    void handleStreamMessage(event);
  };

  eventSource.onerror = () => {
    if (eventSource.readyState === EventSource.CLOSED) {
      isStreaming.value = false;
    }
  };
};

const stopLogStream = () => {
  eventSourceRef.value?.close();
  eventSourceRef.value = null;
  isStreaming.value = false;
};

const clearLogs = () => {
  void resetContent();
};

const scrollToBottom = () => {
  monacoRef.value?.scrollToBottom?.();
};

const handleLineExceed = (lines: number) => {
  console.log(`运行日志行数超出限制：${lines} 行，已自动清理`);
};

const updateStageFilter = (value: StageFilterValue) => {
  if (stageFilter.value === value) {
    return;
  }
  stageFilter.value = value;
  lastSeedSignature.value = "";
  restartStream();
};

watch(
  () => props.items,
  () => {
    void seedInitialLogs();
  },
  {
    deep: true,
    immediate: true,
  },
);

onMounted(() => {
  void seedInitialLogs().then(() => {
    if (props.autoStart) {
      startLogStream();
    }
  });
});

onBeforeUnmount(() => {
  stopLogStream();
});
</script>

<template>
  <YCard
    class="overview-run-log-panel overview-run-log-console"
    :bordered="false"
    :padding="0"
  >
    <div class="overview-run-log-console-head">
      <div class="overview-run-log-console-title">
        <h3>运行日志</h3>
        <p>来源、路由、目标的实时运行信息。</p>
      </div>
      <div class="overview-run-log-console-actions">
        <a-tag color="blue">日志 {{ logCount }}</a-tag>
        <a-tag color="green">行数 {{ totalLines }}</a-tag>
        <a-tag v-if="isStreaming" color="orange">
          <span class="blinking-dot">●</span>
          实时流式
        </a-tag>
      </div>
    </div>

    <div class="overview-run-log-console-toolbar">
      <a-space wrap>
        <a-tag
          v-for="item in stageFilterOptions"
          :key="item.value"
          :color="stageFilter === item.value ? 'blue' : 'default'"
          class="stage-filter-tag"
          @click="updateStageFilter(item.value)"
        >
          {{ item.label }}
        </a-tag>
        <YButton
          v-if="!isStreaming"
          type="primary"
          size="small"
          @click="startLogStream"
        >
          <template #icon><PlayCircleOutlined /></template>
          开始实时日志
        </YButton>
        <YButton v-else danger size="small" @click="stopLogStream">
          <template #icon><PauseCircleOutlined /></template>
          停止日志流
        </YButton>
        <YButton size="small" @click="clearLogs">
          <template #icon><ClearOutlined /></template>
          清空日志
        </YButton>
        <YButton size="small" @click="scrollToBottom">
          <template #icon><ArrowDownOutlined /></template>
          滚动到底部
        </YButton>
      </a-space>
    </div>

    <div class="overview-run-log-console-editor">
      <YMonaco
        ref="monacoRef"
        :model-value="''"
        :log-mode="true"
        :max-lines="2000"
        :height="editorHeight"
        :scroll-threshold="10"
        :auto-scroll="true"
        language="shell"
        :readonly="true"
        :options="{
          fontSize: 13,
          lineNumbers: 'on',
          scrollBeyondLastLine: false,
          minimap: { enabled: false },
          wordWrap: 'off',
        }"
        @line-exceed="handleLineExceed"
      />
    </div>
  </YCard>
</template>

<style scoped lang="less">
.overview-run-log-console {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  min-height: 0;
}

.overview-run-log-console-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.overview-run-log-console-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
}

.overview-run-log-console-title p {
  margin: 4px 0 0;
  color: rgba(15, 23, 42, 0.58);
  font-size: 13px;
  line-height: 1.45;
}

.overview-run-log-console-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  padding-top: 1px;
}

.overview-run-log-console-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.stage-filter-tag {
  cursor: pointer;
  user-select: none;
}

.overview-run-log-console-editor {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.blinking-dot {
  animation: blink 1.5s infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.3;
  }
}
</style>
