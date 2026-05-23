import { getJavaApi } from "./generated/valset";

const generatedApi = getJavaApi();

export const listSystemOutputLogNodes = generatedApi.listNodes;

export const cleanupSystemOutputLogs = (nodeId?: string) =>
  generatedApi.cleanupLogs({ nodeId });

export type {
  MultiResultSystemOutputLogNodeDTO as MultiResultSystemOutputLogNode,
  SingleResultSystemOutputLogCleanupResponse,
  SystemOutputLogCleanupResponse,
  SystemOutputLogNodeDTO as SystemOutputLogNode,
} from "./generated/valset/schemas";
