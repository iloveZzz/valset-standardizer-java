<script setup lang="ts">
import { computed } from "vue";
import { YConditionBuilder } from "@yss-ui/components";
import type { ConditionGroup } from "@yss-ui/components";
import {
  createMailConditionInitial,
  mailConditionOperatorOptions,
} from "../constants/mailCondition";
import { useMailConditionBasic } from "../hooks/useMailConditionBasic";

const props = defineProps<{
  modelValue?: ConditionGroup | null;
}>();

const emit = defineEmits<{
  (event: "update:modelValue", value: ConditionGroup): void;
}>();

const { loadFields, loadValues } = useMailConditionBasic();

const model = computed<ConditionGroup>({
  get: () => props.modelValue || createMailConditionInitial(),
  set: (value) => emit("update:modelValue", value),
});
</script>

<template>
  <div class="mail-condition-builder">
    <div class="mail-condition-builder-panel">
      <YConditionBuilder
        v-model="model"
        :operator-options="mailConditionOperatorOptions"
        :load-fields="loadFields"
        :load-values="(args) => loadValues({ q: args.q, field: args.field })"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
.mail-condition-builder {
  min-height: 220px;
}

.mail-condition-builder-panel {
  min-height: 220px;
}

[data-prefers-color="dark"] {
  .mail-condition-builder-panel {
    color: rgba(255, 255, 255, 0.85);
  }
}
</style>
