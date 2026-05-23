import type { YTablePagination } from "@yss-ui/components";
import type {
  ProductInfoExtractionCandidateDTO,
  ProductInfoExtractionPreviewDTO,
  ProductInfoExtractionSaveResultDTO,
  ProductInfoOptionDTO,
} from "@/api/productInfoExtraction";

export type ProductInfoExtractionStep = 0 | 1 | 2;

export type ProductInfoExtractionCandidateRow = ProductInfoExtractionCandidateDTO & {
  transferId: string;
  originalName: string;
};

export type ProductInfoExtractionPreviewRow = ProductInfoExtractionPreviewDTO & {
  transferId: string;
  originalName: string;
};

export type ProductInfoExtractionOptionRow = ProductInfoOptionDTO & {
  key: string;
};

export type ProductInfoExtractionQueryState = {
  productType: string;
  extractionMode: string;
  extractionStrategy: string;
  originalName: string;
};

export type ProductInfoExtractionPageState = {
  currentStep: ProductInfoExtractionStep;
  loading: boolean;
  previewLoading: boolean;
  saving: boolean;
  rows: ProductInfoExtractionCandidateRow[];
  previewRows: ProductInfoExtractionPreviewRow[];
  productOptions: ProductInfoExtractionOptionRow[];
  result: ProductInfoExtractionSaveResultDTO | null;
  pagination: YTablePagination;
  query: ProductInfoExtractionQueryState;
  selectedCandidateRows: ProductInfoExtractionCandidateRow[];
  selectedPreviewRows: ProductInfoExtractionPreviewRow[];
  canGoPreview: boolean;
  canSave: boolean;
  runQuery: () => void;
  resetQuery: () => void;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
  handleCandidateSelectionChange: (rows: unknown) => void;
  handlePreviewSelectionChange: (rows: unknown) => void;
  deletePreviewRow: (transferId: string) => void;
  loadProductOptions: (keyword?: string) => Promise<ProductInfoExtractionOptionRow[]>;
  syncPreviewRowValue: (
    row: ProductInfoExtractionPreviewRow,
    field: keyof ProductInfoExtractionPreviewRow,
    value: unknown,
  ) => void;
  syncPreviewRowByField: (
    row: ProductInfoExtractionPreviewRow,
    field: "productName" | "productCode" | "managerName",
    value: string,
  ) => void;
  goPreview: () => Promise<void>;
  goCandidate: () => void;
  saveRules: () => Promise<void>;
  startOver: () => void;
};
