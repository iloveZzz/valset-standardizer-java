<script setup lang="ts">
import { computed } from "vue";
import { Handle, Position, type NodeProps } from "@vue-flow/core";
import type { WorkflowMonitorNodeData } from "../types";

defineOptions({ name: "WorkflowMonitorNodeCard" });

const props = defineProps<NodeProps<WorkflowMonitorNodeData>>();

const kindLabelMap: Record<WorkflowMonitorNodeData["kind"], string> = {
  trigger: "触发",
  instance: "实例",
  stage: "阶段",
  task: "任务",
  result: "结果",
};

const nodeClass = computed(() => [
  "workflow-monitor-node-card",
  `workflow-monitor-node-card--${props.data.kind}`,
  {
    "is-active": props.data.active,
    "is-failed": props.data.failed,
    "is-muted": props.data.muted,
    "is-selected": props.selected,
  },
]);
</script>

<template>
  <div :class="nodeClass">
    <Handle
      v-if="data.kind !== 'trigger'"
      type="target"
      :position="Position.Left"
      class="workflow-monitor-node-card__handle"
    />
    <div class="workflow-monitor-node-card__top">
      <span class="workflow-monitor-node-card__kind">
        {{ kindLabelMap[data.kind] }}
      </span>
      <span class="workflow-monitor-node-card__status">
        {{ data.statusLabel }}
      </span>
    </div>
    <div class="workflow-monitor-node-card__title">
      {{ data.title }}
    </div>
    <div class="workflow-monitor-node-card__subtitle">
      {{ data.subtitle || "-" }}
    </div>
    <div class="workflow-monitor-node-card__meta">
      <span>{{ data.meta || "-" }}</span>
      <span>{{ data.timeLabel || "-" }}</span>
    </div>
    <div v-if="data.message && data.message !== '-'" class="workflow-monitor-node-card__message">
      {{ data.message }}
    </div>
    <Handle
      v-if="data.kind !== 'result'"
      type="source"
      :position="Position.Right"
      class="workflow-monitor-node-card__handle"
    />
  </div>
</template>
