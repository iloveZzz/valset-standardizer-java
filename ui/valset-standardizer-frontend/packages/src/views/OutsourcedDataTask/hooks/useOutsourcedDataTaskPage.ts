import { computed, onBeforeUnmount, reactive, ref } from "vue";
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
  OutsourcedDataTaskStageSummary,
  OutsourcedDataTaskStatus,
  OutsourcedDataTaskStepRow,
} from "../types";

const stages: Array<{
  stage: OutsourcedDataTaskStage;
  stageName: string;
  stageDescription: string;
}> = [
  { stage: "FILE_PARSE", stageName: "文件解析", stageDescription: "文件识别、Sheet 解析、原始行列抽取" },
  { stage: "STRUCTURE_STANDARDIZE", stageName: "结构标准化", stageDescription: "字段映射、数据清洗、STG 结构转换" },
  { stage: "SUBJECT_RECOGNIZE", stageName: "科目识别", stageDescription: "科目匹配、属性识别、标签补全" },
  { stage: "STANDARD_LANDING", stageName: "标准表落地", stageDescription: "STG/DWD/标准持仓/估值数据写入" },
  { stage: "DATA_PROCESSING", stageName: "加工任务", stageDescription: "后续数据加工、补充计算、派生数据生成" },
  { stage: "VERIFY_ARCHIVE", stageName: "校验归档", stageDescription: "一致性校验、结果确认、归档完成" },
];

const statusLabelMap: Record<OutsourcedDataTaskStatus, string> = {
  PENDING: "待处理",
  RUNNING: "处理中",
  SUCCESS: "已完成",
  FAILED: "失败",
  STOPPED: "已停止",
  BLOCKED: "阻塞",
};

const defaultQuery = (): OutsourcedDataTaskQueryState => ({
  businessDate: "2025-02-27",
  managerName: "",
  productKeyword: "",
  stage: "",
  status: "",
  sourceType: "",
  errorType: "",
});

const POLLING_INTERVAL_MS = 10000;

const normalizeStage = (value?: string): OutsourcedDataTaskStage => {
  const stage = String(value ?? "").trim() as OutsourcedDataTaskStage;
  return stages.some((item) => item.stage === stage) ? stage : "FILE_PARSE";
};

const normalizeStatus = (value?: string): OutsourcedDataTaskStatus => {
  const status = String(value ?? "").trim() as OutsourcedDataTaskStatus;
  return status in statusLabelMap ? status : "PENDING";
};

const matches = (actual: string | undefined, keyword: string) => {
  if (!keyword.trim()) {
    return true;
  }
  return String(actual ?? "")
    .toLowerCase()
    .includes(keyword.trim().toLowerCase());
};

const getStageOrder = (stage: string) => {
  const index = stages.findIndex((item) => item.stage === stage);
  return index >= 0 ? index : stages.length;
};

const sortSteps = (steps: OutsourcedDataTaskStepRow[]) =>
  [...steps].sort((left, right) => {
    const stageDiff = getStageOrder(left.stage) - getStageOrder(right.stage);
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
  const currentIndex = stages.findIndex((item) => item.stage === currentStage);
  return sortSteps(stages.map((item, index) => {
    const isBefore = index < currentIndex;
    const isCurrent = index === currentIndex;
    const stepStatus: OutsourcedDataTaskStatus = isBefore ? "SUCCESS" : isCurrent ? status : "PENDING";
    return {
      stepId: `${batchId}-${item.stage}`,
      batchId,
      stage: item.stage,
      stageName: item.stageName,
      taskId: `TASK-${batchId}-${index + 1}`,
      taskType: item.stage,
      runNo: 1,
      triggerMode: index === 0 ? "SCHEDULE" : "DEPENDENCY",
      triggerModeName: index === 0 ? "调度执行" : "依赖触发",
      status: stepStatus,
      statusName: statusLabelMap[stepStatus],
      progress: stepStatus === "SUCCESS" ? 100 : isCurrent ? 66 : 0,
      startedAt: "2025-02-27 09:30:00",
      endedAt: stepStatus === "SUCCESS" || stepStatus === "FAILED" ? "2025-02-27 09:32:00" : undefined,
      durationText: stepStatus === "PENDING" ? "-" : "2m",
      inputSummary: item.stageDescription,
      outputSummary: stepStatus === "SUCCESS" ? `${item.stageName}已完成` : "",
      errorCode: isCurrent && status === "FAILED" ? "TASK_FAILED" : "",
      errorMessage: isCurrent && status === "FAILED" ? "标准表落地失败：DWD 持仓写入冲突" : "",
      errorStack:
        isCurrent && status === "FAILED"
          ? "com.yss.valset.task.StandardLandingException: DWD 持仓写入冲突\n\tat com.yss.valset.standardize.StandardLandingService.writeDwd(StandardLandingService.java:128)\n\tat com.yss.valset.batch.dispatcher.DefaultTaskDispatcher.dispatch(DefaultTaskDispatcher.java:76)"
          : "",
      logRef: `task:${batchId}:${item.stage}`,
    };
  }));
};

const mapStep = (step: OutsourcedDataTaskStepDTO): OutsourcedDataTaskStepRow => {
  const stage = normalizeStage(step.stage);
  const status = normalizeStatus(step.status);
  const stageMeta = stages.find((item) => item.stage === stage);
  return {
    stepId: String(step.stepId ?? ""),
    batchId: String(step.batchId ?? ""),
    stage,
    stageName: step.stageName ?? stageMeta?.stageName ?? stage,
    taskId: String(step.taskId ?? ""),
    taskType: String(step.taskType ?? stage),
    runNo: Number(step.runNo ?? 0),
    triggerMode: String(step.triggerMode ?? ""),
    triggerModeName: String(step.triggerModeName ?? "-"),
    status,
    statusName: step.statusName ?? statusLabelMap[status],
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
  const hasFile = Boolean(row?.filesysFileId || row?.fileId || row?.originalFileName);
  const entries: OutsourcedDataTaskDataEntry[] = [
    {
      key: "source-file",
      name: "原始文件",
      description: row?.originalFileName
        ? `${row.originalFileName} / ${row.filesysFileId || row.fileId || "-"}`
        : "等待文件归集",
      status: hasFile ? "READY" : "WAITING",
      statusName: hasFile ? "已归集" : "待生成",
      href: detail?.rawDataUrl,
    },
    {
      key: "parse-result",
      name: "解析结果",
      description: "查看估值文件解析后的结构化明细和识别摘要",
      status: detail?.fileResultUrl ? "READY" : row?.currentStage === "FILE_PARSE" ? "WAITING" : "READY",
      statusName: detail?.fileResultUrl ? "可查看" : row?.currentStage === "FILE_PARSE" ? "处理中" : "已生成",
      href: detail?.fileResultUrl,
    },
    {
      key: "stg",
      name: "STG 数据",
      description: "字段映射、数据清洗后的 STG 暂存层入口",
      status: detail?.stgDataUrl ? "READY" : row?.progress && row.progress >= 45 ? "READY" : "WAITING",
      statusName: detail?.stgDataUrl ? "可查看" : row?.progress && row.progress >= 45 ? "已生成" : "待生成",
      href: detail?.stgDataUrl,
    },
    {
      key: "dwd",
      name: "DWD 数据",
      description: "标准化后的 DWD 外部估值数据入口",
      status:
        row?.currentStage === "STANDARD_LANDING" && row.status === "FAILED"
          ? "ERROR"
          : detail?.dwdDataUrl
            ? "READY"
            : row?.progress && row.progress >= 70
              ? "READY"
              : "WAITING",
      statusName:
        row?.currentStage === "STANDARD_LANDING" && row.status === "FAILED"
          ? "写入异常"
          : detail?.dwdDataUrl
            ? "可查看"
            : row?.progress && row.progress >= 70
              ? "已生成"
              : "待生成",
      href: detail?.dwdDataUrl,
    },
    {
      key: "standard",
      name: "标准表",
      description: "标准持仓、标准估值和后续加工表入口",
      status: detail?.standardDataUrl ? "READY" : row?.status === "SUCCESS" ? "READY" : "WAITING",
      statusName: detail?.standardDataUrl ? "可查看" : row?.status === "SUCCESS" ? "已落地" : "待落地",
      href: detail?.standardDataUrl,
    },
  ];
  return entries;
};

const buildLogRows = (row: OutsourcedDataTaskBatchRow | null): OutsourcedDataTaskLogRow[] =>
  sortSteps(row?.steps ?? []).map((step) => ({
    key: step.stepId,
    stageName: step.stageName,
    status: step.status,
    statusName: step.statusName,
    startedAt: step.startedAt || "-",
    durationText: step.durationText || "-",
    message: step.errorMessage || step.outputSummary || step.inputSummary || "等待阶段执行",
    errorStack: step.errorStack,
  }));

const mapLogRow = (
  log: OutsourcedDataTaskLogDTO,
  activeStep: OutsourcedDataTaskStepRow,
): OutsourcedDataTaskLogRow => ({
  key: String(log.logId ?? `${activeStep.stepId}-${log.occurredAt ?? ""}-${log.message ?? ""}`),
  stageName: activeStep.stageName,
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
  const failedStep = row?.steps.find((step) => ["FAILED", "BLOCKED"].includes(step.status));
  const currentBlockPoint =
    detail?.currentBlockPoint ||
    failedStep?.errorMessage ||
    row?.lastErrorMessage ||
    (row?.status === "SUCCESS" ? "当前批次已完成，无需人工处理" : "暂无明确阻塞点");
  return {
    currentBlockPoint,
    exceptionConfirmText:
      row?.status === "FAILED" || row?.status === "BLOCKED"
        ? "确认异常已定位，处理备注将作为重跑前置说明。"
        : "当前批次未处于异常状态，可补充备注后按需重跑。",
    rerunPrerequisites: [
      "确认原始文件、解析结果、STG/DWD 数据入口均可访问。",
      "确认异常阶段的错误摘要和堆栈已完成定位。",
      "确认人工处理备注已记录处理结论和重跑范围。",
    ],
  };
};

const mapBatch = (
  batch: OutsourcedDataTaskBatchDTO,
  steps?: OutsourcedDataTaskStepDTO[],
): OutsourcedDataTaskBatchRow => {
  const batchId = String(batch.batchId ?? "");
  const stage = normalizeStage(batch.currentStage);
  const status = normalizeStatus(batch.status);
  const stageMeta = stages.find((item) => item.stage === stage);
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
    currentStageName: batch.currentStageName ?? stageMeta?.stageName ?? stage,
    status,
    statusName: batch.statusName ?? statusLabelMap[status],
    progress: Number(batch.progress ?? 0),
    startedAt: String(batch.startedAt ?? ""),
    endedAt: batch.endedAt,
    durationMs: batch.durationMs,
    durationText: String(batch.durationText ?? "-"),
    lastErrorCode: batch.lastErrorCode,
    lastErrorMessage: batch.lastErrorMessage,
    steps: steps?.length ? sortSteps(steps.map(mapStep)) : buildSteps(batchId, stage, status),
  };
};

const sampleRows: OutsourcedDataTaskBatchRow[] = [
  mapBatch({
    batchId: "BATCH-20250227-001",
    batchName: "委外产品5 2025-02-27 估值数据处理",
    businessDate: "2025-02-27",
    valuationDate: "2025-02-27",
    productCode: "W213412",
    productName: "委外产品5",
    managerName: "临时机构",
    fileId: "FILE-001",
    filesysFileId: "FS-001",
    originalFileName: "委外产品5估值表.xlsx",
    sourceType: "MANUAL_UPLOAD",
    currentStage: "SUBJECT_RECOGNIZE",
    currentStageName: "科目识别",
    status: "RUNNING",
    statusName: "处理中",
    progress: 66,
    startedAt: "2025-02-27 09:30:00",
    durationText: "运行中",
  }),
  mapBatch({
    batchId: "BATCH-20250227-002",
    batchName: "委外产品6 2025-02-27 估值数据处理",
    businessDate: "2025-02-27",
    valuationDate: "2025-02-27",
    productCode: "W213413",
    productName: "委外产品6",
    managerName: "临时机构",
    fileId: "FILE-002",
    filesysFileId: "FS-002",
    originalFileName: "委外产品6估值表.xlsx",
    sourceType: "FILESYS",
    currentStage: "STANDARD_LANDING",
    currentStageName: "标准表落地",
    status: "FAILED",
    statusName: "失败",
    progress: 70,
    startedAt: "2025-02-27 09:30:00",
    endedAt: "2025-02-27 09:42:00",
    durationMs: 720000,
    durationText: "12m",
    lastErrorCode: "LANDING_FAILED",
    lastErrorMessage: "标准表落地失败：DWD 持仓写入冲突",
  }),
  mapBatch({
    batchId: "BATCH-20250227-003",
    batchName: "委外产品7 2025-02-27 估值数据处理",
    businessDate: "2025-02-27",
    valuationDate: "2025-02-27",
    productCode: "W213414",
    productName: "委外产品7",
    managerName: "临时机构",
    fileId: "FILE-003",
    filesysFileId: "FS-003",
    originalFileName: "委外产品7估值表.xlsx",
    sourceType: "EMAIL",
    currentStage: "VERIFY_ARCHIVE",
    currentStageName: "校验归档",
    status: "SUCCESS",
    statusName: "已完成",
    progress: 100,
    startedAt: "2025-02-27 09:30:00",
    endedAt: "2025-02-27 09:42:00",
    durationMs: 720000,
    durationText: "12m",
  }),
];

export const useOutsourcedDataTaskPage = (): { page: OutsourcedDataTaskPage } => {
  const query = reactive<OutsourcedDataTaskQueryState>(defaultQuery());
  const rows = ref<OutsourcedDataTaskBatchRow[]>(sampleRows);
  const summary = ref<OutsourcedDataTaskSummaryDTO | null>(null);
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
  let pollingTimer: number | undefined;
  const pagination = ref<YTablePagination>({
    current: 1,
    pageSize: 10,
    total: sampleRows.length,
    showSizeChanger: true,
    pageSizeOptions: ["10", "20", "50"],
  });

  const filteredRows = computed(() =>
    rows.value.filter(
      (row) =>
        matches(row.businessDate, query.businessDate) &&
        matches(row.managerName, query.managerName) &&
        (matches(row.productName, query.productKeyword) || matches(row.productCode, query.productKeyword)) &&
        matches(row.currentStage, query.stage) &&
        matches(row.status, query.status) &&
        matches(row.sourceType, query.sourceType) &&
        (matches(row.lastErrorCode, query.errorType) || matches(row.lastErrorMessage, query.errorType)),
    ),
  );

  const tableData = computed(() => filteredRows.value);
  const totalCount = computed(() => Number(summary.value?.totalCount ?? filteredRows.value.length));
  const runningCount = computed(() =>
    Number(summary.value?.runningCount ?? filteredRows.value.filter((row) => row.status === "RUNNING").length),
  );
  const successCount = computed(() =>
    Number(summary.value?.successCount ?? filteredRows.value.filter((row) => row.status === "SUCCESS").length),
  );
  const failedCount = computed(() =>
    Number(
      summary.value?.failedCount ??
        filteredRows.value.filter((row) => ["FAILED", "BLOCKED"].includes(row.status)).length,
    ),
  );
  const stageSummaries = computed<OutsourcedDataTaskStageSummary[]>(() => {
    if (summary.value?.stageSummaries?.length) {
      return stages.map((item) => {
        const current = summary.value?.stageSummaries?.find((stage) => stage.stage === item.stage);
        return {
          ...item,
          totalCount: Number(current?.totalCount ?? 0),
          runningCount: Number(current?.runningCount ?? 0),
          failedCount: Number(current?.failedCount ?? 0),
          pendingCount: Number(current?.pendingCount ?? 0),
        };
      });
    }
    return stages.map((item) => {
      const currentRows = filteredRows.value.filter((row) => row.currentStage === item.stage);
      return {
        ...item,
        totalCount: currentRows.length,
        runningCount: currentRows.filter((row) => row.status === "RUNNING").length,
        failedCount: currentRows.filter((row) => ["FAILED", "BLOCKED"].includes(row.status)).length,
        pendingCount: currentRows.filter((row) => row.status === "PENDING").length,
      };
    });
  });

  const buildQueryParams = () => ({
    businessDate: query.businessDate || undefined,
    managerName: query.managerName || undefined,
    productKeyword: query.productKeyword || undefined,
    stage: query.stage || undefined,
    status: query.status || undefined,
    sourceType: query.sourceType || undefined,
    errorType: query.errorType || undefined,
    pageIndex: Number(pagination.value.current ?? 1),
    pageSize: Number(pagination.value.pageSize ?? 10),
  });

  const loadList = async () => {
    loading.value = true;
    try {
      const params = buildQueryParams();
      const [summaryRes, pageRes] = await Promise.all([
        getOutsourcedDataTaskSummary(params),
        pageOutsourcedDataTasks(params),
      ]);
      const existingSteps = new Map(rows.value.map((row) => [row.batchId, row.steps]));
      summary.value = unwrapSingleResult(summaryRes) ?? null;
      rows.value = (pageRes.data ?? []).map((item) => {
        const batchId = String(item.batchId ?? "");
        const mapped = mapBatch(item);
        return existingSteps.has(batchId) ? { ...mapped, steps: sortSteps(existingSteps.get(batchId) ?? []) } : mapped;
      });
      pagination.value.total = Number(pageRes.totalCount ?? rows.value.length);
      pagination.value.current = Number(pageRes.pageIndex ?? params.pageIndex ?? 1);
      pagination.value.pageSize = Number(pageRes.pageSize ?? params.pageSize ?? 10);
    } catch {
      summary.value = null;
      rows.value = sampleRows;
      pagination.value.total = filteredRows.value.length;
    } finally {
      loading.value = false;
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

  const selectStage = (stage: string) => {
    query.stage = query.stage === stage ? "" : stage;
    runQuery();
  };

  const selectStatus = (status: string) => {
    query.status = query.status === status ? "" : status;
    runQuery();
  };

  const updateBatchSteps = (batchId: string, steps: OutsourcedDataTaskStepRow[]) => {
    const orderedSteps = sortSteps(steps);
    rows.value = rows.value.map((row) => (row.batchId === batchId ? { ...row, steps: orderedSteps } : row));
    if (selectedRow.value?.batchId === batchId) {
      selectedRow.value = { ...selectedRow.value, steps: orderedSteps };
    }
    return orderedSteps;
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

  const handleExpandChange = (params: { row?: OutsourcedDataTaskBatchRow; expanded?: boolean }) => {
    if (params.expanded && params.row) {
      void loadBatchSteps(params.row);
    }
  };

  const openStepLogs = async (row: OutsourcedDataTaskStepRow) => {
    activeStep.value = row;
    stepLogVisible.value = true;
    stepLogLoading.value = true;
    stepLogRows.value = [];
    try {
      const pageResult = await pageOutsourcedDataTaskLogs(row.batchId, {
        stage: row.stage,
        pageIndex: 1,
        pageSize: 50,
      });
      const logs = pageResult.data ?? [];
      stepLogRows.value = logs.map((log) => mapLogRow(log, row));
      if (!logs.length) {
        stepLogRows.value = [{
          key: row.stepId,
          stageName: row.stageName,
          status: row.status,
          statusName: row.statusName,
          startedAt: row.startedAt || "-",
          durationText: row.durationText || "-",
          message: row.errorMessage || row.outputSummary || row.inputSummary || "暂无阶段日志",
          errorStack: row.errorStack,
          logLevel: row.status === "FAILED" || row.status === "BLOCKED" ? "ERROR" : "INFO",
          occurredAt: row.startedAt,
        }];
      }
    } catch {
      stepLogRows.value = [{
        key: row.stepId,
        stageName: row.stageName,
        status: row.status,
        statusName: row.statusName,
        startedAt: row.startedAt || "-",
        durationText: row.durationText || "-",
        message: row.errorMessage || row.outputSummary || row.inputSummary || "当前日志接口不可用，展示阶段摘要",
        errorStack: row.errorStack,
        logLevel: row.status === "FAILED" || row.status === "BLOCKED" ? "ERROR" : "INFO",
        occurredAt: row.startedAt,
      }];
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
      const detail = unwrapSingleResult(await getOutsourcedDataTask(row.batchId));
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

  const getOrderedSteps = (row: OutsourcedDataTaskBatchRow) => sortSteps(row.steps ?? []);

  const selectedBatchIds = () =>
    selectedRowKeys.value.length ? selectedRowKeys.value : tableData.value.map((row) => row.batchId);

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
    get stageSummaries() {
      return stageSummaries.value;
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
    selectStage,
    selectStatus,
    handleExpandChange,
    getOrderedSteps,
    getStepRowClassName: ({ row }) => `outsourced-task-step-row--${row.status.toLowerCase()}`,
    openDetailDrawer,
    closeDetailDrawer,
    openStepLogs,
    closeStepLogs,
    openStepData,
    closeStepData,
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
        `已提交阶段重跑：${row.stageName}`,
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
  pollingTimer = window.setInterval(() => {
    if (!loading.value) {
      void loadList();
    }
  }, POLLING_INTERVAL_MS);
  onBeforeUnmount(() => {
    if (pollingTimer) {
      window.clearInterval(pollingTimer);
      pollingTimer = undefined;
    }
  });

  return { page };
};
