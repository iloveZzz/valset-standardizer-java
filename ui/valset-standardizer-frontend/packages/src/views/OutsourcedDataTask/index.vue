<script setup lang="ts">
import "./index.less";
import OutsourcedDataTaskWorkspace from "./components/OutsourcedDataTaskWorkspace.vue";
import { useOutsourcedDataTaskPage } from "./hooks/useOutsourcedDataTaskPage";

defineOptions({ name: "OutsourcedDataTaskPage" });

const { page } = useOutsourcedDataTaskPage();
</script>

<template>
  <OutsourcedDataTaskWorkspace :page="page" />
</template>
