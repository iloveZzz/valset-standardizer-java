import type { OutsourcedDataTaskStage, OutsourcedDataTaskStatus } from "./types";

export const outsourcedDataTaskStageCatalog: Array<{
  stage: OutsourcedDataTaskStage;
  step: OutsourcedDataTaskStage;
  stageName: string;
  stepName: string;
  stageDescription: string;
  stepDescription: string;
}> = [
  {
    stage: "FILE_PARSE",
    step: "FILE_PARSE",
    stageName: "文件解析",
    stepName: "文件解析",
    stageDescription: "文件识别、Sheet 解析、结构化解析",
    stepDescription: "文件识别、Sheet 解析、结构化解析",
  },
  {
    stage: "STRUCTURE_STANDARDIZE",
    step: "STRUCTURE_STANDARDIZE",
    stageName: "结构标准化",
    stepName: "结构标准化",
    stageDescription: "字段映射、数据清洗、STG 结构转换",
    stepDescription: "字段映射、数据清洗、STG 结构转换",
  },
  {
    stage: "SUBJECT_RECOGNIZE",
    step: "SUBJECT_RECOGNIZE",
    stageName: "科目识别",
    stepName: "科目识别",
    stageDescription: "科目匹配、属性识别、标签补全",
    stepDescription: "科目匹配、属性识别、标签补全",
  },
  {
    stage: "STANDARD_LANDING",
    step: "STANDARD_LANDING",
    stageName: "标准表落地",
    stepName: "标准表落地",
    stageDescription: "STG/DWD/标准持仓/估值数据写入",
    stepDescription: "STG/DWD/标准持仓/估值数据写入",
  },
  {
    stage: "DATA_PROCESSING",
    step: "DATA_PROCESSING",
    stageName: "加工任务",
    stepName: "加工任务",
    stageDescription: "后续数据加工、补充计算、派生数据生成",
    stepDescription: "后续数据加工、补充计算、派生数据生成",
  },
  {
    stage: "VERIFY_ARCHIVE",
    step: "VERIFY_ARCHIVE",
    stageName: "校验归档",
    stepName: "校验归档",
    stageDescription: "一致性校验、结果确认、归档完成",
    stepDescription: "一致性校验、结果确认、归档完成",
  },
];

export const outsourcedDataTaskStatusCatalog: Array<{
  status: OutsourcedDataTaskStatus;
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
    description: "正在解析、标准化、落地或加工的任务批次",
  },
  success: {
    label: "处理完成",
    description: "已完成校验归档并进入可用状态的批次",
  },
  failed: {
    label: "异常待处理",
    description: "失败或阻塞后等待定位、修复和重跑的批次",
  },
} as const;

export const outsourcedDataTaskPreviewText = {
  stepCompletedSuffix: "已完成",
  landingFailureMessage: "标准表落地失败：DWD 持仓写入冲突",
  currentBatchCompleted: "当前批次已完成，无需人工处理",
  noClearBlockPoint: "暂无明确阻塞点",
  exceptionConfirmText:
    "确认异常已定位，处理备注将作为重跑前置说明。",
  notExceptionalText: "当前批次未处于异常状态，可补充备注后按需重跑。",
  rerunPrerequisites: [
    "确认原始文件、解析结果、STG/DWD 数据入口均可访问。",
    "确认异常步骤的错误摘要和堆栈已完成定位。",
    "确认人工处理备注已记录处理结论和重跑范围。",
  ],
  stepLogWaitingText: "等待步骤执行",
  stepLogUnavailableText: "当前日志接口不可用，展示步骤摘要",
  stepLogEmptyText: "暂无步骤日志",
  currentBlockPointPrefix: "当前阻塞点：",
} as const;

export const outsourcedDataTaskActionTexts = {
  pageHeaderTitle: "估值表解析任务管理",
  pageHeaderDescription: "覆盖文件解析、结构标准化、标准表落地和后续数据加工的任务链路。",
  retryBatchConfirmTitle: "重跑估值表解析任务",
  retryBatchConfirmContent: "将从失败或当前步骤重新执行该批次，是否继续？",
  executeBatchConfirmTitle: "执行估值表解析任务",
  executeBatchConfirmContent: "将提交该批次的数据处理任务，是否继续？",
  stopBatchConfirmTitle: "停止估值表解析任务",
  stopBatchConfirmContent: "将停止该批次当前运行步骤，是否继续？",
  batchExecuteButtonText: "批量执行",
  batchRetryButtonText: "批量重跑",
  batchStopButtonText: "批量停止",
  viewButtonText: "查看",
  executeButtonText: "执行",
  retryButtonText: "重跑",
  stopButtonText: "停止",
  retryStepConfirmTitle: "重跑任务步骤",
  detailHeaderTitle: "估值表解析任务详情",
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
  detailStepSectionDescription: "按配置的业务步骤顺序展示。",
  stepLogButtonText: "日志",
  stepDataButtonText: "数据",
  stepRetryButtonText: "重跑",
  openEntryButtonText: "打开",
  pendingEntryText: "待接入",
  noErrorStackText: "暂无错误堆栈",
  retryStepConfirmContentPrefix: "将重新执行 ",
  retryStepConfirmContentSuffix: " 步骤，是否继续？",
  historyDrawerDescription: "按当前筛选条件查看历史批次",
  historyTotalPrefix: "历史总数：",
  manualExceptionCheckboxText: "异常已确认，允许记录人工处理结论",
  manualRemarkPlaceholder:
    "填写处理备注、数据修正说明、重跑范围或外部确认结果",
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
    valuationDate: "估值日期",
    originalFileName: "文件名称",
    currentStepName: "当前步骤",
    status: "状态",
    progress: "进度",
    startedAt: "开始时间",
    durationText: "耗时",
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
    startedAt: "开始时间",
    durationText: "执行耗时",
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
    stg: "STG 数据",
    dwd: "DWD 数据",
    standard: "标准表",
  },
  dataEntryDescriptions: {
    sourceFileExisting: " / ",
    sourceFileMissing: "等待文件归集",
    parseResult: "查看估值文件解析后的结构化明细和识别摘要",
    stg: "字段映射、数据清洗后的 STG 暂存层入口",
    dwd: "标准化后的 DWD 外部估值数据入口",
    standard: "标准持仓、标准估值和后续加工表入口",
  },
} as const;
