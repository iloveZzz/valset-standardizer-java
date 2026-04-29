import { computed } from "vue";
import type { YTableColumn } from "@yss-ui/components";
import { formatDate, formatDateTime } from "@/utils/format";

const SOURCE_TYPE_LABELS: Record<string, string> = {
  LOCAL_DIR: "本地目录",
  EMAIL: "邮件",
  S3: "S3",
  SFTP: "SFTP",
  HTTP: "HTTP接口",
};

export const useTransferSourceColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "sourceCode",
      title: "来源编码",
      width: 180,
    },
    {
      field: "sourceName",
      title: "来源名称",
      width: 220,
    },
    {
      field: "sourceType",
      title: "来源类型",
      width: 120,
      formatter: (params: any) => {
        const value = String(params?.cellValue ?? "")
          .trim()
          .toUpperCase();
        return value ? (SOURCE_TYPE_LABELS[value] ?? value) : "-";
      },
    },
    {
      field: "formTemplateName",
      title: "模板名",
      width: 160,
    },
    {
      field: "enabled",
      title: "启用",
      width: 100,
    },
    {
      field: "createdAt",
      title: "创建时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
    {
      field: "updatedAt",
      title: "修改时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
  ]);

export const useTransferTargetColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "targetCode",
      title: "目标编码",
      width: 180,
    },
    {
      field: "targetName",
      title: "目标名称",
      width: 220,
    },
    {
      field: "targetType",
      title: "目标类型",
      width: 120,
    },
    {
      field: "formTemplateName",
      title: "模板名",
      width: 160,
    },
    {
      field: "targetPathTemplate",
      title: "目标路径模板",
      width: 260,
    },
    {
      field: "enabled",
      title: "启用",
      width: 100,
    },
    {
      field: "createdAt",
      title: "创建时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
    {
      field: "updatedAt",
      title: "修改时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
  ]);

export const useTransferRuleColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "ruleCode",
      title: "规则编码",
      width: 180,
    },
    {
      field: "ruleName",
      title: "规则名称",
      width: 220,
    },
    {
      field: "ruleVersion",
      title: "版本",
      width: 110,
    },
    {
      field: "matchStrategy",
      title: "匹配策略",
      width: 160,
    },
    {
      field: "priority",
      title: "优先级",
      width: 100,
    },
    {
      field: "formTemplateName",
      title: "模板名",
      width: 160,
    },
    {
      field: "enabled",
      title: "启用",
      width: 100,
    },
  ]);

export const useTransferObjectColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "originalName",
      title: "附件名称",
      width: 320,
    },
    {
      field: "sourceCode",
      title: "来源编码",
      width: 120,
    },
    {
      field: "deliveryStatus",
      title: "投递状态",
      width: 120,
      formatter: (params: any) => {
        const value = String(
          params?.cellValue ?? params?.row?.deliveryStatus ?? "",
        )
          .trim()
          .toUpperCase();
        return value === "已投递" ||
          value === "DELIVERED" ||
          value === "SUCCESS"
          ? "已投递"
          : "未投递";
      },
    },
    {
      field: "errorMessage",
      title: "异常",
      width: 50,
      ellipsis: true,
    },
    {
      field: "status",
      title: "文件状态",
      width: 120,
    },
    {
      field: "sizeBytes",
      title: "大小",
      width: 120,
    },
    {
      field: "transferId",
      title: "分拣ID",
      width: 110,
    },
    {
      field: "businessDate",
      title: "业务日期",
      width: 120,
      formatter: (params: any) => formatDate(params?.cellValue),
    },
    {
      field: "receiveDate",
      title: "收取日期",
      width: 120,
      formatter: (params: any) => formatDate(params?.cellValue),
    },
    {
      field: "receivedAt",
      title: "收取时间",
      width: 180,
      formatter: (params: any) => formatDateTime(params?.cellValue),
    },
    {
      field: "tags",
      title: "标签",
      width: 180,
    },
  ]);

export const useTransferLogColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "deliveryId",
      title: "投递ID",
      width: 110,
    },
    {
      field: "transferId",
      title: "分拣ID",
      width: 110,
    },
    {
      field: "messages",
      title: "消息",
      minWidth: 320,
      ellipsis: true,
    },
    {
      field: "targetCode",
      title: "目标编码",
      width: 100,
      ellipsis: true,
    },
    {
      field: "targetType",
      title: "目标类型",
      width: 100,
      ellipsis: true,
    },
    {
      field: "executeStatus",
      title: "执行状态",
      width: 140,
    },
    {
      field: "deliveredAt",
      title: "投递时间",
      width: 280,
    },
  ]);

export const useTransferRunLogColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "logMessage",
      title: "运行说明",
      minWidth: 260,
      ellipsis: true,
    },
    {
      field: "sourceName",
      title: "来源名称",
      width: 180,
    },
    {
      field: "routeName",
      title: "路由名称",
      width: 240,
    },
    {
      field: "sourceType",
      title: "来源类型",
      width: 120,
      formatter: (params: any) => {
        const value = String(params?.cellValue ?? "")
          .trim()
          .toUpperCase();
        return value ? (SOURCE_TYPE_LABELS[value] ?? value) : "-";
      },
    },
    {
      field: "runStage",
      title: "运行阶段",
      width: 140,
    },
    {
      field: "runStatus",
      title: "运行状态",
      width: 140,
    },
    {
      field: "createdAt",
      title: "创建时间",
      width: 180,
    },
    {
      field: "originalName",
      title: "分拣文件",
      width: 140,
    },
  ]);

export const useTransferOverviewRecentDeliveryColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "deliveryId",
      title: "投递ID",
      width: 130,
    },
    {
      field: "transferId",
      title: "分拣ID",
      width: 130,
    },
    {
      field: "targetCode",
      title: "目标编码",
      width: 180,
    },
    {
      field: "targetType",
      title: "目标类型",
      width: 120,
      formatter: (params: any) => {
        const value = String(params?.cellValue ?? "")
          .trim()
          .toUpperCase();
        if (!value) {
          return "-";
        }
        if (value === "EMAIL") return "邮件";
        if (value === "LOCAL_DIR") return "本地目录";
        if (value === "S3") return "S3";
        if (value === "SFTP") return "SFTP";
        if (value === "FILESYS") return "文件服务";
        return value;
      },
    },
    {
      field: "executeStatusLabel",
      title: "执行状态",
      width: 140,
    },
    {
      field: "deliveredAt",
      title: "投递时间",
      width: 180,
    },
  ]);
