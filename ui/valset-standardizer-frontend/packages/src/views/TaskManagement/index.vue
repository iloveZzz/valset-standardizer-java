<script setup lang="ts">
import "./index.less";
import SpringBatchValuationTaskPage from "../SpringBatchValuationTask/index.vue";

defineOptions({ name: "TaskManagementPage" });
</script>

<template>
  <div class="task-management-page">
    <div class="task-management-content">
      <SpringBatchValuationTaskPage />
    </div>
  </div>
</template>
