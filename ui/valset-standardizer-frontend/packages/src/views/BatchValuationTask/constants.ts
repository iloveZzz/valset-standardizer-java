import type {
  BatchValuationTaskStage,
  BatchValuationTaskStatus,
} from "./types";

export const batchValuationTaskSourceTypeLabelMap: Record<string, string> = {
  EXTRACT_DATA: "原始数据提取",
  PARSE_WORKBOOK: "解析工作簿",
  MATCH_SUBJECT: "标准科目匹配",
  EXPORT_RESULT: "结果导出",
  REFRESH_STANDARD_SUBJECT: "刷新标准科目",
  REFRESH_MAPPING_HINT: "刷新映射提示",
};

export const resolveBatchValuationTaskSourceTypeLabel = (value?: string) => {
  const normalized = String(value ?? "").trim().toUpperCase();
  if (!normalized) {
    return "-";
  }
  return batchValuationTaskSourceTypeLabelMap[normalized] ?? normalized;
};

export const batchValuationTaskStageCatalog: Array<{
  stage: BatchValuationTaskStage;
  step: BatchValuationTaskStage;
  stageName: string;
  stepName: string;
  stageDescription: string;
  stepDescription: string;
  label: string;
  description: string;
}> = [
  {
    stage: "FILE_PARSE",
    step: "FILE_PARSE",
    stageName: "文件解析",
    stepName: "文件解析",
    stageDescription: "解析文件输入与原始内容抽取结果",
    stepDescription: "解析文件输入与原始内容抽取结果",
    label: "文件解析",
    description: "解析文件输入与原始内容抽取结果",
  },
  {
    stage: "STRUCTURE_STANDARDIZE",
    step: "STRUCTURE_STANDARDIZE",
    stageName: "结构标准化",
    stepName: "结构标准化",
    stageDescription: "查看字段映射、清洗和结构标准化执行状态",
    stepDescription: "查看字段映射、清洗和结构标准化执行状态",
    label: "结构标准化",
    description: "查看字段映射、清洗和结构标准化执行状态",
  },
  {
    stage: "STANDARD_LANDING",
    step: "STANDARD_LANDING",
    stageName: "估值贴源数据落地",
    stepName: "估值贴源数据落地",
    stageDescription: "查看加工执行状态和估值表贴源数据",
    stepDescription: "查看加工执行状态和估值表贴源数据",
    label: "估值贴源数据落地",
    description: "查看加工执行状态和估值表贴源数据",
  },
];

export const batchValuationTaskStatusCatalog: Array<{
  status: BatchValuationTaskStatus;
  label: string;
}> = [
  { status: "PENDING", label: "待处理" },
  { status: "RUNNING", label: "处理中" },
  { status: "SUCCESS", label: "已完成" },
  { status: "FAILED", label: "失败" },
  { status: "STOPPED", label: "已停止" },
];

export const batchValuationTaskPageText = {
  title: "批量估值解析任务",
  summary: {
    total: "批次总数",
    running: "处理中",
    success: "已完成",
    failed: "失败或停止",
  },
  query: {
    batchId: "批次ID",
    taskDate: "任务日期",
    businessDate: "业务日期",
    managerName: "管理机构",
    productKeyword: "产品名称/代码",
    stage: "任务阶段",
    status: "状态",
    sourceType: "来源类型",
  },
  table: {
    batchName: "批次名称",
    businessDate: "业务日期",
    productCode: "产品代码",
    productName: "产品名称",
    managerName: "管理机构",
    sourceType: "来源类型",
    currentStageName: "当前阶段",
    statusName: "状态",
    startedAt: "开始时间",
    endedAt: "结束时间",
    durationText: "耗时",
    lastErrorMessage: "异常摘要",
    action: "操作",
  },
  detail: {
    title: "批次详情",
    batchInfo: "批次信息",
    stepInfo: "阶段明细",
    batchId: "批次ID",
    workflowCode: "工作流编码",
    workflowId: "工作流ID",
    versionNo: "版本号",
    currentStage: "当前阶段",
    status: "状态",
    progress: "进度",
    startedAt: "开始时间",
    endedAt: "结束时间",
    durationText: "耗时",
    fileId: "文件ID",
    filesysFileId: "文件服务ID",
    originalFileName: "原始文件名",
    inputSummary: "输入摘要",
    outputSummary: "输出摘要",
    logRef: "日志定位",
  },
} as const;
