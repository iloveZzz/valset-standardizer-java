<script setup lang="ts">
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import "./index.less";
import OutsourcedDataTaskPage from "../OutsourcedDataTask/index.vue";
import HoldingPenetrationTaskPage from "../HoldingPenetrationTask/index.vue";
import WorkflowConfigPage from "../WorkflowConfig/index.vue";

defineOptions({ name: "TaskManagementPage" });

type TaskSceneKey = "valuation" | "holding" | "workflow";

const sceneOptions: Array<{
  key: TaskSceneKey;
  label: string;
  description: string;
  path: string;
  component: typeof OutsourcedDataTaskPage;
}> = [
  {
    key: "valuation",
    label: "估值解析任务",
    description: "管理估值表解析流程，支持按任务日期检索和步骤级查看。",
    path: "/outsourced-data-tasks",
    component: OutsourcedDataTaskPage,
  },
  {
    key: "holding",
    label: "持仓穿透任务",
    description:
      "管理持仓穿透流程，支持净值指标、持仓、标签与证券标准化流程切换。",
    path: "/holding-penetration-tasks",
    component: HoldingPenetrationTaskPage,
  },
  {
    key: "workflow",
    label: "工作流配置",
    description: "维护任务阶段、状态归类和未来调度平台适配参数。",
    path: "/workflow-configs",
    component: WorkflowConfigPage,
  },
];

const route = useRoute();

const resolveSceneKey = (): TaskSceneKey => {
  const scene = String(route.query.scene ?? "").trim();
  if (scene === "holding") {
    return "holding";
  }
  if (scene === "valuation") {
    return "valuation";
  }
  if (scene === "workflow") {
    return "workflow";
  }
  if (route.path.includes("workflow-configs")) {
    return "workflow";
  }
  return route.path.includes("holding-penetration-tasks")
    ? "holding"
    : "valuation";
};

const activeScene = ref<TaskSceneKey>(resolveSceneKey());

watch(
  () => [route.path, route.query.scene],
  () => {
    activeScene.value = resolveSceneKey();
  },
  { immediate: true },
);

const handleTabChange = (sceneKey: string | number) => {
  const nextScene = String(sceneKey) as TaskSceneKey;
  activeScene.value = nextScene;
};
</script>

<template>
  <div class="task-management-page">
    <a-tabs
      v-model:activeKey="activeScene"
      tab-position="left"
      class="task-management-tabs"
      :style="{ height: '100%' }"
      @change="handleTabChange"
    >
      <template #leftExtra>
        <div class="tab-header">
          <div class="tab-header-title">任务管理</div>
        </div>
      </template>
      <a-tab-pane
        v-for="item in sceneOptions"
        :key="item.key"
        :tab="item.label"
      >
        <div class="task-management-content">
          <transition name="task-management-fade" mode="out-in">
            <keep-alive>
              <component
                :is="item.component"
                :key="item.key"
                :show-scene-header="false"
              />
            </keep-alive>
          </transition>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
