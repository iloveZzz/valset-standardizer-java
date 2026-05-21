import type { IWorkbookData } from "@univerjs/presets";
import type {
  FileParseSourceSheetRowDTO,
  FileParseRuleSheetRowDTO,
  ParseIssueHandlingSaveResultDTO,
  ProductMatchRuleSheetRowDTO,
} from "@/api/parseIssueHandling";

export type ParseIssueHandlingTab =
  | "fileParseSource"
  | "fileParseRule"
  | "productMatchRule"
  | "productInfoExtraction";

export type ParseIssueHandlingSheetColumn<T extends Record<string, unknown> = Record<string, unknown>> = {
  field: keyof T & string;
  title: string;
  width?: number;
  readonly?: boolean;
  hidden?: boolean;
};

export type ParseIssueHandlingPageState = {
  activeTab: ParseIssueHandlingTab;
  loading: boolean;
  saving: boolean;
  fileParseSources: FileParseSourceSheetRowDTO[];
  fileParseRules: FileParseRuleSheetRowDTO[];
  productMatchRules: ProductMatchRuleSheetRowDTO[];
  fileParseSourceOriginalIds: string[];
  fileParseRuleOriginalIds: string[];
  productMatchRuleOriginalIds: string[];
  fileParseSourceQuery: {
    fileType: string;
    columnMap: string;
    columnName: string;
    status: string;
  };
  fileParseRuleQuery: {
    fileScene: string;
    fileTypeName: string;
    regionName: string;
    columnMap: string;
    columnMapName: string;
    status: string;
  };
  productMatchRuleQuery: {
    pdCd: string;
    pdNm: string;
    orgNm: string;
    fileType: string;
    isValid: string;
  };
  lastResult: ParseIssueHandlingSaveResultDTO | null;
  exporting: boolean;
  setActiveTab: (tab: ParseIssueHandlingTab) => void;
  refreshCurrent: () => Promise<void>;
  loadFileParseSources: () => Promise<void>;
  loadProductMatchRules: () => Promise<void>;
  resetCurrentQuery: () => void;
  saveCurrent: (workbookData: IWorkbookData | null) => Promise<void>;
  exportCurrent: () => Promise<void>;
};
