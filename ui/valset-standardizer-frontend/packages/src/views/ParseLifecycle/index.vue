<script setup lang="ts">
import "./index.less";
import ParseLifecycleWorkspace from "./components/ParseLifecycleWorkspace.vue";
import { useParseLifecyclePage } from "./hooks/useParseLifecyclePage";

defineOptions({ name: "ParseLifecyclePage" });

const { page } = useParseLifecyclePage();
</script>

<template>
  <ParseLifecycleWorkspace :page="page" />
</template>
