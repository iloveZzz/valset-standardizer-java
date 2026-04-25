type TransferIngestProgressSseEventType =
  | "status"
  | "progress"
  | "message"
  | "complete"
  | "error";

export interface TransferIngestProgressStatusData {
  status: string;
  message?: string;
  triggerType?: string;
  triggeredAt?: string;
}

export interface TransferIngestProgressData {
  processedCount: number;
  totalCount: number;
  message?: string;
}

export interface TransferIngestMessageData {
  message: string;
}

export interface TransferIngestCompleteData {
  message?: string;
}

export interface TransferIngestErrorData {
  code: string;
  message: string;
}

export type TransferIngestProgressMessage =
  | { type: "status"; sourceId: string; data: TransferIngestProgressStatusData }
  | { type: "progress"; sourceId: string; data: TransferIngestProgressData }
  | { type: "message"; sourceId: string; data: TransferIngestMessageData }
  | { type: "complete"; sourceId: string; data: TransferIngestCompleteData }
  | { type: "error"; sourceId: string; data: TransferIngestErrorData };

export type TransferIngestProgressHandler = (
  message: TransferIngestProgressMessage,
) => void;

interface TransferIngestProgressConnection {
  eventSource: EventSource;
  handlers: Set<TransferIngestProgressHandler>;
}

interface TransferIngestProgressConfig {
  baseUrl: string;
}

const DEFAULT_CONFIG: TransferIngestProgressConfig = {
  baseUrl: import.meta.env.VITE_API_BASE_URL || "/api",
};

function normalizeBaseUrl(baseUrl: string) {
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
}

function buildEndpoint(baseUrl: string, sourceId: string) {
  return `${normalizeBaseUrl(baseUrl)}/transfer-sources/${encodeURIComponent(sourceId)}/progress/stream`;
}

function parseJson(event: MessageEvent): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(event.data);
    if (parsed && typeof parsed === "object") {
      return parsed as Record<string, unknown>;
    }
  } catch {
    return null;
  }
  return null;
}

function parseMessage(
  sourceId: string,
  type: TransferIngestProgressSseEventType,
  rawData: Record<string, unknown>,
): TransferIngestProgressMessage | null {
  const nestedData = (rawData as { data?: unknown }).data as Record<string, unknown> | undefined;
  const data = nestedData || rawData;

  const parseMetricCount = (value: unknown) => {
    const normalized = Number(value);
    return Number.isFinite(normalized) ? normalized : 0;
  };

  switch (type) {
    case "status":
      return {
        type: "status",
        sourceId,
        data: {
          status: String(data.status ?? "idle"),
          message: data.message as string | undefined,
          triggerType: data.triggerType as string | undefined,
          triggeredAt: data.triggeredAt as string | undefined,
        },
      };
    case "progress": {
      const processedCount = parseMetricCount(data.processedCount);
      const totalCount = parseMetricCount(data.totalCount);
      return {
        type: "progress",
        sourceId,
        data: {
          processedCount,
          totalCount,
          message: data.message as string | undefined,
        },
      };
    }
    case "message":
      return {
        type: "message",
        sourceId,
        data: {
          message: String(data.message ?? ""),
        },
      };
    case "complete":
      return {
        type: "complete",
        sourceId,
        data: {
          message: data.message as string | undefined,
        },
      };
    case "error":
      return {
        type: "error",
        sourceId,
        data: {
          code: String(data.code ?? "INGEST_FAILED"),
          message: String(data.message ?? "来源收取失败"),
        },
      };
    default:
      return null;
  }
}

class TransferIngestProgressSseService {
  private config: TransferIngestProgressConfig;
  private connections = new Map<string, TransferIngestProgressConnection>();

  constructor(config: Partial<TransferIngestProgressConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  public subscribe(
    sourceId: string,
    handler: TransferIngestProgressHandler,
  ): () => void {
    if (!sourceId) {
      return () => undefined;
    }

    const existing = this.connections.get(sourceId);
    if (existing) {
      existing.handlers.add(handler);
      return () => this.unsubscribe(sourceId, handler);
    }

    const eventSource = new EventSource(buildEndpoint(this.config.baseUrl, sourceId));
    const handlers = new Set<TransferIngestProgressHandler>([handler]);
    const connection: TransferIngestProgressConnection = { eventSource, handlers };

    const dispatch = (
      type: TransferIngestProgressSseEventType,
      event: MessageEvent,
    ) => {
      const parsed = parseJson(event);
      if (!parsed) {
        return;
      }
      const taskId = String(parsed.taskId ?? parsed.sourceId ?? sourceId);
      if (!taskId) {
        return;
      }
      const message = parseMessage(taskId, type, parsed);
      if (!message) {
        return;
      }
      handlers.forEach((item) => {
        try {
          item(message);
        } catch {
          // 忽略单个监听器异常
        }
      });
    };

    eventSource.addEventListener("status", (event) => {
      if (event instanceof MessageEvent) {
        dispatch("status", event);
      }
    });
  eventSource.addEventListener("progress", (event) => {
      if (event instanceof MessageEvent) {
        dispatch("progress", event);
      }
    });
    eventSource.addEventListener("message", (event) => {
      if (event instanceof MessageEvent) {
        dispatch("message", event);
      }
    });
    eventSource.addEventListener("complete", (event) => {
      if (event instanceof MessageEvent) {
        dispatch("complete", event);
      }
    });
    eventSource.addEventListener("error", (event) => {
      if (event instanceof MessageEvent) {
        dispatch("error", event);
      }
    });
    eventSource.onmessage = (event) => {
      if (!(event instanceof MessageEvent)) {
        return;
      }
      const parsed = parseJson(event);
      if (!parsed?.type) {
        return;
      }
      const type = String(parsed.type) as TransferIngestProgressSseEventType;
      if (!["status", "progress", "message", "complete", "error"].includes(type)) {
        return;
      }
      dispatch(type, event);
    };

    this.connections.set(sourceId, connection);
    return () => this.unsubscribe(sourceId, handler);
  }

  public unsubscribeAll(): void {
    this.connections.forEach((connection) => {
      connection.eventSource.close();
    });
    this.connections.clear();
  }

  private unsubscribe(sourceId: string, handler: TransferIngestProgressHandler): void {
    const connection = this.connections.get(sourceId);
    if (!connection) {
      return;
    }

    connection.handlers.delete(handler);
    if (connection.handlers.size > 0) {
      return;
    }

    connection.eventSource.close();
    this.connections.delete(sourceId);
  }
}

export const transferIngestProgressSse = new TransferIngestProgressSseService();
