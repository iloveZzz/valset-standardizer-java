<script setup lang="ts">
import { computed, h, reactive, ref, watch } from "vue";
import { LocaleType, type IWorkbookData } from "@univerjs/presets";
import {
  DeleteOutlined,
  ExclamationCircleOutlined,
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import { Modal } from "ant-design-vue";
import {
  YButton,
  YCard,
} from "@yss-ui/components";
import type {
  FileParseSourceSheetRowDTO,
  FileParseRuleSheetRowDTO,
  ProductMatchRuleSheetRowDTO,
} from "@/api/parseIssueHandling";
import NativeUniverSheet from "@/components/NativeUniverSheet/index.vue";
import ProductInfoExtractionWorkspace from "@/views/ProductInfoExtraction/components/ProductInfoExtractionWorkspace.vue";
import { useProductInfoExtractionPage } from "@/views/ProductInfoExtraction/hooks/useProductInfoExtractionPage";
import type {
  ParseIssueHandlingPageState,
  ParseIssueHandlingSheetColumn,
  ParseIssueHandlingTab,
} from "../types";
import {
  fileParseSourceColumns,
  fileParseRuleColumns,
  parseFileParseSourceWorkbookRows,
  parseFileParseRuleWorkbookRows,
  parseProductMatchRuleWorkbookRows,
  productMatchRuleColumns,
  summarizeSheetChanges,
} from "../hooks/useParseIssueHandlingPage";

const { page } = defineProps<{
  page: ParseIssueHandlingPageState;
}>();

type NativeUniverSheetInstance = InstanceType<typeof NativeUniverSheet> & {
  getWorkbook: () => unknown;
  save: () => IWorkbookData | null;
};

const sheetRef = ref<NativeUniverSheetInstance | null>(null);
const sheetRefreshing = ref(true);
const productInfoExtractionPage = useProductInfoExtractionPage();
const rowContextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  rowIndex: null as number | null,
});

const sheetHeaderStyleId = "parse_issue_header";
const sheetReadonlyHeaderStyleId = "parse_issue_header_readonly";
const sheetDataStyleId = "parse_issue_data";
const sheetReadonlyDataStyleId = "parse_issue_data_readonly";
const sheetDisabledRowStyleId = "parse_issue_disabled_row";
const sheetFilterResourceName = "SHEET_FILTER_PLUGIN";
const sheetDataValidationResourceName = "SHEET_DATA_VALIDATION_PLUGIN";
const sheetDataCellBorder = {
  s: 1,
  cl: { rgb: "#000000" },
};
const fileTypeOptions = ["ALL", "EXCEL"];
const enabledOptions = ["是", "否"];

const tabs: Array<{
  key: ParseIssueHandlingTab;
  label: string;
}> = [
  { key: "fileParseSource", label: "外部列指标映射" },
  { key: "fileParseRule", label: "标准列指标映射" },
  { key: "productMatchRule", label: "产品识别规则配置表" },
  { key: "productInfoExtraction", label: "估值表产品信息提取" },
];

const formatColumnMapOption = (columnMap: unknown) =>
  String(columnMap ?? "").trim();

const uniqueStrings = (values: string[]) =>
  Array.from(new Set(values.map((value) => value.trim()).filter(Boolean)));

const activeColumns = computed(() => {
  if (page.activeTab === "productInfoExtraction") {
    return [];
  }
  if (page.activeTab === "productMatchRule") {
    return productMatchRuleColumns;
  }
  if (page.activeTab === "fileParseRule") {
    return fileParseRuleColumns;
  }
  return fileParseSourceColumns;
});

const visibleActiveColumns = computed(() =>
  activeColumns.value.filter((column) => !column.hidden),
);

const activeRows = computed(() => {
  if (page.activeTab === "productInfoExtraction") {
    return [];
  }
  if (page.activeTab === "productMatchRule") {
    return page.productMatchRules;
  }
  if (page.activeTab === "fileParseRule") {
    return page.fileParseRules;
  }
  return page.fileParseSources;
});

const activeOriginalIds = computed(() => {
  if (page.activeTab === "productInfoExtraction") {
    return [];
  }
  if (page.activeTab === "productMatchRule") {
    return page.productMatchRuleOriginalIds;
  }
  if (page.activeTab === "fileParseRule") {
    return page.fileParseRuleOriginalIds;
  }
  return page.fileParseSourceOriginalIds;
});

const title = computed(() =>
  page.activeTab === "productInfoExtraction"
    ? "估值表产品信息提取"
    : page.activeTab === "fileParseRule"
      ? "标准列指标映射"
    : page.activeTab === "productMatchRule"
      ? "产品识别规则配置表"
      : "外部列指标映射",
);

const totalRows = computed(() => activeRows.value.length);

const changedTip = computed(() =>
  page.activeTab === "productInfoExtraction"
    ? "从已识别估值表文件提取产品信息并生成识别规则"
    : activeOriginalIds.value.length
      ? `本次加载 ${activeOriginalIds.value.length} 条已有记录`
      : "当前查询未加载已有记录",
);

const normalizeSheetCellValue = (value: unknown): string | number | boolean => {
  if (value === undefined || value === null || value === "") {
    return "";
  }
  if (typeof value === "boolean") {
    return value ? "是" : "否";
  }
  if (typeof value === "number" || typeof value === "string") {
    return value;
  }
  return String(value);
};

const normalizeSheetOptionValue = (
  field: string,
  value: unknown,
): string | number | boolean => {
  const normalized = normalizeSheetCellValue(value);
  if (field !== "status" && field !== "isValid") {
    return normalized;
  }
  const text = String(normalized).trim().toLowerCase();
  if (["1", "true", "yes", "y", "启用", "是"].includes(text)) {
    return "是";
  }
  if (["0", "false", "no", "n", "停用", "否"].includes(text)) {
    return "否";
  }
  return normalized;
};

const isDisabledStatusValue = (value: unknown) => {
  const text = String(normalizeSheetCellValue(value)).trim().toLowerCase();
  return ["0", "false", "no", "n", "停用", "否"].includes(text);
};

const isDisabledRow = (id: string, row: Record<string, unknown>) => {
  if (id === "parse_issue_file_parse_sources") {
    return isDisabledStatusValue(row.status);
  }
  if (id === "parse_issue_file_parse_rules") {
    return isDisabledStatusValue(row.status);
  }
  if (id === "parse_issue_product_match_rules") {
    return isDisabledStatusValue(row.isValid);
  }
  return false;
};

const getRowBusinessName = (row: Record<string, unknown>) => {
  if (page.activeTab === "fileParseRule") {
    const fileScene = String(row.fileScene ?? "").trim();
    const fileTypeName = String(row.fileTypeName ?? "").trim();
    const regionName = String(row.regionName ?? "").trim();
    const columnMap = String(row.columnMap ?? "").trim();
    return [fileScene, fileTypeName, regionName, columnMap].filter(Boolean).join(" / ") || "-";
  }
  if (page.activeTab === "productMatchRule") {
    const pdCd = String(row.pdCd ?? "").trim();
    const pdNm = String(row.pdNm ?? "").trim();
    return [pdCd, pdNm].filter(Boolean).join(" / ") || "-";
  }
  const fileType = String(row.fileType ?? "").trim();
  const columnMap = String(row.columnMap ?? "").trim();
  const columnName = String(row.columnName ?? "").trim();
  return [fileType, columnMap, columnName].filter(Boolean).join(" / ") || "-";
};

const getPreviewFields = (
  row: Record<string, unknown>,
  columns: ParseIssueHandlingSheetColumn[],
) =>
  columns
    .filter((column) => !column.hidden && !column.readonly)
    .slice(0, 5)
    .map((column) => {
      const value = row[column.field];
      return `${column.title}：${value === undefined || value === null || value === "" ? "-" : String(value)}`;
    })
    .join("；");

const renderChangeGroup = (
  label: string,
  rows: Array<Record<string, unknown>>,
  type: "create" | "update" | "delete",
) => {
  const previewRows = rows.slice(0, 8);
  return h("div", { class: "parse-issue-confirm-group" }, [
    h(
      "div",
      { class: `parse-issue-confirm-group__title parse-issue-confirm-group__title--${type}` },
      `${label}（${rows.length}）`,
    ),
    rows.length
      ? h(
          "div",
          { class: "parse-issue-confirm-group__list" },
          [
            ...previewRows.map((row, index) =>
              h("div", { class: "parse-issue-confirm-row" }, [
                h("div", { class: "parse-issue-confirm-row__name" }, `${index + 1}. ${getRowBusinessName(row)}`),
                h("div", { class: "parse-issue-confirm-row__fields" }, getPreviewFields(row, visibleActiveColumns.value)),
              ]),
            ),
            rows.length > previewRows.length
              ? h("div", { class: "parse-issue-confirm-row parse-issue-confirm-row--more" }, `还有 ${rows.length - previewRows.length} 条未展开显示`)
              : null,
          ],
        )
      : h("div", { class: "parse-issue-confirm-group__empty" }, "无"),
  ]);
};

const toWorkbook = (
  id: string,
  name: string,
  columns: ParseIssueHandlingSheetColumn[],
  rows: Array<Record<string, unknown>>,
): IWorkbookData => {
  const sheetId = `${id}_sheet`;
  const rowCount = Math.max(rows.length + 20, 60);
  const visibleColumns = columns.filter((column) => !column.hidden);
  const columnCount = Math.max(columns.length, visibleColumns.length + 1, 12);
  const cellData: NonNullable<IWorkbookData["sheets"][string]["cellData"]> = {};
  const getColumnIndex = (field: string) =>
    columns.findIndex((column) => column.field === field);
  const createListValidationRule = (
    uid: string,
    columnIndex: number,
    options: string[],
  ) => ({
    uid,
    type: "list",
    formula1: options.join(","),
    allowBlank: true,
    showDropDown: true,
    showErrorMessage: true,
    errorStyle: 1,
    errorTitle: "数据校验失败",
    error: `只能选择：${options.join("、")}`,
    renderMode: 1,
    ranges: [
      {
        startRow: 1,
        endRow: rowCount - 1,
        startColumn: columnIndex,
        endColumn: columnIndex,
      },
    ],
  });
  const validationRules = id === "parse_issue_file_parse_sources"
    ? [
        {
          field: "columnMap",
          options: uniqueStrings(
            page.fileParseRules.map((rule) => formatColumnMapOption(rule.columnMap)),
          ),
        },
        { field: "fileType", options: fileTypeOptions },
        { field: "status", options: enabledOptions },
      ].flatMap(({ field, options }) => {
        const columnIndex = getColumnIndex(field);
        return columnIndex >= 0
          ? [createListValidationRule(`${sheetId}_${field}_validation`, columnIndex, options)]
          : [];
      })
    : id === "parse_issue_file_parse_rules"
      ? [
          {
            field: "columnMap",
            options: uniqueStrings(
              page.fileParseRules.map((rule) => formatColumnMapOption(rule.columnMap)),
            ),
          },
          { field: "fileScene", options: ["ALL", "VALUATION"] },
          { field: "status", options: enabledOptions },
          { field: "multiIndex", options: enabledOptions },
          { field: "required", options: enabledOptions },
        ].flatMap(({ field, options }) => {
          const columnIndex = getColumnIndex(field);
          return columnIndex >= 0
            ? [createListValidationRule(`${sheetId}_${field}_validation`, columnIndex, options)]
            : [];
        })
    : [];

  cellData[0] = Object.fromEntries(
    columns.map((column, columnIndex) => [
      columnIndex,
      {
        v: column.title,
        s: column.readonly ? sheetReadonlyHeaderStyleId : sheetHeaderStyleId,
      },
    ]),
  );

  rows.forEach((row, rowIndex) => {
    const disabledRow = isDisabledRow(id, row);
    cellData[rowIndex + 1] = Object.fromEntries(
      columns.map((column, columnIndex) => [
        columnIndex,
        {
          v: normalizeSheetOptionValue(column.field, row[column.field]),
          s: disabledRow
            ? sheetDisabledRowStyleId
            : column.readonly
              ? sheetReadonlyDataStyleId
              : sheetDataStyleId,
        },
      ]),
    );
  });

  return {
    id,
    name,
    appVersion: "3.0.0",
    locale: LocaleType.ZH_CN,
    sheetOrder: [sheetId],
    resources: [
      {
        name: sheetFilterResourceName,
        data: JSON.stringify({
          [sheetId]: {
            ref: {
              startRow: 0,
              endRow: Math.max(1, rows.length),
              startColumn: 0,
              endColumn: Math.max(0, columns.length - 1),
            },
            filterColumns: columns
              .map((column, columnIndex) => ({ column, columnIndex }))
              .filter(({ column }) => !column.hidden)
              .map(({ columnIndex }) => ({ colId: columnIndex })),
          },
        }),
      },
      {
        name: sheetDataValidationResourceName,
        data: JSON.stringify({
          [sheetId]: validationRules,
        }),
      },
    ],
    styles: {
      [sheetHeaderStyleId]: {
        bg: { rgb: "#f1f3f7" },
        cl: { rgb: "#1f2937" },
        bl: 1,
        ht: 2,
        vt: 2,
        tb: 2,
        bd: {
          t: sheetDataCellBorder,
          r: sheetDataCellBorder,
          b: sheetDataCellBorder,
          l: sheetDataCellBorder,
        },
      },
      [sheetReadonlyHeaderStyleId]: {
        bg: { rgb: "#e8edf5" },
        cl: { rgb: "#4b5563" },
        bl: 1,
        ht: 2,
        vt: 2,
        tb: 2,
        bd: {
          t: sheetDataCellBorder,
          r: sheetDataCellBorder,
          b: sheetDataCellBorder,
          l: sheetDataCellBorder,
        },
      },
      [sheetDataStyleId]: {
        ht: 1,
        vt: 2,
        bd: {
          t: sheetDataCellBorder,
          r: sheetDataCellBorder,
          b: sheetDataCellBorder,
          l: sheetDataCellBorder,
        },
      },
      [sheetReadonlyDataStyleId]: {
        bg: { rgb: "#f8fafc" },
        cl: { rgb: "#64748b" },
        ht: 1,
        vt: 2,
        bd: {
          t: sheetDataCellBorder,
          r: sheetDataCellBorder,
          b: sheetDataCellBorder,
          l: sheetDataCellBorder,
        },
      },
      [sheetDisabledRowStyleId]: {
        bg: { rgb: "#fff1f0" },
        cl: { rgb: "#a8071a" },
        ht: 1,
        vt: 2,
        bd: {
          t: sheetDataCellBorder,
          r: sheetDataCellBorder,
          b: sheetDataCellBorder,
          l: sheetDataCellBorder,
        },
      },
    },
    sheets: {
      [sheetId]: {
        id: sheetId,
        name,
        rowCount,
        columnCount,
        cellData,
        columnData: Object.fromEntries(
          columns.map((column, columnIndex) => [
            columnIndex,
            { w: column.hidden ? 0 : column.width ?? 140, hd: column.hidden ? 1 : 0 },
          ]),
        ),
        rowData: {
          0: { h: 34 },
        },
        freeze: {
          startRow: 1,
          startColumn: 0,
          ySplit: 1,
          xSplit: 0,
        },
      },
    },
  };
};

const workbookData = computed(() =>
  toWorkbook(
    page.activeTab === "productMatchRule"
      ? "parse_issue_product_match_rules"
      : page.activeTab === "fileParseRule"
        ? "parse_issue_file_parse_rules"
      : "parse_issue_file_parse_sources",
    title.value,
    activeColumns.value,
    activeRows.value as Array<Record<string, unknown>>,
  ),
);

type ActiveSheetLike = {
  deleteRow?: (rowPosition: number) => unknown;
  getSelection?: () => {
    getCurrentCell?: () => { actualRow?: number; actualColumn?: number } | null;
    getActiveRange?: () => {
      getColumn?: () => number;
      getRow?: () => number;
    } | null;
  } | null;
  insertRowsAfter?: (afterPosition: number, howMany: number) => unknown;
};

const getActiveSheet = () => {
  const workbookLike = sheetRef.value?.getWorkbook() as {
    getActiveSheet?: () => ActiveSheetLike | null;
  } | null;
  return workbookLike?.getActiveSheet?.() ?? null;
};

const handleWorkbookCreated = () => {
  // 列级只读通过表格样式和键盘拦截控制，不再施加范围保护。
  sheetRefreshing.value = false;
};

const handleSheetError = () => {
  sheetRefreshing.value = false;
};

const getSelectedCellPosition = () => {
  const selection = getActiveSheet()?.getSelection?.();
  const currentCell = selection?.getCurrentCell?.();
  if (
    typeof currentCell?.actualRow === "number" &&
    typeof currentCell.actualColumn === "number"
  ) {
    return {
      rowIndex: currentCell.actualRow,
      columnIndex: currentCell.actualColumn,
    };
  }
  const activeRange = selection?.getActiveRange?.();
  const rowIndex = activeRange?.getRow?.();
  const columnIndex = activeRange?.getColumn?.();
  if (typeof rowIndex === "number" && typeof columnIndex === "number") {
    return {
      rowIndex,
      columnIndex,
    };
  }
  return null;
};

const isReadonlyColumnIndex = (columnIndex: number) =>
  activeColumns.value[columnIndex]?.readonly === true;

const getSelectedColumnIndex = () => {
  return getSelectedCellPosition()?.columnIndex ?? null;
};

const isReadonlyEditKey = (event: KeyboardEvent) => {
  if (event.metaKey || event.ctrlKey || event.altKey) {
    return false;
  }
  return event.key.length === 1 || [
    "Backspace",
    "Delete",
    "Enter",
    "F2",
  ].includes(event.key);
};

const preventReadonlyCellEdit = (event: Event) => {
  const columnIndex = getSelectedColumnIndex();
  if (columnIndex === null || !isReadonlyColumnIndex(columnIndex)) {
    return;
  }
  event.preventDefault();
  event.stopPropagation();
};

const handleSheetKeydown = (event: KeyboardEvent) => {
  if (isReadonlyEditKey(event)) {
    preventReadonlyCellEdit(event);
  }
};

watch(
  () => page.activeTab,
  () => {
    sheetRef.value = null;
    rowContextMenu.visible = false;
    sheetRefreshing.value = page.activeTab !== "productInfoExtraction";
  },
);

watch(
  workbookData,
  () => {
    if (page.activeTab !== "productInfoExtraction") {
      sheetRefreshing.value = true;
    }
  },
);

const hideRowContextMenu = () => {
  rowContextMenu.visible = false;
};

const handleSheetContextMenu = (event: MouseEvent) => {
  const selectedCell = getSelectedCellPosition();
  rowContextMenu.rowIndex = selectedCell?.rowIndex ?? null;
  rowContextMenu.x = event.clientX;
  rowContextMenu.y = event.clientY;
  rowContextMenu.visible = selectedCell !== null;
};

const insertRowAfterSelected = () => {
  const rowIndex = rowContextMenu.rowIndex;
  hideRowContextMenu();
  if (rowIndex === null || rowIndex < 0) {
    return;
  }
  getActiveSheet()?.insertRowsAfter?.(rowIndex, 1);
};

const deleteSelectedRow = () => {
  const rowIndex = rowContextMenu.rowIndex;
  hideRowContextMenu();
  if (rowIndex === null || rowIndex < 1) {
    Modal.info({
      title: "表头不可删除",
      content: "当前表格的表头行不能删除。",
      okText: "知道了",
    });
    return;
  }
  getActiveSheet()?.deleteRow?.(rowIndex);
};

const handleSave = () => {
  if (page.activeTab === "productInfoExtraction") {
    return;
  }
  const snapshot = sheetRef.value?.save() ?? null;
  const changeSummary =
    page.activeTab === "productMatchRule"
      ? summarizeSheetChanges<ProductMatchRuleSheetRowDTO>(
          parseProductMatchRuleWorkbookRows(snapshot),
          page.productMatchRules,
          productMatchRuleColumns,
        )
      : page.activeTab === "fileParseRule"
        ? summarizeSheetChanges<FileParseRuleSheetRowDTO>(
            parseFileParseRuleWorkbookRows(snapshot),
            page.fileParseRules,
            fileParseRuleColumns,
          )
      : summarizeSheetChanges<FileParseSourceSheetRowDTO>(
          parseFileParseSourceWorkbookRows(snapshot),
          page.fileParseSources,
          fileParseSourceColumns,
        );
  const changedCount =
    changeSummary.createdRows.length +
    changeSummary.updatedRows.length +
    changeSummary.deletedRows.length;

  if (!changedCount) {
    Modal.info({
      title: "无变更数据",
      content: "当前页签没有需要保存的新增、修改或删除记录。",
      okText: "知道了",
    });
    return;
  }

  Modal.confirm({
    title: `确认保存${title.value}`,
    icon: h(ExclamationCircleOutlined),
    width: 820,
    okText: "确认保存",
    cancelText: "取消",
    content: h("div", { class: "parse-issue-confirm" }, [
      h("div", { class: "parse-issue-confirm__summary" }, [
        h("span", `新增 ${changeSummary.createdRows.length} 条`),
        h("span", `修改 ${changeSummary.updatedRows.length} 条`),
        h("span", `删除 ${changeSummary.deletedRows.length} 条`),
      ]),
      h("div", { class: "parse-issue-confirm__body" }, [
        renderChangeGroup("新增记录", changeSummary.createdRows, "create"),
        renderChangeGroup("修改记录", changeSummary.updatedRows, "update"),
        renderChangeGroup("删除记录", changeSummary.deletedRows, "delete"),
      ]),
    ]),
    onOk: () => page.saveCurrent(snapshot),
  });
};
</script>

<template>
  <div class="parse-issue-page">
    <YCard class="parse-issue-card">
      <div class="parse-issue-header">
        <div>
          <div class="parse-issue-header__title">解析问题处理</div>
          <div class="parse-issue-header__desc">
            {{ title }} · {{ changedTip }}
          </div>
        </div>
        <a-space>
          <a-tag color="blue">总行数：{{ totalRows }}</a-tag>
          <a-tag v-if="page.lastResult" color="green">
            新增 {{ page.lastResult.createdCount ?? 0 }} / 更新 {{ page.lastResult.updatedCount ?? 0 }} / 删除 {{ page.lastResult.deletedCount ?? 0 }}
          </a-tag>
          <a-tag v-if="page.lastResult?.failedCount" color="red">
            失败 {{ page.lastResult.failedCount }}
          </a-tag>
        </a-space>
      </div>

      <div class="parse-issue-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="parse-issue-tab"
          :class="page.activeTab === tab.key ? 'parse-issue-tab--active' : ''"
          @click="page.setActiveTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="parse-issue-toolbar">
        <a-space v-if="page.activeTab === 'fileParseSource'" wrap>
          <a-input
            v-model:value="page.fileParseSourceQuery.fileType"
            size="small"
            allow-clear
            placeholder="文件类型"
            style="width: 140px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.fileParseSourceQuery.columnMap"
            size="small"
            allow-clear
            placeholder="标准列编码"
            style="width: 160px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.fileParseSourceQuery.columnName"
            size="small"
            allow-clear
            placeholder="来源列名称"
            style="width: 180px"
            @pressEnter="page.refreshCurrent"
          />
          <a-select
            v-model:value="page.fileParseSourceQuery.status"
            size="small"
            allow-clear
            placeholder="启用状态"
            style="width: 120px"
            :options="[
              { label: '启用', value: '启用' },
              { label: '停用', value: '停用' },
            ]"
          />
        </a-space>
        <a-space v-else-if="page.activeTab === 'fileParseRule'" wrap>
          <a-input
            v-model:value="page.fileParseRuleQuery.fileScene"
            size="small"
            allow-clear
            placeholder="文件场景"
            style="width: 120px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.fileParseRuleQuery.fileTypeName"
            size="small"
            allow-clear
            placeholder="文件类型名称"
            style="width: 160px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.fileParseRuleQuery.regionName"
            size="small"
            allow-clear
            placeholder="标准区域名称"
            style="width: 150px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.fileParseRuleQuery.columnMap"
            size="small"
            allow-clear
            placeholder="标准列编码"
            style="width: 160px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.fileParseRuleQuery.columnMapName"
            size="small"
            allow-clear
            placeholder="标准列名称"
            style="width: 160px"
            @pressEnter="page.refreshCurrent"
          />
          <a-select
            v-model:value="page.fileParseRuleQuery.status"
            size="small"
            allow-clear
            placeholder="启用状态"
            style="width: 120px"
            :options="[
              { label: '启用', value: '启用' },
              { label: '停用', value: '停用' },
            ]"
          />
        </a-space>
        <a-space v-else-if="page.activeTab === 'productMatchRule'" wrap>
          <a-input
            v-model:value="page.productMatchRuleQuery.pdCd"
            size="small"
            allow-clear
            placeholder="产品代码"
            style="width: 140px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.productMatchRuleQuery.pdNm"
            size="small"
            allow-clear
            placeholder="产品名称"
            style="width: 180px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.productMatchRuleQuery.orgNm"
            size="small"
            allow-clear
            placeholder="托管人名称"
            style="width: 160px"
            @pressEnter="page.refreshCurrent"
          />
          <a-input
            v-model:value="page.productMatchRuleQuery.fileType"
            size="small"
            allow-clear
            placeholder="文件类型"
            style="width: 140px"
            @pressEnter="page.refreshCurrent"
          />
          <a-select
            v-model:value="page.productMatchRuleQuery.isValid"
            size="small"
            allow-clear
            placeholder="启用状态"
            style="width: 120px"
            :options="[
              { label: '启用', value: '启用' },
              { label: '停用', value: '停用' },
            ]"
          />
        </a-space>
        <a-space v-if="page.activeTab !== 'productInfoExtraction'">
          <YButton size="small" :loading="page.loading" @click="page.refreshCurrent">
            <template #icon><SearchOutlined /></template>
            查询
          </YButton>
          <YButton size="small" :disabled="page.loading || page.saving" @click="page.resetCurrentQuery">
            <template #icon><ReloadOutlined /></template>
            重置
          </YButton>
          <YButton size="small" :loading="page.exporting" :disabled="page.loading || page.saving" @click="page.exportCurrent">
            <template #icon><DownloadOutlined /></template>
            导出当前页签
          </YButton>
          <YButton size="small" type="primary" :loading="page.saving" :disabled="page.loading" @click="handleSave">
            <template #icon><SaveOutlined /></template>
            保存当前页签
          </YButton>
        </a-space>
      </div>

      <div v-if="page.activeTab === 'productInfoExtraction'" class="parse-issue-product-extraction">
        <ProductInfoExtractionWorkspace
          :page="productInfoExtractionPage"
          embedded
        />
      </div>

      <div v-else class="parse-issue-sheet">
        <div
          v-if="page.loading || sheetRefreshing"
          class="parse-issue-sheet-skeleton"
        >
          <div class="parse-issue-sheet-skeleton__toolbar">
            <span></span>
            <span></span>
          </div>
          <div class="parse-issue-sheet-skeleton__grid">
            <div class="parse-issue-sheet-skeleton__header">
              <span v-for="index in 8" :key="`header-${index}`"></span>
            </div>
            <div
              v-for="rowIndex in 10"
              :key="`row-${rowIndex}`"
              class="parse-issue-sheet-skeleton__row"
            >
              <span
                v-for="columnIndex in 8"
                :key="`cell-${rowIndex}-${columnIndex}`"
                :style="{ width: `${70 + ((rowIndex + columnIndex) % 4) * 8}%` }"
              ></span>
            </div>
          </div>
        </div>
        <NativeUniverSheet
          :key="page.activeTab"
          ref="sheetRef"
          class="parse-issue-univer-sheet"
          :class="{ 'parse-issue-univer-sheet--refreshing': page.loading || sheetRefreshing }"
          :model-value="workbookData"
          height="calc(100vh - 260px)"
          :config="{
            header: false,
            toolbar: false,
            formulaBar: false,
            footer: { addSheetButtonConfig: { show: false } },
            contextMenu: false
          }"
          @keydown.capture="handleSheetKeydown"
          @beforeinput.capture="preventReadonlyCellEdit"
          @paste.capture="preventReadonlyCellEdit"
          @drop.capture="preventReadonlyCellEdit"
          @dblclick.capture="preventReadonlyCellEdit"
          @contextmenu.capture.prevent="handleSheetContextMenu"
          @workbook-created="handleWorkbookCreated"
          @error="handleSheetError"
          @click.capture="hideRowContextMenu"
        />
        <div
          v-if="rowContextMenu.visible"
          class="parse-issue-row-menu"
          :style="{ left: `${rowContextMenu.x}px`, top: `${rowContextMenu.y}px` }"
          @click.stop
        >
          <button type="button" @click="insertRowAfterSelected">
            <PlusOutlined />
            <span>新增行</span>
          </button>
          <button
            type="button"
            :disabled="rowContextMenu.rowIndex === null || rowContextMenu.rowIndex < 1"
            @click="deleteSelectedRow"
          >
            <DeleteOutlined />
            <span>删除行</span>
          </button>
        </div>
      </div>
    </YCard>
  </div>
</template>
