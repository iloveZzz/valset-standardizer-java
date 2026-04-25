<script setup lang="ts">
import {
  computed,
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

type RunLogConsoleItem = {
  key: string;
  title: string;
  stageLabel: string;
  statusLabel: string;
  statusColor: string;
  createdAt?: string;
  description: string;
};

const props = withDefaults(
  defineProps<{
    items: RunLogConsoleItem[];
    height?: number;
    autoStart?: boolean;
  }>(),
  {
    height: 486,
    autoStart: true,
  },
);

const monacoRef = ref<any>(null);
const isStreaming = ref(true);
const logCount = ref(0);
const totalLines = ref(0);
let streamTimer: number | null = null;
let batchCounter = 1;

const LOG_LEVELS = ["INFO", "WARN", "ERROR", "DEBUG"];
const consoleSeed = ref("");

const seedLineCount = computed(() =>
  consoleSeed.value
    ? consoleSeed.value
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean).length
    : 0,
);

const generateLogLine = (): string => {
  const timestamp = new Date().toISOString();
  const level = LOG_LEVELS[Math.floor(Math.random() * LOG_LEVELS.length)];
  const messages = [
    "来源收取成功，等待路由判定",
    "来源附件解析完成，进入规则匹配",
    "路由命中目标 财务中心，准备投递",
    "路由命中目标 业务归档，准备投递",
    "目标投递成功，结果已写入日志",
    "目标投递失败，等待下一次重试",
    "来源线程完成增量扫描",
    "路由结果已刷新，目标状态同步完成",
  ];
  const stageLabels = ["收取", "识别", "路由", "投递"];
  const message = messages[Math.floor(Math.random() * messages.length)];
  const stage = stageLabels[Math.floor(Math.random() * stageLabels.length)];
  const threadId = Math.floor(Math.random() * 100);

  return `[${timestamp}] [${level}] [${stage}] [Thread-${threadId}] ${message}`;
};

const buildSeedContent = () =>
  props.items
    .map((item) => {
      const timestamp = item.createdAt || new Date().toISOString();
      const description = item.description ? ` - ${item.description}` : "";
      const sourceTitle = item.title || "来源";
      const stageLabel = item.stageLabel || "路由";
      const statusLabel = item.statusLabel || "运行中";
      return `[${timestamp}] [${statusLabel}] [${stageLabel}] ${sourceTitle}${description}`;
    })
    .join("\n");

const syncContent = async () => {
  await nextTick();
  const content = buildSeedContent();
  consoleSeed.value = content;

  if (!monacoRef.value) {
    totalLines.value = seedLineCount.value;
    return;
  }

  monacoRef.value.clearContent?.();
  if (content) {
    monacoRef.value.appendContent(content);
  }
  totalLines.value = monacoRef.value.getLineCount?.() ?? seedLineCount.value;
  logCount.value = seedLineCount.value;
};

function appendGeneratedLogs(count: number) {
  if (!monacoRef.value) {
    return;
  }

  const lines: string[] = [];
  for (let index = 0; index < count; index += 1) {
    lines.push(generateLogLine());
    logCount.value += 1;
  }

  monacoRef.value.appendContent(lines.join("\n"));
  totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
}

const startLogStream = () => {
  if (isStreaming.value) {
    return;
  }

  isStreaming.value = true;
  logCount.value = seedLineCount.value;
  appendGeneratedLogs(8);

  streamTimer = window.setInterval(() => {
    if (!monacoRef.value) {
      return;
    }

    appendGeneratedLogs(Math.floor(Math.random() * 6) + 5);
  }, 500);
};

const stopLogStream = () => {
  if (streamTimer) {
    clearInterval(streamTimer);
    streamTimer = null;
  }
  isStreaming.value = false;
};

const clearLogs = () => {
  stopLogStream();
  monacoRef.value?.clearContent?.();
  totalLines.value = 0;
  logCount.value = 0;
  batchCounter = 1;
};

const scrollToBottom = () => {
  monacoRef.value?.scrollToBottom?.();
};

const handleScrollEnd = () => {
  if (!monacoRef.value || !isStreaming.value) {
    return;
  }

  const moreLogs = Array.from({ length: 20 }, () => generateLogLine());
  logCount.value += moreLogs.length;
  monacoRef.value.appendContent(
    `\n--- 批次 ${batchCounter++} ---\n${moreLogs.join("\n")}`,
  );
  totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
};

const handleLineExceed = (lines: number) => {
  console.log(`运行日志行数超出限制：${lines} 行，已自动清理`);
};

watch(
  () => props.items,
  () => {
    void syncContent();
  },
  {
    deep: true,
    immediate: true,
  },
);

onMounted(() => {
  void syncContent().then(() => {
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
        <YButton
          v-if="!isStreaming"
          type="primary"
          size="small"
          @click="startLogStream"
        >
          <template #icon><PlayCircleOutlined /></template>
          开始日志流
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
        height="100%"
        :scroll-threshold="10"
        :auto-scroll="true"
        language="shell"
        :height="props.height"
        :readonly="true"
        :options="{
          fontSize: 13,
          lineNumbers: 'on',
          scrollBeyondLastLine: false,
          minimap: { enabled: false },
          wordWrap: 'off',
        }"
        @scroll-end="handleScrollEnd"
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
  margin-bottom: 18px;
}

.overview-run-log-console-editor {
  flex: 1;
  min-height: 0;
  overflow: hidden;
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
