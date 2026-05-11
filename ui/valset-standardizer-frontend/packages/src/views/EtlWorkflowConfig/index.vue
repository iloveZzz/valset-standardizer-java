<script setup lang="ts">
import "./index.less";
import EtlWorkflowWorkspace from "./components/EtlWorkflowWorkspace.vue";
import { useEtlWorkflowConfigPage } from "./hooks/useEtlWorkflowConfigPage";

defineOptions({ name: "EtlWorkflowConfigPage" });

const { page } = useEtlWorkflowConfigPage();
</script>

<template>
  <EtlWorkflowWorkspace :page="page" mode="definition" />
</template>
