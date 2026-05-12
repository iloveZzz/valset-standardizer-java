import { customInstance } from "./mutator";

export type TransferDeliveryRecordSummaryDTO = {
  todayDeliveryCount?: number;
  todaySuccessCount?: number;
  todayFailedCount?: number;
  successRate?: number;
};

export type SingleResultTransferDeliveryRecordSummaryDTO = {
  data?: TransferDeliveryRecordSummaryDTO;
};

export const getTransferDeliveryRecordSummary = () =>
  customInstance<SingleResultTransferDeliveryRecordSummaryDTO>({
    url: "/transfer-delivery-records/summary",
    method: "GET",
  });
