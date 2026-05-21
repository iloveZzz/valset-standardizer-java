import { reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  pageProductInfoExtractionCandidates,
  pageProductInfoOptions,
  previewProductInfoExtraction,
  saveProductInfoExtractionRules,
  type ProductInfoExtractionPreviewDTO,
  type ProductInfoExtractionSaveResultDTO,
  type ProductInfoOptionDTO,
} from "@/api/productInfoExtraction";
import {
  unwrapMultiResult,
  unwrapSingleResult,
} from "@/utils/api-response";
import type {
  ProductInfoExtractionCandidateRow,
  ProductInfoExtractionPageState,
  ProductInfoExtractionOptionRow,
  ProductInfoExtractionPreviewRow,
  ProductInfoExtractionStep,
} from "../types";

const PAGE_SIZE = 10;

const defaultQuery = () => ({
  productType: "委外产品",
  extractionMode: "自动识别",
  extractionStrategy: "FILE_NAME",
  originalName: "",
});

const normalizeCandidateRow = (row: any): ProductInfoExtractionCandidateRow => ({
  ...row,
  transferId: String(row?.transferId ?? ""),
  originalName: String(row?.originalName ?? ""),
});

const normalizePreviewRow = (row: ProductInfoExtractionPreviewDTO): ProductInfoExtractionPreviewRow => ({
  ...row,
  transferId: String(row?.transferId ?? ""),
  originalName: String(row?.originalName ?? ""),
});

const normalizeOptionRow = (row: ProductInfoOptionDTO, index: number): ProductInfoExtractionOptionRow => ({
  ...row,
  key: `${String(row?.productCode ?? "")}__${String(row?.productName ?? "")}__${String(row?.managerName ?? "")}__${index}`,
  productCode: String(row?.productCode ?? ""),
  productName: String(row?.productName ?? ""),
  managerCode: String(row?.managerCode ?? ""),
  managerName: String(row?.managerName ?? ""),
});

const normalizeSelectionRows = <T>(payload: unknown): T[] => {
  if (Array.isArray(payload)) {
    return payload as T[];
  }
  if (payload && typeof payload === "object") {
    const record = payload as Record<string, unknown>;
    if (Array.isArray(record.records)) {
      return record.records as T[];
    }
    if (Array.isArray(record.selection)) {
      return record.selection as T[];
    }
    if (Array.isArray(record.checkedRecords)) {
      return record.checkedRecords as T[];
    }
    if (Array.isArray(record.rows)) {
      return record.rows as T[];
    }
  }
  return [];
};

export const useProductInfoExtractionPage = (): ProductInfoExtractionPageState => {
  const currentStep = ref<ProductInfoExtractionStep>(0);
  const loading = ref(false);
  const previewLoading = ref(false);
  const saving = ref(false);
  const rows = ref<ProductInfoExtractionCandidateRow[]>([]);
  const previewRows = ref<ProductInfoExtractionPreviewRow[]>([]);
  const productOptions = ref<ProductInfoExtractionOptionRow[]>([]);
  const result = ref<ProductInfoExtractionSaveResultDTO | null>(null);
  const selectedCandidateRows = ref<ProductInfoExtractionCandidateRow[]>([]);
  const selectedPreviewRows = ref<ProductInfoExtractionPreviewRow[]>([]);
  const query = reactive(defaultQuery());
  const pagination = reactive({
    current: 1,
    pageSize: PAGE_SIZE,
    total: 0,
  });

  const loadCandidates = async () => {
    loading.value = true;
    try {
      const response = await pageProductInfoExtractionCandidates({
        productType: query.productType,
        originalName: query.originalName,
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
      });
      rows.value = (response?.data ?? []).map(normalizeCandidateRow);
      pagination.total = Number(response?.totalCount ?? 0);
      selectedCandidateRows.value = selectedCandidateRows.value.filter((selected) =>
        rows.value.some((row) => row.transferId === selected.transferId),
      );
    } catch (error) {
      rows.value = [];
      pagination.total = 0;
      message.error("查询未匹配文件失败");
    } finally {
      loading.value = false;
    }
  };

  const validatePreviewRows = () => {
    const invalid = selectedPreviewRows.value.find(
      (row) =>
        !String(row.productCode ?? "").trim() ||
        !String(row.productName ?? "").trim() ||
        !String(row.matchRule ?? "").trim(),
    );
    if (invalid) {
      message.warning("产品代码、产品名称和匹配规则不能为空");
      return false;
    }
    return true;
  };

  const loadProductOptions = async (keyword?: string) => {
    try {
      const response = await pageProductInfoOptions({
        keyword,
        pageIndex: 0,
        pageSize: 20,
      });
      productOptions.value = (response?.data ?? []).map(normalizeOptionRow);
      return productOptions.value;
    } catch (error) {
      productOptions.value = [];
      return [];
    }
  };

  const findMatchingProductOption = (row: ProductInfoExtractionPreviewRow, field: "productName" | "productCode" | "managerName") => {
    const current = String(row[field] ?? "").trim();
    if (!current) {
      return null;
    }
    const normalized = current.toUpperCase();
    const direct = productOptions.value.find((option) => {
      if (field === "productName") {
        return String(option.productName ?? "").toUpperCase() === normalized;
      }
      if (field === "productCode") {
        return String(option.productCode ?? "").toUpperCase() === normalized;
      }
      return String(option.managerName ?? "").toUpperCase() === normalized;
    });
    return direct ?? null;
  };

  const applyProductOption = (row: ProductInfoExtractionPreviewRow, option: ProductInfoOptionDTO) => {
    row.productName = String(option.productName ?? row.productName ?? "");
    row.productCode = String(option.productCode ?? row.productCode ?? "");
    row.managerCode = String(option.managerCode ?? row.managerCode ?? "");
    row.managerName = String(option.managerName ?? row.managerName ?? "");
  };

  const pageState = reactive<ProductInfoExtractionPageState>({
    get currentStep() {
      return currentStep.value;
    },
    get loading() {
      return loading.value;
    },
    get previewLoading() {
      return previewLoading.value;
    },
    get saving() {
      return saving.value;
    },
    get rows() {
      return rows.value;
    },
    get previewRows() {
      return previewRows.value;
    },
    get productOptions() {
      return productOptions.value;
    },
    get result() {
      return result.value;
    },
    pagination,
    query,
    get selectedCandidateRows() {
      return selectedCandidateRows.value;
    },
    get selectedPreviewRows() {
      return selectedPreviewRows.value;
    },
    get canGoPreview() {
      return selectedCandidateRows.value.length > 0;
    },
    get canSave() {
      return selectedPreviewRows.value.length > 0;
    },
    runQuery() {
      pagination.current = 1;
      void loadCandidates();
    },
    resetQuery() {
      Object.assign(query, defaultQuery());
      pagination.current = 1;
      void loadCandidates();
    },
    handlePageChange(params) {
      pagination.current = params.current;
      pagination.pageSize = params.pageSize;
      void loadCandidates();
    },
    handleCandidateSelectionChange(nextRows) {
      selectedCandidateRows.value = normalizeSelectionRows<ProductInfoExtractionCandidateRow>(nextRows);
    },
    handlePreviewSelectionChange(nextRows) {
      selectedPreviewRows.value = normalizeSelectionRows<ProductInfoExtractionPreviewRow>(nextRows);
    },
    deletePreviewRow(transferId) {
      const normalizedId = String(transferId ?? "").trim();
      if (!normalizedId) {
        return;
      }
      previewRows.value = previewRows.value.filter((row) => row.transferId !== normalizedId);
      selectedPreviewRows.value = selectedPreviewRows.value.filter((row) => row.transferId !== normalizedId);
    },
    async loadProductOptions(keyword) {
      return loadProductOptions(keyword);
    },
    syncPreviewRowByField(row, field, value) {
      const nextValue = String(value ?? "");
      row[field] = nextValue;
      const option = findMatchingProductOption({ ...row, [field]: nextValue }, field);
      if (option) {
        applyProductOption(row, option);
      }
    },
    async goPreview() {
      if (!selectedCandidateRows.value.length) {
        message.warning("请选择文件信息");
        return;
      }
      previewLoading.value = true;
      try {
        const response = await previewProductInfoExtraction({
          productType: query.productType,
          extractionStrategy: query.extractionStrategy,
          transferIds: selectedCandidateRows.value.map((row) => row.transferId),
        });
        previewRows.value = unwrapMultiResult(response).map(normalizePreviewRow);
        selectedPreviewRows.value = [...previewRows.value];
        await loadProductOptions();
        currentStep.value = 1;
      } catch (error) {
        previewRows.value = [];
        selectedPreviewRows.value = [];
        message.error("提取产品信息失败");
      } finally {
        previewLoading.value = false;
      }
    },
    goCandidate() {
      currentStep.value = 0;
    },
    async saveRules() {
      if (!selectedPreviewRows.value.length) {
        message.warning("请选择需要保存的产品信息");
        return;
      }
      if (!validatePreviewRows()) {
        return;
      }
      saving.value = true;
      try {
        const response = await saveProductInfoExtractionRules({
          items: selectedPreviewRows.value,
        });
        result.value = unwrapSingleResult(response) ?? null;
        currentStep.value = 2;
        message.success("产品规则保存完成");
      } catch (error) {
        message.error("保存产品规则失败");
      } finally {
        saving.value = false;
      }
    },
    startOver() {
      currentStep.value = 0;
      previewRows.value = [];
      selectedPreviewRows.value = [];
      productOptions.value = [];
      result.value = null;
      void loadCandidates();
    },
  });

  void loadCandidates();

  return pageState;
};
