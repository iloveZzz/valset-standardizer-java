<script setup lang="ts">
import { computed, h, ref, watch } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { Modal } from "ant-design-vue";
import { LocaleType, type IWorkbookData } from "@univerjs/presets";
import {
  DownloadOutlined,
  ExportOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons-vue";
import {
  YButton,
  YCard,
  YTable,
  type YTableColumn,
} from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import NativeUniverSheet from "@/components/NativeUniverSheet/index.vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import {
  batchValuationTaskPageText,
  batchValuationTaskStageCatalog,
  batchValuationTaskStatusCatalog,
  resolveBatchValuationTaskSourceTypeLabel,
} from "../constants";
import type {
  BatchValuationTaskBatchRow,
  BatchValuationTaskPageState,
  BatchValuationTaskStandardRawColumn,
} from "../types";

const { page } = defineProps<{
  page: BatchValuationTaskPageState;
}>();
const yTableRef = ref<InstanceType<typeof YTable> | null>(null);
const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: true,
});
type NativeUniverSheetInstance = InstanceType<typeof NativeUniverSheet> & {
  save: () => IWorkbookData | null;
};
const standardBasicSheetRef = ref<NativeUniverSheetInstance | null>(null);
const standardSubjectSheetRef = ref<NativeUniverSheetInstance | null>(null);
const standardMetricSheetRef = ref<NativeUniverSheetInstance | null>(null);
const standardRawSheetRef = ref<NativeUniverSheetInstance | null>(null);

watch(
  yTableRef,
  (instance) => {
    page.setTableRef(instance);
  },
  { immediate: true },
);

const summaryCards = computed(() => [
  {
    key: "total",
    label: "批次总数",
    value: page.summary.totalCount ?? 0,
    color: "blue",
    clickable: false,
  },
  {
    key: "running",
    label: "处理中",
    value: page.summary.runningCount ?? 0,
    color: "cyan",
    clickable: true,
    status: "RUNNING",
  },
  {
    key: "success",
    label: "已完成",
    value: page.summary.successCount ?? 0,
    color: "green",
    clickable: true,
    status: "SUCCESS",
  },
  {
    key: "failed",
    label: "失败或停止",
    value: page.summary.failedCount ?? 0,
    color: "red",
    clickable: true,
    status: "FAILED",
  },
]);

const isSummaryCardActive = (status?: string) => {
  const normalized = String(page.query.status ?? "").trim().toUpperCase();
  if (!normalized) {
    return false;
  }
  if (status === "FAILED") {
    return normalized === "FAILED" || normalized === "STOPPED";
  }
  return normalized === String(status ?? "").trim().toUpperCase();
};

const summaryDescription = computed(
  () =>
    `当前筛选：${page.currentFilterSummary}。总批次 ${page.summary.totalCount ?? 0} 条；处理中 ${page.summary.runningCount ?? 0} 条，已完成 ${page.summary.successCount ?? 0} 条，失败或停止 ${page.summary.failedCount ?? 0} 条。`,
);

const stageCards = computed(() => {
  const summaryMap = new Map(
    (page.summary.stepSummaries ?? []).map((item) => [String(item.stage ?? "").trim().toUpperCase(), item]),
  );
  const activeStageCode = String(page.query.taskStage ?? "").trim().toUpperCase();
  return batchValuationTaskStageCatalog.map((item, index) => {
    const summary = summaryMap.get(item.stage);
    const pendingCount = summary?.pendingCount ?? 0;
    const runningCount = summary?.runningCount ?? 0;
    const failedCount = summary?.failedCount ?? 0;
    const totalCount = summary?.totalCount ?? 0;
    return {
      index: index + 1,
      key: item.stage,
      label: item.label,
      description: item.description,
      active: activeStageCode === item.stage,
      totalCount,
      pendingCount,
      runningCount,
      doneCount:
        Math.max(
          0,
          totalCount - pendingCount - runningCount - failedCount,
        ),
      failedCount,
    };
  });
});

const columns = computed<YTableColumn[]>(() => [
  {
    type: "checkbox",
    width: 50,
    align: "center",
    fixed: "left",
  },
  {
    field: "batchName",
    title: batchValuationTaskPageText.table.batchName,
    width: 240,
    ellipsis: true,
  },
  {
    field: "businessDate",
    title: batchValuationTaskPageText.table.businessDate,
    width: 120,
  },
  {
    field: "productCode",
    title: batchValuationTaskPageText.table.productCode,
    width: 120,
  },
  {
    field: "productName",
    title: batchValuationTaskPageText.table.productName,
    width: 160,
    ellipsis: true,
  },
  {
    field: "managerName",
    title: batchValuationTaskPageText.table.managerName,
    width: 140,
    ellipsis: true,
  },
  {
    field: "sourceTypeName",
    title: batchValuationTaskPageText.table.sourceType,
    width: 120,
    ellipsis: true,
  },
  {
    field: "currentStageName",
    title: batchValuationTaskPageText.table.currentStageName,
    width: 140,
  },
  {
    field: "statusName",
    title: batchValuationTaskPageText.table.statusName,
    width: 100,
  },
  {
    field: "startedAt",
    title: batchValuationTaskPageText.table.startedAt,
    width: 160,
  },
  {
    field: "endedAt",
    title: batchValuationTaskPageText.table.endedAt,
    width: 160,
  },
  {
    field: "durationText",
    title: batchValuationTaskPageText.table.durationText,
    width: 90,
  },
  {
    field: "lastErrorMessage",
    title: batchValuationTaskPageText.table.lastErrorMessage,
    width: 240,
    ellipsis: true,
  },
  {
    type: "action",
    title: batchValuationTaskPageText.table.action,
    width: 240,
    fixed: "right",
    align: "center",
  },
]);

const TASK_DATE_FORMAT = "YYYY-MM-DD";

const taskDateValue = computed<Dayjs | undefined>({
  get: () => {
    const text = String(page.query.taskDate ?? "").trim();
    const parsed = dayjs(text);
    return parsed.isValid() ? parsed : undefined;
  },
  set: (value) => {
    page.query.taskDate = value ? value.format(TASK_DATE_FORMAT) : "";
  },
});

const businessDateValue = computed<Dayjs | undefined>({
  get: () => {
    const text = String(page.query.businessDate ?? "").trim();
    const parsed = dayjs(text);
    return parsed.isValid() ? parsed : undefined;
  },
  set: (value) => {
    page.query.businessDate = value ? value.format(TASK_DATE_FORMAT) : "";
  },
});

const actionConfig = useTableActionConfig({
  width: 240,
  fixed: "right",
  displayLimit: 3,
  moreRenderType: "moreButton",
  buttons: [
  {
    text: "查看数据",
    key: "standardData",
    type: "link",
    clickFn: ({ row }: { row: BatchValuationTaskBatchRow }) =>
    page.openStandardDataModal(row),
  },
    {
      text: "查看详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: { row: BatchValuationTaskBatchRow }) =>
        page.openDetailDrawer(row),
    },
    {
      text: "重新解析",
      key: "retry",
      type: "link",
      disabledFn: ({ row }: { row: BatchValuationTaskBatchRow }) =>
        !page.canRetryBatch(row),
      clickFn: ({ row }: { row: BatchValuationTaskBatchRow }) => {
        Modal.confirm({
          title: "确认重新解析",
          icon: () => h(ExclamationCircleOutlined),
          content: `确认重新解析批次 ${row.batchId || "-"} 吗？`,
          okText: "确认",
          cancelText: "取消",
          onOk: () => page.retryBatchRow(row),
        });
      },
    },
  ],
});

const stageSummaryTone = (key: string) => {
  switch (key) {
    case "FILE_PARSE":
      return "blue";
    case "STRUCTURE_STANDARDIZE":
      return "cyan";
    case "STANDARD_LANDING":
      return "green";
    default:
      return "default";
  }
};

const columnsWithAction = computed<YTableColumn[]>(() => columns.value);

type StandardSheetColumn = {
  field: string;
  title: string;
  width?: number;
  autoWidth?: boolean;
};

type StandardSheetMergeRange = {
  startRow: number;
  endRow: number;
  startColumn: number;
  endColumn: number;
};

type StandardSheetHeaderLayout = {
  headerRows: string[][];
  headerDepth: number;
  mergeData: StandardSheetMergeRange[];
};

type StandardSheetFreeze = {
  startRow: number;
  startColumn: number;
  ySplit: number;
  xSplit: number;
};

const sheetHeaderStyleId = "sheet_header";
const sheetLeftHeaderStyleId = "sheet_header_left";
const sheetDataStyleId = "sheet_data_border";
const sheetLeftTextDataStyleId = "sheet_data_left_text";
const sheetNumericDataStyleId = "sheet_data_numeric";
const sheetFilterResourceName = "SHEET_FILTER_PLUGIN";
const numericCellValueType = 2;
const sheetDataCellBorder = {
  s: 1,
  cl: { rgb: "#000000" },
};
const sheetDataCellStyle = {
  ht: 2,
  vt: 2,
  bd: {
    t: sheetDataCellBorder,
    r: sheetDataCellBorder,
    b: sheetDataCellBorder,
    l: sheetDataCellBorder,
  },
};
const sheetNumericDataCellStyle = {
  ...sheetDataCellStyle,
  ht: 3,
  cl: { rgb: "#165dff" },
  n: {
    pattern: "0.0000",
  },
};
const sheetLeftTextDataCellStyle = {
  ...sheetDataCellStyle,
  ht: 1,
};
const sheetHeaderCellStyle = {
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
};
const sheetLeftHeaderCellStyle = {
  ...sheetHeaderCellStyle,
  ht: 1,
};

const normalizeSheetCellValue = (value: unknown): string | number | boolean => {
  if (value === undefined || value === null || value === "") {
    return "-";
  }
  if (typeof value === "boolean") {
    return value ? "是" : "否";
  }
  if (typeof value === "number" || typeof value === "string") {
    return value;
  }
  return String(value);
};

const normalizeNumericCellValue = (value: unknown): number | null => {
  if (typeof value === "number" && Number.isFinite(value)) {
    return Number(value.toFixed(4));
  }
  if (typeof value === "string") {
    const trimmed = value.trim().replace(/,/g, "");
    if (!trimmed) {
      return null;
    }
    const numericValue = Number(trimmed);
    return Number.isFinite(numericValue) ? Number(numericValue.toFixed(4)) : null;
  }
  return null;
};

const splitHeaderTitle = (title: string) =>
  String(title ?? "")
    .split("|")
    .map((item) => item.trim())
    .filter(Boolean);

const isLeftAlignedHeader = (title: string) => /编码|名称|层级/.test(title);

const resolveHeaderStyleId = (title: string) =>
  isLeftAlignedHeader(title) ? sheetLeftHeaderStyleId : sheetHeaderStyleId;

const toSheetCellData = (
  column: StandardSheetColumn,
  value: unknown,
  formatNumericCell = true,
) => {
  if (isLeftAlignedHeader(column.title)) {
    return {
      v: normalizeSheetCellValue(value),
      s: sheetLeftTextDataStyleId,
    };
  }
  const numericValue = formatNumericCell ? normalizeNumericCellValue(value) : null;
  if (numericValue !== null) {
    return {
      v: numericValue,
      t: numericCellValueType,
      s: sheetNumericDataStyleId,
    };
  }
  return {
    v: normalizeSheetCellValue(value),
    s: sheetDataStyleId,
  };
};

const resolveSheetColumnWidth = (
  column: StandardSheetColumn,
  rows: Array<Record<string, unknown>>,
) => {
  const headerLength = splitHeaderTitle(column.title).join("").length;
  const contentLength = Math.max(
    0,
    ...rows.slice(0, 100).map((row) => String(row[column.field] ?? "").length),
  );
  const length = Math.max(headerLength, contentLength);
  return Math.min(320, Math.max(80, length * 14 + 32));
};

const resolveConfiguredSheetColumnWidth = (
  column: StandardSheetColumn,
  rows: Array<Record<string, unknown>>,
) => {
  if (column.autoWidth || !column.width) {
    return resolveSheetColumnWidth(column, rows);
  }
  return column.width;
};

const buildHeaderLayout = (columns: StandardSheetColumn[]): StandardSheetHeaderLayout => {
  const titleLevels = columns.map((column) => {
    const levels = splitHeaderTitle(column.title);
    return levels.length ? levels : [column.title];
  });
  const headerDepth = Math.max(1, ...titleLevels.map((levels) => levels.length));
  const headerRows = Array.from({ length: headerDepth }, () =>
    Array.from({ length: columns.length }, () => ""),
  );
  const mergeData: StandardSheetMergeRange[] = [];
  const verticalMergedCells = new Set<string>();
  const markVerticalMergeCells = (startRow: number, endRow: number, columnIndex: number) => {
    for (let rowIndex = startRow; rowIndex <= endRow; rowIndex += 1) {
      verticalMergedCells.add(`${rowIndex}:${columnIndex}`);
    }
  };

  titleLevels.forEach((levels, columnIndex) => {
    levels.forEach((level, rowIndex) => {
      headerRows[rowIndex][columnIndex] = level;
    });

    if (headerDepth <= 1) {
      return;
    }

    if (levels.length === 1) {
      markVerticalMergeCells(0, headerDepth - 1, columnIndex);
      mergeData.push({
        startRow: 0,
        endRow: headerDepth - 1,
        startColumn: columnIndex,
        endColumn: columnIndex,
      });
      for (let rowIndex = 1; rowIndex < headerDepth; rowIndex += 1) {
        headerRows[rowIndex][columnIndex] = "";
      }
      return;
    }

    let rowIndex = 0;
    while (rowIndex < levels.length) {
      const value = levels[rowIndex];
      let endRow = rowIndex + 1;
      while (endRow < levels.length && levels[endRow] === value) {
        endRow += 1;
      }
      if (endRow - rowIndex > 1) {
        markVerticalMergeCells(rowIndex, endRow - 1, columnIndex);
        mergeData.push({
          startRow: rowIndex,
          endRow: endRow - 1,
          startColumn: columnIndex,
          endColumn: columnIndex,
        });
        for (let mergeRow = rowIndex + 1; mergeRow < endRow; mergeRow += 1) {
          headerRows[mergeRow][columnIndex] = "";
        }
      }
      rowIndex = endRow;
    }

    if (levels.length < headerDepth) {
      const startRow = levels.length - 1;
      if (startRow < headerDepth - 1) {
        markVerticalMergeCells(startRow, headerDepth - 1, columnIndex);
        mergeData.push({
          startRow,
          endRow: headerDepth - 1,
          startColumn: columnIndex,
          endColumn: columnIndex,
        });
        for (let mergeRow = startRow + 1; mergeRow < headerDepth; mergeRow += 1) {
          headerRows[mergeRow][columnIndex] = "";
        }
      }
    }
  });

  headerRows.forEach((headerRow, rowIndex) => {
    let columnIndex = 0;
    while (columnIndex < columns.length) {
      const title = headerRow[columnIndex];
      if (!title || verticalMergedCells.has(`${rowIndex}:${columnIndex}`)) {
        columnIndex += 1;
        continue;
      }

      let endColumn = columnIndex + 1;
      while (
        endColumn < columns.length &&
        headerRow[endColumn] === title &&
        !verticalMergedCells.has(`${rowIndex}:${endColumn}`)
      ) {
        endColumn += 1;
      }

      if (endColumn - columnIndex > 1) {
        mergeData.push({
          startRow: rowIndex,
          endRow: rowIndex,
          startColumn: columnIndex,
          endColumn: endColumn - 1,
        });
        for (let mergeColumn = columnIndex + 1; mergeColumn < endColumn; mergeColumn += 1) {
          headerRow[mergeColumn] = "";
        }
      }

      columnIndex = endColumn;
    }
  });

  return {
    headerRows,
    headerDepth,
    mergeData,
  };
};

const buildHeaderFreeze = (headerDepth: number, frozenColumnCount = 0): StandardSheetFreeze => {
  const frozenRowCount = Math.max(0, headerDepth);
  const normalizedFrozenColumnCount = Math.max(0, frozenColumnCount);
  return {
    startRow: frozenRowCount,
    startColumn: normalizedFrozenColumnCount,
    ySplit: frozenRowCount,
    xSplit: normalizedFrozenColumnCount,
  };
};

const toStandardWorkbook = (
  id: string,
  name: string,
  columns: StandardSheetColumn[],
  rows: Array<Record<string, unknown>>,
  frozenColumnCount = 0,
  formatNumericCell = true,
): IWorkbookData => {
  const sheetId = `${id}_sheet`;
  const headerLayout = buildHeaderLayout(columns);
  const rowCount = Math.max(rows.length + headerLayout.headerDepth, 30);
  const columnCount = Math.max(columns.length, 8);
  const cellData: NonNullable<IWorkbookData["sheets"][string]["cellData"]> = {};
  const filterEndRow = Math.max(headerLayout.headerDepth, rows.length + headerLayout.headerDepth - 1);
  const filterEndColumn = Math.max(0, columns.length - 1);

  headerLayout.headerRows.forEach((headerRow, rowIndex) => {
    cellData[rowIndex] = Object.fromEntries(
      headerRow.map((title, columnIndex) => [
        columnIndex,
        {
          v: title,
          s: resolveHeaderStyleId(title),
        },
      ]),
    );
  });

  rows.forEach((row, rowIndex) => {
    cellData[rowIndex + headerLayout.headerDepth] = Object.fromEntries(
      columns.map((column, columnIndex) => [
        columnIndex,
        toSheetCellData(column, row[column.field], formatNumericCell),
      ]),
    );
  });

  if (!rows.length) {
    cellData[headerLayout.headerDepth] = {
      0: {
        v: "暂无数据",
        s: sheetDataStyleId,
      },
    };
  }

  return {
    id,
    name,
    appVersion: "3.0.0",
    locale: LocaleType.ZH_CN,
    sheetOrder: [sheetId],
    resources: columns.length
      ? [
          {
            name: sheetFilterResourceName,
            data: JSON.stringify({
              [sheetId]: {
                ref: {
                  startRow: 0,
                  endRow: filterEndRow,
                  startColumn: 0,
                  endColumn: filterEndColumn,
                },
                filterColumns: columns.map((_, columnIndex) => ({ colId: columnIndex })),
              },
            }),
          },
        ]
      : undefined,
    styles: {
      [sheetHeaderStyleId]: {
        ...sheetHeaderCellStyle,
      },
      [sheetLeftHeaderStyleId]: {
        ...sheetLeftHeaderCellStyle,
      },
      [sheetDataStyleId]: {
        ...sheetDataCellStyle,
      },
      [sheetLeftTextDataStyleId]: {
        ...sheetLeftTextDataCellStyle,
      },
      [sheetNumericDataStyleId]: {
        ...sheetNumericDataCellStyle,
      },
    },
    sheets: {
      [sheetId]: {
        id: sheetId,
        name,
        rowCount,
        columnCount,
        cellData,
        mergeData: headerLayout.mergeData,
        columnData: Object.fromEntries(
          columns.map((column, columnIndex) => [
            columnIndex,
            { w: resolveConfiguredSheetColumnWidth(column, rows) },
          ]),
        ),
        rowData: Object.fromEntries(
          Array.from({ length: headerLayout.headerDepth }, (_, rowIndex) => [
            rowIndex,
            { h: 34 },
          ]),
        ),
        freeze: {
          ...buildHeaderFreeze(headerLayout.headerDepth, frozenColumnCount),
        },
      },
    },
  };
};

const standardBasicSheetColumns = computed<StandardSheetColumn[]>(() => [
  { field: "category", title: "分类", width: 140 },
  { field: "fieldName", title: "字段", width: 180 },
  { field: "fieldValue", title: "值", width: 420 },
]);

const collectFallbackRawColumns = (
  rows: Array<{ rawValues?: Record<string, string> }>,
): BatchValuationTaskStandardRawColumn[] => {
  const columns: BatchValuationTaskStandardRawColumn[] = [];
  const fieldSet = new Set<string>();
  rows.forEach((row) => {
    Object.keys(row.rawValues ?? {}).forEach((fieldKey) => {
      if (fieldSet.has(fieldKey)) {
        return;
      }
      fieldSet.add(fieldKey);
      columns.push({
        fieldKey: `rawFallback_${columns.length}`,
        title: fieldKey,
      });
    });
  });
  return columns;
};

const standardRawColumns = computed<BatchValuationTaskStandardRawColumn[]>(() => {
  const rawColumns = page.standardDataBasic?.rawColumns ?? [];
  if (rawColumns.length) {
    return rawColumns;
  }
  if (page.standardDataActiveTab === "subjects") {
    return collectFallbackRawColumns(page.standardDataSubjects);
  }
  if (page.standardDataActiveTab === "metrics") {
    return collectFallbackRawColumns(page.standardDataMetrics);
  }
  return [];
});

const standardSubjectRows = computed(() =>
  page.standardDataSubjects.map((row) => ({
    ...row,
    ...Object.fromEntries(
      standardRawColumns.value
        .filter((column) => column.fieldKey && column.title)
        .map((column) => {
          const fieldKey = column.fieldKey as string;
          const title = column.title as string;
          return [fieldKey, row.rawValues?.[fieldKey] ?? row.rawValues?.[title] ?? ""];
        }),
    ),
  })),
);

const semanticMetricType = (value?: string) => {
  const normalized = String(value ?? "").trim().toLowerCase();
  if (normalized === "metric_row") {
    return "指标行";
  }
  if (normalized === "metric_data") {
    return "指标值";
  }
  return value ?? "";
};

const semanticMetricValue = (type?: string, value?: string) => {
  const normalized = String(type ?? "").trim().toLowerCase();
  if (normalized === "metric_row") {
    return "-";
  }
  if (normalized === "metric_data") {
    return value ?? "";
  }
  return value ?? "";
};

const standardMetricRows = computed(() =>
  page.standardDataMetrics.map((row) => ({
    ...row,
    metricType: semanticMetricType(row.metricType),
    metricValue: semanticMetricValue(row.metricType, row.metricValue),
    ...Object.fromEntries(
      standardRawColumns.value
        .filter((column) => column.fieldKey && column.title)
        .map((column) => {
          const fieldKey = column.fieldKey as string;
          const title = column.title as string;
          return [fieldKey, row.rawValues?.[fieldKey] ?? row.rawValues?.[title] ?? ""];
        }),
    ),
  })),
);

const standardRawSheetColumns = computed<StandardSheetColumn[]>(() =>
  standardRawColumns.value
    .filter((column) => column.fieldKey && column.title)
    .map((column) => ({
      field: column.fieldKey as string,
      title: column.title as string,
      autoWidth: true,
    })),
);

const standardSubjectSheetColumns = computed<StandardSheetColumn[]>(() => [
  { field: "subjectCode", title: "科目编码", width: 130 },
  { field: "subjectName", title: "科目名称", width: 200 },
  { field: "levelNo", title: "层级", width: 80 },
  { field: "parentCode", title: "父级编码", width: 130 },
  { field: "rootCode", title: "根编码", width: 120 },
  { field: "leaf", title: "叶子", width: 80 },
  ...standardRawSheetColumns.value,
]);

const standardMetricSheetColumns = computed<StandardSheetColumn[]>(() => [
  { field: "metricName", title: "指标名称", width: 200 },
  { field: "metricType", title: "指标类型", width: 120 },
  { field: "metricValue", title: "指标值", width: 160 },
  ...standardRawSheetColumns.value,
]);

const standardBasicWorkbook = computed<IWorkbookData>(() =>
  toStandardWorkbook(
    "valuation_standard_basic",
    "基础信息",
    standardBasicSheetColumns.value,
    page.standardDataBasicRows,
    0,
    false,
  ),
);

const standardSubjectWorkbook = computed<IWorkbookData>(() =>
  toStandardWorkbook(
    "valuation_standard_subjects",
    "估值明细",
    standardSubjectSheetColumns.value,
    standardSubjectRows.value,
    2,
  ),
);

const standardMetricWorkbook = computed<IWorkbookData>(() =>
  toStandardWorkbook(
    "valuation_standard_metrics",
    "指标数据",
    standardMetricSheetColumns.value,
    standardMetricRows.value,
    2,
  ),
);

const standardDataSubtitleTags = computed(() => {
  const row = page.standardDataSelectedRow;
  return [
    {
      key: "batch",
      label: "批次",
      value: row?.batchName || row?.batchId,
      color: "blue",
    },
    {
      key: "businessDate",
      label: "业务日期",
      value: row?.businessDate,
      color: "green",
    },
  ].filter((item) => Boolean(item.value));
});

const standardDataTotalRows = computed(() => {
  if (page.standardDataActiveTab === "raw") {
    return page.standardDataRawWorkbook?.rowCount ?? 0;
  }
  if (page.standardDataActiveTab === "subjects") {
    return standardSubjectRows.value.length;
  }
  if (page.standardDataActiveTab === "metrics") {
    return standardMetricRows.value.length;
  }
  return page.standardDataBasicRows.length;
});

const standardDataTableLoading = computed(() => {
  if (page.standardDataActiveTab === "raw") {
    return page.standardDataRawLoading;
  }
  if (page.standardDataActiveTab === "subjects") {
    return page.standardDataSubjectsLoading;
  }
  if (page.standardDataActiveTab === "metrics") {
    return page.standardDataMetricsLoading;
  }
  return page.standardDataBasicLoading;
});

const standardDataExportDisabled = computed(() => {
  if (standardDataTableLoading.value || page.standardDataExportLoading) {
    return true;
  }
  if (page.standardDataActiveTab === "subjects") {
    return !standardSubjectRows.value.length;
  }
  if (page.standardDataActiveTab === "metrics") {
    return !standardMetricRows.value.length;
  }
  return !page.standardDataBasicRows.length;
});

const standardDataSkeletonColumns = computed(() =>
  page.standardDataActiveTab === "basic" ? 3 : page.standardDataActiveTab === "raw" ? 8 : 6,
);

const currentStandardDataSheet = () => {
  if (page.standardDataActiveTab === "raw") {
    return standardRawSheetRef.value;
  }
  if (page.standardDataActiveTab === "subjects") {
    return standardSubjectSheetRef.value;
  }
  if (page.standardDataActiveTab === "metrics") {
    return standardMetricSheetRef.value;
  }
  return standardBasicSheetRef.value;
};

const currentStandardDataSheetName = () => {
  if (page.standardDataActiveTab === "raw") {
    return "原始估值表";
  }
  if (page.standardDataActiveTab === "subjects") {
    return "估值明细";
  }
  if (page.standardDataActiveTab === "metrics") {
    return "指标数据";
  }
  return "基础信息";
};

const handleExportStandardDataSheet = () => {
  const workbookData = currentStandardDataSheet()?.save() ?? null;
  void page.exportStandardDataSheet(
    workbookData as unknown as Record<string, unknown> | null,
    currentStandardDataSheetName(),
  );
};

const handleStageCardClick = (stage: string) => {
  page.handleStageSelect(stage);
};

const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
  page.handlePageChange({
    current: pagination.current ?? page.pagination.current ?? 1,
    pageSize: pagination.pageSize ?? page.pagination.pageSize ?? 10,
  });
};

const confirmBatchRetry = () => {
  const selectedBatchIds = page.selectedBatchIds ?? [];
  if (!selectedBatchIds.length) {
    Modal.warning({
      title: "请先选择批次",
      content: "请选择至少一个已完成、失败或已停止的批次后再执行批量重新解析。",
    });
    return;
  }
  Modal.confirm({
    title: "确认批量重新解析",
    icon: () => h(ExclamationCircleOutlined),
    content: `确认对 ${selectedBatchIds.length} 个批次执行批量重新解析吗？\n${selectedBatchIds
      .slice(0, 8)
      .join("、")}${selectedBatchIds.length > 8 ? "…" : ""}`,
    okText: "确认",
    cancelText: "取消",
    onOk: () => page.batchRetrySelected(),
  });
};

const selectCurrentPageRows = async () => {
  await page.selectCurrentPageRows();
};

const selectedDetail = computed(() => page.detail?.batch ?? page.selectedRow);

const getStatusColor = (status?: string) => page.formatStatusColor(status);

const handleSummaryCardClick = (status?: string) => {
  if (!status) {
    return;
  }
  page.handleStatusSelect(status);
};

const rawWorkbookData = computed(
  () => page.standardDataRawWorkbook?.workbookData as IWorkbookData | undefined,
);

</script>

<template>
  <div class="batch-task-page">
    <YCard class="batch-task-header" :bordered="false" :padding="12">
      <div class="batch-task-metrics">
        <div
          v-for="card in summaryCards"
          :key="card.key"
          class="batch-task-metric"
          :role="card.clickable ? 'button' : undefined"
          :tabindex="card.clickable ? 0 : undefined"
          :aria-pressed="card.clickable ? isSummaryCardActive(card.status) : undefined"
          :aria-label="card.clickable ? `${card.label}，点击筛选` : undefined"
          :class="[
            `batch-task-metric--${card.color}`,
            card.clickable ? 'batch-task-metric--clickable' : '',
            card.status && isSummaryCardActive(card.status) ? 'batch-task-metric--active' : '',
          ]"
          @click="handleSummaryCardClick(card.status)"
          @keydown.enter.prevent="handleSummaryCardClick(card.status)"
          @keydown.space.prevent="handleSummaryCardClick(card.status)"
        >
          <div class="batch-task-metric__head">
            <div class="batch-task-metric__label">
              {{ card.label }}
            </div>
            <div class="batch-task-metric__value">
              <span :class="`batch-task-metric__value--${card.color}`">{{ card.value }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="batch-task-stage-area">
        <div class="batch-task-stage-chain">
          <button
            v-for="card in stageCards"
            :key="card.key"
            type="button"
            class="batch-task-stage-card"
            :class="[
              `batch-task-stage-card--${stageSummaryTone(card.key)}`,
              card.active ? 'batch-task-stage-card--active' : '',
            ]"
            :role="'button'"
            :tabindex="0"
            :aria-pressed="card.active"
            :aria-label="`按${card.label}筛选批量估值解析任务`"
            @click="handleStageCardClick(card.key)"
            @keydown.enter.prevent="handleStageCardClick(card.key)"
            @keydown.space.prevent="handleStageCardClick(card.key)"
          >
            <div class="batch-task-stage-card__main">
              <span class="batch-task-stage-card__index">{{ card.index }}</span>
              <strong class="batch-task-stage-card__title">{{ card.label }}</strong>
            </div>
            <div class="batch-task-stage-card__desc">
              {{ card.description }}
            </div>
          </button>
        </div>
      </div>

    <div class="batch-task-header__meta">
            <span class="batch-task-pill">
              {{ summaryDescription }}
            </span>
    </div>
    </YCard>

    <YCard class="batch-task-list-card" :bordered="false" :padding="12">
      <div class="batch-task-query-bar">
        <a-form layout="inline" class="batch-task-query-form" :model="page.query">
          <a-form-item :label="batchValuationTaskPageText.query.batchId">
            <a-input
              v-model:value="page.query.batchId"
              placeholder="批次ID"
              allow-clear
              style="width: 180px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="batchValuationTaskPageText.query.taskDate">
            <a-date-picker
              v-model:value="taskDateValue"
              format="YYYY-MM-DD"
              allow-clear
              style="width: 150px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="batchValuationTaskPageText.query.businessDate">
            <a-date-picker
              v-model:value="businessDateValue"
              format="YYYY-MM-DD"
              allow-clear
              style="width: 150px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="batchValuationTaskPageText.query.managerName">
            <a-input
              v-model:value="page.query.managerName"
              placeholder="管理机构"
              allow-clear
              style="width: 150px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="batchValuationTaskPageText.query.productKeyword">
            <a-input
              v-model:value="page.query.productKeyword"
              placeholder="产品名称或代码"
              allow-clear
              style="width: 180px"
              size="small"
            />
          </a-form-item>
          <a-form-item :label="batchValuationTaskPageText.query.stage">
            <a-select
              v-model:value="page.query.taskStage"
              allow-clear
              placeholder="全部"
              style="width: 150px"
              size="small"
            >
              <a-select-option
                v-for="item in batchValuationTaskStageCatalog"
                :key="item.stage"
                :value="item.stage"
              >
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="batchValuationTaskPageText.query.status">
            <a-select
              v-model:value="page.query.status"
              allow-clear
              placeholder="全部"
              style="width: 130px"
              size="small"
            >
              <a-select-option
                v-for="item in batchValuationTaskStatusCatalog"
                :key="item.status"
                :value="item.status"
              >
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <div class="batch-task-query-actions">
              <div class="batch-task-auto-refresh">
            <span class="batch-task-auto-refresh__time">
              <ClockCircleOutlined />
              最近更新 {{ page.lastUpdatedAt || "--:--:--" }}
            </span>
                <span class="batch-task-auto-refresh__label">
              <ReloadOutlined />
              自动刷新
            </span>
                <a-select
                    class="batch-task-auto-refresh__select"
                :value="page.autoRefreshInterval"
                size="small"
                @change="(value) => page.setAutoRefreshInterval(Number(value))"
                >
                <a-select-option
                    v-for="item in page.autoRefreshOptions"
                :key="item.value"
                :value="item.value"
                >
                {{ item.label }}
              </a-select-option>
            </a-select>
          </div>
<YButton size="small" type="primary" @click="page.runQuery">
    <template #icon><SearchOutlined /></template>
查询
</YButton>
<YButton
    size="small"
    type="primary"
    ghost
            :loading="page.batchRetryLoading"
:disabled="!page.selectedBatchIds.length"
@click="confirmBatchRetry"
    >
    批量重新解析
    </YButton>
<YButton size="small" @click="page.resetQuery">
    <template #icon><ReloadOutlined /></template>
重置
</YButton>
</div>
          </a-form-item>
        </a-form>

      </div>
      <div ref="tableAreaRef" class="batch-task-table">
        <YTable
          ref="yTableRef"
          :columns="columnsWithAction"
          :action-config="actionConfig"
          :data="page.rows"
          :loading="page.loading || page.detailLoading"
          :max-height="tableHeight"
          :row-config="{ keyField: 'batchId' }"
          :checkbox-config="{ highlight: true }"
          :pageable="true"
          :autoFlexColumn="false"
          :border="false"
          v-model:pagination="page.pagination"
          :toolbar-config="{ custom: false }"
          @page-change="handleTableChange"
          @checkbox-change="page.syncSelectionFromTable"
          @checkbox-all="page.syncSelectionFromTable"
        >
          <template #toolbar-left>
            <WorkspaceTableToolbar
              title="批次列表"
              :description="`总数 ${page.totalCount} 条，已选 ${page.selectedBatchIds.length} 条。仅允许已完成、失败或已停止的批次重新解析。`"
              :meta="`当前页 ${page.rows.length} 条`"
            >
              <a-space>
                <YButton size="small" :disabled="!page.rows.length" @click="selectCurrentPageRows">
                  选中当前页
                </YButton>
                <YButton
                  size="small"
                  :disabled="!page.selectedBatchIds.length"
                  @click="page.clearSelection"
                >
                  清除选择
                </YButton>
              </a-space>
            </WorkspaceTableToolbar>
          </template>
          <template #sourceTypeName="{ row }">
            {{ row.sourceTypeName || resolveBatchValuationTaskSourceTypeLabel(row.sourceType) }}
          </template>
          <template #statusName="{ row }">
            <a-tag :color="getStatusColor(row.status)">
              {{ row.statusName || row.status }}
            </a-tag>
          </template>
          <template #lastErrorMessage="{ row }">
            <span class="batch-task-table__ellipsis">
              {{ row.lastErrorMessage || "-" }}
            </span>
          </template>
        </YTable>
      </div>
    </YCard>

    <a-drawer
      :open="page.detailVisible"
      width="920"
      :title="batchValuationTaskPageText.detail.title"
      :destroy-on-close="true"
      @close="page.closeDetailDrawer"
    >
        <a-spin :spinning="page.detailLoading">
          <a-descriptions :column="2" bordered size="small" class="batch-task-detail">
          <a-descriptions-item :label="batchValuationTaskPageText.detail.batchId">
            {{ selectedDetail?.batchId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.workflowCode">
            {{ page.summary.workflowCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.workflowId">
            {{ page.summary.workflowId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.versionNo">
            {{ page.summary.versionNo || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.currentStage">
            {{ selectedDetail?.currentStageName || selectedDetail?.currentStage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.status">
            <a-tag :color="getStatusColor(selectedDetail?.status)">{{ selectedDetail?.statusName || selectedDetail?.status || "-" }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.progress">
            {{ selectedDetail?.progress ?? "-" }}%
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.startedAt">
            {{ selectedDetail?.startedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.endedAt">
            {{ selectedDetail?.endedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.durationText">
            {{ selectedDetail?.durationText || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.fileId">
            {{ selectedDetail?.fileId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.filesysFileId">
            {{ selectedDetail?.filesysFileId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item :label="batchValuationTaskPageText.detail.originalFileName" :span="2">
            {{ selectedDetail?.originalFileName || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions :column="1" bordered size="small" class="batch-task-detail__summary">
          <a-descriptions-item :label="batchValuationTaskPageText.detail.logRef">
            {{ selectedDetail?.batchId || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="batch-task-detail__steps">
          <div class="batch-task-detail__title">{{ batchValuationTaskPageText.detail.stepInfo }}</div>
          <YTable
            :columns="[
              {
                field: 'stepName',
                title: '阶段',
                width: 140,
              },
              {
                field: 'statusName',
                title: '状态',
                width: 100,
              },
              {
                field: 'startedAt',
                title: '开始时间',
                width: 160,
              },
              {
                field: 'endedAt',
                title: '结束时间',
                width: 160,
              },
              {
                field: 'durationText',
                title: '耗时',
                width: 90,
              },
              {
                field: 'errorMessage',
                title: '错误摘要',
                showOverflow: 'tooltip',
              },
            ]"
            :data="page.detail?.steps ?? []"
            :pageable="false"
            :row-config="{ keyField: 'stepId' }"
            :border="false"
            :autoFlexColumn="false"
          >
            <template #statusName="{ row }">
              <a-tag :color="getStatusColor(row.status)">
                {{ row.statusName || row.status }}
              </a-tag>
            </template>
            <template #errorMessage="{ row }">
              <span class="batch-task-table__ellipsis">
                {{ row.errorMessage || "-" }}
              </span>
            </template>
          </YTable>
        </div>
      </a-spin>
    </a-drawer>

    <a-modal
      class="batch-standard-modal"
      :open="page.standardDataVisible"
      width="90vw"
      :mask-closable="false"
      :keyboard="false"
      :footer="null"
      :destroy-on-close="false"
      @cancel="page.closeStandardDataModal"
    >
      <template #title>
        <div class="batch-standard-modal__title-row">
          <div class="batch-standard-modal__title">
            <span>估值解析数据查看</span>
          </div>
          <a-space>
            <a-tag
              size="small"
              color="blue"
            >
              总行数：{{ standardDataTotalRows }}
            </a-tag>
            <a-tag
              v-for="item in standardDataSubtitleTags"
              :key="item.key"
              size="small"
              :color="item.color"
            >
              {{ item.label }}：{{ item.value }}
            </a-tag>
          </a-space>
        </div>
      </template>

      <div class="batch-standard-shell">
        <div class="batch-standard-tabs">
          <button
            type="button"
            class="batch-standard-tab"
            :class="page.standardDataActiveTab === 'basic' ? 'batch-standard-tab--active' : ''"
            @click="page.handleStandardDataTabChange('basic')"
          >
            基础信息
          </button>
          <button
            type="button"
            class="batch-standard-tab"
            :class="page.standardDataActiveTab === 'subjects' ? 'batch-standard-tab--active' : ''"
            @click="page.handleStandardDataTabChange('subjects')"
          >
            估值明细
          </button>
          <button
            type="button"
            class="batch-standard-tab"
            :class="page.standardDataActiveTab === 'metrics' ? 'batch-standard-tab--active' : ''"
            @click="page.handleStandardDataTabChange('metrics')"
          >
            指标数据
          </button>
          <button
            type="button"
            class="batch-standard-tab"
            :class="page.standardDataActiveTab === 'raw' ? 'batch-standard-tab--active' : ''"
            @click="page.handleStandardDataTabChange('raw')"
          >
            原始估值表
          </button>
        </div>

        <div class="batch-standard-panel">
          <div class="batch-standard-toolbar">
            <a-input-search
              v-if="page.standardDataActiveTab === 'subjects'"
              v-model:value="page.standardDataSubjectsKeyword"
              size="small"
              placeholder="搜索科目编码或名称..."
              allow-clear
              style="width: 320px"
              @change="!page.standardDataSubjectsKeyword && page.searchStandardDataSubjects()"
              @search="page.searchStandardDataSubjects"
            />
            <a-input-search
              v-else-if="page.standardDataActiveTab === 'metrics'"
              v-model:value="page.standardDataMetricsKeyword"
              placeholder="搜索指标名称、类型或值..."
              size="small"
              allow-clear
              style="width: 320px"
              @change="!page.standardDataMetricsKeyword && page.searchStandardDataMetrics()"
              @search="page.searchStandardDataMetrics"
            />
            <div v-else-if="page.standardDataActiveTab === 'raw'" class="batch-standard-toolbar__placeholder">
              文件：{{ page.standardDataRawWorkbook?.fileName || "-" }} · Sheet：{{ page.standardDataRawWorkbook?.sheetCount ?? 0 }} · 行数：{{ page.standardDataRawWorkbook?.rowCount ?? 0 }}
              <a-tag
                v-if="page.standardDataRawWorkbook?.downloadedFromTarget"
                color="orange"
                class="batch-standard-raw-tip"
              >
                {{ page.standardDataRawWorkbook?.fallbackMessage || "已重新下载源文件" }}
              </a-tag>
            </div>
            <div v-else class="batch-standard-toolbar__placeholder">
              估值ID：{{ page.standardDataBasic?.valuationId || "-" }} · Sheet：{{ page.standardDataBasic?.sheetName || "-" }}
            </div>
            <a-space>
              <YButton size="small" :loading="standardDataTableLoading" @click="page.refreshStandardData">
              <template #icon><ReloadOutlined /></template>
            刷新
          </YButton>
          <YButton
            v-if="page.standardDataActiveTab === 'raw'"
            size="small"
            :loading="page.standardDataRawDownloadLoading"
            :disabled="standardDataTableLoading"
            @click="page.downloadRawWorkbook"
          >
            <template #icon><DownloadOutlined /></template>
            下载
          </YButton>
          <YButton
              v-else
              size="small"
          :loading="page.standardDataExportLoading"
          :disabled="standardDataExportDisabled"
          @click="handleExportStandardDataSheet"
          >
          <template #icon><ExportOutlined /></template>
            导出
            </YButton>
            </a-space>
          </div>

          <div class="batch-standard-table">
            <div
              v-if="standardDataTableLoading"
              class="batch-standard-sheet-skeleton"
              :style="{ '--batch-standard-skeleton-columns': standardDataSkeletonColumns }"
            >
              <div class="batch-standard-sheet-skeleton__toolbar">
                <span class="batch-standard-sheet-skeleton__pill"></span>
                <span class="batch-standard-sheet-skeleton__pill batch-standard-sheet-skeleton__pill--short"></span>
              </div>
              <div class="batch-standard-sheet-skeleton__grid">
                <div class="batch-standard-sheet-skeleton__header">
                  <span
                    v-for="index in standardDataSkeletonColumns"
                    :key="`header-${index}`"
                  ></span>
                </div>
                <div
                  v-for="rowIndex in 9"
                  :key="`row-${rowIndex}`"
                  class="batch-standard-sheet-skeleton__row"
                >
                  <span
                    v-for="columnIndex in standardDataSkeletonColumns"
                    :key="`cell-${rowIndex}-${columnIndex}`"
                    :style="{ width: `${72 + ((rowIndex + columnIndex) % 3) * 12}%` }"
                  ></span>
                </div>
              </div>
            </div>

            <template v-else-if="page.standardDataActiveTab === 'basic'">
              <template v-if="page.standardDataBasicRows.length">
                <NativeUniverSheet
                  ref="standardBasicSheetRef"
                  :model-value="standardBasicWorkbook"
                  :readonly="true"
                  :config="{
                    header: false,
                    toolbar: false,
                    formulaBar: false,
                    footer: { addSheetButtonConfig: { show: false } },
                    contextMenu: false
                  }"
                />
              </template>
              <template v-else>
                <div class="batch-standard-empty">
                  <strong>还没有估值基础数据</strong>
                  <span>估值贴源数据落地完成后，这里会展示估值主表和基础信息。</span>
                </div>
              </template>
            </template>

            <template v-else-if="page.standardDataActiveTab === 'subjects'">
              <template v-if="standardSubjectRows.length">
                <NativeUniverSheet
                  ref="standardSubjectSheetRef"
                  :model-value="standardSubjectWorkbook"
                  :readonly="true"
                  :config="{
                    header: false,
                    toolbar: false,
                    formulaBar: false,
                    footer: { addSheetButtonConfig: { show: false } },
                    contextMenu: false
                  }"
                />
              </template>
              <template v-else>
                <div class="batch-standard-empty">
                  <strong>还没有估值明细</strong>
                  <span>估值贴源数据落地完成后，这里会展示科目明细数据。</span>
                </div>
              </template>
            </template>

            <template v-else-if="page.standardDataActiveTab === 'metrics'">
              <template v-if="standardMetricRows.length">
                <NativeUniverSheet
                  ref="standardMetricSheetRef"
                  :model-value="standardMetricWorkbook"
                  :readonly="true"
                  :config="{
                    header: false,
                    toolbar: false,
                    formulaBar: false,
                    footer: { addSheetButtonConfig: { show: false } },
                    contextMenu: false
                  }"
                />
              </template>
              <template v-else>
                <div class="batch-standard-empty">
                  <strong>还没有指标数据</strong>
                  <span>估值贴源数据落地完成后，这里会展示指标明细数据。</span>
                </div>
              </template>
            </template>

            <template v-else>
              <template v-if="rawWorkbookData">
                <NativeUniverSheet
                  ref="standardRawSheetRef"
                  :model-value="rawWorkbookData"
                  :readonly="true"
                  :config="{
                    header: false,
                    toolbar: false,
                    formulaBar: false,
                    footer: { addSheetButtonConfig: { show: false } },
                    contextMenu: false
                  }"
                />
              </template>
              <template v-else>
                <div class="batch-standard-empty">
                  <strong>原始估值表不可用</strong>
                  <span>{{ page.standardDataRawError || "切换到原始估值表后将从源文件加载全工作簿内容。" }}</span>
                </div>
              </template>
            </template>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>
