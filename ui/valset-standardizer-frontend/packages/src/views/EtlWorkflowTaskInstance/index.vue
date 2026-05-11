<script setup lang="ts">
import "./index.less";
import WorkflowTaskInstanceWorkspace from "./components/Workspace.vue";
import { useEtlWorkflowTaskInstancePage } from "./hooks/useEtlWorkflowTaskInstancePage";

defineOptions({ name: "WorkflowTaskInstancePage" });

const { page } = useEtlWorkflowTaskInstancePage();
</script>

<template>
  <WorkflowTaskInstanceWorkspace :page="page" />
</template>
