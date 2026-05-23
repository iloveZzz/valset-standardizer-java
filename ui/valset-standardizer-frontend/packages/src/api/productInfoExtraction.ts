import { getJavaApi } from "./generated/valset";

const generatedApi = getJavaApi();

export const pageProductInfoExtractionCandidates = generatedApi.pageCandidates;
export const pageProductInfoOptions = generatedApi.pageProductOptions;
export const previewProductInfoExtraction = generatedApi.preview;
export const saveProductInfoExtractionRules = generatedApi.saveRules;

export type {
  MultiResultProductInfoExtractionPreviewDTO,
  PageCandidatesParams as ProductInfoExtractionCandidateParams,
  PageProductOptionsParams as ProductInfoOptionParams,
  PageResultProductInfoExtractionCandidateDTO,
  PageResultProductInfoOptionDTO,
  ProductInfoExtractionCandidateDTO,
  ProductInfoExtractionPreviewCommand,
  ProductInfoExtractionPreviewDTO,
  ProductInfoExtractionSaveCommand,
  ProductInfoExtractionSaveResultDTO,
  ProductInfoOptionDTO,
  SingleResultProductInfoExtractionSaveResultDTO,
} from "./generated/valset/schemas";
