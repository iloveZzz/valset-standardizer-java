import type { YTablePagination } from "@yss-ui/components";
import type {
  TransferRouteViewDTO,
  TransferRuleViewDTO,
  TransferSourceViewDTO,
  TransferTargetViewDTO,
} from "@/api/generated/valset/schemas";
import type { GetTemplateName1SourceType } from "@/api/generated/valset/schemas/getTemplateName1SourceType";
import type { GetTemplateName2TargetType } from "@/api/generated/valset/schemas/getTemplateName2TargetType";

export interface RouteFormState {
  routeId?: string;
  sourceId?: string;
  sourceCode: string;
  sourceType: string;
  targetCode: string;
  targetType: string;
  targetPath: string;
  ruleId?: string;
  renamePattern: string;
}

export type RouteSelectOption = {
  label: string;
  value: number | string;
};

export interface RouteFlowPreview {
  sourceTitle: string;
  sourceMeta: string;
  ruleTitle: string;
  ruleMeta: string;
  targetTitle: string;
  targetMeta: string;
  routeMetaSummary: string;
}

export interface RouteFlowStats {
  sourceCount: number;
  targetCount: number;
  routedCount: number;
  pendingCount: number;
}

export interface RouteFlowAnchor {
  label: string;
  detail: string;
}

export interface RouteFlowFactMessage {
  stage: "source" | "route" | "target";
  title: string;
  content: string;
  timeText: string;
}

export interface SourceIngestMessage {
  title: string;
  content: string;
  timeText: string;
}

export interface RouteFlowChainNode {
  key: string;
  title: string;
  statusKey: "success" | "error" | "loading" | "pending";
  statusLabel: string;
  content: string;
  timeText: string;
}

export interface RouteConfigPage {
  loading: boolean;
  rows: TransferRouteViewDTO[];
  tableData: TransferRouteViewDTO[];
  total: number;
  pageSize: number;
  pagination: YTablePagination;
  flowPreview: RouteFlowPreview;
  flowAnchor: RouteFlowAnchor;
  flowStats: RouteFlowStats;
  flowTableLegend: Array<{
    label: string;
    hint: string;
    tone: "source" | "route" | "target";
  }>;
  query: {
    sourceCode: string;
    sourceType: string;
    ruleId: string;
    targetCode: string;
    targetType: string;
    limit: number;
  };
  sourceTypeOptions: Array<{ label: string; value: GetTemplateName1SourceType }>;
  targetTypeOptions: Array<{ label: string; value: GetTemplateName2TargetType }>;
  sourceOptions: RouteSelectOption[];
  targetOptions: RouteSelectOption[];
  ruleOptions: RouteSelectOption[];
  selectLoading: boolean;
  sourceActionLoadingIds: Record<string, boolean>;
  sourceIngestStates: Record<
    string,
    {
      ingestBusy?: boolean;
      ingestFinishedAt?: string;
      ingestStartedAt?: string;
      ingestTriggerType?: string;
      ingestStatus?: string;
      processedCount?: number;
      progressPercent?: number;
      statusMessage?: string;
      totalCount?: number;
    }
  >;
  routeFlowFactMessages: Record<string, RouteFlowFactMessage[]>;
  sourceIngestMessages: Record<string, SourceIngestMessage[]>;
  getSourceIngestChainItems: (
    row: TransferRouteViewDTO | null,
  ) => RouteFlowChainNode[];
  formVisible: boolean;
  formMode: "create" | "edit";
  formState: RouteFormState;
  detailVisible: boolean;
  selectedRow: TransferRouteViewDTO | null;
  formSubmitting: boolean;
  openCreateDialog: () => Promise<void> | void;
  openEditDialog: (row: TransferRouteViewDTO) => Promise<void> | void;
  openDetailDrawer: (row: TransferRouteViewDTO) => Promise<void> | void;
  confirmDelete: (row: TransferRouteViewDTO) => void;
  getSourceIngestState: (row: TransferRouteViewDTO | null) => {
    ingestBusy?: boolean;
    ingestFinishedAt?: string;
    ingestStartedAt?: string;
    ingestTriggerType?: string;
    ingestStatus?: string;
    processedCount?: number;
    progressPercent?: number;
    statusMessage?: string;
    totalCount?: number;
  } | null;
  getSourceIngestStatusText: (row: TransferRouteViewDTO | null) => string;
  canTriggerSource: (row: TransferRouteViewDTO | null) => boolean;
  canStopSource: (row: TransferRouteViewDTO | null) => boolean;
  getSourceIngestProgressPercent: (row: TransferRouteViewDTO | null) => number;
  getSourceIngestProgressText: (row: TransferRouteViewDTO | null) => string;
  getRouteFlowFactMessages: (row: TransferRouteViewDTO | null) => RouteFlowFactMessage[];
  getSourceIngestMessages: (row: TransferRouteViewDTO | null) => SourceIngestMessage[];
  getRuleDisplayName: (ruleId?: string | number | null) => string;
  triggerSource: (row: TransferRouteViewDTO) => Promise<void> | void;
  stopSource: (row: TransferRouteViewDTO) => Promise<void> | void;
  isSourceTriggering: (sourceId?: string) => boolean;
  isSourceStopping: (sourceId?: string) => boolean;
  runQuery: () => Promise<void> | void;
  resetQuery: () => Promise<void> | void;
  submitForm: () => Promise<void> | void;
  closeForm: () => void;
  closeDetail: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleSourceChange: (value: unknown) => void;
  handleTargetChange: (value: unknown) => void;
  handleRuleChange: (value: unknown) => void;
  setFormRef: (instance: unknown) => void;
  copyRouteField: (value: string | number | undefined | null, label: string) => Promise<void>;
  formatRouteMetaSummary: (row: TransferRouteViewDTO | null) => string;
}

export type {
  TransferRuleViewDTO,
  TransferSourceViewDTO,
  TransferTargetViewDTO,
};
