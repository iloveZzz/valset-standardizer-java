import { customInstance } from "./mutator";

export type QlexpressFunctionViewDTO = {
  functionId?: string;
  functionCnName?: string;
  functionName?: string;
  remark?: string;
  scriptBody?: string;
  enabled?: boolean;
  extInfo?: Record<string, any> | string;
  sourceModules?: string[];
  flowLabels?: string[];
  usageStatus?: string;
  usageStatusName?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type QlexpressFunctionUpsertCommand = {
  functionId?: string;
  functionCnName?: string;
  functionName?: string;
  remark?: string;
  scriptBody?: string;
  enabled?: boolean;
  extInfo?: Record<string, any>;
};

export type QlexpressFunctionDebugCommand = {
  functionId?: string;
  functionName?: string;
  scriptBody?: string;
  debugExpression?: string;
  context?: Record<string, any>;
};

export type QlexpressFunctionDebugResultDTO = {
  success?: boolean;
  result?: any;
  errorMessage?: string;
  costMs?: number;
  outFunctions?: string[];
  outVarNames?: string[];
};

export type QlexpressFunctionFlowUsageDTO = {
  flowCode?: string;
  flowName?: string;
  runnerScope?: string;
  matched?: boolean;
};

export type QlexpressFunctionUsageReferenceDTO = {
  flowCode?: string;
  flowName?: string;
  runnerScope?: string;
  expression?: string;
  sourceType?: string;
  sourceTypeName?: string;
  sourceId?: string;
  sourceName?: string;
  enabled?: boolean;
  referencedFunctions?: string[];
};

export type QlexpressFunctionUsageDTO = {
  functionId?: string;
  functionName?: string;
  sourceModules?: string[];
  flowUsages?: QlexpressFunctionFlowUsageDTO[];
  directReferences?: QlexpressFunctionUsageReferenceDTO[];
  dependencyReferences?: QlexpressFunctionUsageReferenceDTO[];
  usageStatus?: string;
  usageStatusName?: string;
};

export type PageResultQlexpressFunctionViewDTO = {
  data?: QlexpressFunctionViewDTO[];
  totalCount?: number;
  pageSize?: number;
  pageIndex?: number;
};

export type SingleResultQlexpressFunctionViewDTO = {
  data?: QlexpressFunctionViewDTO;
};

export type SingleResultQlexpressFunctionMutationResponse = {
  data?: {
    operation?: string;
    message?: string;
    function?: QlexpressFunctionViewDTO;
  };
};

export type SingleResultQlexpressFunctionDebugResultDTO = {
  data?: QlexpressFunctionDebugResultDTO;
};

export type SingleResultQlexpressFunctionUsageDTO = {
  data?: QlexpressFunctionUsageDTO;
};

export type QlexpressFunctionPageParams = {
  functionCnName?: string;
  functionName?: string;
  enabled?: boolean;
  pageIndex?: number;
  pageSize?: number;
};

export const pageQlexpressFunctions = (params?: QlexpressFunctionPageParams) =>
  customInstance<PageResultQlexpressFunctionViewDTO>({
    url: "/qlexpress-functions",
    method: "GET",
    params,
  });

export const getQlexpressFunction = (functionId: string) =>
  customInstance<SingleResultQlexpressFunctionViewDTO>({
    url: `/qlexpress-functions/${functionId}`,
    method: "GET",
  });

export const getQlexpressFunctionUsage = (functionId: string) =>
  customInstance<SingleResultQlexpressFunctionUsageDTO>({
    url: `/qlexpress-functions/${functionId}/usage`,
    method: "GET",
  });

export const createQlexpressFunction = (
  data: QlexpressFunctionUpsertCommand,
) =>
  customInstance<SingleResultQlexpressFunctionMutationResponse>({
    url: "/qlexpress-functions",
    method: "POST",
    data,
  });

export const updateQlexpressFunction = (
  functionId: string,
  data: QlexpressFunctionUpsertCommand,
) =>
  customInstance<SingleResultQlexpressFunctionMutationResponse>({
    url: `/qlexpress-functions/${functionId}`,
    method: "PUT",
    data,
  });

export const deleteQlexpressFunction = (functionId: string) =>
  customInstance<SingleResultQlexpressFunctionMutationResponse>({
    url: `/qlexpress-functions/${functionId}`,
    method: "DELETE",
  });

export const enableQlexpressFunction = (functionId: string) =>
  customInstance<SingleResultQlexpressFunctionMutationResponse>({
    url: `/qlexpress-functions/${functionId}/enable`,
    method: "POST",
  });

export const disableQlexpressFunction = (functionId: string) =>
  customInstance<SingleResultQlexpressFunctionMutationResponse>({
    url: `/qlexpress-functions/${functionId}/disable`,
    method: "POST",
  });

export const debugQlexpressFunction = (
  data: QlexpressFunctionDebugCommand,
) =>
  customInstance<SingleResultQlexpressFunctionDebugResultDTO>({
    url: "/qlexpress-functions/debug",
    method: "POST",
    data,
  });
