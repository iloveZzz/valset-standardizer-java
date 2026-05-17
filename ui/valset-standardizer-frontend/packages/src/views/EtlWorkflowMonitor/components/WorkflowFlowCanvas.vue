<script setup lang="ts">
import { computed, markRaw } from "vue";
import { VueFlow, type NodeMouseEvent } from "@vue-flow/core";
import { Background } from "@vue-flow/background";
import { Controls } from "@vue-flow/controls";
import { MiniMap } from "@vue-flow/minimap";
import WorkflowNodeCard from "./WorkflowNodeCard.vue";
import type {
  WorkflowMonitorEdge,
  WorkflowMonitorNode,
  WorkflowMonitorNodeData,
} from "../types";

defineOptions({ name: "WorkflowMonitorFlowCanvas" });

const props = defineProps<{
  nodes: WorkflowMonitorNode[];
  edges: WorkflowMonitorEdge[];
  loading: boolean;
  emptyText: string;
}>();

const emit = defineEmits<{
  nodeClick: [node: WorkflowMonitorNodeData];
}>();

const nodeTypes = {
  monitor: markRaw(WorkflowNodeCard),
};

const hasGraph = computed(() => props.nodes.length > 0);

const getMiniMapColor = (node: WorkflowMonitorNode) => {
  const status = String(node.data?.status ?? "").toUpperCase();
  if (["SUCCEEDED", "SUCCESS"].includes(status)) return "#52c41a";
  if (["RUNNING", "SUBMITTED", "RETRYING"].includes(status)) return "#1677ff";
  if (["FAILED", "FAILURE"].includes(status)) return "#ff4d4f";
  if (["STOPPED", "PAUSE"].includes(status)) return "#faad14";
  return "#94a3b8";
};

const handleNodeClick = ({ node }: NodeMouseEvent) => {
  emit("nodeClick", node.data as WorkflowMonitorNodeData);
};
</script>

<template>
  <div class="workflow-monitor-flow-canvas">
    <a-spin :spinning="loading">
      <div v-if="hasGraph" class="workflow-monitor-flow-canvas__inner">
        <VueFlow
          :nodes="nodes"
          :edges="edges"
          :node-types="nodeTypes"
          :fit-view-on-init="true"
          :nodes-draggable="false"
          :nodes-connectable="false"
          :elements-selectable="true"
          :zoom-on-double-click="false"
          :min-zoom="0.35"
          :max-zoom="1.4"
          class="workflow-monitor-vue-flow"
          @node-click="handleNodeClick"
        >
          <Background variant="dots" :gap="18" :size="1.2" color="#cbd5e1" />
          <MiniMap
            :node-color="getMiniMapColor"
            :node-border-radius="8"
            :node-stroke-width="2"
            mask-color="rgba(15, 23, 42, 0.08)"
            position="bottom-right"
          />
          <Controls position="bottom-left" />
        </VueFlow>
      </div>
      <a-empty v-else :description="emptyText" class="workflow-monitor-flow-empty" />
    </a-spin>
  </div>
</template>
