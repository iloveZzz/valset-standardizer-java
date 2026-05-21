import { customInstance } from "./mutator";

export type FileParseSourceSheetRowDTO = {
  id?: string;
  fileType?: string;
  columnMap?: string;
  columnMapName?: string;
  columnName?: string;
  fileExtInfo?: string;
  status?: string;
  creater?: string;
  createTime?: string;
  modifier?: string;
  modifyTime?: string;
};

export type FileParseRuleSheetRowDTO = {
  id?: string;
  fileScene?: string;
  fileTypeName?: string;
  regionName?: string;
  columnMap?: string;
  columnMapName?: string;
  status?: string;
  multiIndex?: string;
  required?: string;
  creater?: string;
  createTime?: string;
  modifier?: string;
  modifyTime?: string;
};

export type ProductMatchRuleSheetRowDTO = {
  id?: string;
  fileTypeName?: string;
  pdCd?: string;
  pdNm?: string;
  orgCd?: string;
  orgNm?: string;
  pdType?: string;
  subjectSystem?: string;
  holdingStatus?: string;
  establishedDate?: string;
  effectiveFrequency?: string;
  delayDays?: string;
  approvalRequired?: string;
  fileType?: string;
  matchRules?: string;
  isValid?: string;
  memo?: string;
  debugName?: string;
  jobName?: string;
  jobScene?: string;
  creater?: string;
  createTime?: string;
  modifier?: string;
  modifyTime?: string;
};

export type ParseIssueHandlingSaveErrorDTO = {
  rowNumber?: number;
  id?: string;
  message?: string;
};

export type ParseIssueHandlingSaveResultDTO = {
  createdCount?: number;
  updatedCount?: number;
  deletedCount?: number;
  skippedCount?: number;
  failedCount?: number;
  errors?: ParseIssueHandlingSaveErrorDTO[];
};

export type FileParseSourceQueryParams = {
  fileType?: string;
  columnMap?: string;
  columnName?: string;
  status?: string;
};

export type FileParseRuleQueryParams = {
  fileScene?: string;
  fileTypeName?: string;
  regionName?: string;
  columnMap?: string;
  columnMapName?: string;
  status?: string;
};

export type ProductMatchRuleQueryParams = {
  pdCd?: string;
  pdNm?: string;
  orgNm?: string;
  fileType?: string;
  isValid?: string;
};

export type FileParseSourceSheetSaveCommand = {
  rows: FileParseSourceSheetRowDTO[];
  originalIds: string[];
};

export type FileParseRuleSheetSaveCommand = {
  rows: FileParseRuleSheetRowDTO[];
  originalIds: string[];
};

export type ProductMatchRuleSheetSaveCommand = {
  rows: ProductMatchRuleSheetRowDTO[];
  originalIds: string[];
};

export type MultiResultFileParseSourceSheetRowDTO = {
  data?: FileParseSourceSheetRowDTO[];
};

export type MultiResultFileParseRuleSheetRowDTO = {
  data?: FileParseRuleSheetRowDTO[];
};

export type MultiResultProductMatchRuleSheetRowDTO = {
  data?: ProductMatchRuleSheetRowDTO[];
};

export type SingleResultParseIssueHandlingSaveResultDTO = {
  data?: ParseIssueHandlingSaveResultDTO;
};

export const listParseIssueFileParseSources = (
  params: FileParseSourceQueryParams,
) =>
  customInstance<MultiResultFileParseSourceSheetRowDTO>({
    url: "/parse-issue-handling/file-parse-sources",
    method: "GET",
    params,
  });

export const saveParseIssueFileParseSources = (
  data: FileParseSourceSheetSaveCommand,
) =>
  customInstance<SingleResultParseIssueHandlingSaveResultDTO>({
    url: "/parse-issue-handling/file-parse-sources",
    method: "PUT",
    data,
  });

export const exportParseIssueFileParseSources = (
  params: FileParseSourceQueryParams,
) =>
  customInstance<Blob>({
    url: "/parse-issue-handling/file-parse-sources/export",
    method: "GET",
    params,
    responseType: "blob",
  });

export const listParseIssueFileParseRules = (
  params: FileParseRuleQueryParams,
) =>
  customInstance<MultiResultFileParseRuleSheetRowDTO>({
    url: "/parse-issue-handling/file-parse-rules",
    method: "GET",
    params,
  });

export const saveParseIssueFileParseRules = (
  data: FileParseRuleSheetSaveCommand,
) =>
  customInstance<SingleResultParseIssueHandlingSaveResultDTO>({
    url: "/parse-issue-handling/file-parse-rules",
    method: "PUT",
    data,
  });

export const exportParseIssueFileParseRules = (
  params: FileParseRuleQueryParams,
) =>
  customInstance<Blob>({
    url: "/parse-issue-handling/file-parse-rules/export",
    method: "GET",
    params,
    responseType: "blob",
  });

export const listParseIssueProductMatchRules = (
  params: ProductMatchRuleQueryParams,
) =>
  customInstance<MultiResultProductMatchRuleSheetRowDTO>({
    url: "/parse-issue-handling/product-match-rules",
    method: "GET",
    params,
  });

export const saveParseIssueProductMatchRules = (
  data: ProductMatchRuleSheetSaveCommand,
) =>
  customInstance<SingleResultParseIssueHandlingSaveResultDTO>({
    url: "/parse-issue-handling/product-match-rules",
    method: "PUT",
    data,
  });

export const exportParseIssueProductMatchRules = (
  params: ProductMatchRuleQueryParams,
) =>
  customInstance<Blob>({
    url: "/parse-issue-handling/product-match-rules/export",
    method: "GET",
    params,
    responseType: "blob",
  });
