import { computed } from "vue";
import type { YTableColumn } from "@yss-ui/components";

export const useTransferRouteConfigColumns = () =>
  computed<YTableColumn[]>(() => [
    {
      type: "checkbox",
      width: 50,
      align: "center",
      fixed: "left" as const,
    },
    {
      field: "routeId",
      title: "映射 / ID",
      width: 110,
    },
    {
      field: "sourceCode",
      title: "来源 / 编码",
      width: 150,
    },
    {
      field: "ruleId",
      title: "规则 / ID",
      width: 140,
    },
    {
      field: "targetCode",
      title: "目标 / 编码",
      width: 150,
    },
    {
      field: "targetPath",
      title: "目标 / 路径",
      width: 130,
    },
    {
      field: "renamePattern",
      title: "映射 / 重命名模板",
      width: 130,
    },
    {
      field: "flowSummary",
      title: "映射 / 摘要",
      width: 180,
    },
  ]);
