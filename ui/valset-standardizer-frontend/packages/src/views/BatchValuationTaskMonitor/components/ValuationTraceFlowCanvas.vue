<script setup lang="ts">
import { computed, markRaw, nextTick, watch } from "vue";
import { VueFlow, useVueFlow, type NodeMouseEvent } from "@vue-flow/core";
import { Background } from "@vue-flow/background";
import { Controls } from "@vue-flow/controls";
import { MiniMap } from "@vue-flow/minimap";
import ValuationTraceNodeCard from "./ValuationTraceNodeCard.vue";
import type {
  ValuationTraceEdge,
  ValuationTraceNode,
  ValuationTraceNodeData,
} from "../types";

defineOptions({ name: "ValuationTraceFlowCanvas" });

const props = defineProps<{
  nodes: ValuationTraceNode[];
  edges: ValuationTraceEdge[];
  loading: boolean;
  emptyText: string;
}>();

const emit = defineEmits<{
  nodeClick: [node: ValuationTraceNodeData];
}>();

const nodeTypes = {
  monitor: markRaw(ValuationTraceNodeCard),
};

const { fitView } = useVueFlow();

const hasGraph = computed(() => props.nodes.length > 0);

const getMiniMapColor = (node: ValuationTraceNode) => {
  const status = String(node.data?.status ?? "").toUpperCase();
  if (status === "SUCCESS") return "#52c41a";
  if (["RUNNING", "PENDING"].includes(status)) return "#1677ff";
  if (status === "FAILED") return "#ff4d4f";
  if (status === "STOPPED") return "#faad14";
  return "#94a3b8";
};

const handleNodeClick = ({ node }: NodeMouseEvent) => {
  emit("nodeClick", node.data as ValuationTraceNodeData);
};

watch(
  () => [props.nodes.length, props.edges.length],
  async ([nodeCount]) => {
    if (!nodeCount) {
      return;
    }
    await nextTick();
    window.requestAnimationFrame(() => {
      void fitView({
        padding: 0.16,
        duration: 180,
        includeHiddenNodes: true,
      });
    });
  },
  { flush: "post" },
);
</script>

<template>
  <div class="valuation-monitor-flow-canvas">
    <a-spin :spinning="loading">
      <div v-if="hasGraph" class="valuation-monitor-flow-canvas__inner">
        <VueFlow
          :nodes="nodes"
          :edges="edges"
          :node-types="nodeTypes"
          :fit-view-on-init="true"
          :default-viewport="{ x: 0, y: 0, zoom: 0.72 }"
          :nodes-draggable="false"
          :nodes-connectable="false"
          :elements-selectable="true"
          :zoom-on-double-click="false"
          :min-zoom="0.35"
          :max-zoom="1.35"
          class="valuation-monitor-vue-flow"
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
      <a-empty v-else :description="emptyText" class="valuation-monitor-flow-empty" />
    </a-spin>
  </div>
</template>
