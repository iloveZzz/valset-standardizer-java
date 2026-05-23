import { getJavaApi } from "./generated/valset";

const generatedApi = getJavaApi();

export const listTransferObjectTrends = generatedApi.trendObjects;

export type {
  MultiResultTransferObjectTrendViewDTO,
  TransferObjectTrendViewDTO,
  TrendObjectsParams as TransferObjectTrendQueryParams,
} from "./generated/valset/schemas";
