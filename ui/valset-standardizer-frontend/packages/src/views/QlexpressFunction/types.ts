import type { YTablePagination } from "@yss-ui/components";
import type {
  QlexpressFunctionDebugResultDTO,
  QlexpressFunctionUsageDTO,
  QlexpressFunctionViewDTO,
} from "@/api/qlexpressFunction";

export type QlexpressFunctionQueryState = {
  functionCnName: string;
  functionName: string;
  enabled: string;
};

export type QlexpressFunctionDebugState = {
  debugExpression: string;
  contextText: string;
};

export interface QlexpressFunctionPageState {
  loading: boolean;
  rows: QlexpressFunctionViewDTO[];
  tableData: QlexpressFunctionViewDTO[];
  total: number;
  pagination: YTablePagination;
  query: QlexpressFunctionQueryState;
  selectedRow: QlexpressFunctionViewDTO | null;
  selectedUsage: QlexpressFunctionUsageDTO | null;
  usageLoading: boolean;
  detailVisible: boolean;
  formVisible: boolean;
  formMode: "create" | "edit";
  formSubmitting: boolean;
  templateValues: Record<string, any>;
  templateSchema: any;
  templateInitialValues: Record<string, any>;
  templateMode: 0 | 1 | 2;
  templateReadPretty: boolean;
  templateScope: Record<string, any>;
  templateDetailOptions: Record<string, any>;
  templateGridDefaults: Record<string, any>;
  setTemplateFormRef: (instance: unknown) => void;
  debugVisible: boolean;
  debugSubmitting: boolean;
  debugTarget: QlexpressFunctionViewDTO | null;
  debugState: QlexpressFunctionDebugState;
  debugResult: QlexpressFunctionDebugResultDTO | null;
  operatingIds: Record<string, boolean>;
  isOperating: (functionId?: string) => boolean;
  isActive: (row?: QlexpressFunctionViewDTO | null) => boolean;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  openCreateDialog: () => void;
  openEditDialog: (row: QlexpressFunctionViewDTO) => void;
  closeForm: () => void;
  submitForm: () => Promise<void> | void;
  openDetailDrawer: (row: QlexpressFunctionViewDTO) => void;
  openUsageDrawer: (row: QlexpressFunctionViewDTO) => void;
  closeDetail: () => void;
  confirmDelete: (row: QlexpressFunctionViewDTO) => void;
  toggleEnabled: (row: QlexpressFunctionViewDTO, checked: boolean) => Promise<void> | void;
  openDebugDrawer: (row?: QlexpressFunctionViewDTO | null) => void;
  closeDebug: () => void;
  submitDebug: () => Promise<void> | void;
  formatEnabled: (value?: boolean) => string;
  resolveScriptEditorLanguage: (value?: string) => string;
}
