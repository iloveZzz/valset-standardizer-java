<script setup lang="ts">
import "./index.less";
import WorkflowInstanceWorkspace from "./components/WorkflowInstanceWorkspace.vue";
import { useWorkflowInstancePage } from "./hooks/useWorkflowInstancePage";

defineOptions({ name: "WorkflowInstancePage" });

const { page } = useWorkflowInstancePage();
</script>

<template>
  <WorkflowInstanceWorkspace :page="page" />
</template>
