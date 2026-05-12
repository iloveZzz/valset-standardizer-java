import { getJavaApi } from "./generated/valset";
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

const generatedApi = getJavaApi();

export const listEtlWorkflowDefinitions = () =>
  generatedApi.listDefinitions() as Promise<MultiResult<WorkflowDefinitionDTO>>;

export const listEtlWorkflowPlatforms = () =>
  generatedApi.listPlatforms() as Promise<MultiResult<WorkflowPlatformMetadataDTO>>;

export const getEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  generatedApi.getDefinition(
    workflowCode,
    workflowVersionNo,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const validateEtlWorkflowDefinition = (
  definition: WorkflowDefinitionDTO,
) =>
  generatedApi.validateDefinition(
    definition,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const saveEtlWorkflowDefinition = (definition: WorkflowDefinitionDTO) =>
  generatedApi.saveDefinition(definition) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const syncEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  generatedApi.syncDefinition(
    workflowCode,
    workflowVersionNo,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const onlineEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  generatedApi.onlineDefinition(
    workflowCode,
    workflowVersionNo,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const offlineEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  generatedApi.offlineDefinition(
    workflowCode,
    workflowVersionNo,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const deleteEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  generatedApi.deleteDefinition(
    workflowCode,
    workflowVersionNo,
  ) as Promise<SingleResult<boolean>>;

export const runEtlWorkflowDefinition = (
  workflowCode: string,
  workflowVersionNo: number,
) =>
  generatedApi.runDefinition(
    workflowCode,
    workflowVersionNo,
  ) as Promise<SingleResult<WorkflowInstanceDTO>>;
