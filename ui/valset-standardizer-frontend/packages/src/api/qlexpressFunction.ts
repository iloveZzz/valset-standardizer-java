import { getJavaApi } from "./generated/valset";

const generatedApi = getJavaApi();

export const pageQlexpressFunctions = generatedApi.pageFunctions;
export const getQlexpressFunction = generatedApi.getFunction;
export const getQlexpressFunctionUsage = generatedApi.getFunctionUsage;
export const createQlexpressFunction = generatedApi.createFunction;
export const updateQlexpressFunction = generatedApi.updateFunction;
export const deleteQlexpressFunction = generatedApi.deleteFunction;
export const enableQlexpressFunction = generatedApi.enableFunction;
export const disableQlexpressFunction = generatedApi.disableFunction;
export const debugQlexpressFunction = generatedApi.debug;

export type {
  PageFunctionsParams as QlexpressFunctionPageParams,
  PageResultQlexpressFunctionViewDTO,
  QlexpressFunctionDebugCommand,
  QlexpressFunctionDebugResultDTO,
  QlexpressFunctionFlowUsageDTO,
  QlexpressFunctionMutationResponse,
  QlexpressFunctionUpsertCommand,
  QlexpressFunctionUsageDTO,
  QlexpressFunctionUsageReferenceDTO,
  QlexpressFunctionViewDTO,
  SingleResultQlexpressFunctionDebugResultDTO,
  SingleResultQlexpressFunctionMutationResponse,
  SingleResultQlexpressFunctionUsageDTO,
  SingleResultQlexpressFunctionViewDTO,
} from "./generated/valset/schemas";
