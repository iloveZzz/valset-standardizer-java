import { customInstance } from "./mutator";

export type TransferObjectTrendQueryParams = {
  days?: number;
  taskDate?: string;
};

export type TransferObjectTrendViewDTO = {
  trendDate?: string;
  deliveredCount?: number;
  undeliveredCount?: number;
};

export type MultiResultTransferObjectTrendViewDTO = {
  data?: TransferObjectTrendViewDTO[];
};

export const listTransferObjectTrends = (
  params?: TransferObjectTrendQueryParams,
) =>
  customInstance<MultiResultTransferObjectTrendViewDTO>({
    url: `/transfer-objects/trend`,
    method: "GET",
    params,
  });
