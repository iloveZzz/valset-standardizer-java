import { getJavaApi } from "./generated/valset";
import type {
  WorkflowDefinitionDTO as GeneratedWorkflowDefinitionDTO,
  WorkflowEngineBindingDTO as GeneratedWorkflowEngineBindingDTO,
  WorkflowPlatformMetadataDTO,
  WorkflowStageDTO as GeneratedWorkflowStageDTO,
} from "./generated/valset/schemas";
import type { WorkflowInstanceDTO } from "./etlWorkflowInstance";

const generatedApi = getJavaApi();

export type EtlPlatformType = "SPRING_BATCH" | "DOLPHIN_SCHEDULER" | "XXL_JOB";
export type WorkflowSyncStatus = "UNSYNCED" | "SYNCING" | "SYNCED" | "FAILED";
export type WorkflowStageDTO = Omit<GeneratedWorkflowStageDTO, "timeoutSeconds"> & {
  timeoutSeconds?: number | null;
};
export type WorkflowEngineBindingDTO = Omit<
  GeneratedWorkflowEngineBindingDTO,
  "attributes"
> & {
  attributes?: Record<string, unknown>;
};
export type WorkflowDefinitionDTO = Omit<
  GeneratedWorkflowDefinitionDTO,
  "engineBinding" | "stages"
> & {
  engineBinding?: WorkflowEngineBindingDTO | null;
  stages?: WorkflowStageDTO[];
};

export type { WorkflowPlatformMetadataDTO };

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
    definition as GeneratedWorkflowDefinitionDTO,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

export const saveEtlWorkflowDefinition = (definition: WorkflowDefinitionDTO) =>
  generatedApi.saveDefinition(
    definition as GeneratedWorkflowDefinitionDTO,
  ) as Promise<SingleResult<WorkflowDefinitionDTO>>;

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
