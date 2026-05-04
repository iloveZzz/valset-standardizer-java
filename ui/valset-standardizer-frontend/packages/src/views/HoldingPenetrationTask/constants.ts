import type {
  HoldingPenetrationTaskStage,
  HoldingPenetrationTaskStatus,
} from "./types";

export const outsourcedDataTaskStageCatalog: Array<{
  stage: HoldingPenetrationTaskStage;
  step: HoldingPenetrationTaskStage;
  stageName: string;
  stepName: string;
  stageDescription: string;
  stepDescription: string;
}> = [
  {
    stage: "NET_VALUE_STANDARDIZE",
    step: "NET_VALUE_STANDARDIZE",
    stageName: "净值指标标准化",
    stepName: "净值指标标准化",
    stageDescription: "净值指标校验、标准化转换、指标口径统一",
    stepDescription: "净值指标校验、标准化转换、指标口径统一",
  },
  {
    stage: "POSITION_STANDARDIZE",
    step: "POSITION_STANDARDIZE",
    stageName: "持仓标准化",
    stepName: "持仓标准化",
    stageDescription: "持仓字段映射、数据清洗、标准持仓转换",
    stepDescription: "持仓字段映射、数据清洗、标准持仓转换",
  },
  {
    stage: "TAG_VERIFY",
    step: "TAG_VERIFY",
    stageName: "标签校验",
    stepName: "标签校验",
    stageDescription: "标签匹配、标签完整性与一致性校验",
    stepDescription: "标签匹配、标签完整性与一致性校验",
  },
  {
    stage: "POSITION_VERIFY",
    step: "POSITION_VERIFY",
    stageName: "持仓校验",
    stepName: "持仓校验",
    stageDescription: "持仓数据一致性校验、异常定位与确认",
    stepDescription: "持仓数据一致性校验、异常定位与确认",
  },
  {
    stage: "SECURITY_STANDARDIZE",
    step: "SECURITY_STANDARDIZE",
    stageName: "证券标准化",
    stepName: "证券标准化",
    stageDescription: "证券标识映射、标准证券结果输出",
    stepDescription: "证券标识映射、标准证券结果输出",
  },
];

export const outsourcedDataTaskStatusCatalog: Array<{
  status: HoldingPenetrationTaskStatus;
  label: string;
}> = [
  { status: "PENDING", label: "待处理" },
  { status: "RUNNING", label: "处理中" },
  { status: "SUCCESS", label: "已完成" },
  { status: "FAILED", label: "失败" },
  { status: "STOPPED", label: "已停止" },
  { status: "BLOCKED", label: "阻塞" },
];

export const outsourcedDataTaskTriggerModeLabels: Record<string, string> = {
  SCHEDULE: "调度执行",
  DEPENDENCY: "依赖触发",
  MANUAL: "手动执行",
};

export const outsourcedDataTaskDataEntryStatusLabels: Record<string, string> = {
  READY: "可查看",
  WAITING: "待生成",
  ERROR: "写入异常",
};

export const outsourcedDataTaskMetricCardText = {
  total: {
    label: "今日数据批次",
    description: "当前任务日期内累计进入处理链路的数据批次",
  },
  running: {
    label: "处理中",
    description: "正在解析、标准化或落地的任务批次",
  },
  success: {
    label: "处理完成",
    description: "已完成处理并进入可用状态的批次",
  },
  failed: {
    label: "异常待处理",
    description: "失败或阻塞后等待定位、修复和重跑的批次",
  },
} as const;

export const outsourcedDataTaskPreviewText = {
  stepCompletedSuffix: "已完成",
  landingFailureMessage: "结果写入失败：持仓数据写入冲突",
  currentBatchCompleted: "当前批次已完成，无需人工处理",
  noClearBlockPoint: "暂无明确阻塞点",
  exceptionConfirmText: "确认异常已定位，处理备注将作为重跑前置说明。",
  notExceptionalText: "当前批次未处于异常状态，可补充备注后按需重跑。",
  rerunPrerequisites: [
    "确认原始文件、标准化结果和结果层数据入口均可访问。",
    "确认异常步骤的错误摘要和堆栈已完成定位。",
    "确认人工处理备注已记录处理结论和重跑范围。",
  ],
  stepLogWaitingText: "等待步骤执行",
  stepLogUnavailableText: "当前日志接口不可用，展示步骤摘要",
  stepLogEmptyText: "暂无步骤日志",
  currentBlockPointPrefix: "当前阻塞点：",
} as const;

export const outsourcedDataTaskActionTexts = {
  pageHeaderTitle: "持仓穿透任务管理",
  pageHeaderDescription:
    "覆盖净值指标标准化、持仓标准化、标签校验、持仓校验和证券标准化，支持手动执行、失败续跑与重跑。",
  retryBatchConfirmTitle: "重跑持仓穿透任务",
  retryBatchConfirmContent: "将从流程起点重新执行该批次，是否继续？",
  executeBatchConfirmTitle: "手动执行持仓穿透任务",
  executeBatchConfirmContent: "将提交该批次的手动执行请求，是否继续？",
  continueExecuteBatchConfirmTitle: "继续执行持仓穿透任务",
  executeBatchContinueContent: "将从失败处继续执行该批次，是否继续？",
  stopBatchConfirmTitle: "停止持仓穿透任务",
  stopBatchConfirmContent: "将停止该批次当前运行步骤，是否继续？",
  batchExecuteButtonText: "批量手动执行",
  batchRetryButtonText: "批量重跑",
  batchStopButtonText: "批量停止",
  viewButtonText: "查看",
  executeButtonText: "手动执行",
  executeContinueButtonText: "继续执行",
  retryButtonText: "重跑",
  stopButtonText: "停止",
  retryStepConfirmTitle: "重跑任务步骤",
  detailHeaderTitle: "持仓穿透任务详情",
  detailOverviewTabTitle: "链路概览",
  detailDataTabTitle: "文件与数据",
  detailLogsTabTitle: "执行日志",
  detailManualTabTitle: "人工处理",
  historyButtonText: "历史记录",
  historyDrawerTitle: "历史记录",
  detailStepLogSuffix: "执行日志",
  detailStepDataSuffix: "步骤数据",
  detailStepLogFallbackTitle: "步骤执行日志",
  detailStepDataFallbackTitle: "步骤数据",
  detailStepSectionTitle: "步骤执行明细",
  detailStepSectionDescription: "按配置的步骤顺序展示。",
  stepLogButtonText: "日志",
  stepDataButtonText: "数据",
  stepRetryButtonText: "步骤重跑",
  openEntryButtonText: "打开",
  pendingEntryText: "待接入",
  noErrorStackText: "暂无错误堆栈",
  retryStepConfirmContentPrefix: "将从当前步骤重新执行 ",
  retryStepConfirmContentSuffix: " 步骤，是否继续？",
  historyDrawerDescription: "按当前筛选条件查看历史批次",
  historyTotalPrefix: "历史总数：",
  manualExceptionCheckboxText: "异常已确认，允许记录人工处理结论",
  manualRemarkPlaceholder: "填写处理备注、数据修正说明、重跑范围或外部确认结果",
  manualPrerequisitesTitle: "重跑前置提示",
  manualConfirmButtonText: "确认异常",
  manualRerunButtonText: "按前置检查重跑",
} as const;

export const outsourcedDataTaskTableTexts = {
  batchColumns: {
    batchName: "数据批次名称",
    productCode: "产品代码",
    productName: "产品名称",
    managerName: "管理人",
    businessDate: "业务日期",
    originalFileName: "文件名称",
    currentStepName: "当前步骤",
    status: "状态",
    progress: "进度",
    startedAt: "任务开始时间",
    endedAt: "任务结束时间",
    lastErrorMessage: "异常原因",
    action: "操作",
  },
  stepColumns: {
    stepName: "步骤名称",
    startedAt: "任务开始时间",
    durationText: "执行耗时",
    runNo: "执行次数",
    triggerModeName: "触发方式",
    status: "状态",
    errorMessage: "错误摘要",
    action: "操作",
  },
  dataEntryColumns: {
    name: "入口名称",
    description: "说明",
    status: "状态",
    action: "操作",
  },
  logColumns: {
    stepName: "步骤",
    logLevel: "级别",
    occurredAt: "日志时间",
    startedAt: "开始时间",
    durationText: "耗时",
    status: "状态",
    message: "步骤日志",
    errorStack: "错误堆栈",
  },
  detailFields: {
    batchId: "批次ID",
    currentStepName: "当前步骤",
    originalFileName: "原始文件",
    filesysFileId: "文件服务ID",
    startedAt: "任务开始时间",
    durationText: "任务耗时",
    lastErrorMessage: "异常原因",
    step: "步骤",
    taskId: "任务ID",
    taskType: "任务类型",
    inputSummary: "输入摘要",
    outputSummary: "输出摘要",
    errorMessage: "错误摘要",
    logRef: "日志定位",
    log: "日志",
    data: "数据",
    noErrorStack: "暂无错误堆栈",
  },
  dataEntryNames: {
    sourceFile: "原始文件",
    parseResult: "解析结果",
    stg: "中间层数据",
    dwd: "结果层数据",
    standard: "标准表",
  },
  dataEntryDescriptions: {
    sourceFileExisting: " / ",
    sourceFileMissing: "等待文件归集",
    parseResult: "查看净值指标标准化后的结构化明细和摘要",
    stg: "查看持仓标准化后的中间层数据入口",
    dwd: "查看持仓校验后的结果数据入口",
    standard: "查看证券标准化结果入口",
  },
} as const;

export const outsourcedDataTaskQueryTexts = {
  taskDate: "任务日期",
  managerName: "管理人",
  productKeyword: "产品",
  status: "状态",
  sourceType: "来源",
  errorType: "异常",
  queryButtonText: "查询",
  resetButtonText: "重置",
  currentTaskText: "当前任务",
  allTasksText: "全部任务",
  taskDatePrefix: "任务日期：",
  managerNamePrefix: "管理人：",
  productKeywordPrefix: "产品：",
  stepPrefix: "步骤：",
  statusPrefix: "状态：",
  sourceTypePrefix: "来源：",
  errorTypePrefix: "异常：",
} as const;

export const outsourcedDataTaskFeedbackTexts = {
  backendUnavailableWarning: "当前后端接口不可用，页面保留空态",
  submitExecuteSuccessPrefix: "已提交手动执行：",
  submitExecuteContinueSuccessPrefix: "已提交继续执行：",
  submitRetrySuccessPrefix: "已提交重跑：",
  submitStopSuccessPrefix: "已提交停止：",
  submitStepRetrySuccessPrefix: "已提交步骤重跑：",
  submitBatchExecuteSuccess: "已提交批量手动执行",
  submitBatchRetrySuccess: "已提交批量重跑",
  submitBatchStopSuccess: "已提交批量停止",
} as const;
