import type { YTablePagination } from "@yss-ui/components";
import type { ComponentPublicInstance } from "vue";
import type {
  TransferObjectAnalysisViewDTO,
  TransferObjectViewDTO as GeneratedTransferObjectViewDTO,
} from "@/api/generated/valset/schemas";

export type InboxAttachmentViewDTO = Pick<
  GeneratedTransferObjectViewDTO,
  | "transferId"
  | "originalName"
  | "localTempPath"
  | "realStoragePath"
  | "mimeType"
  | "sizeBytes"
>;

export type InboxMailViewDTO = GeneratedTransferObjectViewDTO & {
  primaryTransferId?: string;
  transferIds?: string[];
  attachments?: InboxAttachmentViewDTO[];
  attachmentCount?: number;
  tags?: Array<{
    id?: string;
    transferId?: string;
    tagCode?: string;
    tagName?: string;
    tagValue?: string;
  }>;
};

export type InboxFolderStat = {
  mailFolder?: string;
  mailFolderLabel: string;
  count: number;
};

export type InboxQueryState = {
  keyword: string;
  mailId: string;
  sourceCode: string;
  mailFolder: string;
  deliveryStatus: string;
};

export type InboxPage = {
  loading: boolean;
  loadingMore: boolean;
  analysisLoading: boolean;
  detailLoading: boolean;
  downloadLoading: boolean;
  hasMoreRows: boolean;
  mailBodyMode: "fit" | "raw";
  rows: InboxMailViewDTO[];
  filteredRows: InboxMailViewDTO[];
  selectedRow: InboxMailViewDTO | null;
  query: InboxQueryState;
  analysis: TransferObjectAnalysisViewDTO | null;
  folderStats: InboxFolderStat[];
  total: number;
  filteredTotal: number;
  pagination: YTablePagination;
  refreshInbox: () => void;
  resetQuery: () => void;
  selectFolder: (mailFolder?: string) => void;
  selectDeliveryStatus: (deliveryStatus?: string) => void;
  setListContainerRef: (
    el: Element | ComponentPublicInstance | null,
  ) => void;
  handleListScroll: (event: Event) => void;
  selectRow: (row: InboxMailViewDTO) => void;
  downloadRow: (row?: InboxMailViewDTO | null) => Promise<void>;
  downloadAttachment: (row: InboxAttachmentViewDTO) => Promise<void>;
  setMailBodyMode: (mode: "fit" | "raw") => void;
  formatDateTime: (value?: string) => string;
  formatBytes: (value?: string | number | null) => string;
  formatDeliveryStatus: (value?: string) => string;
  getSenderInitial: (row?: InboxMailViewDTO | null) => string;
  getPreviewText: (row?: InboxMailViewDTO | null) => string;
  getFolderLabel: (value?: string | null) => string;
  getAttachmentLabel: (row?: InboxMailViewDTO | null) => string;
  renderMailBodySrcdoc: (value?: string | null) => string;
  safeJson: (value: unknown) => string;
};
