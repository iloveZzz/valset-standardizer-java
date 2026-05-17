import { customInstance } from "./mutator";

export type TransferObjectTagSummaryQueryParams = {
  taskDate?: string;
};

export type TransferObjectTagSummaryViewDTO = {
  tagCode?: string;
  tagName?: string;
  tagCount?: number;
};

export type MultiResultTransferObjectTagSummaryViewDTO = {
  data?: TransferObjectTagSummaryViewDTO[];
};

export const listTransferObjectTagSummaries = (
  params?: TransferObjectTagSummaryQueryParams,
) =>
  customInstance<MultiResultTransferObjectTagSummaryViewDTO>({
    url: `/transfer-objects/tag-summary`,
    method: "GET",
    params,
  });
