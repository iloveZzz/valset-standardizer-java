import { getJavaApi } from "./generated/valset";

const generatedApi = getJavaApi();

export const getTransferDeliveryRecordSummary = generatedApi.summarizeToday;

export type {
  SingleResultTransferDeliveryRecordSummaryViewDTO as SingleResultTransferDeliveryRecordSummaryDTO,
  SummarizeTodayParams as TransferDeliveryRecordSummaryQueryParams,
  TransferDeliveryRecordSummaryViewDTO as TransferDeliveryRecordSummaryDTO,
} from "./generated/valset/schemas";
