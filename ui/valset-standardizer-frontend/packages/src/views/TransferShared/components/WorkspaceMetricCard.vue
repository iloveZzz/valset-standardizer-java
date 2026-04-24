<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
  desc: string;
}>();
</script>

<template>
  <div class="workspace-stat-card">
    <div class="workspace-stat-label">{{ label }}</div>
    <div class="workspace-stat-value">{{ value }}</div>
    <div class="workspace-stat-desc">{{ desc }}</div>
  </div>
</template>
