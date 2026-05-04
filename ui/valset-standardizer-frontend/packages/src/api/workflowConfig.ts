import { customInstance } from "./mutator";

export type WorkflowStageDTO = {
  stageId?: string;
  workflowId?: string;
  stageCode?: string;
  stepCode?: string;
  stageName?: string;
  stepName?: string;
  stageDescription?: string;
  stepDescription?: string;
  sortOrder?: number;
  retryable?: boolean;
  skippable?: boolean;
  enabled?: boolean;
  taskTypes?: string[];
  taskStages?: string[];
  parseLifecycleStages?: string[];
};

export type WorkflowStatusMappingDTO = {
  mappingId?: string;
  workflowId?: string;
  sourceType?: string;
  sourceStatus?: string;
  targetStatus?: string;
  statusLabel?: string;
};

export type WorkflowExecutorBindingDTO = {
  bindingId?: string;
  workflowId?: string;
  stageId?: string;
  stageCode?: string;
  engineType?: string;
  externalRef?: string;
  configJson?: string;
  enabled?: boolean;
};

export type WorkflowDefinitionDTO = {
  workflowId?: string;
  workflowCode?: string;
  workflowName?: string;
  businessType?: string;
  engineType?: string;
  parseFallbackStage?: string;
  workflowFallbackStage?: string;
  versionNo?: number;
  enabled?: boolean;
  status?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
  stages?: WorkflowStageDTO[];
  statusMappings?: WorkflowStatusMappingDTO[];
  executorBindings?: WorkflowExecutorBindingDTO[];
  ignoredParseLifecycleStages?: string[];
  ignoredWorkflowTaskTypes?: string[];
};

export type WorkflowConfigQueryParams = {
  workflowCode?: string;
  workflowName?: string;
  businessType?: string;
  engineType?: string;
  status?: string;
  enabled?: boolean;
  pageIndex?: number;
  pageSize?: number;
};

export type WorkflowConfigSaveCommand = WorkflowDefinitionDTO;

export type PageResultWorkflowDefinitionDTO = {
  data?: WorkflowDefinitionDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type SingleResultWorkflowDefinitionDTO = {
  data?: WorkflowDefinitionDTO;
};

export const pageWorkflowConfigs = (params?: WorkflowConfigQueryParams) =>
  customInstance<PageResultWorkflowDefinitionDTO>({
    url: "/workflow-configs",
    method: "GET",
    params,
  });

export const getWorkflowConfig = (workflowId: string) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/${workflowId}`,
    method: "GET",
  });

export const getActiveWorkflowConfig = (workflowCode: string) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/active/${workflowCode}`,
    method: "GET",
  });

export const saveWorkflowConfigDraft = (command: WorkflowConfigSaveCommand) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: "/workflow-configs/draft",
    method: "POST",
    data: command,
  });

export const publishWorkflowConfig = (workflowId: string) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/${workflowId}/publish`,
    method: "POST",
  });

export const disableWorkflowConfig = (workflowId: string) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/${workflowId}/disable`,
    method: "POST",
  });
