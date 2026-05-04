import {
  ApartmentOutlined,
  ApiOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  FileSearchOutlined,
  HistoryOutlined,
  InboxOutlined,
  ProjectOutlined,
  SwapOutlined,
  ThunderboltOutlined,
  TagsOutlined,
} from "@ant-design/icons-vue";
import type { Component } from "vue";
import type { RouteRecordRaw } from "vue-router";
import { transferSectionOptions } from "@/views/TransferOverview/schemas/transferSchemas";

const transferSectionIconMap: Record<string, Component> = {
  overview: DatabaseOutlined,
  inbox: InboxOutlined,
  object: FileSearchOutlined,
  source: InboxOutlined,
  target: ApiOutlined,
  rule: ThunderboltOutlined,
  tag: TagsOutlined,
  "route-config": ApartmentOutlined,
  log: SwapOutlined,
  "run-log": HistoryOutlined,
  "parse-queue": FileSearchOutlined,
  "parse-lifecycle": HistoryOutlined,
  guide: FileTextOutlined,
};

export const workspaceNav: Array<{
  title: string;
  path: string;
  icon: Component;
  children?: Array<{
    title: string;
    path: string;
    icon: Component;
  }>;
}> = [
  {
    title: "源数据分拣",
    path: "/transfer",
    icon: DatabaseOutlined,
    children: transferSectionOptions.map((item) => ({
      title: item.label,
      path: `/transfer/${item.value}`,
      icon: transferSectionIconMap[item.value] || FileTextOutlined,
    })),
  },
  {
    title: "任务管理",
    path: "/task-management",
    icon: ProjectOutlined,
    children: [
      {
        title: "估值表解析任务",
        path: "/task-management?scene=valuation",
        icon: FileSearchOutlined,
      },
      {
        title: "持仓穿透任务",
        path: "/task-management?scene=holding",
        icon: FileSearchOutlined,
      },
      {
        title: "工作流配置",
        path: "/task-management?scene=workflow",
        icon: ProjectOutlined,
      },
    ],
  },
];

const transferPageComponentMap = {
  overview: () => import("@/views/TransferOverview/index.vue"),
  inbox: () => import("@/views/TransferInbox/index.vue"),
  source: () => import("@/views/TransferSource/index.vue"),
  target: () => import("@/views/TransferTarget/index.vue"),
  rule: () => import("@/views/TransferRule/index.vue"),
  tag: () => import("@/views/TransferTag/index.vue"),
  "route-config": () => import("@/views/TransferRouteConfig/index.vue"),
  "run-log": () => import("@/views/TransferRunLog/index.vue"),
  "parse-queue": () => import("@/views/ParseQueue/index.vue"),
  "parse-lifecycle": () => import("@/views/ParseLifecycle/index.vue"),
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
  {
    path: "/task-management",
    name: "task-management",
    component: () => import("@/views/TaskManagement/index.vue"),
    meta: {
      title: "任务管理",
      keepAlive: false,
    },
  },
  {
    path: "/outsourced-data-tasks",
    name: "outsourced-data-tasks",
    component: () => import("@/views/TaskManagement/index.vue"),
    meta: {
      title: "估值表解析任务",
      keepAlive: false,
    },
  },
  {
    path: "/holding-penetration-tasks",
    name: "holding-penetration-tasks",
    component: () => import("@/views/TaskManagement/index.vue"),
    meta: {
      title: "持仓穿透任务",
      keepAlive: false,
    },
  },
  {
    path: "/workflow-configs",
    name: "workflow-configs",
    component: () => import("@/views/TaskManagement/index.vue"),
    meta: {
      title: "工作流配置",
      keepAlive: false,
    },
  },
  ...transferSectionRoutes,
  {
    path: "/:pathMatch(.*)*",
    redirect: "/transfer/overview",
  },
];
