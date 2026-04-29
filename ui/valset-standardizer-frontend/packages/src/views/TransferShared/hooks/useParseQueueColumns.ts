import { computed } from "vue";
import type { YTableColumn } from "@yss-ui/components";
import { formatDateTime } from "@/utils/format";

const statusLabelMap: Record<string, string> = {
  PENDING: "待解析",
  PARSING: "解析中",
  PARSED: "已解析",
  FAILED: "解析失败",
};

const triggerModeLabelMap: Record<string, string> = {
  AUTO: "自动生成",
  MANUAL: "手工生成",
};

const formatMappedLabel = (
  value: string | undefined,
  mapping: Record<string, string>,
) => {
  const text = String(value ?? "")
    .trim()
    .toUpperCase();
  if (!text) {
    return "-";
  }
  return mapping[text] ?? text;
};

export const useParseQueueColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "originalName",
      title: "原始文件名",
      minWidth: 260,
      ellipsis: true,
    },
    {
      field: "tagName",
      title: "标签",
      width: 150,
      formatter: (params: any) =>
        params?.cellValue || params?.row?.tagCode || "-",
    },
    {
      field: "fileStatus",
      title: "文件状态",
      width: 120,
      formatter: (params: any) => params?.cellValue || "-",
    },
    {
      field: "parseStatus",
      title: "解析状态",
      width: 130,
      formatter: (params: any) =>
        formatMappedLabel(params?.cellValue, statusLabelMap),
    },
    {
      field: "triggerMode",
      title: "触发方式",
      width: 130,
      formatter: (params: any) =>
        formatMappedLabel(params?.cellValue, triggerModeLabelMap),
    },
    {
      field: "retryCount",
      title: "重试次数",
      width: 100,
    },
    {
      field: "createdAt",
      title: "创建时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
    {
      field: "parsedAt",
      title: "解析完成时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
    {
      field: "lastErrorMessage",
      title: "错误信息",
      minWidth: 240,
      ellipsis: true,
    },
  ]);
