<script setup lang="ts">
import { computed } from "vue";
import { Handle, Position, type NodeProps } from "@vue-flow/core";
import type { ValuationTraceNodeData } from "../types";

defineOptions({ name: "ValuationTraceNodeCard" });

const props = defineProps<NodeProps<ValuationTraceNodeData>>();

const kindLabelMap: Record<ValuationTraceNodeData["kind"], string> = {
  transferObject: "投递对象",
  parseQueue: "解析队列",
  parseTask: "解析任务",
  jobExecution: "Batch Job",
  stepExecution: "Batch Step",
  result: "解析结果",
};

const nodeClass = computed(() => [
  "valuation-monitor-node-card",
  `valuation-monitor-node-card--${props.data.kind}`,
  {
    "is-active": props.data.active,
    "is-failed": props.data.failed,
    "is-missing": props.data.missing,
    "is-selected": props.selected,
  },
]);
</script>

<template>
  <div :class="nodeClass">
    <Handle
      v-if="data.kind !== 'transferObject'"
      type="target"
      :position="Position.Top"
      class="valuation-monitor-node-card__handle"
    />
    <div class="valuation-monitor-node-card__top">
      <span class="valuation-monitor-node-card__kind">
        {{ kindLabelMap[data.kind] }}
      </span>
      <span class="valuation-monitor-node-card__status">
        {{ data.statusLabel }}
      </span>
    </div>
    <div class="valuation-monitor-node-card__title">
      {{ data.title }}
    </div>
    <div class="valuation-monitor-node-card__subtitle">
      {{ data.subtitle || "-" }}
    </div>
    <div class="valuation-monitor-node-card__meta">
      <span>{{ data.meta || "-" }}</span>
      <span>{{ data.timeLabel || "-" }}</span>
    </div>
    <div v-if="data.message" class="valuation-monitor-node-card__message">
      {{ data.message }}
    </div>
    <Handle
      v-if="data.kind !== 'result'"
      type="source"
      :position="Position.Bottom"
      class="valuation-monitor-node-card__handle"
    />
  </div>
</template>
