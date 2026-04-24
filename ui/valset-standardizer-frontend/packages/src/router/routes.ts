import {
  ApartmentOutlined,
  ApiOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  FileSearchOutlined,
  HistoryOutlined,
  InboxOutlined,
  SwapOutlined,
  ThunderboltOutlined,
} from "@ant-design/icons-vue";
import type { Component } from "vue";
import type { RouteRecordRaw } from "vue-router";
import { transferSectionOptions } from "@/views/TransferOverview/schemas/transferSchemas";

export const workspaceNav: Array<{
  title: string;
  path: string;
  icon: Component;
}> = transferSectionOptions.map((item) => {
  const iconMap: Record<string, Component> = {
    overview: DatabaseOutlined,
    object: FileSearchOutlined,
    source: InboxOutlined,
    target: ApiOutlined,
    rule: ThunderboltOutlined,
    "route-config": ApartmentOutlined,
    log: SwapOutlined,
    "run-log": HistoryOutlined,
    guide: FileTextOutlined,
  };

  return {
    title: item.label,
    path: `/transfer/${item.value}`,
    icon: iconMap[item.value] || FileTextOutlined,
  };
});

const transferPageComponentMap = {
  overview: () => import("@/views/TransferOverview/index.vue"),
  source: () => import("@/views/TransferSource/index.vue"),
  target: () => import("@/views/TransferTarget/index.vue"),
  rule: () => import("@/views/TransferRule/index.vue"),
  "route-config": () => import("@/views/TransferRouteConfig/index.vue"),
  log: () => import("@/views/TransferLog/index.vue"),
  "run-log": () => import("@/views/TransferRunLog/index.vue"),
  guide: () => import("@/views/TransferGuide/index.vue"),
  object: () => import("@/views/TransferObject/index.vue"),
} as const;

const transferSectionRoutes: RouteRecordRaw[] = transferSectionOptions.map(
  (item) => ({
    path: `/transfer/${item.value}`,
    name: `transfer-${item.value}`,
    component: transferPageComponentMap[item.value],
    meta: {
      title: item.label,
      keepAlive: false,
    },
  }),
);

export const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/transfer/overview",
  },
  {
    path: "/transfer",
    redirect: "/transfer/overview",
  },
  ...transferSectionRoutes,
  {
    path: "/:pathMatch(.*)*",
    redirect: "/transfer/overview",
  },
];
