<script setup lang="ts">
import "./index.less";
import SpringBatchValuationTaskWorkspace from "./components/SpringBatchValuationTaskWorkspace.vue";
import { useSpringBatchValuationTaskPage } from "./hooks/useSpringBatchValuationTaskPage";

defineOptions({ name: "SpringBatchValuationTaskPage" });

const page = useSpringBatchValuationTaskPage();
</script>

<template>
  <SpringBatchValuationTaskWorkspace :page="page" />
</template>
