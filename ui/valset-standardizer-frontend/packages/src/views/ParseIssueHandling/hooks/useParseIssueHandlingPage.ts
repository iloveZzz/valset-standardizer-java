import { reactive, ref } from "vue";
import type { IWorkbookData } from "@univerjs/presets";
import { message } from "ant-design-vue";
import {
  exportParseIssueFileParseRules,
  exportParseIssueFileParseSources,
  exportParseIssueProductMatchRules,
  listParseIssueFileParseSources,
  listParseIssueFileParseRules,
  listParseIssueProductMatchRules,
  saveParseIssueFileParseSources,
  saveParseIssueFileParseRules,
  saveParseIssueProductMatchRules,
  type FileParseSourceSheetRowDTO,
  type FileParseRuleSheetRowDTO,
  type ProductMatchRuleSheetRowDTO,
} from "@/api/parseIssueHandling";
import {
  unwrapMultiResult,
  unwrapSingleResult,
} from "@/utils/api-response";
import type {
  ParseIssueHandlingPageState,
  ParseIssueHandlingSheetColumn,
  ParseIssueHandlingTab,
} from "../types";

const fileParseSourceQueryDefault = () => ({
  fileType: "",
  columnMap: "",
  columnName: "",
  status: "",
});

const fileParseRuleQueryDefault = () => ({
  fileScene: "",
  fileTypeName: "",
  regionName: "",
  columnMap: "",
  columnMapName: "",
  status: "",
});

const productMatchRuleQueryDefault = () => ({
  pdCd: "",
  pdNm: "",
  orgNm: "",
  fileType: "",
  isValid: "",
});

const normalizeText = (value: unknown) => {
  if (value === undefined || value === null || value === "-") {
    return "";
  }
  return String(value).trim();
};

const parseContentDispositionFileName = (value: string | undefined) => {
  if (!value) {
    return "";
  }
  const utf8FileName = value.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8FileName?.[1]) {
    try {
      return decodeURIComponent(utf8FileName[1].trim());
    } catch {
      return utf8FileName[1].trim();
    }
  }
  const plainFileName = value.match(/filename="?([^";]+)"?/i);
  return plainFileName?.[1]?.trim() ?? "";
};

const triggerBrowserDownload = (blob: Blob, fileName: string) => {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = fileName || "解析问题处理.xlsx";
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
};

const splitColumnMapDisplayValue = (value: unknown) => {
  const text = normalizeText(value);
  if (!text) {
    return { columnMap: "", columnMapName: "" };
  }
  const separatorIndex = text.indexOf("｜");
  if (separatorIndex < 0) {
    return { columnMap: text, columnMapName: "" };
  }
  return {
    columnMap: normalizeText(text.slice(0, separatorIndex)),
    columnMapName: normalizeText(text.slice(separatorIndex + 1)),
  };
};

const normalizeFileParseSourceRow = (
  row: FileParseSourceSheetRowDTO,
): FileParseSourceSheetRowDTO => ({
  ...row,
  id: normalizeText(row.id),
  fileType: normalizeText(row.fileType),
  columnMap: splitColumnMapDisplayValue(row.columnMap).columnMap,
  columnMapName: normalizeText(row.columnMapName),
  columnName: normalizeText(row.columnName),
  fileExtInfo: normalizeText(row.fileExtInfo),
  status: normalizeText(row.status),
  creater: normalizeText(row.creater),
  createTime: normalizeText(row.createTime),
  modifier: normalizeText(row.modifier),
  modifyTime: normalizeText(row.modifyTime),
});

const normalizeFileParseRuleRow = (
  row: FileParseRuleSheetRowDTO,
): FileParseRuleSheetRowDTO => ({
  ...row,
  id: normalizeText(row.id),
  fileScene: normalizeText(row.fileScene),
  fileTypeName: normalizeText(row.fileTypeName),
  regionName: normalizeText(row.regionName),
  columnMap: splitColumnMapDisplayValue(row.columnMap).columnMap,
  columnMapName: normalizeText(row.columnMapName),
  status: normalizeText(row.status),
  multiIndex: normalizeText(row.multiIndex),
  required: normalizeText(row.required),
  creater: normalizeText(row.creater),
  createTime: normalizeText(row.createTime),
  modifier: normalizeText(row.modifier),
  modifyTime: normalizeText(row.modifyTime),
});

const normalizeProductMatchRuleRow = (
  row: ProductMatchRuleSheetRowDTO,
): ProductMatchRuleSheetRowDTO => ({
  ...row,
  id: normalizeText(row.id),
  fileTypeName: normalizeText(row.fileTypeName),
  pdCd: normalizeText(row.pdCd),
  pdNm: normalizeText(row.pdNm),
  orgCd: normalizeText(row.orgCd),
  orgNm: normalizeText(row.orgNm),
  pdType: normalizeText(row.pdType),
  subjectSystem: normalizeText(row.subjectSystem),
  holdingStatus: normalizeText(row.holdingStatus),
  establishedDate: normalizeText(row.establishedDate),
  effectiveFrequency: normalizeText(row.effectiveFrequency),
  delayDays: normalizeText(row.delayDays),
  approvalRequired: normalizeText(row.approvalRequired),
  fileType: normalizeText(row.fileType),
  matchRules: normalizeText(row.matchRules),
  isValid: normalizeText(row.isValid),
  memo: normalizeText(row.memo),
  debugName: normalizeText(row.debugName),
  jobName: normalizeText(row.jobName),
  jobScene: normalizeText(row.jobScene),
  creater: normalizeText(row.creater),
  createTime: normalizeText(row.createTime),
  modifier: normalizeText(row.modifier),
  modifyTime: normalizeText(row.modifyTime),
});

const collectIds = (rows: Array<{ id?: string }>) =>
  rows
    .map((row) => normalizeText(row.id))
    .filter(Boolean);

const firstWorkbookSheet = (workbookData: IWorkbookData | null | undefined) => {
  if (!workbookData?.sheets) {
    return null;
  }
  const sheetId = workbookData.sheetOrder?.[0] || Object.keys(workbookData.sheets)[0];
  return sheetId ? workbookData.sheets[sheetId] : null;
};

const isBlankRow = (row: Record<string, string>) =>
  Object.values(row).every((value) => !normalizeText(value));

const extractRowsFromWorkbook = <T extends Record<string, unknown>>(
  workbookData: IWorkbookData | null,
  columns: ParseIssueHandlingSheetColumn<T>[],
): T[] => {
  const sheet = firstWorkbookSheet(workbookData);
  const cellData = sheet?.cellData ?? {};
  const rowIndexes = Object.keys(cellData)
    .map((key) => Number(key))
    .filter((value) => Number.isFinite(value) && value > 0)
    .sort((left, right) => left - right);

  return rowIndexes
    .map((rowIndex) => {
      const rowCells = cellData[rowIndex] ?? {};
      const row = columns.reduce((record, column, columnIndex) => {
        const cell = rowCells[columnIndex];
        record[column.field] = normalizeText(cell?.v ?? cell?.p ?? cell?.f);
        return record;
      }, {} as Record<string, string>);
      return row;
    })
    .filter((row) => !isBlankRow(row)) as T[];
};

const buildOriginalRowMap = <T extends { id?: string }>(rows: T[]) =>
  new Map(
    rows
      .map((row) => [normalizeText(row.id), row] as const)
      .filter(([id]) => Boolean(id)),
  );

const isSameRowValue = (left: unknown, right: unknown) =>
  normalizeText(left) === normalizeText(right);

const isChangedRow = <T extends Record<string, unknown>>(
  currentRow: T,
  originalRow: T | undefined,
  columns: ParseIssueHandlingSheetColumn<T>[],
) => {
  if (!originalRow) {
    return true;
  }
  return columns.some((column) => {
    if (column.field === "id" || column.readonly) {
      return false;
    }
    return !isSameRowValue(currentRow[column.field], originalRow[column.field]);
  });
};

export type ParseIssueHandlingChangeSummary<T extends { id?: string } = { id?: string } & Record<string, unknown>> = {
  rows: T[];
  createdRows: T[];
  updatedRows: T[];
  deletedRows: T[];
};

export const summarizeSheetChanges = <T extends Record<string, unknown> & { id?: string }>(
  rows: T[],
  originalRows: T[],
  columns: ParseIssueHandlingSheetColumn<T>[],
): ParseIssueHandlingChangeSummary<T> => {
  const originalMap = buildOriginalRowMap(originalRows);
  const currentIdSet = new Set<string>();
  const createdRows: T[] = [];
  const updatedRows: T[] = [];

  rows.forEach((row) => {
    const id = normalizeText(row.id);
    if (!id) {
      createdRows.push(row);
      return;
    }
    currentIdSet.add(id);
    if (isChangedRow(row, originalMap.get(id) as T | undefined, columns)) {
      updatedRows.push(row);
    }
  });

  const deletedRows = originalRows.filter((row) => {
    const id = normalizeText(row.id);
    return Boolean(id) && !currentIdSet.has(id);
  });

  return {
    rows,
    createdRows,
    updatedRows,
    deletedRows,
  };
};

export const fileParseSourceColumns: ParseIssueHandlingSheetColumn<FileParseSourceSheetRowDTO>[] = [
  { field: "id", title: "ID", width: 0, readonly: true, hidden: true },
  { field: "fileType", title: "文件类型", width: 130 },
  { field: "columnMap", title: "标准列编码", width: 160 },
  { field: "columnMapName", title: "标准列名称", width: 180, readonly: true },
  { field: "columnName", title: "来源列名称", width: 220 },
  { field: "fileExtInfo", title: "扩展信息", width: 220 },
  { field: "status", title: "启用状态", width: 100 },
  { field: "creater", title: "创建人", width: 130, readonly: true },
  { field: "createTime", title: "创建时间", width: 170, readonly: true },
  { field: "modifier", title: "修改人", width: 130, readonly: true },
  { field: "modifyTime", title: "修改时间", width: 170, readonly: true },
];

export const fileParseRuleColumns: ParseIssueHandlingSheetColumn<FileParseRuleSheetRowDTO>[] = [
  { field: "id", title: "ID", width: 0, readonly: true, hidden: true },
  { field: "fileScene", title: "文件场景", width: 120 },
  { field: "fileTypeName", title: "文件类型名称", width: 150 },
  { field: "regionName", title: "标准区域名称", width: 130 },
  { field: "columnMap", title: "标准列编码", width: 160 },
  { field: "columnMapName", title: "标准列名称", width: 200 },
  { field: "status", title: "启用状态", width: 100 },
  { field: "multiIndex", title: "是否多实例指标", width: 120 },
  { field: "required", title: "是否必需", width: 100 },
  { field: "creater", title: "创建人", width: 130, readonly: true },
  { field: "createTime", title: "创建时间", width: 170, readonly: true },
  { field: "modifier", title: "修改人", width: 130, readonly: true },
  { field: "modifyTime", title: "修改时间", width: 170, readonly: true },
];

export const productMatchRuleColumns: ParseIssueHandlingSheetColumn<ProductMatchRuleSheetRowDTO>[] = [
  { field: "id", title: "ID", width: 0, readonly: true, hidden: true },
  { field: "fileTypeName", title: "文件类型名称", width: 140 },
  { field: "pdCd", title: "产品代码", width: 130 },
  { field: "pdNm", title: "产品名称", width: 220 },
  { field: "orgCd", title: "托管人代码", width: 130 },
  { field: "orgNm", title: "托管人名称", width: 180 },
  { field: "pdType", title: "产品类型", width: 130 },
  { field: "subjectSystem", title: "科目体系", width: 140 },
  { field: "holdingStatus", title: "持仓状态", width: 110 },
  { field: "establishedDate", title: "成立日", width: 120 },
  { field: "effectiveFrequency", title: "时效频率", width: 110 },
  { field: "delayDays", title: "延迟天数", width: 100 },
  { field: "approvalRequired", title: "是否审批", width: 100 },
  { field: "fileType", title: "文件类型", width: 130 },
  { field: "matchRules", title: "匹配规则", width: 260 },
  { field: "isValid", title: "启用状态", width: 100 },
  { field: "memo", title: "备注", width: 200 },
  { field: "debugName", title: "调试名称", width: 200 },
  { field: "jobName", title: "作业名称", width: 160 },
  { field: "jobScene", title: "作业场景", width: 140 },
  { field: "creater", title: "创建人", width: 130, readonly: true },
  { field: "createTime", title: "创建时间", width: 170, readonly: true },
  { field: "modifier", title: "修改人", width: 130, readonly: true },
  { field: "modifyTime", title: "修改时间", width: 170, readonly: true },
];

export const parseFileParseSourceWorkbookRows = (workbookData: IWorkbookData | null) =>
  extractRowsFromWorkbook<FileParseSourceSheetRowDTO>(
    workbookData,
    fileParseSourceColumns,
  ).map((row) => {
    const parsedColumnMap = splitColumnMapDisplayValue(row.columnMap);
    return normalizeFileParseSourceRow({
      ...row,
      columnMap: parsedColumnMap.columnMap,
      columnMapName: row.columnMapName || parsedColumnMap.columnMapName,
    });
  });

export const parseFileParseRuleWorkbookRows = (workbookData: IWorkbookData | null) =>
  extractRowsFromWorkbook<FileParseRuleSheetRowDTO>(
    workbookData,
    fileParseRuleColumns,
  ).map((row) => {
    const parsedColumnMap = splitColumnMapDisplayValue(row.columnMap);
    return normalizeFileParseRuleRow({
      ...row,
      columnMap: parsedColumnMap.columnMap,
      columnMapName: row.columnMapName || parsedColumnMap.columnMapName,
    });
  });

export const parseProductMatchRuleWorkbookRows = (workbookData: IWorkbookData | null) =>
  extractRowsFromWorkbook<ProductMatchRuleSheetRowDTO>(
    workbookData,
    productMatchRuleColumns,
  ).map(normalizeProductMatchRuleRow);

export const useParseIssueHandlingPage = (): ParseIssueHandlingPageState => {
  const activeTab = ref<ParseIssueHandlingTab>("fileParseSource");
  const loading = ref(false);
  const saving = ref(false);
  const exporting = ref(false);
  const fileParseSources = ref<FileParseSourceSheetRowDTO[]>([]);
  const fileParseRules = ref<FileParseRuleSheetRowDTO[]>([]);
  const productMatchRules = ref<ProductMatchRuleSheetRowDTO[]>([]);
  const fileParseSourceOriginalIds = ref<string[]>([]);
  const fileParseRuleOriginalIds = ref<string[]>([]);
  const productMatchRuleOriginalIds = ref<string[]>([]);
  const lastResult = ref<ParseIssueHandlingPageState["lastResult"]>(null);
  const fileParseSourceQuery = reactive(fileParseSourceQueryDefault());
  const fileParseRuleQuery = reactive(fileParseRuleQueryDefault());
  const productMatchRuleQuery = reactive(productMatchRuleQueryDefault());

  const loadFileParseRules = async (options: { silent?: boolean } = {}) => {
    loading.value = true;
    try {
      const response = await listParseIssueFileParseRules(fileParseRuleQuery);
      const rows = unwrapMultiResult(response).map(normalizeFileParseRuleRow);
      fileParseRules.value = rows;
      fileParseRuleOriginalIds.value = collectIds(rows);
      lastResult.value = null;
    } catch (error) {
      fileParseRules.value = [];
      fileParseRuleOriginalIds.value = [];
      if (!options.silent) {
        message.error("加载标准列指标映射失败");
      }
    } finally {
      loading.value = false;
    }
  };

  const loadFileParseSources = async () => {
    loading.value = true;
    try {
      if (!fileParseRules.value.length) {
        const ruleResponse = await listParseIssueFileParseRules(fileParseRuleQuery);
        const ruleRows = unwrapMultiResult(ruleResponse).map(normalizeFileParseRuleRow);
        fileParseRules.value = ruleRows;
        fileParseRuleOriginalIds.value = collectIds(ruleRows);
      }
      const response = await listParseIssueFileParseSources(fileParseSourceQuery);
      const rows = unwrapMultiResult(response).map(normalizeFileParseSourceRow);
      fileParseSources.value = rows;
      fileParseSourceOriginalIds.value = collectIds(rows);
      lastResult.value = null;
    } catch (error) {
      fileParseSources.value = [];
      fileParseSourceOriginalIds.value = [];
      message.error("加载解析字段映射表失败");
    } finally {
      loading.value = false;
    }
  };

  const loadProductMatchRules = async () => {
    loading.value = true;
    try {
      const response = await listParseIssueProductMatchRules(productMatchRuleQuery);
      const rows = unwrapMultiResult(response).map(normalizeProductMatchRuleRow);
      productMatchRules.value = rows;
      productMatchRuleOriginalIds.value = collectIds(rows);
      lastResult.value = null;
    } catch (error) {
      productMatchRules.value = [];
      productMatchRuleOriginalIds.value = [];
      message.error("加载产品识别规则配置表失败");
    } finally {
      loading.value = false;
    }
  };

  const refreshCurrent = async () => {
    if (activeTab.value === "fileParseRule") {
      await loadFileParseRules();
      return;
    }
    if (activeTab.value === "productMatchRule") {
      await loadProductMatchRules();
      return;
    }
    await loadFileParseSources();
  };

  const saveCurrent = async (workbookData: IWorkbookData | null) => {
    saving.value = true;
    try {
      const response =
        activeTab.value === "productMatchRule"
          ? await saveParseIssueProductMatchRules({
              rows: parseProductMatchRuleWorkbookRows(workbookData),
              originalIds: productMatchRuleOriginalIds.value,
            })
          : activeTab.value === "fileParseRule"
            ? await saveParseIssueFileParseRules({
                rows: parseFileParseRuleWorkbookRows(workbookData),
                originalIds: fileParseRuleOriginalIds.value,
              })
          : await saveParseIssueFileParseSources({
              rows: parseFileParseSourceWorkbookRows(workbookData),
              originalIds: fileParseSourceOriginalIds.value,
            });
      lastResult.value = unwrapSingleResult(response) ?? null;
      if (lastResult.value?.failedCount) {
        message.warning("部分行保存失败，请查看保存结果提示");
      } else {
        message.success("保存成功");
      }
      await refreshCurrent();
    } catch (error) {
      message.error("保存失败");
    } finally {
      saving.value = false;
    }
  };

  const defaultExportFileName = () => {
    if (activeTab.value === "productMatchRule") {
      return "产品识别规则配置表.xlsx";
    }
    if (activeTab.value === "fileParseRule") {
      return "标准列指标映射.xlsx";
    }
    return "外部列指标映射.xlsx";
  };

  const exportCurrent = async () => {
    if (exporting.value) {
      return;
    }
    exporting.value = true;
    try {
      const response = (await (
        activeTab.value === "productMatchRule"
          ? exportParseIssueProductMatchRules(productMatchRuleQuery)
          : activeTab.value === "fileParseRule"
            ? exportParseIssueFileParseRules(fileParseRuleQuery)
            : exportParseIssueFileParseSources(fileParseSourceQuery)
      )) as { data?: Blob; headers?: Record<string, string> } | Blob;
      const blob = response instanceof Blob ? response : response.data;
      if (!blob) {
        throw new Error("导出响应为空");
      }
      const headers = response instanceof Blob ? undefined : response.headers;
      const fileName =
        parseContentDispositionFileName(headers?.["content-disposition"] || headers?.["Content-Disposition"]) ||
        defaultExportFileName();
      triggerBrowserDownload(blob, fileName);
      message.success("导出成功");
    } catch (error) {
      console.error("导出解析问题处理配置失败:", error);
      message.error("导出失败，请稍后重试");
    } finally {
      exporting.value = false;
    }
  };

  const pageState = reactive<ParseIssueHandlingPageState>({
    get activeTab() {
      return activeTab.value;
    },
    get loading() {
      return loading.value;
    },
    get saving() {
      return saving.value;
    },
    get exporting() {
      return exporting.value;
    },
    get fileParseSources() {
      return fileParseSources.value;
    },
    get fileParseRules() {
      return fileParseRules.value;
    },
    get productMatchRules() {
      return productMatchRules.value;
    },
    get fileParseSourceOriginalIds() {
      return fileParseSourceOriginalIds.value;
    },
    get fileParseRuleOriginalIds() {
      return fileParseRuleOriginalIds.value;
    },
    get productMatchRuleOriginalIds() {
      return productMatchRuleOriginalIds.value;
    },
    fileParseSourceQuery,
    fileParseRuleQuery,
    productMatchRuleQuery,
    get lastResult() {
      return lastResult.value;
    },
    setActiveTab(tab) {
      activeTab.value = tab;
      if (tab === "productInfoExtraction") {
        return;
      }
      if (tab === "productMatchRule" && !productMatchRules.value.length) {
        void loadProductMatchRules();
      }
      if (tab === "fileParseRule" && !fileParseRules.value.length) {
        void loadFileParseRules();
      }
      if (tab === "fileParseSource" && !fileParseSources.value.length) {
        void loadFileParseSources();
      }
    },
    refreshCurrent,
    loadFileParseSources,
    loadProductMatchRules,
    resetCurrentQuery() {
      if (activeTab.value === "productMatchRule") {
        Object.assign(productMatchRuleQuery, productMatchRuleQueryDefault());
      } else if (activeTab.value === "fileParseRule") {
        Object.assign(fileParseRuleQuery, fileParseRuleQueryDefault());
      } else if (activeTab.value === "fileParseSource") {
        Object.assign(fileParseSourceQuery, fileParseSourceQueryDefault());
      }
      void refreshCurrent();
    },
    saveCurrent,
    exportCurrent,
  });

  void loadFileParseSources();

  return pageState;
};
