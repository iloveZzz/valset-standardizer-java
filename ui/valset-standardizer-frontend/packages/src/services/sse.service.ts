type SSEMessageType = "progress" | "status" | "complete" | "error";

export interface TransferSSEProgressData {
  uploadedBytes: number;
  totalBytes: number;
  uploadedChunks: number;
  totalChunks: number;
}

export interface TransferSSEStatusData {
  status: string;
  message?: string;
}

export interface TransferSSECompleteData {
  message?: string;
}

export interface TransferSSEErrorData {
  code: string;
  message: string;
}

export type TransferSSEMessage =
  | { type: "progress"; taskId: string; data: TransferSSEProgressData }
  | { type: "status"; taskId: string; data: TransferSSEStatusData }
  | { type: "complete"; taskId: string; data: TransferSSECompleteData }
  | { type: "error"; taskId: string; data: TransferSSEErrorData };

export type TransferSSEMessageHandler = (message: TransferSSEMessage) => void;
export type TransferSSEConnectionHandler = (connected: boolean) => void;

interface SSEServiceConfig {
  baseUrl: string;
  endpoint: string;
  syncOnReconnect: boolean;
}

const DEFAULT_CONFIG: SSEServiceConfig = {
  baseUrl: import.meta.env.VITE_API_BASE_URL || "/api",
  endpoint: "/transfers/subscribe",
  syncOnReconnect: true,
};

function normalizeBaseUrl(baseUrl: string) {
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
}

function parseProgressData(data: Record<string, unknown>): TransferSSEProgressData {
  return {
    uploadedBytes: Number(data.uploadedBytes ?? data.transferredBytes) || 0,
    totalBytes: Number(data.totalBytes) || 0,
    uploadedChunks: Number(data.uploadedChunks ?? data.transferredChunks) || 0,
    totalChunks: Number(data.totalChunks) || 0,
  };
}

function parseStatusData(data: Record<string, unknown>): TransferSSEStatusData {
  return {
    status: String(data.status || "idle").toLowerCase(),
    message: data.message as string | undefined,
  };
}

function parseCompleteData(data: Record<string, unknown>): TransferSSECompleteData {
  return {
    message: data.message as string | undefined,
  };
}

function parseErrorData(data: Record<string, unknown>): TransferSSEErrorData {
  return {
    code: String(data.code || "UNKNOWN_ERROR"),
    message: String(data.message || "Unknown error occurred"),
  };
}

class SSEService {
  private static instance: SSEService | null = null;
  private eventSource: EventSource | null = null;
  private currentUserId: string | null = null;
  private messageHandlers: Set<TransferSSEMessageHandler> = new Set();
  private connectionHandlers: Set<TransferSSEConnectionHandler> = new Set();
  private config: SSEServiceConfig;
  private connected = false;
  private reconnectSyncCallback: (() => Promise<void>) | null = null;
  private reconnectAttempts = 0;
  private shouldReconnect = true;
  private readonly MAX_RECONNECT_ATTEMPTS = 5;
  private readonly RECONNECT_BASE_DELAY = 2000;

  private constructor(config: Partial<SSEServiceConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config };
  }

  public static getInstance(config?: Partial<SSEServiceConfig>): SSEService {
    if (!SSEService.instance) {
      SSEService.instance = new SSEService(config);
    }
    return SSEService.instance;
  }

  public connect(userId: string): void {
    if (
      this.eventSource &&
      this.currentUserId === userId &&
      this.eventSource.readyState !== EventSource.CLOSED
    ) {
      return;
    }

    if (this.eventSource && this.eventSource.readyState === EventSource.CLOSED) {
      this.eventSource = null;
    }

    if (this.eventSource) {
      this.disconnect();
    }

    this.currentUserId = userId;
    this.shouldReconnect = true;

    const url = `${normalizeBaseUrl(this.config.baseUrl)}${this.config.endpoint}`;

    try {
      this.eventSource = new EventSource(url);
      this.setupEventListeners();
    } catch (error) {
      console.error("SSE 连接失败:", error);
      this.setConnected(false);
    }
  }

  public disconnect(): void {
    this.shouldReconnect = false;
    this.reconnectAttempts = 0;

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.currentUserId = null;
    this.setConnected(false);
  }

  public isConnected(): boolean {
    return this.connected;
  }

  public onMessage(handler: TransferSSEMessageHandler): () => void {
    this.messageHandlers.add(handler);
    return () => {
      this.messageHandlers.delete(handler);
    };
  }

  public onConnectionChange(handler: TransferSSEConnectionHandler): () => void {
    this.connectionHandlers.add(handler);
    return () => {
      this.connectionHandlers.delete(handler);
    };
  }

  public setReconnectSyncCallback(callback: () => Promise<void>): void {
    this.reconnectSyncCallback = callback;
  }

  private setConnected(connected: boolean): void {
    const wasConnected = this.connected;
    this.connected = connected;

    this.connectionHandlers.forEach((handler) => {
      try {
        handler(connected);
      } catch {
        // Silent
      }
    });

    if (!wasConnected && connected && this.config.syncOnReconnect) {
      void this.triggerReconnectSync();
    }
  }

  private async triggerReconnectSync(): Promise<void> {
    if (!this.reconnectSyncCallback) return;
    try {
      await this.reconnectSyncCallback();
    } catch {
      // Silent
    }
  }

  private setupEventListeners(): void {
    if (!this.eventSource) return;

    this.eventSource.onopen = () => {
      this.reconnectAttempts = 0;
      this.setConnected(true);
    };

    this.eventSource.onerror = (error) => {
      console.error("SSE 连接错误:", error);

      if (!this.shouldReconnect) {
        this.setConnected(false);
        return;
      }

      if (this.eventSource?.readyState === EventSource.CLOSED) {
        this.setConnected(false);

        if (this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
          this.reconnectAttempts += 1;
          const delay = this.RECONNECT_BASE_DELAY * this.reconnectAttempts;

          window.setTimeout(() => {
            if (this.shouldReconnect && this.currentUserId) {
              this.connect(this.currentUserId);
            }
          }, delay);
        }
      }
    };

    this.eventSource.addEventListener("progress", (event) => {
      this.handleTypedEvent("progress", event);
    });

    this.eventSource.addEventListener("status", (event) => {
      this.handleTypedEvent("status", event);
    });

    this.eventSource.addEventListener("complete", (event) => {
      this.handleTypedEvent("complete", event);
    });

    this.eventSource.addEventListener("error", (event) => {
      if (event instanceof MessageEvent) {
        this.handleTypedEvent("error", event);
      }
    });

    this.eventSource.onmessage = (event) => {
      this.handleGenericMessage(event);
    };
  }

  private handleTypedEvent(type: SSEMessageType, event: Event): void {
    if (!(event instanceof MessageEvent)) return;

    try {
      const rawData = JSON.parse(event.data);
      const message = this.parseMessage(type, rawData);
      if (message) {
        this.dispatchMessage(message);
      }
    } catch {
      // Silent
    }
  }

  private handleGenericMessage(event: MessageEvent): void {
    try {
      const rawData = JSON.parse(event.data);
      if (rawData?.type && rawData?.taskId) {
        const message = this.parseMessage(rawData.type as SSEMessageType, rawData);
        if (message) {
          this.dispatchMessage(message);
        }
      }
    } catch {
      // Silent
    }
  }

  private parseMessage(
    type: SSEMessageType,
    rawData: Record<string, unknown>,
  ): TransferSSEMessage | null {
    const nestedData = (rawData as { data?: unknown }).data as Record<string, unknown> | undefined;
    const taskId = String(rawData.taskId || nestedData?.taskId || "");
    if (!taskId) return null;

    const data = nestedData || rawData;

    switch (type) {
      case "progress":
        return { type: "progress", taskId, data: parseProgressData(data) };
      case "status":
        return { type: "status", taskId, data: parseStatusData(data) };
      case "complete":
        return { type: "complete", taskId, data: parseCompleteData(data) };
      case "error":
        return { type: "error", taskId, data: parseErrorData(data) };
      default:
        return null;
    }
  }

  private dispatchMessage(message: TransferSSEMessage): void {
    this.messageHandlers.forEach((handler) => {
      try {
        handler(message);
      } catch {
        // Silent
      }
    });
  }
}

export const sseService = SSEService.getInstance();
