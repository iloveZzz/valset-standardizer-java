<script setup lang="ts">
import "./index.less";
import ParseQueueWorkspace from "./components/ParseQueueWorkspace.vue";
import { useParseQueuePage } from "./hooks/useParseQueuePage";

defineOptions({ name: "ParseQueuePage" });

const { page } = useParseQueuePage();
</script>

<template>
  <ParseQueueWorkspace :page="page" />
</template>
