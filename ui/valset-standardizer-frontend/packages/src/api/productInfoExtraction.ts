import { customInstance } from "./mutator";

export type ProductInfoExtractionCandidateDTO = {
  transferId?: string;
  originalName?: string;
  sourceType?: string;
  sourceCode?: string;
  status?: string;
  deliveryStatus?: string;
  valuationTagName?: string;
  receiveMode?: string;
  receivedAt?: string;
};

export type ProductInfoExtractionPreviewDTO = {
  transferId?: string;
  originalName?: string;
  productType?: string;
  subjectSystem?: string;
  managerCode?: string;
  managerName?: string;
  holdingStatus?: string;
  establishedDate?: string;
  productCode?: string;
  productName?: string;
  matchRule?: string;
  effectiveFrequency?: string;
  delayDays?: number;
  approvalRequired?: boolean;
};

export type ProductInfoExtractionSaveItemDTO = {
  transferId?: string;
  originalName?: string;
  productCode?: string;
  productName?: string;
  ruleId?: string;
  status?: string;
  message?: string;
};

export type ProductInfoExtractionSaveResultDTO = {
  successCount?: number;
  skippedCount?: number;
  failedCount?: number;
  items?: ProductInfoExtractionSaveItemDTO[];
};

export type ProductInfoOptionDTO = {
  productCode?: string;
  productName?: string;
  managerCode?: string;
  managerName?: string;
};

export type ProductInfoExtractionCandidateParams = {
  productType?: string;
  originalName?: string;
  pageIndex?: number;
  pageSize?: number;
};

export type ProductInfoExtractionPreviewCommand = {
  productType?: string;
  extractionStrategy?: string;
  transferIds: string[];
};

export type ProductInfoExtractionSaveCommand = {
  items: ProductInfoExtractionPreviewDTO[];
};

export type PageResultProductInfoExtractionCandidateDTO = {
  data?: ProductInfoExtractionCandidateDTO[];
  totalCount?: number;
  pageSize?: number;
  pageIndex?: number;
};

export type PageResultProductInfoOptionDTO = {
  data?: ProductInfoOptionDTO[];
  totalCount?: number;
  pageSize?: number;
  pageIndex?: number;
};

export type MultiResultProductInfoExtractionPreviewDTO = {
  data?: ProductInfoExtractionPreviewDTO[];
};

export type SingleResultProductInfoExtractionSaveResultDTO = {
  data?: ProductInfoExtractionSaveResultDTO;
};

export const pageProductInfoExtractionCandidates = (
  params: ProductInfoExtractionCandidateParams,
) =>
  customInstance<PageResultProductInfoExtractionCandidateDTO>({
    url: "/product-info-extractions/candidates",
    method: "GET",
    params,
  });

export const pageProductInfoOptions = (
  params: { keyword?: string; pageIndex?: number; pageSize?: number },
) =>
  customInstance<PageResultProductInfoOptionDTO>({
    url: "/product-info-extractions/options",
    method: "GET",
    params,
  });

export const previewProductInfoExtraction = (
  data: ProductInfoExtractionPreviewCommand,
) =>
  customInstance<MultiResultProductInfoExtractionPreviewDTO>({
    url: "/product-info-extractions/preview",
    method: "POST",
    data,
  });

export const saveProductInfoExtractionRules = (
  data: ProductInfoExtractionSaveCommand,
) =>
  customInstance<SingleResultProductInfoExtractionSaveResultDTO>({
    url: "/product-info-extractions/rules",
    method: "POST",
    data,
  });
