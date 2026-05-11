import type { YTablePagination } from "@yss-ui/components";
import type {
  EtlPlatformType,
  WorkflowDefinitionDTO,
  WorkflowEngineBindingDTO,
  WorkflowPlatformMetadataDTO,
  WorkflowStageDTO,
  WorkflowSyncStatus,
} from "@/api/etlWorkflowConfig";

export type EtlWorkflowStageFormState = WorkflowStageDTO & {
  __rowKey: string;
};

export type EtlWorkflowEngineBindingFormState = WorkflowEngineBindingDTO & {
  attributesText: string;
};

export type EtlWorkflowDefinitionFormState = WorkflowDefinitionDTO & {
  stages: EtlWorkflowStageFormState[];
  engineBinding: EtlWorkflowEngineBindingFormState;
};

export type EtlWorkflowDefinitionRow = WorkflowDefinitionDTO & {
  workflowKey: string;
  stageCount: number;
  bindingSummary: string;
  platformLabel: string;
  statusLabel: string;
  syncStatus: WorkflowSyncStatus | null | undefined;
  syncStatusLabel: string;
  externalOnline: boolean | null | undefined;
  externalReleaseState: string | null | undefined;
  externalStateLabel: string;
  syncTimeLabel: string;
  syncFailureReason?: string | null;
  supportedOperations: string[];
};

export type EtlWorkflowConfigPage = {
  loading: boolean;
  saving: boolean;
  validating: boolean;
  definitionRows: EtlWorkflowDefinitionRow[];
  tableData: EtlWorkflowDefinitionRow[];
  platformOptions: Array<{
    label: string;
    value: EtlPlatformType;
    description: string;
  }>;
  platformMetadataList: WorkflowPlatformMetadataDTO[];
  formState: EtlWorkflowDefinitionFormState;
  query: {
    keyword: string;
    platformType: string;
    enabled: string;
  };
  pagination: YTablePagination;
  totalCount: number;
  filteredCount: number;
  enabledCount: number;
  platformCount: number;
  stageCount: number;
  summaryText: string;
  currentBoundaryText: string;
  runQuery: () => Promise<void>;
  applyQuery: () => void;
  resetQuery: () => void;
  resetForm: () => void;
  loadDefinition: (row: WorkflowDefinitionDTO | null) => Promise<void>;
  createWorkflowDraft: (row?: WorkflowDefinitionDTO | null) => void;
  saveDefinition: () => Promise<boolean>;
  syncDefinition: (
    definition?: WorkflowDefinitionDTO | null,
  ) => Promise<boolean>;
  onlineDefinition: (
    definition?: WorkflowDefinitionDTO | null,
  ) => Promise<boolean>;
  offlineDefinition: (
    definition?: WorkflowDefinitionDTO | null,
  ) => Promise<boolean>;
  deleteDefinition: (
    definition?: WorkflowDefinitionDTO | null,
  ) => Promise<boolean>;
  addStageFromDraft: (stage: Partial<EtlWorkflowStageFormState>) => void;
  validateDefinition: () => Promise<void>;
  refreshPlatforms: () => Promise<void>;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  addStage: () => void;
  removeStage: (index: number) => void;
  moveStage: (fromIndex: number, toIndex: number) => void;
  getStageIndexByKey: (rowKey: string) => number;
  moveStageByKey: (rowKey: string, direction: -1 | 1) => void;
  removeStageByKey: (rowKey: string) => void;
  setBindingPlatformType: (value: EtlPlatformType) => void;
  formatPlatformLabel: (value?: EtlPlatformType | string | null) => string;
  getPlatformMetadata: (
    value?: EtlPlatformType | string | null,
  ) => WorkflowPlatformMetadataDTO | null;
};
