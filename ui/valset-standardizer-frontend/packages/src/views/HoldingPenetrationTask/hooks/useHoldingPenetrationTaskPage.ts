import { computed, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type { YTablePagination } from "@yss-ui/components";
import {
  batchExecuteHoldingPenetrationTasks,
  batchRetryHoldingPenetrationTasks,
  batchStopHoldingPenetrationTasks,
  executeHoldingPenetrationTask,
  getHoldingPenetrationTask,
  getHoldingPenetrationTaskSummary,
  listHoldingPenetrationTaskSteps,
  pageHoldingPenetrationTaskLogs,
  pageHoldingPenetrationTasks,
  retryHoldingPenetrationTask,
  retryHoldingPenetrationTaskStep,
  stopHoldingPenetrationTask,
  type HoldingPenetrationTaskBatchDetailDTO,
  type HoldingPenetrationTaskBatchDTO,
  type HoldingPenetrationTaskLogDTO,
  type HoldingPenetrationTaskStepDTO,
  type HoldingPenetrationTaskSummaryDTO,
} from "@/api/holdingPenetrationTask";
import { unwrapMultiResult, unwrapSingleResult } from "@/utils/api-response";
import type {
  HoldingPenetrationTaskBatchRow,
  HoldingPenetrationTaskDataEntry,
  HoldingPenetrationTaskLogRow,
  HoldingPenetrationTaskManualState,
  HoldingPenetrationTaskPage,
  HoldingPenetrationTaskQueryState,
  HoldingPenetrationTaskStage,
  HoldingPenetrationTaskStepRow,
  HoldingPenetrationTaskStepSummary,
  HoldingPenetrationTaskStatus,
} from "../types";
import {
  outsourcedDataTaskStageCatalog,
  outsourcedDataTaskStatusCatalog,
  outsourcedDataTaskTriggerModeLabels,
  outsourcedDataTaskDataEntryStatusLabels,
  outsourcedDataTaskPreviewText,
  outsourcedDataTaskFeedbackTexts,
  outsourcedDataTaskActionTexts,
  outsourcedDataTaskQueryTexts,
  outsourcedDataTaskTableTexts,
} from "../constants";

const defaultStageCatalog = outsourcedDataTaskStageCatalog;
const activeStageCatalog = ref(defaultStageCatalog);
const normalizeVisibleStage = (value?: string): HoldingPenetrationTaskStage =>
  normalizeStage(value);

const normalizeStageCatalog = (
  summaries?: HoldingPenetrationTaskStepSummary[],
): Array<{
  stage: HoldingPenetrationTaskStage;
  step: HoldingPenetrationTaskStage;
  stageName: string;
  stepName: string;
  stageDescription: string;
  stepDescription: string;
}> =>
  summaries?.length
    ? (() => {
        const visibleSummaries = summaries.filter((item) =>
          defaultStageCatalog.some(
            (catalogItem) =>
              catalogItem.stage === String(item.stage ?? item.step ?? "").trim(),
          ),
        );
        if (!visibleSummaries.length) {
          return defaultStageCatalog;
        }
        return visibleSummaries.map((item) => {
          const stage = String(item.stage ?? item.step ?? "").trim() as HoldingPenetrationTaskStage;
          return {
            stage,
            step: stage,
            stageName: item.stageName ?? item.stepName ?? stage,
            stepName: item.stepName ?? item.stageName ?? stage,
            stageDescription: item.stageDescription ?? item.stepDescription ?? "",
            stepDescription: item.stepDescription ?? item.stageDescription ?? "",
          };
        });
      })()
    : defaultStageCatalog;

const defaultQuery = (): HoldingPenetrationTaskQueryState => ({
  batchId: "",
  taskDate: "",
  managerName: "",
  productKeyword: "",
  step: "",
  status: "",
  sourceType: "",
  errorType: "",
  includeHistory: false,
});

const LIVE_STATUS_SET = new Set<HoldingPenetrationTaskStatus>([
  "PENDING",
  "RUNNING",
]);

const RECOVERABLE_EXECUTE_STATUS_SET = new Set<HoldingPenetrationTaskStatus>([
  "FAILED",
  "BLOCKED",
]);

const normalizeStage = (value?: string): HoldingPenetrationTaskStage => {
  const stage = String(value ?? "").trim() as HoldingPenetrationTaskStage;
  return activeStageCatalog.value.some((item) => item.stage === stage)
    ? stage
    : activeStageCatalog.value[0]?.stage ?? "NET_VALUE_STANDARDIZE";
};

const normalizeStatus = (value?: string): HoldingPenetrationTaskStatus => {
  const status = String(value ?? "").trim() as HoldingPenetrationTaskStatus;
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

const canManualExecute = (status?: string) => {
  const normalized = normalizeStatus(status);
  return normalized !== "RUNNING" && normalized !== "SUCCESS";
};

const isContinueExecuteStatus = (status?: string) =>
  RECOVERABLE_EXECUTE_STATUS_SET.has(normalizeStatus(status));

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

const hasStage = (row: HoldingPenetrationTaskBatchRow, stage: string) =>
  row.currentStep === stage ||
  row.currentStage === stage ||
  row.steps.some((step) => step.step === stage || step.stage === stage);

const parseTimelineDate = (value?: string) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return null;
  }
  const normalized = text.includes("T") ? text : text.replace(" ", "T");
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatTimelineDate = (date?: Date | null) => {
  if (!date) {
    return "-";
  }
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const formatTimelineDuration = (durationMs?: number) => {
  if (durationMs === undefined || durationMs === null || durationMs < 0) {
    return "-";
  }
  if (durationMs === 0) {
    return "1s";
  }
  const seconds = Math.max(1, Math.ceil(durationMs / 1000));
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = seconds % 60;
  return remainSeconds ? `${minutes}m${remainSeconds}s` : `${minutes}m`;
};

const alignStepTimeline = <T extends { startedAt?: string; endedAt?: string; durationText?: string }>(
  steps: T[],
  batchStartedAt?: string,
  batchEndedAt?: string,
): T[] => {
  const stepStartedAtList = steps
    .map((step) => parseTimelineDate(step.startedAt))
    .filter((date): date is Date => Boolean(date));
  const stepEndedAtList = steps
    .map((step) => parseTimelineDate(step.endedAt))
    .filter((date): date is Date => Boolean(date));
  const startedAt =
    parseTimelineDate(batchStartedAt) ??
    stepStartedAtList[0] ??
    stepEndedAtList[0] ??
    null;
  const endedAt =
    parseTimelineDate(batchEndedAt) ??
    stepEndedAtList[stepEndedAtList.length - 1] ??
    stepStartedAtList[stepStartedAtList.length - 1] ??
    null;
  if (!startedAt || !endedAt) {
    return steps;
  }
  const timeWindowEndedAt =
    endedAt.getTime() > startedAt.getTime()
      ? endedAt
      : new Date(startedAt.getTime() + Math.max(steps.length - 1, 1) * 1000);
  if (!steps.length) {
    return steps;
  }
  const totalMs = timeWindowEndedAt.getTime() - startedAt.getTime();
  const count = steps.length;
  return steps.map((step, index) => {
    const nextStartMs = index === 0
      ? startedAt.getTime()
      : startedAt.getTime() + Math.floor((totalMs * index) / count);
    const nextEndMs = index === count - 1
      ? timeWindowEndedAt.getTime()
      : startedAt.getTime() + Math.floor((totalMs * (index + 1)) / count);
    const nextStartedAt = new Date(nextStartMs);
    const nextEndedAt = new Date(Math.max(nextEndMs, nextStartMs));
    const durationMs = nextEndedAt.getTime() - nextStartedAt.getTime();
    return {
      ...step,
      startedAt: formatTimelineDate(nextStartedAt),
      endedAt: formatTimelineDate(nextEndedAt),
      durationText: formatTimelineDuration(durationMs),
    };
  });
};

const resolveBatchStatusFromSteps = (steps: HoldingPenetrationTaskStepRow[]) => {
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

const resolveBatchStageFromSteps = (steps: HoldingPenetrationTaskStepRow[]) => {
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
    return normalizeVisibleStage(currentStep.stage ?? currentStep.step);
  }
  const lastStep = steps[steps.length - 1];
  return lastStep
    ? normalizeVisibleStage(lastStep.stage ?? lastStep.step)
    : "NET_VALUE_STANDARDIZE";
};

const normalizeBatchRowFromSteps = (
  row: HoldingPenetrationTaskBatchRow,
  steps: HoldingPenetrationTaskStepRow[],
): HoldingPenetrationTaskBatchRow => {
  const orderedSteps = alignStepTimeline(sortSteps(steps), row.startedAt, row.endedAt);
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
    startedAt: String(orderedSteps[0]?.startedAt ?? row.startedAt ?? currentStep?.startedAt ?? ""),
    endedAt:
      String(orderedSteps[orderedSteps.length - 1]?.endedAt ?? row.endedAt ?? currentStep?.endedAt ?? ""),
    durationText: String(currentStep?.durationText ?? row.durationText ?? "-"),
    steps: orderedSteps,
  };
};

const sortSteps = (steps: HoldingPenetrationTaskStepRow[]) =>
  [...steps]
    .sort((left, right) => {
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
  currentStage: HoldingPenetrationTaskStage,
  status: HoldingPenetrationTaskStatus,
  batchStartedAt?: string,
  batchEndedAt?: string,
): HoldingPenetrationTaskStepRow[] => {
  const stageCatalog = activeStageCatalog.value;
  const currentIndex = stageCatalog.findIndex(
    (item) => item.stage === currentStage,
  );
  return alignStepTimeline(
    sortSteps(
      stageCatalog.map((item, index) => {
      const isBefore = index < currentIndex;
      const isCurrent = index === currentIndex;
      const stepStatus: HoldingPenetrationTaskStatus = isBefore
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
            ? "com.yss.valset.task.HoldingPenetrationException: 持仓结果写入冲突\n\tat com.yss.valset.holding.HoldingPenetrationService.writeResult(HoldingPenetrationService.java:128)\n\tat com.yss.valset.batch.dispatcher.DefaultTaskDispatcher.dispatch(DefaultTaskDispatcher.java:76)"
            : "",
        logRef: `task:${batchId}:${item.stage}`,
      };
    }),
    ),
    batchStartedAt,
    batchEndedAt,
  );
};

const mapStep = (
  step: HoldingPenetrationTaskStepDTO,
): HoldingPenetrationTaskStepRow => {
  const stage = normalizeVisibleStage(step.step ?? step.stage);
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
  row: HoldingPenetrationTaskBatchRow | null,
  detail?: HoldingPenetrationTaskBatchDetailDTO | null,
): HoldingPenetrationTaskDataEntry[] => {
  const hasFile = Boolean(
    row?.filesysFileId || row?.fileId || row?.originalFileName,
  );
  const entries: HoldingPenetrationTaskDataEntry[] = [
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
        : row?.currentStep === "NET_VALUE_STANDARDIZE" ||
            row?.currentStage === "NET_VALUE_STANDARDIZE"
          ? "WAITING"
          : "READY",
      statusName: detail?.fileResultUrl
        ? outsourcedDataTaskDataEntryStatusLabels.READY
        : row?.currentStep === "NET_VALUE_STANDARDIZE" ||
            row?.currentStage === "NET_VALUE_STANDARDIZE"
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
        (row?.currentStep === "POSITION_VERIFY" ||
          row?.currentStage === "POSITION_VERIFY") &&
        row.status === "FAILED"
          ? "ERROR"
          : detail?.dwdDataUrl
            ? "READY"
            : row?.progress && row.progress >= 70
              ? "READY"
              : "WAITING",
      statusName:
        (row?.currentStep === "POSITION_VERIFY" ||
          row?.currentStage === "POSITION_VERIFY") &&
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
  row: HoldingPenetrationTaskBatchRow | null,
): HoldingPenetrationTaskLogRow[] =>
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
  log: HoldingPenetrationTaskLogDTO,
  activeStep: HoldingPenetrationTaskStepRow,
): HoldingPenetrationTaskLogRow => ({
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
  row: HoldingPenetrationTaskBatchRow | null,
  detail?: HoldingPenetrationTaskBatchDetailDTO | null,
): HoldingPenetrationTaskManualState => {
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
    rerunPrerequisites: [...outsourcedDataTaskPreviewText.rerunPrerequisites],
  };
};

const mapBatch = (
  batch: HoldingPenetrationTaskBatchDTO,
  steps?: HoldingPenetrationTaskStepDTO[],
): HoldingPenetrationTaskBatchRow => {
  const batchId = String(batch.batchId ?? "");
  const stage = normalizeVisibleStage(batch.currentStep ?? batch.currentStage);
  const status = normalizeStatus(batch.status);
  const stageMeta = activeStageCatalog.value.find((item) => item.stage === stage);
  return {
    batchId,
    batchName: String(batch.batchName ?? batchId),
    businessDate: String(batch.businessDate ?? ""),
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
      ? alignStepTimeline(
          sortSteps(steps.map(mapStep)),
          batch.startedAt,
          batch.endedAt,
        )
      : buildSteps(batchId, stage, status, batch.startedAt, batch.endedAt),
  };
};

export const useHoldingPenetrationTaskPage = (): {
  page: HoldingPenetrationTaskPage;
} => {
  const query = reactive<HoldingPenetrationTaskQueryState>(defaultQuery());
  const historyQuery = reactive<HoldingPenetrationTaskQueryState>(defaultQuery());
  const rows = ref<HoldingPenetrationTaskBatchRow[]>([]);
  const historyRows = ref<HoldingPenetrationTaskBatchRow[]>([]);
  const summary = ref<HoldingPenetrationTaskSummaryDTO | null>(null);
  watch(
    () => summary.value?.stepSummaries,
    (value) => {
      activeStageCatalog.value = normalizeStageCatalog(
        value as HoldingPenetrationTaskStepSummary[] | undefined,
      ) as typeof defaultStageCatalog;
    },
    { immediate: true },
  );
  const selectedRowKeys = ref<string[]>([]);
  const selectedRow = ref<HoldingPenetrationTaskBatchRow | null>(null);
  const selectedDetail = ref<HoldingPenetrationTaskBatchDetailDTO | null>(null);
  const activeStep = ref<HoldingPenetrationTaskStepRow | null>(null);
  const stepLogVisible = ref(false);
  const stepLogLoading = ref(false);
  const stepLogRows = ref<HoldingPenetrationTaskLogRow[]>([]);
  const stepDataVisible = ref(false);
  const detailVisible = ref(false);
  const loading = ref(false);
  const historyLoading = ref(false);
  const historyVisible = ref(false);
  const historyAnchorRow = ref<HoldingPenetrationTaskBatchRow | null>(null);
  const expandedBatchIds = ref<string[]>([]);
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: 0,
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
  const historyDrawerTitle = computed(() =>
    historyAnchorRow.value
      ? `${outsourcedDataTaskActionTexts.historyDrawerTitle} · ${historyAnchorRow.value.batchName || historyAnchorRow.value.batchId}`
      : outsourcedDataTaskActionTexts.historyDrawerTitle,
  );
  const historyDrawerDescription = computed(() =>
    historyAnchorRow.value
      ? `当前批次：${historyAnchorRow.value.batchId} · ${historyAnchorRow.value.batchName}`
      : outsourcedDataTaskActionTexts.historyDrawerDescription,
  );
  const historyDrawerFilterSummary = computed(() => {
    const historyStepName =
      activeStageCatalog.value.find((item) => item.stage === historyQuery.step)
        ?.stepName ?? historyQuery.step;
    const filters = [
      historyQuery.taskDate &&
        `${outsourcedDataTaskQueryTexts.taskDatePrefix}${historyQuery.taskDate}`,
      historyQuery.managerName &&
        `${outsourcedDataTaskQueryTexts.managerNamePrefix}${historyQuery.managerName}`,
      historyQuery.productKeyword &&
        `${outsourcedDataTaskQueryTexts.productKeywordPrefix}${historyQuery.productKeyword}`,
      historyQuery.step &&
        `${outsourcedDataTaskQueryTexts.stepPrefix}${historyStepName}`,
      historyQuery.status &&
        `${outsourcedDataTaskQueryTexts.statusPrefix}${statusLabel(historyQuery.status)}`,
      historyQuery.sourceType &&
        `${outsourcedDataTaskQueryTexts.sourceTypePrefix}${historyQuery.sourceType}`,
      historyQuery.errorType &&
        `${outsourcedDataTaskQueryTexts.errorTypePrefix}${historyQuery.errorType}`,
    ].filter(Boolean);
    return filters.length
      ? `当前筛选：${filters.join(" / ")}`
      : "当前筛选：全部";
  });

  const filteredRows = computed(() =>
    rows.value.filter(
      (row) =>
        matches(row.startedAt, query.taskDate) &&
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
  const stepSummaries = computed<HoldingPenetrationTaskStepSummary[]>(() => {
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
  sourceQuery: HoldingPenetrationTaskQueryState,
  sourcePagination: YTablePagination,
  includeHistory = false,
) => ({
  batchId: sourceQuery.batchId || undefined,
  taskDate: sourceQuery.taskDate || undefined,
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
        getHoldingPenetrationTaskSummary(params),
        pageHoldingPenetrationTasks(params),
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
      rows.value = [];
      pagination.value.total = 0;
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
      const pageRes = await pageHoldingPenetrationTasks(params);
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

  const openHistoryDrawer = (row?: HoldingPenetrationTaskBatchRow) => {
    historyAnchorRow.value = row ?? null;
    const source = row
      ? {
          batchId: row.batchId,
          taskDate: query.taskDate,
          managerName: row.managerName || query.managerName,
          productKeyword: row.productName || query.productKeyword,
          step: row.currentStep || query.step,
          status: row.status || query.status,
          sourceType: row.sourceType || query.sourceType,
          errorType: query.errorType,
        }
      : query;
    Object.assign(historyQuery, {
      batchId: source.batchId,
      taskDate: source.taskDate,
      managerName: source.managerName,
      productKeyword: source.productKeyword,
      step: source.step,
      status: source.status,
      sourceType: source.sourceType,
      errorType: source.errorType,
      includeHistory: true,
    });
    historyPagination.value.current = 1;
    historyVisible.value = true;
    void loadHistoryList();
  };

  const closeHistoryDrawer = () => {
    historyVisible.value = false;
    historyAnchorRow.value = null;
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
    steps: HoldingPenetrationTaskStepRow[],
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

  const refreshBatchSteps = async (row: HoldingPenetrationTaskBatchRow) => {
    try {
      const result = await listHoldingPenetrationTaskSteps(row.batchId);
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
      const detail = unwrapSingleResult(await getHoldingPenetrationTask(batchId));
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

  const loadBatchSteps = async (row: HoldingPenetrationTaskBatchRow) => {
    try {
      const result = await listHoldingPenetrationTaskSteps(row.batchId);
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
    row?: HoldingPenetrationTaskBatchRow;
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

  const openStepLogs = async (row: HoldingPenetrationTaskStepRow) => {
    activeStep.value = row;
    stepLogVisible.value = true;
    stepLogLoading.value = true;
    stepLogRows.value = [];
    try {
      const pageResult = await pageHoldingPenetrationTaskLogs(row.batchId, {
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

  const openStepData = (row: HoldingPenetrationTaskStepRow) => {
    activeStep.value = row;
    stepDataVisible.value = true;
  };

  const closeStepData = () => {
    stepDataVisible.value = false;
  };

  const openDetailDrawer = async (row: HoldingPenetrationTaskBatchRow) => {
    selectedRow.value = row;
    selectedDetail.value = null;
    detailVisible.value = true;
    try {
      const detail = unwrapSingleResult(
        await getHoldingPenetrationTask(row.batchId),
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

  const getOrderedSteps = (row: HoldingPenetrationTaskBatchRow) =>
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
      message.warning(outsourcedDataTaskFeedbackTexts.backendUnavailableWarning);
    }
  };

  const page: HoldingPenetrationTaskPage = reactive({
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
    get historyDrawerTitle() {
      return historyDrawerTitle.value;
    },
    get historyDrawerDescription() {
      return historyDrawerDescription.value;
    },
    get historyDrawerFilterSummary() {
      return historyDrawerFilterSummary.value;
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
      const successPrefix = isContinueExecuteStatus(row.status)
        ? outsourcedDataTaskFeedbackTexts.submitExecuteContinueSuccessPrefix
        : outsourcedDataTaskFeedbackTexts.submitExecuteSuccessPrefix;
      void runAction(
        executeHoldingPenetrationTask(row.batchId),
        `${successPrefix}${row.batchName}`,
      );
    },
    retryBatch: (row) => {
      void runAction(
        retryHoldingPenetrationTask(row.batchId),
        `${outsourcedDataTaskFeedbackTexts.submitRetrySuccessPrefix}${row.batchName}`,
      );
    },
    stopBatch: (row) => {
      void runAction(
        stopHoldingPenetrationTask(row.batchId),
        `${outsourcedDataTaskFeedbackTexts.submitStopSuccessPrefix}${row.batchName}`,
      );
    },
    retryStep: (row) => {
      void runAction(
        retryHoldingPenetrationTaskStep(row.batchId, row.stepId),
        `${outsourcedDataTaskFeedbackTexts.submitStepRetrySuccessPrefix}${row.stepName}`,
      );
    },
    batchExecute: () => {
      void runAction(
        batchExecuteHoldingPenetrationTasks({ batchIds: selectedBatchIds() }),
        outsourcedDataTaskFeedbackTexts.submitBatchExecuteSuccess,
      );
    },
    batchRetry: () => {
      void runAction(
        batchRetryHoldingPenetrationTasks({ batchIds: selectedBatchIds() }),
        outsourcedDataTaskFeedbackTexts.submitBatchRetrySuccess,
      );
    },
    batchStop: () => {
      void runAction(
        batchStopHoldingPenetrationTasks({ batchIds: selectedBatchIds() }),
        outsourcedDataTaskFeedbackTexts.submitBatchStopSuccess,
      );
    },
    formatStatusColor: (status) => {
      if (status === "SUCCESS") return "green";
      if (status === "RUNNING") return "processing";
      if (status === "FAILED" || status === "BLOCKED") return "red";
      if (status === "STOPPED") return "orange";
      return "default";
    },
    canManualExecute,
    isContinueExecuteStatus,
  } as HoldingPenetrationTaskPage);

  void loadList();

  return { page };
};
