<script setup lang="ts">
import "./index.less";
import BatchValuationTaskWorkspace from "./components/BatchValuationTaskWorkspace.vue";
import { useBatchValuationTaskPage } from "./hooks/useBatchValuationTaskPage";

defineOptions({ name: "BatchValuationTaskPage" });

const page = useBatchValuationTaskPage();
</script>

<template>
  <BatchValuationTaskWorkspace :page="page" />
</template>
