import dayjs from "dayjs";
import { computed, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import {
  batchExecuteOutsourcedDataTasks,
  batchRetryOutsourcedDataTasks,
  batchStopOutsourcedDataTasks,
  executeOutsourcedDataTask,
  getOutsourcedDataTask,
  getOutsourcedDataTaskSummary,
  listOutsourcedDataTaskSteps,
  pageOutsourcedDataTaskLogs,
  pageOutsourcedDataTasks,
  retryOutsourcedDataTask,
  retryOutsourcedDataTaskStep,
  stopOutsourcedDataTask,
  type OutsourcedDataTaskBatchDetailDTO,
  type OutsourcedDataTaskBatchDTO,
  type OutsourcedDataTaskLogDTO,
  type OutsourcedDataTaskStepDTO,
  type OutsourcedDataTaskSummaryDTO,
} from "@/api/outsourcedDataTask";
import { unwrapMultiResult, unwrapSingleResult } from "@/utils/api-response";
import type {
  OutsourcedDataTaskBatchRow,
  OutsourcedDataTaskDataEntry,
  OutsourcedDataTaskLogRow,
  OutsourcedDataTaskManualState,
  OutsourcedDataTaskPage,
  OutsourcedDataTaskQueryState,
  OutsourcedDataTaskStage,
  OutsourcedDataTaskStepRow,
  OutsourcedDataTaskStepSummary,
  OutsourcedDataTaskStatus,
} from "../types";
import {
  outsourcedDataTaskStageCatalog,
  outsourcedDataTaskStatusCatalog,
  outsourcedDataTaskTriggerModeLabels,
  outsourcedDataTaskDataEntryStatusLabels,
  outsourcedDataTaskMetricCardText,
  outsourcedDataTaskPreviewText,
  outsourcedDataTaskTableTexts,
} from "../constants";

const defaultStageCatalog = outsourcedDataTaskStageCatalog;
const activeStageCatalog = ref(defaultStageCatalog);
const defaultStageName = (stage: OutsourcedDataTaskStage) =>
  defaultStageCatalog.find((item) => item.stage === stage)?.stageName ?? stage;

const normalizeStageCatalog = (
  summaries?: OutsourcedDataTaskStepSummary[],
): Array<{
  stage: OutsourcedDataTaskStage;
  step: OutsourcedDataTaskStage;
  stageName: string;
  stepName: string;
  stageDescription: string;
  stepDescription: string;
}> =>
  summaries?.length
    ? summaries.map((item) => {
        const stage = String(item.stage ?? item.step ?? "").trim() as OutsourcedDataTaskStage;
        return {
          stage,
          step: stage,
          stageName: item.stageName ?? item.stepName ?? stage,
          stepName: item.stepName ?? item.stageName ?? stage,
          stageDescription: item.stageDescription ?? item.stepDescription ?? "",
          stepDescription: item.stepDescription ?? item.stageDescription ?? "",
        };
      })
    : defaultStageCatalog;

const todayBusinessDate = dayjs().format("YYYY-MM-DD");

const defaultQuery = (): OutsourcedDataTaskQueryState => ({
  businessDate: todayBusinessDate,
  managerName: "",
  productKeyword: "",
  step: "",
  status: "",
  sourceType: "",
  errorType: "",
  includeHistory: false,
});

const LIVE_STATUS_SET = new Set<OutsourcedDataTaskStatus>([
  "PENDING",
  "RUNNING",
]);

const normalizeStage = (value?: string): OutsourcedDataTaskStage => {
  const stage = String(value ?? "").trim() as OutsourcedDataTaskStage;
  return activeStageCatalog.value.some((item) => item.stage === stage)
    ? stage
    : activeStageCatalog.value[0]?.stage ?? "FILE_PARSE";
};

const normalizeStatus = (value?: string): OutsourcedDataTaskStatus => {
  const status = String(value ?? "").trim() as OutsourcedDataTaskStatus;
  return outsourcedDataTaskStatusCatalog.some((item) => item.status === status)
    ? status
    : "PENDING";
};

const statusLabel = (value?: string) => {
  const status = normalizeStatus(value);
  return (
    outsourcedDataTaskStatusCatalog.find((item) => item.status === status)?.label ??
    status
  );
};

const triggerModeLabel = (value: string) =>
  outsourcedDataTaskTriggerModeLabels[value] ?? value;

const matches = (actual: string | undefined, keyword: string) => {
  if (!keyword.trim()) {
    return true;
  }
  return String(actual ?? "")
    .toLowerCase()
    .includes(keyword.trim().toLowerCase());
};

const getStageOrder = (stage: string) => {
  const index = activeStageCatalog.value.findIndex((item) => item.stage === stage);
  return index >= 0 ? index : activeStageCatalog.value.length;
};

const hasStage = (row: OutsourcedDataTaskBatchRow, stage: string) =>
  row.currentStep === stage ||
  row.currentStage === stage ||
  row.steps.some((step) => step.step === stage || step.stage === stage);

const resolveBatchStatusFromSteps = (steps: OutsourcedDataTaskStepRow[]) => {
  if (steps.some((step) => step.status === "FAILED")) {
    return "FAILED" as const;
  }
  if (steps.some((step) => step.status === "BLOCKED")) {
    return "BLOCKED" as const;
  }
  if (steps.some((step) => step.status === "RUNNING")) {
    return "RUNNING" as const;
  }
  if (steps.length > 0 && steps.every((step) => step.status === "STOPPED")) {
    return "STOPPED" as const;
  }
  if (steps.length > 0 && steps.every((step) => step.status === "SUCCESS")) {
    return "SUCCESS" as const;
  }
  return "PENDING" as const;
};

const resolveBatchStageFromSteps = (steps: OutsourcedDataTaskStepRow[]) => {
  const currentStep = [...steps]
    .filter(
      (step) =>
        step.status === "RUNNING" ||
        step.status === "FAILED" ||
        step.status === "BLOCKED",
    )
    .sort((left, right) => {
      const updatedAtDiff = String(right.startedAt ?? "").localeCompare(
        String(left.startedAt ?? ""),
      );
      if (updatedAtDiff !== 0) {
        return updatedAtDiff;
      }
      return (
        getStageOrder(normalizeStage(right.stage ?? right.step)) -
        getStageOrder(normalizeStage(left.stage ?? left.step))
      );
    })[0];
  if (currentStep) {
    return normalizeStage(currentStep.stage ?? currentStep.step);
  }
  const lastStep = steps[steps.length - 1];
  return lastStep
    ? normalizeStage(lastStep.stage ?? lastStep.step)
    : "FILE_PARSE";
};

const normalizeBatchRowFromSteps = (
  row: OutsourcedDataTaskBatchRow,
  steps: OutsourcedDataTaskStepRow[],
): OutsourcedDataTaskBatchRow => {
  const orderedSteps = sortSteps(steps);
  if (!orderedSteps.length) {
    return { ...row, steps: orderedSteps };
  }
  const currentStage = resolveBatchStageFromSteps(orderedSteps);
  const currentStatus = resolveBatchStatusFromSteps(orderedSteps);
  const currentStep =
    [...orderedSteps].reverse().find((step) => step.stage === currentStage) ??
    orderedSteps[orderedSteps.length - 1];
  const stageMeta = activeStageCatalog.value.find((item) => item.stage === currentStage);
  return {
    ...row,
    currentStage,
    currentStep: currentStage,
    currentStageName: stageMeta?.stageName ?? row.currentStageName,
    currentStepName:
      stageMeta?.stageName ?? row.currentStepName ?? row.currentStageName,
    status: currentStatus,
    statusName: statusLabel(currentStatus),
    progress:
      currentStatus === "SUCCESS"
        ? 100
        : currentStatus === "FAILED" || currentStatus === "BLOCKED"
          ? Number(currentStep?.progress ?? row.progress ?? 0)
          : currentStatus === "RUNNING"
            ? Number(currentStep?.progress ?? row.progress ?? 0)
            : Number(row.progress ?? 0),
    startedAt: row.startedAt || String(currentStep?.startedAt ?? ""),
    endedAt:
      currentStatus === "SUCCESS" ||
      currentStatus === "FAILED" ||
      currentStatus === "BLOCKED" ||
      currentStatus === "STOPPED"
        ? (currentStep?.endedAt ?? row.endedAt)
        : row.endedAt,
    durationText: String(currentStep?.durationText ?? row.durationText ?? "-"),
    steps: orderedSteps,
  };
};

const sortSteps = (steps: OutsourcedDataTaskStepRow[]) =>
  [...steps].sort((left, right) => {
    const stageDiff =
      getStageOrder(normalizeStage(left.stage ?? left.step)) -
      getStageOrder(normalizeStage(right.stage ?? right.step));
    if (stageDiff !== 0) {
      return stageDiff;
    }
    return Number(left.runNo ?? 0) - Number(right.runNo ?? 0);
  });

const buildSteps = (
  batchId: string,
  currentStage: OutsourcedDataTaskStage,
  status: OutsourcedDataTaskStatus,
): OutsourcedDataTaskStepRow[] => {
  const stageCatalog = activeStageCatalog.value;
  const currentIndex = stageCatalog.findIndex(
    (item) => item.stage === currentStage,
  );
  return sortSteps(
    stageCatalog.map((item, index) => {
      const isBefore = index < currentIndex;
      const isCurrent = index === currentIndex;
      const stepStatus: OutsourcedDataTaskStatus = isBefore
        ? "SUCCESS"
        : isCurrent
          ? status
          : "PENDING";
      return {
        stepId: `${batchId}-${item.stage}`,
        batchId,
        stage: item.stage,
        step: item.stage,
        stageName: item.stageName,
        stepName: item.stageName,
        taskId: `TASK-${batchId}-${index + 1}`,
        taskType: item.stage,
        runNo: 1,
        triggerMode: index === 0 ? "SCHEDULE" : "DEPENDENCY",
        triggerModeName: triggerModeLabel(
          index === 0 ? "SCHEDULE" : "DEPENDENCY",
        ),
        status: stepStatus,
        statusName: statusLabel(stepStatus),
        progress: stepStatus === "SUCCESS" ? 100 : isCurrent ? 66 : 0,
        startedAt: "2025-02-27 09:30:00",
        endedAt:
          stepStatus === "SUCCESS" || stepStatus === "FAILED"
            ? "2025-02-27 09:32:00"
            : undefined,
        durationText: stepStatus === "PENDING" ? "-" : "2m",
        inputSummary: item.stageDescription,
        outputSummary:
          stepStatus === "SUCCESS"
            ? `${item.stageName}${outsourcedDataTaskPreviewText.stepCompletedSuffix}`
            : "",
        errorCode: isCurrent && status === "FAILED" ? "TASK_FAILED" : "",
        errorMessage:
          isCurrent && status === "FAILED"
            ? outsourcedDataTaskPreviewText.landingFailureMessage
            : "",
        errorStack:
          isCurrent && status === "FAILED"
            ? "com.yss.valset.task.StandardLandingException: DWD 持仓写入冲突\n\tat com.yss.valset.standardize.StandardLandingService.writeDwd(StandardLandingService.java:128)\n\tat com.yss.valset.batch.dispatcher.DefaultTaskDispatcher.dispatch(DefaultTaskDispatcher.java:76)"
            : "",
        logRef: `task:${batchId}:${item.stage}`,
      };
    }),
  );
};

const mapStep = (
  step: OutsourcedDataTaskStepDTO,
): OutsourcedDataTaskStepRow => {
  const stage = normalizeStage(step.step ?? step.stage);
  const status = normalizeStatus(step.status);
  const stageMeta = activeStageCatalog.value.find((item) => item.stage === stage);
  return {
    stepId: String(step.stepId ?? ""),
    batchId: String(step.batchId ?? ""),
    stage,
    step: stage,
    stageName: step.stepName ?? step.stageName ?? stageMeta?.stageName ?? stage,
    stepName: step.stepName ?? step.stageName ?? stageMeta?.stageName ?? stage,
    taskId: String(step.taskId ?? ""),
    taskType: String(step.taskType ?? stage),
    runNo: Number(step.runNo ?? 0),
    triggerMode: String(step.triggerMode ?? ""),
    triggerModeName: String(step.triggerModeName ?? "-"),
    status,
    statusName: step.statusName ?? statusLabel(status),
    progress: Number(step.progress ?? 0),
    startedAt: String(step.startedAt ?? ""),
    endedAt: step.endedAt,
    durationText: String(step.durationText ?? "-"),
    inputSummary: step.inputSummary,
    outputSummary: step.outputSummary,
    errorCode: step.errorCode,
    errorMessage: step.errorMessage,
    errorStack: step.errorStack,
    logRef: step.logRef,
  };
};

const buildDataEntries = (
  row: OutsourcedDataTaskBatchRow | null,
  detail?: OutsourcedDataTaskBatchDetailDTO | null,
): OutsourcedDataTaskDataEntry[] => {
  const hasFile = Boolean(
    row?.filesysFileId || row?.fileId || row?.originalFileName,
  );
  const entries: OutsourcedDataTaskDataEntry[] = [
    {
      key: "source-file",
      name: outsourcedDataTaskTableTexts.dataEntryNames.sourceFile,
      description: row?.originalFileName
        ? `${row.originalFileName}${outsourcedDataTaskTableTexts.dataEntryDescriptions.sourceFileExisting}${row.filesysFileId || row.fileId || "-"}`
        : outsourcedDataTaskTableTexts.dataEntryDescriptions.sourceFileMissing,
      status: hasFile ? "READY" : "WAITING",
      statusName: hasFile
        ? outsourcedDataTaskDataEntryStatusLabels.READY
        : outsourcedDataTaskDataEntryStatusLabels.WAITING,
      href: detail?.rawDataUrl,
    },
    {
      key: "parse-result",
      name: outsourcedDataTaskTableTexts.dataEntryNames.parseResult,
      description: outsourcedDataTaskTableTexts.dataEntryDescriptions.parseResult,
      status: detail?.fileResultUrl
        ? "READY"
        : row?.currentStep === "FILE_PARSE" ||
            row?.currentStage === "FILE_PARSE"
          ? "WAITING"
          : "READY",
      statusName: detail?.fileResultUrl
        ? outsourcedDataTaskDataEntryStatusLabels.READY
        : row?.currentStep === "FILE_PARSE" ||
            row?.currentStage === "FILE_PARSE"
          ? statusLabel("RUNNING")
          : outsourcedDataTaskDataEntryStatusLabels.READY,
      href: detail?.fileResultUrl,
    },
    {
      key: "stg",
      name: outsourcedDataTaskTableTexts.dataEntryNames.stg,
      description: outsourcedDataTaskTableTexts.dataEntryDescriptions.stg,
      status: detail?.stgDataUrl
        ? "READY"
        : row?.progress && row.progress >= 45
          ? "READY"
          : "WAITING",
      statusName: detail?.stgDataUrl
        ? outsourcedDataTaskDataEntryStatusLabels.READY
        : row?.progress && row.progress >= 45
          ? outsourcedDataTaskDataEntryStatusLabels.READY
          : outsourcedDataTaskDataEntryStatusLabels.WAITING,
      href: detail?.stgDataUrl,
    },
    {
      key: "dwd",
      name: outsourcedDataTaskTableTexts.dataEntryNames.dwd,
      description: outsourcedDataTaskTableTexts.dataEntryDescriptions.dwd,
      status:
        (row?.currentStep === "STANDARD_LANDING" ||
          row?.currentStage === "STANDARD_LANDING") &&
        row.status === "FAILED"
          ? "ERROR"
          : detail?.dwdDataUrl
            ? "READY"
            : row?.progress && row.progress >= 70
              ? "READY"
              : "WAITING",
      statusName:
        (row?.currentStep === "STANDARD_LANDING" ||
          row?.currentStage === "STANDARD_LANDING") &&
        row.status === "FAILED"
          ? outsourcedDataTaskDataEntryStatusLabels.ERROR
          : detail?.dwdDataUrl
            ? outsourcedDataTaskDataEntryStatusLabels.READY
            : row?.progress && row.progress >= 70
              ? outsourcedDataTaskDataEntryStatusLabels.READY
              : outsourcedDataTaskDataEntryStatusLabels.WAITING,
      href: detail?.dwdDataUrl,
    },
    {
      key: "standard",
      name: outsourcedDataTaskTableTexts.dataEntryNames.standard,
      description: outsourcedDataTaskTableTexts.dataEntryDescriptions.standard,
      status: detail?.standardDataUrl
        ? "READY"
        : row?.status === "SUCCESS"
          ? "READY"
          : "WAITING",
      statusName: detail?.standardDataUrl
        ? outsourcedDataTaskDataEntryStatusLabels.READY
        : row?.status === "SUCCESS"
          ? statusLabel("SUCCESS")
          : outsourcedDataTaskDataEntryStatusLabels.WAITING,
      href: detail?.standardDataUrl,
    },
  ];
  return entries;
};

const buildLogRows = (
  row: OutsourcedDataTaskBatchRow | null,
): OutsourcedDataTaskLogRow[] =>
  sortSteps(row?.steps ?? []).map((step) => ({
    key: step.stepId,
    stageName: step.stageName,
    stepName: step.stepName,
    status: step.status,
    statusName: step.statusName,
    startedAt: step.startedAt || "-",
    durationText: step.durationText || "-",
    message:
      step.errorMessage ||
      step.outputSummary ||
      step.inputSummary ||
      outsourcedDataTaskPreviewText.stepLogWaitingText,
    errorStack: step.errorStack,
  }));

const mapLogRow = (
  log: OutsourcedDataTaskLogDTO,
  activeStep: OutsourcedDataTaskStepRow,
): OutsourcedDataTaskLogRow => ({
  key: String(
    log.logId ??
      `${activeStep.stepId}-${log.occurredAt ?? ""}-${log.message ?? ""}`,
  ),
  stageName: activeStep.stageName,
  stepName: activeStep.stepName,
  status: activeStep.status,
  statusName: activeStep.statusName,
  startedAt: String(log.occurredAt ?? activeStep.startedAt ?? "-"),
  durationText: activeStep.durationText || "-",
  message: String(log.message ?? ""),
  logLevel: String(log.logLevel ?? "INFO"),
  occurredAt: String(log.occurredAt ?? ""),
});

const buildManualState = (
  row: OutsourcedDataTaskBatchRow | null,
  detail?: OutsourcedDataTaskBatchDetailDTO | null,
): OutsourcedDataTaskManualState => {
  const failedStep = row?.steps.find((step) =>
    ["FAILED", "BLOCKED"].includes(step.status),
  );
  const currentBlockPoint =
    detail?.currentBlockPoint ||
    failedStep?.errorMessage ||
    row?.lastErrorMessage ||
    (row?.status === "SUCCESS"
      ? outsourcedDataTaskPreviewText.currentBatchCompleted
      : outsourcedDataTaskPreviewText.noClearBlockPoint);
  return {
    currentBlockPoint,
    exceptionConfirmText:
      row?.status === "FAILED" || row?.status === "BLOCKED"
        ? outsourcedDataTaskPreviewText.exceptionConfirmText
        : outsourcedDataTaskPreviewText.notExceptionalText,
    rerunPrerequisites: outsourcedDataTaskPreviewText.rerunPrerequisites,
  };
};

const mapBatch = (
  batch: OutsourcedDataTaskBatchDTO,
  steps?: OutsourcedDataTaskStepDTO[],
): OutsourcedDataTaskBatchRow => {
  const batchId = String(batch.batchId ?? "");
  const stage = normalizeStage(batch.currentStep ?? batch.currentStage);
  const status = normalizeStatus(batch.status);
  const stageMeta = activeStageCatalog.value.find((item) => item.stage === stage);
  return {
    batchId,
    batchName: String(batch.batchName ?? batchId),
    businessDate: String(batch.businessDate ?? ""),
    valuationDate: String(batch.valuationDate ?? ""),
    productCode: String(batch.productCode ?? ""),
    productName: String(batch.productName ?? ""),
    managerName: String(batch.managerName ?? ""),
    fileId: String(batch.fileId ?? ""),
    filesysFileId: String(batch.filesysFileId ?? ""),
    originalFileName: String(batch.originalFileName ?? ""),
    sourceType: String(batch.sourceType ?? ""),
    currentStage: stage,
    currentStep: stage,
    currentStageName:
      batch.currentStepName ??
      batch.currentStageName ??
      stageMeta?.stageName ??
      stage,
    currentStepName:
      batch.currentStepName ??
      batch.currentStageName ??
      stageMeta?.stageName ??
      stage,
    status,
    statusName: batch.statusName ?? statusLabel(status),
    progress: Number(batch.progress ?? 0),
    startedAt: String(batch.startedAt ?? ""),
    endedAt: batch.endedAt,
    durationMs: batch.durationMs,
    durationText: String(batch.durationText ?? "-"),
    lastErrorCode: batch.lastErrorCode,
    lastErrorMessage: batch.lastErrorMessage,
    steps: steps?.length
      ? sortSteps(steps.map(mapStep))
      : buildSteps(batchId, stage, status),
  };
};

const sampleRows: OutsourcedDataTaskBatchRow[] = [
  mapBatch({
    batchId: `BATCH-${todayBusinessDate.replace(/-/g, "")}-001`,
    batchName: `估值产品5 ${todayBusinessDate} 估值数据处理`,
    businessDate: todayBusinessDate,
    valuationDate: todayBusinessDate,
    productCode: "W213412",
    productName: "估值产品5",
    managerName: "临时机构",
    fileId: "FILE-001",
    filesysFileId: "FS-001",
    originalFileName: "估值产品5估值表.xlsx",
    sourceType: "MANUAL_UPLOAD",
    currentStage: "SUBJECT_RECOGNIZE",
    currentStageName: defaultStageName("SUBJECT_RECOGNIZE"),
    currentStep: "SUBJECT_RECOGNIZE",
    currentStepName: defaultStageName("SUBJECT_RECOGNIZE"),
    status: "RUNNING",
    statusName: statusLabel("RUNNING"),
    progress: 66,
    startedAt: `${todayBusinessDate} 09:30:00`,
    durationText: "运行中",
  }),
  mapBatch({
    batchId: `BATCH-${todayBusinessDate.replace(/-/g, "")}-002`,
    batchName: `估值产品6 ${todayBusinessDate} 估值数据处理`,
    businessDate: todayBusinessDate,
    valuationDate: todayBusinessDate,
    productCode: "W213413",
    productName: "估值产品6",
    managerName: "临时机构",
    fileId: "FILE-002",
    filesysFileId: "FS-002",
    originalFileName: "估值产品6估值表.xlsx",
    sourceType: "FILESYS",
    currentStage: "STANDARD_LANDING",
    currentStageName: defaultStageName("STANDARD_LANDING"),
    currentStep: "STANDARD_LANDING",
    currentStepName: defaultStageName("STANDARD_LANDING"),
    status: "FAILED",
    statusName: statusLabel("FAILED"),
    progress: 70,
    startedAt: `${todayBusinessDate} 09:30:00`,
    endedAt: `${todayBusinessDate} 09:42:00`,
    durationMs: 720000,
    durationText: "12m",
    lastErrorCode: "LANDING_FAILED",
    lastErrorMessage: outsourcedDataTaskPreviewText.landingFailureMessage,
  }),
  mapBatch({
    batchId: `BATCH-${todayBusinessDate.replace(/-/g, "")}-003`,
    batchName: `估值产品7 ${todayBusinessDate} 估值数据处理`,
    businessDate: todayBusinessDate,
    valuationDate: todayBusinessDate,
    productCode: "W213414",
    productName: "估值产品7",
    managerName: "临时机构",
    fileId: "FILE-003",
    filesysFileId: "FS-003",
    originalFileName: "估值产品7估值表.xlsx",
    sourceType: "EMAIL",
    currentStage: "VERIFY_ARCHIVE",
    currentStageName: defaultStageName("VERIFY_ARCHIVE"),
    currentStep: "VERIFY_ARCHIVE",
    currentStepName: defaultStageName("VERIFY_ARCHIVE"),
    status: "SUCCESS",
    statusName: statusLabel("SUCCESS"),
    progress: 100,
    startedAt: `${todayBusinessDate} 09:30:00`,
    endedAt: `${todayBusinessDate} 09:42:00`,
    durationMs: 720000,
    durationText: "12m",
  }),
];

export const useOutsourcedDataTaskPage = (): {
  page: OutsourcedDataTaskPage;
} => {
  const query = reactive<OutsourcedDataTaskQueryState>(defaultQuery());
  const historyQuery = reactive<OutsourcedDataTaskQueryState>(defaultQuery());
  const rows = ref<OutsourcedDataTaskBatchRow[]>(sampleRows);
  const historyRows = ref<OutsourcedDataTaskBatchRow[]>([]);
  const summary = ref<OutsourcedDataTaskSummaryDTO | null>(null);
  watch(
    () => summary.value?.stepSummaries,
    (value) => {
      activeStageCatalog.value = normalizeStageCatalog(value);
    },
    { immediate: true },
  );
  const selectedRowKeys = ref<string[]>([]);
  const selectedRow = ref<OutsourcedDataTaskBatchRow | null>(null);
  const selectedDetail = ref<OutsourcedDataTaskBatchDetailDTO | null>(null);
  const activeStep = ref<OutsourcedDataTaskStepRow | null>(null);
  const stepLogVisible = ref(false);
  const stepLogLoading = ref(false);
  const stepLogRows = ref<OutsourcedDataTaskLogRow[]>([]);
  const stepDataVisible = ref(false);
  const detailVisible = ref(false);
  const loading = ref(false);
  const historyLoading = ref(false);
  const historyVisible = ref(false);
  const expandedBatchIds = ref<string[]>([]);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: sampleRows.length,
    showSizeChanger: true,
    pageSizeOptions: ["10", "20", "50"],
  });
  const historyPagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    pageSizeOptions: ["10", "20", "50"],
  });

  const filteredRows = computed(() =>
    rows.value.filter(
      (row) =>
        matches(row.businessDate, query.businessDate) &&
        matches(row.managerName, query.managerName) &&
        (matches(row.productName, query.productKeyword) ||
          matches(row.productCode, query.productKeyword)) &&
        (query.step ? hasStage(row, query.step) : true) &&
        matches(row.status, query.status) &&
        matches(row.sourceType, query.sourceType) &&
        (matches(row.lastErrorCode, query.errorType) ||
          matches(row.lastErrorMessage, query.errorType)),
    ),
  );

  const tableData = computed(() => filteredRows.value);
  const totalCount = computed(() =>
    Number(summary.value?.totalCount ?? filteredRows.value.length),
  );
  const runningCount = computed(() =>
    Number(
      summary.value?.runningCount ??
        filteredRows.value.filter((row) => row.status === "RUNNING").length,
    ),
  );
  const successCount = computed(() =>
    Number(
      summary.value?.successCount ??
        filteredRows.value.filter((row) => row.status === "SUCCESS").length,
    ),
  );
  const failedCount = computed(() =>
    Number(
      summary.value?.failedCount ??
        filteredRows.value.filter((row) =>
          ["FAILED", "BLOCKED"].includes(row.status),
        ).length,
    ),
  );
  const stepSummaries = computed<OutsourcedDataTaskStepSummary[]>(() => {
    const catalog = activeStageCatalog.value;
    const sourceSummaries = summary.value?.stepSummaries;
    if (sourceSummaries?.length) {
      return catalog.map((item) => {
        const current = sourceSummaries.find(
          (stage) => stage.stage === item.stage || stage.step === item.stage,
        );
        return {
          ...item,
          step: item.stage,
          stepName: item.stepName,
          stepDescription: item.stepDescription,
          totalCount: Number(current?.totalCount ?? 0),
          runningCount: Number(current?.runningCount ?? 0),
          failedCount: Number(current?.failedCount ?? 0),
          pendingCount: Number(current?.pendingCount ?? 0),
        };
      });
    }
    return catalog.map((item) => {
      const currentRows = filteredRows.value.filter((row) =>
        hasStage(row, item.stage),
      );
      return {
        ...item,
        step: item.stage,
        stepName: item.stepName,
        stepDescription: item.stepDescription,
        totalCount: currentRows.length,
        runningCount: currentRows.filter((row) => row.status === "RUNNING")
          .length,
        failedCount: currentRows.filter((row) =>
          ["FAILED", "BLOCKED"].includes(row.status),
        ).length,
        pendingCount: currentRows.filter((row) => row.status === "PENDING")
          .length,
      };
    });
  });

  const buildQueryParams = (
    sourceQuery: OutsourcedDataTaskQueryState,
    sourcePagination: YTablePagination,
    includeHistory = false,
  ) => ({
    businessDate: sourceQuery.businessDate || undefined,
    managerName: sourceQuery.managerName || undefined,
    productKeyword: sourceQuery.productKeyword || undefined,
    step: sourceQuery.step || undefined,
    status: sourceQuery.status || undefined,
    sourceType: sourceQuery.sourceType || undefined,
    errorType: sourceQuery.errorType || undefined,
    includeHistory,
    pageIndex: Number(sourcePagination.current ?? 1),
    pageSize: Number(sourcePagination.pageSize ?? 10),
  });

  const loadList = async () => {
    loading.value = true;
    try {
      const params = buildQueryParams(query, pagination.value, false);
      const [summaryRes, pageRes] = await Promise.all([
        getOutsourcedDataTaskSummary(params),
        pageOutsourcedDataTasks(params),
      ]);
      const existingSteps = new Map(
        rows.value.map((row) => [row.batchId, row.steps]),
      );
      summary.value = unwrapSingleResult(summaryRes) ?? null;
      rows.value = (pageRes.data ?? []).map((item) => {
        const batchId = String(item.batchId ?? "");
        const mapped = mapBatch(item);
        return existingSteps.has(batchId)
          ? normalizeBatchRowFromSteps(mapped, existingSteps.get(batchId) ?? [])
          : mapped;
      });
      pagination.value.total = Number(pageRes.totalCount ?? rows.value.length);
      pagination.value.current = Number(
        pageRes.pageIndex ?? params.pageIndex ?? 1,
      );
      pagination.value.pageSize = Number(
        pageRes.pageSize ?? params.pageSize ?? 10,
      );
      await refreshVisibleTaskStates();
    } catch {
      summary.value = null;
      rows.value = sampleRows;
      pagination.value.total = filteredRows.value.length;
    } finally {
      loading.value = false;
    }
  };

  const loadHistoryList = async () => {
    historyLoading.value = true;
    try {
      const params = buildQueryParams(
        historyQuery,
        historyPagination.value,
        true,
      );
      const pageRes = await pageOutsourcedDataTasks(params);
      historyRows.value = (pageRes.data ?? []).map((item) => mapBatch(item));
      historyPagination.value.total = Number(
        pageRes.totalCount ?? historyRows.value.length,
      );
      historyPagination.value.current = Number(
        pageRes.pageIndex ?? params.pageIndex ?? 1,
      );
      historyPagination.value.pageSize = Number(
        pageRes.pageSize ?? params.pageSize ?? 10,
      );
    } catch {
      historyRows.value = [];
      historyPagination.value.total = 0;
    } finally {
      historyLoading.value = false;
    }
  };

  const runQuery = () => {
    pagination.value.current = 1;
    pagination.value.total = filteredRows.value.length;
    void loadList();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    runQuery();
  };

  const handlePageChange = (params: { current: number; pageSize: number }) => {
    pagination.value.current = params.current;
    pagination.value.pageSize = params.pageSize;
    void loadList();
  };

  const selectStep = (stage: string) => {
    query.step = query.step === stage ? "" : stage;
    runQuery();
  };

  const selectStatus = (status: string) => {
    query.status = query.status === status ? "" : status;
    runQuery();
  };

  const openHistoryDrawer = () => {
    Object.assign(historyQuery, {
      businessDate: query.businessDate,
      managerName: query.managerName,
      productKeyword: query.productKeyword,
      step: query.step,
      status: query.status,
      sourceType: query.sourceType,
      errorType: query.errorType,
      includeHistory: true,
    });
    historyPagination.value.current = 1;
    historyVisible.value = true;
    void loadHistoryList();
  };

  const closeHistoryDrawer = () => {
    historyVisible.value = false;
  };

  const handleHistoryPageChange = (params: {
    current: number;
    pageSize: number;
  }) => {
    historyPagination.value.current = params.current;
    historyPagination.value.pageSize = params.pageSize;
    void loadHistoryList();
  };

  const updateBatchSteps = (
    batchId: string,
    steps: OutsourcedDataTaskStepRow[],
  ) => {
    const orderedSteps = sortSteps(steps);
    rows.value = rows.value.map((row) =>
      row.batchId === batchId
        ? normalizeBatchRowFromSteps(row, orderedSteps)
        : row,
    );
    if (selectedRow.value?.batchId === batchId) {
      selectedRow.value = normalizeBatchRowFromSteps(
        selectedRow.value,
        orderedSteps,
      );
    }
    return orderedSteps;
  };

  const refreshBatchSteps = async (row: OutsourcedDataTaskBatchRow) => {
    try {
      const result = await listOutsourcedDataTaskSteps(row.batchId);
      const steps = unwrapMultiResult(result).map(mapStep);
      if (steps.length) {
        updateBatchSteps(row.batchId, steps);
        return;
      }
    } catch {
      // 走本地缓存兜底。
    }
    if (row.steps.length) {
      updateBatchSteps(row.batchId, row.steps);
    }
  };

  const refreshSelectedDetail = async (batchId: string) => {
    try {
      const detail = unwrapSingleResult(await getOutsourcedDataTask(batchId));
      selectedDetail.value = detail ?? null;
      if (detail?.batch) {
        const nextRow = normalizeBatchRowFromSteps(
          mapBatch(detail.batch),
          (detail.steps ?? []).map(mapStep),
        );
        selectedRow.value = nextRow;
        updateBatchSteps(nextRow.batchId, nextRow.steps);
      }
    } catch {
      // 详情接口不可用时保留当前展示内容。
    }
  };

  const refreshVisibleTaskStates = async () => {
    const targetBatchIds = new Set<string>();
    rows.value.forEach((row) => {
      if (
        LIVE_STATUS_SET.has(row.status) ||
        expandedBatchIds.value.includes(row.batchId)
      ) {
        targetBatchIds.add(row.batchId);
      }
    });
    if (detailVisible.value && selectedRow.value?.batchId) {
      targetBatchIds.add(selectedRow.value.batchId);
    }

    await Promise.all(
      [...targetBatchIds].map(async (batchId) => {
        const currentRow =
          rows.value.find((row) => row.batchId === batchId) ??
          (selectedRow.value?.batchId === batchId ? selectedRow.value : null);
        if (!currentRow) {
          return;
        }
        if (detailVisible.value && selectedRow.value?.batchId === batchId) {
          await refreshSelectedDetail(batchId);
          return;
        }
        await refreshBatchSteps(currentRow);
      }),
    );
  };

  const loadBatchSteps = async (row: OutsourcedDataTaskBatchRow) => {
    try {
      const result = await listOutsourcedDataTaskSteps(row.batchId);
      const steps = unwrapMultiResult(result).map(mapStep);
      if (steps.length) {
        return updateBatchSteps(row.batchId, steps);
      }
    } catch {
      return updateBatchSteps(row.batchId, row.steps);
    }
    return updateBatchSteps(row.batchId, row.steps);
  };

  const handleExpandChange = (params: {
    row?: OutsourcedDataTaskBatchRow;
    expanded?: boolean;
  }) => {
    if (params.expanded && params.row) {
      if (!expandedBatchIds.value.includes(params.row.batchId)) {
        expandedBatchIds.value = [
          ...expandedBatchIds.value,
          params.row.batchId,
        ];
      }
      void loadBatchSteps(params.row);
      return;
    }
    if (params.row) {
      expandedBatchIds.value = expandedBatchIds.value.filter(
        (batchId) => batchId !== params.row?.batchId,
      );
    }
  };

  const openStepLogs = async (row: OutsourcedDataTaskStepRow) => {
    activeStep.value = row;
    stepLogVisible.value = true;
    stepLogLoading.value = true;
    stepLogRows.value = [];
    try {
      const pageResult = await pageOutsourcedDataTaskLogs(row.batchId, {
        stage: row.stage ?? row.step,
        step: row.step ?? row.stage,
        pageIndex: 1,
        pageSize: 50,
      });
      const logs = pageResult.data ?? [];
      stepLogRows.value = logs.map((log) => mapLogRow(log, row));
      if (!logs.length) {
        stepLogRows.value = [
          {
            key: row.stepId,
            stepName: row.stepName,
            stageName: row.stageName,
            status: row.status,
            statusName: row.statusName,
            startedAt: row.startedAt || "-",
            durationText: row.durationText || "-",
            message:
              row.errorMessage ||
              row.outputSummary ||
              row.inputSummary ||
              outsourcedDataTaskPreviewText.stepLogEmptyText,
            errorStack: row.errorStack,
            logLevel:
              row.status === "FAILED" || row.status === "BLOCKED"
                ? "ERROR"
                : "INFO",
            occurredAt: row.startedAt,
          },
        ];
      }
    } catch {
      stepLogRows.value = [
        {
          key: row.stepId,
          stepName: row.stepName,
          stageName: row.stageName,
          status: row.status,
          statusName: row.statusName,
          startedAt: row.startedAt || "-",
          durationText: row.durationText || "-",
          message:
            row.errorMessage ||
            row.outputSummary ||
            row.inputSummary ||
            outsourcedDataTaskPreviewText.stepLogUnavailableText,
          errorStack: row.errorStack,
          logLevel:
            row.status === "FAILED" || row.status === "BLOCKED"
              ? "ERROR"
              : "INFO",
          occurredAt: row.startedAt,
        },
      ];
    } finally {
      stepLogLoading.value = false;
    }
  };

  const closeStepLogs = () => {
    stepLogVisible.value = false;
  };

  const openStepData = (row: OutsourcedDataTaskStepRow) => {
    activeStep.value = row;
    stepDataVisible.value = true;
  };

  const closeStepData = () => {
    stepDataVisible.value = false;
  };

  const openDetailDrawer = async (row: OutsourcedDataTaskBatchRow) => {
    selectedRow.value = row;
    selectedDetail.value = null;
    detailVisible.value = true;
    try {
      const detail = unwrapSingleResult(
        await getOutsourcedDataTask(row.batchId),
      );
      selectedDetail.value = detail ?? null;
      if (detail?.batch) {
        const nextRow = mapBatch(detail.batch, detail.steps);
        selectedRow.value = nextRow;
        updateBatchSteps(nextRow.batchId, nextRow.steps);
      }
    } catch {
      selectedRow.value = row;
      selectedDetail.value = null;
    }
  };

  const closeDetailDrawer = () => {
    detailVisible.value = false;
  };

  const getOrderedSteps = (row: OutsourcedDataTaskBatchRow) =>
    sortSteps(row.steps ?? []);

  const selectedBatchIds = () =>
    selectedRowKeys.value.length
      ? selectedRowKeys.value
      : tableData.value.map((row) => row.batchId);

  const refreshAfterAction = async (messageText: string) => {
    await loadList();
    message.success(messageText);
  };

  const runAction = async (
    request: Promise<unknown>,
    successMessage: string,
  ) => {
    try {
      await request;
      await refreshAfterAction(successMessage);
    } catch {
      message.warning("当前后端接口不可用，页面保留预览数据");
    }
  };

  const page: OutsourcedDataTaskPage = reactive({
    get loading() {
      return loading.value;
    },
    get rows() {
      return rows.value;
    },
    get tableData() {
      return tableData.value;
    },
    get historyLoading() {
      return historyLoading.value;
    },
    get historyRows() {
      return historyRows.value;
    },
    get stepSummaries() {
      return stepSummaries.value;
    },
    get totalCount() {
      return totalCount.value;
    },
    get runningCount() {
      return runningCount.value;
    },
    get successCount() {
      return successCount.value;
    },
    get failedCount() {
      return failedCount.value;
    },
    get pagination() {
      return pagination.value;
    },
    set pagination(value: YTablePagination) {
      pagination.value = value;
    },
    get historyPagination() {
      return historyPagination.value;
    },
    set historyPagination(value: YTablePagination) {
      historyPagination.value = value;
    },
    query,
    get selectedRowKeys() {
      return selectedRowKeys.value;
    },
    set selectedRowKeys(value: string[]) {
      selectedRowKeys.value = value;
    },
    get selectedRow() {
      return selectedRow.value;
    },
    get detailDataEntries() {
      return buildDataEntries(selectedRow.value, selectedDetail.value);
    },
    get detailLogRows() {
      return buildLogRows(selectedRow.value);
    },
    get manualState() {
      return buildManualState(selectedRow.value, selectedDetail.value);
    },
    get detailVisible() {
      return detailVisible.value;
    },
    get historyVisible() {
      return historyVisible.value;
    },
    get stepLogVisible() {
      return stepLogVisible.value;
    },
    get stepLogLoading() {
      return stepLogLoading.value;
    },
    get stepLogRows() {
      return stepLogRows.value;
    },
    get stepDataVisible() {
      return stepDataVisible.value;
    },
    get activeStep() {
      return activeStep.value;
    },
    runQuery,
    resetQuery,
    handlePageChange,
    selectStep,
    selectStatus,
    handleExpandChange,
    getOrderedSteps,
    getStepRowClassName: ({ row }) =>
      `outsourced-task-step-row--${row.status.toLowerCase()}`,
    openDetailDrawer,
    closeDetailDrawer,
    openStepLogs,
    closeStepLogs,
    openStepData,
    closeStepData,
    openHistoryDrawer,
    closeHistoryDrawer,
    handleHistoryPageChange,
    executeBatch: (row) => {
      void runAction(
        executeOutsourcedDataTask(row.batchId),
        `已提交执行：${row.batchName}`,
      );
    },
    retryBatch: (row) => {
      void runAction(
        retryOutsourcedDataTask(row.batchId),
        `已提交重跑：${row.batchName}`,
      );
    },
    stopBatch: (row) => {
      void runAction(
        stopOutsourcedDataTask(row.batchId),
        `已提交停止：${row.batchName}`,
      );
    },
    retryStep: (row) => {
      void runAction(
        retryOutsourcedDataTaskStep(row.batchId, row.stepId),
        `已提交步骤重跑：${row.stepName}`,
      );
    },
    batchExecute: () => {
      void runAction(
        batchExecuteOutsourcedDataTasks({ batchIds: selectedBatchIds() }),
        "已提交批量执行",
      );
    },
    batchRetry: () => {
      void runAction(
        batchRetryOutsourcedDataTasks({ batchIds: selectedBatchIds() }),
        "已提交批量重跑",
      );
    },
    batchStop: () => {
      void runAction(
        batchStopOutsourcedDataTasks({ batchIds: selectedBatchIds() }),
        "已提交批量停止",
      );
    },
    formatStatusColor: (status) => {
      if (status === "SUCCESS") return "green";
      if (status === "RUNNING") return "processing";
      if (status === "FAILED" || status === "BLOCKED") return "red";
      if (status === "STOPPED") return "orange";
      return "default";
    },
  } as OutsourcedDataTaskPage);

  void loadList();

  return { page };
};
