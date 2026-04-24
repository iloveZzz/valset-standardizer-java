<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
  meta: string;
}>();
</script>

<template>
  <div class="workspace-table-toolbar">
    <div class="workspace-table-toolbar-header">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="workspace-table-toolbar-meta">
        {{ meta }}
      </div>
    </div>
    <slot />
  </div>
</template>
