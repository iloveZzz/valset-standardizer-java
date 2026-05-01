export type ParseLifecycleEventStage =
  | "CYCLE_STARTED"
  | "CYCLE_FINISHED"
  | "BATCH_STARTED"
  | "BATCH_EMPTY"
  | "BATCH_FINISHED"
  | "QUEUE_DISCOVERED"
  | "QUEUE_GENERATED"
  | "QUEUE_BACKFILLED"
  | "QUEUE_REUSED"
  | "QUEUE_UPDATED"
  | "QUEUE_RETRIED"
  | "QUEUE_SUBSCRIBE_ATTEMPTED"
  | "QUEUE_SUBSCRIBED"
  | "QUEUE_SUBSCRIBE_CONFLICT"
  | "QUEUE_SUBSCRIBE_SKIPPED"
  | "QUEUE_FILE_INFO_REPAIR_STARTED"
  | "QUEUE_FILE_INFO_REPAIR_COMPLETED"
  | "QUEUE_FILE_INFO_REPAIR_FAILED"
  | "TASK_REQUEST_BUILT"
  | "TASK_CREATED"
  | "TASK_REUSED"
  | "TASK_DISPATCHED"
  | "TASK_EXECUTION_STARTED"
  | "TASK_RAW_PARSED"
  | "TASK_STANDARDIZED"
  | "TASK_PERSISTED"
  | "TASK_SUCCEEDED"
  | "TASK_FAILED"
  | "QUEUE_COMPLETED"
  | "QUEUE_FAILED"
  | "QUEUE_SKIPPED";

export type ParseLifecycleEventDTO = {
  eventId?: string;
  occurredAt?: string;
  stage?: ParseLifecycleEventStage | string;
  source?: string;
  queueId?: string;
  transferId?: string;
  businessKey?: string;
  taskId?: number | null;
  fileId?: number | null;
  dataSourceType?: string;
  triggerMode?: string;
  subscribedBy?: string;
  message?: string;
  errorMessage?: string;
  attributes?: Record<string, unknown>;
};

export type ParseLifecycleEventStreamFilters = {
  source?: string;
  queueId?: string;
  transferId?: string;
  taskId?: number | null;
  stage?: ParseLifecycleEventStage | string;
};

export type ParseLifecycleEventStreamCallbacks = {
  onEvent?: (event: ParseLifecycleEventDTO) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: unknown) => void;
};

export type ParseLifecycleEventStreamConnection = {
  eventSource: EventSource;
  close: () => void;
};

const normalizeBaseUrl = (baseUrl?: string) => {
  if (!baseUrl) {
    return "/api";
  }
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
};

const buildStreamUrl = (
  baseUrl?: string,
  filters?: ParseLifecycleEventStreamFilters,
) => {
  const url = new URL(
    `${normalizeBaseUrl(baseUrl)}/parse-lifecycle-events/stream`,
    window.location.origin,
  );
  if (filters?.source) {
    url.searchParams.set("source", filters.source);
  }
  if (filters?.queueId) {
    url.searchParams.set("queueId", filters.queueId);
  }
  if (filters?.transferId) {
    url.searchParams.set("transferId", filters.transferId);
  }
  if (filters?.taskId != null) {
    url.searchParams.set("taskId", String(filters.taskId));
  }
  if (filters?.stage) {
    url.searchParams.set("stage", filters.stage);
  }
  return `${url.pathname}${url.search}${url.hash}`;
};

const parseEvent = (event: MessageEvent): ParseLifecycleEventDTO | null => {
  try {
    const parsed = JSON.parse(String(event.data || ""));
    if (!parsed) {
      return null;
    }
    return parsed as ParseLifecycleEventDTO;
  } catch {
    return null;
  }
};

export function subscribeParseLifecycleEventStream(
  filters: ParseLifecycleEventStreamFilters = {},
  callbacks: ParseLifecycleEventStreamCallbacks = {},
  baseUrl?: string,
): ParseLifecycleEventStreamConnection {
  const eventSource = new EventSource(buildStreamUrl(baseUrl, filters));
  const handler = (event: MessageEvent) => {
    const data = parseEvent(event);
    if (data && callbacks.onEvent) {
      callbacks.onEvent(data);
    }
  };
  eventSource.addEventListener("parse-lifecycle", handler as EventListener);
  eventSource.onopen = () => {
    callbacks.onOpen?.();
  };
  eventSource.onerror = (event) => {
    callbacks.onError?.(event);
    if (eventSource.readyState === EventSource.CLOSED) {
      callbacks.onClose?.();
    }
  };
  return {
    eventSource,
    close: () => {
      eventSource.close();
      callbacks.onClose?.();
    },
  };
}
