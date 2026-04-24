<script setup lang="ts">
defineProps<{
  hint: string;
  panelTitle: string;
  panelSubtitle: string;
  loading: boolean;
  hasSchema: boolean;
  emptyDescription: string;
}>();
</script>

<template>
  <div class="source-form-banner">
    <div>
      <div class="source-form-banner-label">当前模板预览</div>
    </div>
    <div class="source-form-banner-hint">
      {{ hint }}
    </div>
  </div>

  <slot name="selector" />

  <div class="template-form-panel">
    <div class="template-form-panel-header">
      <div>
        <div class="template-form-panel-title">{{ panelTitle }}</div>
        <div class="template-form-panel-subtitle">
          {{ panelSubtitle }}
        </div>
      </div>
      <div class="template-form-panel-meta">
        <slot name="meta" />
      </div>
    </div>

    <div v-if="loading" class="template-form-skeleton">
      <a-skeleton active :paragraph="{ rows: 6 }" />
    </div>
    <slot v-else-if="hasSchema" />
    <a-empty
      v-else
      class="template-form-empty"
      :description="emptyDescription"
    />
  </div>
</template>
