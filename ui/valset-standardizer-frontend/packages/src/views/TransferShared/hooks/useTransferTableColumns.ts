import { computed } from "vue";
import type { YTableColumn } from "@yss-ui/components";
import { formatDateTime } from "@/utils/format";

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
    {
      field: "pollCron",
      title: "轮询表达式",
      width: 180,
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
      field: "transferId",
      title: "分拣ID",
      width: 110,
    },
    {
      field: "sourceCode",
      title: "来源编码",
      width: 180,
    },
    {
      field: "sourceType",
      title: "来源类型",
      width: 120,
    },
    {
      field: "status",
      title: "文件状态",
      width: 120,
    },
    {
      field: "originalName",
      title: "附件名称",
      width: 240,
    },
    {
      field: "sizeBytes",
      title: "大小",
      width: 120,
    },
    {
      field: "receivedAt",
      title: "收取时间",
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
      field: "targetCode",
      title: "目标编码",
      width: 180,
    },
    {
      field: "targetType",
      title: "目标类型",
      width: 120,
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
      field: "fileName",
      title: "文件名称",
      width: 240,
    },
    {
      field: "source",
      title: "来源",
      width: 160,
    },
    {
      field: "target",
      title: "目标",
      width: 160,
    },
    {
      field: "route",
      title: "路由",
      width: 130,
    },
    {
      field: "status",
      title: "结果",
      width: 110,
    },
    {
      field: "deliveredAt",
      title: "投递时间",
      width: 180,
    },
  ]);
