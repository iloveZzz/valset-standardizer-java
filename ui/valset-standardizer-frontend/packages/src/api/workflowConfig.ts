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

export type WorkflowConfigAuditDTO = {
  auditId?: string;
  workflowId?: string;
  workflowCode?: string;
  versionNo?: number;
  actionType?: string;
  actionResult?: string;
  operatorName?: string;
  operatorId?: string;
  beforeJson?: string;
  afterJson?: string;
  remark?: string;
  createdAt?: string;
};

export type WorkflowVersionDiffItemDTO = {
  path?: string;
  leftValue?: string;
  rightValue?: string;
  changeType?: string;
};

export type WorkflowVersionDiffDTO = {
  leftWorkflowId?: string;
  rightWorkflowId?: string;
  leftVersionNo?: number;
  rightVersionNo?: number;
  leftWorkflowCode?: string;
  rightWorkflowCode?: string;
  items?: WorkflowVersionDiffItemDTO[];
};

export type WorkflowRuntimeParamDTO = {
  runtimeParamId?: string;
  paramNamespace?: string;
  skipExcelStyleParsing?: boolean;
  enableMatchProcess?: boolean;
  persistStandardizedDwdDetails?: boolean;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
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

export type WorkflowRuntimeParamSaveCommand = WorkflowRuntimeParamDTO;

export type PageResultWorkflowDefinitionDTO = {
  data?: WorkflowDefinitionDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type WorkflowConfigAuditQueryParams = {
  workflowCode?: string;
  versionNo?: number;
  actionType?: string;
  actionResult?: string;
  pageIndex?: number;
  pageSize?: number;
};

export type PageResultWorkflowConfigAuditDTO = {
  data?: WorkflowConfigAuditDTO[];
  totalCount?: number;
  pageIndex?: number;
  pageSize?: number;
};

export type SingleResultWorkflowConfigAuditDTO = {
  data?: WorkflowConfigAuditDTO;
};

export type SingleResultWorkflowDefinitionDTO = {
  data?: WorkflowDefinitionDTO;
};

export type SingleResultWorkflowVersionDiffDTO = {
  data?: WorkflowVersionDiffDTO;
};

export type SingleResultWorkflowRuntimeParamDTO = {
  data?: WorkflowRuntimeParamDTO;
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

export const copyWorkflowConfigVersion = (workflowId: string) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/${workflowId}/copy`,
    method: "POST",
  });

export const importWorkflowConfig = (command: WorkflowConfigSaveCommand) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: "/workflow-configs/import",
    method: "POST",
    data: command,
  });

export const validateWorkflowConfig = (command: WorkflowConfigSaveCommand) =>
  customInstance<{ data?: boolean }>({
    url: "/workflow-configs/validate",
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

export const compareWorkflowConfigs = (
  leftWorkflowId: string,
  rightWorkflowId: string,
) =>
  customInstance<SingleResultWorkflowVersionDiffDTO>({
    url: "/workflow-configs/compare",
    method: "GET",
    params: {
      leftWorkflowId,
      rightWorkflowId,
    },
  });

export const exportWorkflowConfig = (workflowId: string) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/${workflowId}/export`,
    method: "GET",
  });

export const rollbackWorkflowConfigVersion = (
  workflowId: string,
  sourceWorkflowId: string,
) =>
  customInstance<SingleResultWorkflowDefinitionDTO>({
    url: `/workflow-configs/${workflowId}/rollback/${sourceWorkflowId}`,
    method: "POST",
  });

export const getWorkflowRuntimeParam = () =>
  customInstance<SingleResultWorkflowRuntimeParamDTO>({
    url: "/workflow-configs/runtime-params",
    method: "GET",
  });

export const saveWorkflowRuntimeParam = (command: WorkflowRuntimeParamSaveCommand) =>
  customInstance<SingleResultWorkflowRuntimeParamDTO>({
    url: "/workflow-configs/runtime-params",
    method: "PUT",
    data: command,
  });

export const pageWorkflowConfigAudits = (
  params?: WorkflowConfigAuditQueryParams,
) =>
  customInstance<PageResultWorkflowConfigAuditDTO>({
    url: "/workflow-configs/audits",
    method: "GET",
    params,
  });

export const getWorkflowConfigAudit = (auditId: string) =>
  customInstance<SingleResultWorkflowConfigAuditDTO>({
    url: `/workflow-configs/audits/${auditId}`,
    method: "GET",
  });
