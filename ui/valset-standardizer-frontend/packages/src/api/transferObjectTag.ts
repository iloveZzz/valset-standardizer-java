import { getJavaApi } from "./generated/valset";

const generatedApi = getJavaApi();

export const listTransferObjectTagSummaries = generatedApi.summarizeTags;

export type {
  MultiResultTransferObjectTagSummaryViewDTO,
  SummarizeTagsParams as TransferObjectTagSummaryQueryParams,
  TransferObjectTagSummaryViewDTO,
} from "./generated/valset/schemas";
