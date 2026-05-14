import type {
  SpringBatchValuationTaskStage,
  SpringBatchValuationTaskStatus,
} from "./types";

export const springBatchValuationTaskStageCatalog: Array<{
  stage: SpringBatchValuationTaskStage;
  step: SpringBatchValuationTaskStage;
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
    stageDescription: "解析 Spring Batch 文件输入与原始内容抽取结果",
    stepDescription: "解析 Spring Batch 文件输入与原始内容抽取结果",
    label: "文件解析",
    description: "解析 Spring Batch 文件输入与原始内容抽取结果",
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
    stageName: "标准表落地",
    stepName: "标准表落地",
    stageDescription: "查看标准表写入、落地和后续加工执行状态",
    stepDescription: "查看标准表写入、落地和后续加工执行状态",
    label: "标准表落地",
    description: "查看标准表写入、落地和后续加工执行状态",
  },
];

export const springBatchValuationTaskStatusCatalog: Array<{
  status: SpringBatchValuationTaskStatus;
  label: string;
}> = [
  { status: "PENDING", label: "待处理" },
  { status: "RUNNING", label: "处理中" },
  { status: "SUCCESS", label: "已完成" },
  { status: "FAILED", label: "失败" },
  { status: "STOPPED", label: "已停止" },
];

export const springBatchValuationTaskPageText = {
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
    productKeyword: "产品名称/代码",
    stage: "阶段",
    status: "状态",
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
    lastErrorMessage: "异常摘要",
    currentBlockPoint: "当前阻塞点",
    inputSummary: "输入摘要",
    outputSummary: "输出摘要",
    errorMessage: "错误摘要",
    logRef: "日志定位",
  },
} as const;
