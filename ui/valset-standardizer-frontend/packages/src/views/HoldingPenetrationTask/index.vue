<script setup lang="ts">
import "./index.less";
import HoldingPenetrationTaskWorkspace from "./components/HoldingPenetrationTaskWorkspace.vue";
import { useHoldingPenetrationTaskPage } from "./hooks/useHoldingPenetrationTaskPage";

defineOptions({ name: "HoldingPenetrationTaskPage" });

const { page } = useHoldingPenetrationTaskPage();
</script>

<template>
  <HoldingPenetrationTaskWorkspace :page="page" />
</template>
