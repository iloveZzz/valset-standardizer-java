import { getJavaApi } from "./generated/valset";

import type {
  FileParseRuleSheetRowDTO as GeneratedFileParseRuleSheetRowDTO,
  FileParseRuleSheetSaveCommand as GeneratedFileParseRuleSheetSaveCommand,
  FileParseSourceSheetRowDTO as GeneratedFileParseSourceSheetRowDTO,
  FileParseSourceSheetSaveCommand as GeneratedFileParseSourceSheetSaveCommand,
  ListFileParseRulesParams,
  ListFileParseSourcesParams,
  ListProductMatchRulesParams,
  ProductMatchRuleSheetRowDTO as GeneratedProductMatchRuleSheetRowDTO,
  ProductMatchRuleSheetSaveCommand as GeneratedProductMatchRuleSheetSaveCommand,
} from "./generated/valset/schemas";

const generatedApi = getJavaApi();

export type MultiResultFileParseSourceSheetRowDTO = {
  data?: FileParseSourceSheetRowDTO[];
};
export type MultiResultFileParseRuleSheetRowDTO = {
  data?: FileParseRuleSheetRowDTO[];
};
export type MultiResultProductMatchRuleSheetRowDTO = {
  data?: ProductMatchRuleSheetRowDTO[];
};

export const listParseIssueFileParseSources = (params?: FileParseSourceQueryParams) =>
  generatedApi.listFileParseSources(
    params,
  ) as Promise<MultiResultFileParseSourceSheetRowDTO>;
export const saveParseIssueFileParseSources =
  generatedApi.saveFileParseSources;
export const exportParseIssueFileParseSources =
  generatedApi.exportFileParseSources;
export const listParseIssueFileParseRules = (params?: FileParseRuleQueryParams) =>
  generatedApi.listFileParseRules(
    params,
  ) as Promise<MultiResultFileParseRuleSheetRowDTO>;
export const saveParseIssueFileParseRules = generatedApi.saveFileParseRules;
export const exportParseIssueFileParseRules = generatedApi.exportFileParseRules;
export const listParseIssueProductMatchRules = (
  params?: ProductMatchRuleQueryParams,
) =>
  generatedApi.listProductMatchRules(
    params,
  ) as Promise<MultiResultProductMatchRuleSheetRowDTO>;
export const saveParseIssueProductMatchRules =
  generatedApi.saveProductMatchRules;
export const exportParseIssueProductMatchRules =
  generatedApi.exportProductMatchRules;

export type FileParseSourceSheetRowDTO =
  GeneratedFileParseSourceSheetRowDTO & Record<string, unknown>;
export type FileParseRuleSheetRowDTO =
  GeneratedFileParseRuleSheetRowDTO & Record<string, unknown>;
export type ProductMatchRuleSheetRowDTO =
  GeneratedProductMatchRuleSheetRowDTO & Record<string, unknown>;
export type FileParseSourceSheetSaveCommand =
  GeneratedFileParseSourceSheetSaveCommand;
export type FileParseRuleSheetSaveCommand =
  GeneratedFileParseRuleSheetSaveCommand;
export type ProductMatchRuleSheetSaveCommand =
  GeneratedProductMatchRuleSheetSaveCommand;
export type FileParseSourceQueryParams = ListFileParseSourcesParams;
export type FileParseRuleQueryParams = ListFileParseRulesParams;
export type ProductMatchRuleQueryParams = ListProductMatchRulesParams;

export type {
  ParseIssueHandlingSaveResultDTO,
  SingleResultParseIssueHandlingSaveResultDTO,
} from "./generated/valset/schemas";
