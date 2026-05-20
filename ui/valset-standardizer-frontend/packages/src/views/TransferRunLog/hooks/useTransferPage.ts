import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  cleanupSystemOutputLogs,
  listSystemOutputLogNodes,
  type SystemOutputLogNode,
} from "@/api";
import type {
  RunLogPage,
  SystemOutputLogNodeOption,
  SystemOutputLogStreamItem,
  SystemOutputLogStreamPayload,
} from "../types";

const STREAM_LIMIT = 10000;
const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
const STREAM_URL = "/system-output-logs/stream";

const normalizeBaseUrl = (baseUrl: string) =>
  baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;

const buildStreamUrl = (nodeId: string) => {
  const url = new URL(
    `${normalizeBaseUrl(DEFAULT_BASE_URL)}${STREAM_URL}`,
    window.location.origin,
  );
  url.searchParams.set("limit", String(STREAM_LIMIT));
  if (nodeId) {
    url.searchParams.set("nodeId", nodeId);
  }
  return `${url.pathname}${url.search}${url.hash}`;
};

const parsePayload = (event: MessageEvent): SystemOutputLogStreamItem[] => {
  try {
    const parsed = JSON.parse(String(event.data || "")) as SystemOutputLogStreamPayload | SystemOutputLogStreamItem;
    const data = "data" in parsed ? parsed.data : parsed;
    if (!data) {
      return [];
    }
    return Array.isArray(data) ? data : [data as SystemOutputLogStreamItem];
  } catch {
    return [];
  }
};

const resolveLogLine = (item: SystemOutputLogStreamItem) => {
  if (item.formattedLine) {
    return item.formattedLine;
  }
  const timestamp =
    item.timestamp || new Date().toISOString().slice(0, 19).replace("T", " ");
  const level = item.level || "INFO";
  const threadName = item.threadName || "-";
  const loggerName = item.loggerName || "-";
  const text = item.message || "";
  return `${timestamp} [${threadName}] ${level.padEnd(5, " ")} ${loggerName} - ${text}`;
};

export const useTransferPage = (): { page: RunLogPage } => {
  const monacoRef = ref<any>(null);
  const isStreaming = ref(false);
  const cleanupLoading = ref(false);
  const nodeLoading = ref(false);
  const logCount = ref(0);
  const totalLines = ref(0);
  const selectedNodeId = ref("");
  const nodeOptions = ref<SystemOutputLogNodeOption[]>([]);
  const eventSourceRef = ref<EventSource | null>(null);
  const seenSequences = new Set<number>();

  const setMonacoRef = (instance: any) => {
    monacoRef.value = instance;
  };

  const appendLogs = async (items: SystemOutputLogStreamItem[]) => {
    await nextTick();
    if (!items.length || !monacoRef.value) {
      totalLines.value = monacoRef.value?.getLineCount?.() ?? totalLines.value;
      return;
    }

    const lines: string[] = [];
    for (const item of items) {
      const sequence = Number(item.sequence ?? 0);
      if (sequence > 0 && seenSequences.has(sequence)) {
        continue;
      }
      if (sequence > 0) {
        seenSequences.add(sequence);
      }
      lines.push(resolveLogLine(item));
    }

    if (!lines.length) {
      totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
      return;
    }

    logCount.value += lines.length;
    monacoRef.value.appendContent(lines.join("\n"));
    totalLines.value = monacoRef.value.getLineCount?.() ?? totalLines.value;
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
    seenSequences.clear();
    monacoRef.value?.clearContent?.();
    logCount.value = 0;
    totalLines.value = 0;
  };

  const normalizeNodeOptions = (nodes: SystemOutputLogNode[]) =>
    nodes
      .filter((node) => !!node.nodeId)
      .map((node) => ({
        label:
          node.nodeName ||
          [node.serviceName, node.host, node.port].filter(Boolean).join(" / ") ||
          node.nodeId ||
          "当前节点",
        value: node.nodeId || "",
      }));

  const loadNodes = async () => {
    nodeLoading.value = true;
    try {
      const response = await listSystemOutputLogNodes();
      const options = normalizeNodeOptions(response?.data || []);
      nodeOptions.value = options;
      if (!selectedNodeId.value && options.length) {
        selectedNodeId.value = options[0].value;
      }
    } catch (error) {
      console.error("加载系统日志节点失败:", error);
      message.error("加载系统日志节点失败");
    } finally {
      nodeLoading.value = false;
    }
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
    const eventSource = new EventSource(buildStreamUrl(selectedNodeId.value));
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

  const restartLogStream = async () => {
    stopLogStream();
    await resetContent();
    startLogStream();
  };

  const changeNode = (nodeId: string) => {
    if (selectedNodeId.value === nodeId) {
      return;
    }
    selectedNodeId.value = nodeId;
    void restartLogStream();
  };

  const clearLogs = async () => {
    if (cleanupLoading.value) {
      return;
    }
    cleanupLoading.value = true;
    try {
      await cleanupSystemOutputLogs(selectedNodeId.value || undefined);
      await resetContent();
      message.success("系统输出日志已清空");
    } catch (error) {
      console.error("清理系统输出日志失败:", error);
      message.error("清理系统输出日志失败");
    } finally {
      cleanupLoading.value = false;
    }
  };

  const scrollToBottom = () => {
    monacoRef.value?.scrollToBottom?.();
  };

  const handleLineExceed = (lines: number) => {
    console.log(`系统输出日志行数超出限制：${lines} 行，已自动滚动清理`);
  };

  onMounted(() => {
    void loadNodes().finally(() => {
      startLogStream();
    });
  });

  onBeforeUnmount(() => {
    stopLogStream();
  });

  const page = reactive({
    setMonacoRef,
    isStreaming,
    cleanupLoading,
    nodeLoading,
    logCount,
    totalLines,
    selectedNodeId,
    nodeOptions,
    changeNode,
    startLogStream,
    stopLogStream,
    clearLogs,
    scrollToBottom,
    handleLineExceed,
  });

  return {
    page,
  };
};
