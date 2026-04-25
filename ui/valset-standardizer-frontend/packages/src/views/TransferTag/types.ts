import type { YTablePagination } from "@yss-ui/components";

export interface TagViewDTO {
  tagId?: string;
  tagCode?: string;
  tagName?: string;
  tagValue?: string;
  enabled?: boolean;
  priority?: number;
  matchStrategy?: string;
  scriptLanguage?: string;
  scriptBody?: string;
  regexPattern?: string;
  tagMeta?: Record<string, any> | string;
  formTemplateName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TagMutationResponse {
  operation?: string;
  message?: string;
  formTemplateName?: string;
  tag?: TagViewDTO;
}

export interface TagTestResultDTO {
  tagId?: string;
  matched?: boolean;
  matchStrategy?: string;
  matchReason?: string;
  matchedByScript?: boolean;
  matchedByRegex?: boolean;
  matchedField?: string;
  matchedValue?: string;
  contextSnapshot?: Record<string, any>;
  errorMessage?: string;
}

export interface TagPage {
  loading: boolean;
  rows: TagViewDTO[];
  tableData: TagViewDTO[];
  total: number;
  pageSize: number;
  pagination: YTablePagination;
  query: {
    tagCode: string;
    enabled: string;
    matchStrategy: string;
  };
  enabledUpdatingIds: Record<string, boolean>;
  isEnabledUpdating: (tagId?: string) => boolean;
  toggleEnabled: (row: TagViewDTO, checked: boolean) => Promise<void> | void;
  templateNamePreview: string;
  templateDescription: string;
  templateVersion: string;
  templateMode: 0 | 1 | 2;
  templateReadPretty: boolean;
  templateSchema: any | null;
  templateInitialValues: Record<string, any>;
  templateValues: Record<string, any>;
  templateScope: Record<string, any>;
  templateLoading: boolean;
  templateDetailOptions: Record<string, any>;
  templateGridDefaults: Record<string, any>;
  setTemplateFormRef: (instance: any) => void;
  formVisible: boolean;
  formMode: "create" | "edit";
  formSubmitting: boolean;
  detailVisible: boolean;
  selectedRow: TagViewDTO | null;
  testVisible: boolean;
  testSubmitting: boolean;
  testState: {
    sourceType: string;
    sourceCode: string;
    fileName: string;
    mimeType: string;
    fileSize: string;
    sender: string;
    subject: string;
    path: string;
    mailFolder: string;
    body: string;
    attributesText: string;
  };
  testResult: TagTestResultDTO | null;
  testTag: TagViewDTO | null;
  openCreateDialog: () => void;
  openEditDialog: (row: TagViewDTO) => Promise<void> | void;
  openDetailDrawer: (row: TagViewDTO) => void;
  openTestDrawer: (row: TagViewDTO) => void;
  confirmDelete: (row: TagViewDTO) => void;
  runQuery: () => void;
  resetQuery: () => void;
  submitForm: () => Promise<void> | void;
  closeForm: () => void;
  closeDetail: () => void;
  submitTest: () => Promise<void> | void;
  closeTest: () => void;
  formatEnabled: (value: boolean | undefined) => string;
  formatMatchStrategy: (value?: string) => string;
  resolveScriptEditorLanguage: (value: string | undefined) => string;
  resetScriptBody: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
}
