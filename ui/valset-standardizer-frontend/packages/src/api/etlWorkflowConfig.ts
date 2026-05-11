import { customInstance } from "./mutator";
import type { WorkflowInstanceDTO } from "./etlWorkflowInstance";

export type EtlPlatformType = "SPRING_BATCH" | "DOLPHIN_SCHEDULER" | "XXL_JOB";
export type WorkflowSyncStatus = "UNSYNCED" | "SYNCING" | "SYNCED" | "FAILED";

export type WorkflowStageDTO = {
  stageCode?: string;
  stageName?: string;
  stageOrder?: number;
  description?: string;
  retryable?: boolean;
  timeoutSeconds?: number | null;
};

export type WorkflowEngineBindingDTO = {
  platformType?: EtlPlatformType;
  externalWorkflowId?: string;
  externalProjectCode?: string;
  externalNamespace?: string;
  externalJobGroup?: string;
  externalJobHandler?: string;
  configJson?: string;
  externalOnline?: boolean | null;
  externalReleaseState?: string | null;
  syncStatus?: WorkflowSyncStatus | null;
  firstSyncedAt?: string | null;
  lastSyncedAt?: string | null;
  syncFailureReason?: string | null;
  remoteWorkflowVersionNo?: number | null;
  attributes?: Record<string, unknown>;
};

export type WorkflowDefinitionDTO = {
  workflowCode?: string;
  workflowName?: string;
  workflowVersionNo?: number;
  platformType?: EtlPlatformType;
  description?: string;
  enabled?: boolean;
  stages?: WorkflowStageDTO[];
  engineBinding?: WorkflowEngineBindingDTO | null;
};

export type WorkflowPlatformMetadataDTO = {
  platformType?: EtlPlatformType;
  platformName?: string;
  description?: string;
  requiredBindingFields?: string[];
  supportedOperations?: string[];
};

export type SingleResult<T> = {
  data?: T;
  success?: boolean;
  message?: string;
};

export type MultiResult<T> = {
  data?: T[];
  success?: boolean;
  message?: string;
};

export const listEtlWorkflowDefinitions = () =>
  customInstance<MultiResult<WorkflowDefinitionDTO>>({
    url: "/etl/workflows",
    method: "GET",
  });

export const listEtlWorkflowPlatforms = () =>
  customInstance<MultiResult<WorkflowPlatformMetadataDTO>>({
    url: "/etl/workflows/platforms",
    method: "GET",
  });

export const getEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  customInstance<SingleResult<WorkflowDefinitionDTO>>({
    url: `/etl/workflows/${workflowCode}/${workflowVersionNo}`,
    method: "GET",
  });

export const validateEtlWorkflowDefinition = (
  definition: WorkflowDefinitionDTO,
) =>
  customInstance<SingleResult<WorkflowDefinitionDTO>>({
    url: "/etl/workflows/validate",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: definition,
  });

export const saveEtlWorkflowDefinition = (definition: WorkflowDefinitionDTO) =>
  customInstance<SingleResult<WorkflowDefinitionDTO>>({
    url: "/etl/workflows",
    method: "POST",
    headers: { "Content-Type": "application/json" },
    data: definition,
  });

export const syncEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  customInstance<SingleResult<WorkflowDefinitionDTO>>({
    url: `/etl/workflows/${encodeURIComponent(workflowCode)}/${workflowVersionNo}/sync`,
    method: "POST",
  });

export const onlineEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  customInstance<SingleResult<WorkflowDefinitionDTO>>({
    url: `/etl/workflows/${encodeURIComponent(workflowCode)}/${workflowVersionNo}/online`,
    method: "POST",
  });

export const offlineEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  customInstance<SingleResult<WorkflowDefinitionDTO>>({
    url: `/etl/workflows/${encodeURIComponent(workflowCode)}/${workflowVersionNo}/offline`,
    method: "POST",
  });

export const deleteEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  customInstance<SingleResult<boolean>>({
    url: `/etl/workflows/${encodeURIComponent(workflowCode)}/${workflowVersionNo}`,
    method: "DELETE",
  });

export const runEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  customInstance<SingleResult<WorkflowInstanceDTO>>({
    url: `/etl/workflows/${encodeURIComponent(workflowCode)}/${workflowVersionNo}/run`,
    method: "POST",
  });
