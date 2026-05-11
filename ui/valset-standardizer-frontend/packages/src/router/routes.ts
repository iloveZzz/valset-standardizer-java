import {
  ApartmentOutlined,
  ApiOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  FileSearchOutlined,
  InboxOutlined,
  ProjectOutlined,
  SettingOutlined,
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
  "run-log": SwapOutlined,
  "parse-queue": FileSearchOutlined,
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
    title: "分拣HUB",
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
    ],
  },
  {
    title: "工作流管理",
    path: "/workflow-management",
    icon: SettingOutlined,
    children: [
      {
        title: "工作流定义",
        path: "/workflow-management/workflow-list",
        icon: FileTextOutlined,
      },
      {
        title: "任务阶段定义",
        path: "/workflow-management/stage-definition",
        icon: ThunderboltOutlined,
      },
      {
        title: "工作流实例",
        path: "/workflow-management/workflow-instances",
        icon: FileSearchOutlined,
      },
      {
        title: "任务实例",
        path: "/workflow-management/task-instances",
        icon: FileSearchOutlined,
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
    path: "/workflow-management",
    redirect: "/workflow-management/workflow-list",
  },
  {
    path: "/workflow-management/workflow-list",
    name: "workflow-management-workflow-list",
    component: () => import("@/views/EtlWorkflowConfig/index.vue"),
    meta: {
      title: "工作流定义",
      keepAlive: false,
    },
  },
  {
    path: "/workflow-management/stage-definition",
    name: "workflow-management-stage-definition",
    component: () => import("@/views/EtlWorkflowConfig/StagePage.vue"),
    meta: {
      title: "任务阶段定义",
      keepAlive: false,
    },
  },
  {
    path: "/workflow-management/workflow-instances",
    name: "workflow-management-workflow-instances",
    component: () => import("@/views/EtlWorkflowInstance/index.vue"),
    meta: {
      title: "工作流实例",
      keepAlive: false,
    },
  },
  {
    path: "/workflow-management/task-instances",
    name: "workflow-management-task-instances",
    component: () => import("@/views/EtlWorkflowTaskInstance/index.vue"),
    meta: {
      title: "任务实例",
      keepAlive: false,
    },
  },
  {
    path: "/etl-workflow-config",
    redirect: "/workflow-management/workflow-list",
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
  ...transferSectionRoutes,
  {
    path: "/:pathMatch(.*)*",
    redirect: "/transfer/overview",
  },
];
