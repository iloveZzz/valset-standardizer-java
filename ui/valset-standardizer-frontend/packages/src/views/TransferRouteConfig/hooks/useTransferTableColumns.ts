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
      field: "flowSummary",
      title: "映射 / 摘要",
      width: 460,
    },
    {
      field: "sourceIngestStatus",
      title: "收取状态",
      minWidth: 680,
    },
  ]);
