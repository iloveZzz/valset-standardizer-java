import { getJavaApi } from "./generated/valset";

export type TransferDeliveryRecordSummaryDTO = {
  todayDeliveryCount?: number;
  todaySuccessCount?: number;
  todayFailedCount?: number;
  successRate?: number;
};

export type SingleResultTransferDeliveryRecordSummaryDTO = {
  data?: TransferDeliveryRecordSummaryDTO;
};

const generatedApi = getJavaApi();

export const getTransferDeliveryRecordSummary = () =>
  generatedApi.summarizeToday() as Promise<SingleResultTransferDeliveryRecordSummaryDTO>;
