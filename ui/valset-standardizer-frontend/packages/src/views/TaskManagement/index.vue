<script setup lang="ts">
import "./index.less";
import BatchValuationTaskPage from "../BatchValuationTask/index.vue";

defineOptions({ name: "TaskManagementPage" });
</script>

<template>
  <div class="task-management-page">
    <div class="task-management-content">
      <BatchValuationTaskPage />
    </div>
  </div>
</template>
