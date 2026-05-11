<script setup lang="ts">
import "./index.less";
import EtlWorkflowStageWorkspace from "./components/EtlWorkflowStageWorkspace.vue";
import { useEtlWorkflowConfigPage } from "./hooks/useEtlWorkflowConfigPage";

defineOptions({ name: "EtlWorkflowStagePage" });

const { page } = useEtlWorkflowConfigPage();
</script>

<template>
  <EtlWorkflowStageWorkspace :page="page" />
</template>
