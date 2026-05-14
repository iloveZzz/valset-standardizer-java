import { customInstance } from "./mutator";

export type TransferRunLogTrendQueryParams = {
  days?: number;
  taskDate?: string;
};

export type TransferRunLogTrendViewDTO = {
  trendDate?: string;
  count?: number;
};

export type MultiResultTransferRunLogTrendViewDTO = {
  data?: TransferRunLogTrendViewDTO[];
};

export const listTransferRunLogTrends = (
  params?: TransferRunLogTrendQueryParams,
) =>
  customInstance<MultiResultTransferRunLogTrendViewDTO>({
    url: `/transfer-run-logs/trend`,
    method: "GET",
    params,
  });
