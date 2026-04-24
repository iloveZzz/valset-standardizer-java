import type {
  ConditionGroup,
  OperatorOption,
  OptionItem,
} from "@yss-ui/components";

export const mailConditionFieldOptions: OptionItem[] = [
  { label: "文件类型", value: "attachmentFileType" },
  { label: "邮箱主题", value: "mailSubject" },
  { label: "收取上限", value: "limit" },
  { label: "发件人", value: "mailFrom" },
  { label: "收件人", value: "mailTo" },
];

export const mailAttachmentTypeOptions: OptionItem[] = [
  { label: "PDF", value: "pdf" },
  { label: "DOC", value: "doc" },
  { label: "DOCX", value: "docx" },
  { label: "XLS", value: "xls" },
  { label: "XLSX", value: "xlsx" },
  { label: "PPT", value: "ppt" },
  { label: "PPTX", value: "pptx" },
  { label: "TXT", value: "txt" },
  { label: "CSV", value: "csv" },
  { label: "ZIP", value: "zip" },
  { label: "RAR", value: "rar" },
  { label: "7Z", value: "7z" },
];

export const mailConditionOperatorOptions: OperatorOption[] = [
  { label: "等于", value: "EQ", kind: "single" },
  { label: "包含", value: "CONTAINS", kind: "multiple" },
  { label: "大于", value: ">=", kind: "single" },
  { label: "小于", value: "<=", kind: "single" },
];

export const createMailConditionInitial = (): ConditionGroup => ({
  id: "root",
  type: "GROUP",
  logicalOp: "AND",
  children: [],
});
