import { customInstance } from "./mutator";

export type SystemOutputLogCleanupResponse = {
  deletedCount?: number;
  remainingCount?: number;
};

export type SystemOutputLogNode = {
  nodeId?: string;
  nodeName?: string;
  serviceName?: string;
  host?: string;
  port?: string;
  pid?: string;
  current?: boolean;
};

export type SingleResultSystemOutputLogCleanupResponse = {
  data?: SystemOutputLogCleanupResponse;
};

export type MultiResultSystemOutputLogNode = {
  data?: SystemOutputLogNode[];
};

export const listSystemOutputLogNodes = () =>
  customInstance<MultiResultSystemOutputLogNode>({
    url: "/system-output-logs/nodes",
    method: "GET",
  });

export const cleanupSystemOutputLogs = (nodeId?: string) =>
  customInstance<SingleResultSystemOutputLogCleanupResponse>({
    url: "/system-output-logs/cleanup",
    method: "POST",
    params: {
      nodeId,
    },
  });
