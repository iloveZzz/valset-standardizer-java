import { customInstance } from "./generated/valset/mutator";

export type TransferDeliveryRecordSummaryDTO = {
  todayDeliveryCount?: number;
  todaySuccessCount?: number;
  todayFailedCount?: number;
  successRate?: number;
};

export type TransferDeliveryRecordSummaryQueryParams = {
  taskDate?: string;
};

export type SingleResultTransferDeliveryRecordSummaryDTO = {
  data?: TransferDeliveryRecordSummaryDTO;
};

export const getTransferDeliveryRecordSummary = (
  params?: TransferDeliveryRecordSummaryQueryParams,
) =>
  customInstance<SingleResultTransferDeliveryRecordSummaryDTO>({
    url: `/transfer-delivery-records/summary`,
    method: "GET",
    params,
  });
