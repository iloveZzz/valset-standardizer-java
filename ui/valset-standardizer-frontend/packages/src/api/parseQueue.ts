import { customInstance } from "./mutator";

export type ParseQueueStatus =
  | "PENDING"
  | "PARSING"
  | "PARSED"
  | "FAILED";

export type ParseQueueTriggerMode = "AUTO" | "MANUAL";

export type ParseQueueViewDTO = {
  queueId?: string;
  businessKey?: string;
  transferId?: string;
  originalName?: string;
  sourceId?: string;
  sourceType?: string;
  sourceCode?: string;
  routeId?: string;
  deliveryId?: string;
  tagId?: string;
  tagCode?: string;
  tagName?: string;
  fileStatus?: string;
  deliveryStatus?: string;
  parseStatus?: ParseQueueStatus;
  triggerMode?: ParseQueueTriggerMode;
  retryCount?: number;
  subscribedBy?: string;
  subscribedAt?: string;
  parsedAt?: string;
  lastErrorMessage?: string;
  objectSnapshotJson?: unknown;
  deliverySnapshotJson?: unknown;
  parseRequestJson?: unknown;
  parseResultJson?: unknown;
  createdAt?: string;
  updatedAt?: string;
};

export type PageResultParseQueueViewDTO = {
  data?: ParseQueueViewDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type MultiResultParseQueueViewDTO = {
  data?: ParseQueueViewDTO[];
};

export type SingleResultParseQueueViewDTO = {
  data?: ParseQueueViewDTO | null;
};

export type ParseQueueGenerateCommand = {
  transferId?: string;
  businessKey?: string;
  sourceId?: string;
  routeId?: string;
  tagCode?: string;
  forceRebuild?: boolean;
};

export type ParseQueueBackfillCommand = {
  transferId?: string;
  sourceId?: string;
  sourceCode?: string;
  routeId?: string;
  tagCode?: string;
  status?: string;
  deliveryStatus?: string;
  parseStatus?: string;
  forceRebuild?: boolean;
  dryRun?: boolean;
};

export type ParseQueueSubscribeCommand = {
  subscribedBy?: string;
};

export type ParseQueueCompleteCommand = {
  parseResultJson?: unknown;
};

export type ParseQueueFailCommand = {
  errorMessage?: string;
};

export type ParseQueueRetryCommand = {
  forceRebuild?: boolean;
};

export type ParseQueueQueryParams = {
  transferId?: string;
  businessKey?: string;
  sourceCode?: string;
  routeId?: string;
  tagCode?: string;
  fileStatus?: string;
  deliveryStatus?: string;
  parseStatus?: string;
  triggerMode?: string;
  pageIndex?: number;
  pageSize?: number;
};

export const listParseQueues = (
  params?: ParseQueueQueryParams,
) => {
  return customInstance<PageResultParseQueueViewDTO>({
    url: `/transfer-parse-queues`,
    method: "GET",
    params,
  });
};

export const getParseQueue = (queueId: string) => {
  return customInstance<SingleResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/${queueId}`,
    method: "GET",
  });
};

export const generateParseQueue = (
  command: ParseQueueGenerateCommand,
) => {
  return customInstance<SingleResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/generate`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: command,
  });
};

export const backfillParseQueue = (
  command: ParseQueueBackfillCommand,
) => {
  return customInstance<MultiResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/backfill`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: command,
  });
};

export const subscribeParseQueue = (
  queueId: string,
  command: ParseQueueSubscribeCommand,
) => {
  return customInstance<SingleResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/${queueId}/subscribe`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: command,
  });
};

export const completeParseQueue = (
  queueId: string,
  command: ParseQueueCompleteCommand,
) => {
  return customInstance<SingleResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/${queueId}/complete`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: command,
  });
};

export const failParseQueue = (
  queueId: string,
  command: ParseQueueFailCommand,
) => {
  return customInstance<SingleResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/${queueId}/fail`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: command,
  });
};

export const retryParseQueue = (
  queueId: string,
  command: ParseQueueRetryCommand,
) => {
  return customInstance<SingleResultParseQueueViewDTO>({
    url: `/transfer-parse-queues/${queueId}/retry`,
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: command,
  });
};
