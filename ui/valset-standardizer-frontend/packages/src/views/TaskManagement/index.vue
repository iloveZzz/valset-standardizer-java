<script setup lang="ts">
import "./index.less";
import OutsourcedDataTaskPage from "../OutsourcedDataTask/index.vue";

defineOptions({ name: "TaskManagementPage" });
</script>

<template>
  <div class="task-management-page">
    <div class="task-management-content">
      <OutsourcedDataTaskPage />
    </div>
  </div>
</template>
