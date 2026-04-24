<script setup lang="ts">
import TransferTemplateSection from "./TransferTemplateSection.vue";

defineProps<{
  open: boolean;
  title: string;
  hint: string;
  panelTitle: string;
  panelSubtitle: string;
  loading: boolean;
  hasSchema: boolean;
  emptyDescription: string;
  confirmLoading: boolean;
  width?: number;
}>();

defineEmits<{
  (event: "ok"): void;
  (event: "cancel"): void;
}>();
</script>

<template>
  <a-modal
    class="source-modal"
    :open="open"
    :title="title"
    :width="width || 1120"
    :confirm-loading="confirmLoading"
    @ok="$emit('ok')"
    @cancel="$emit('cancel')"
    destroy-on-close
  >
    <TransferTemplateSection
      :hint="hint"
      :panel-title="panelTitle"
      :panel-subtitle="panelSubtitle"
      :loading="loading"
      :has-schema="hasSchema"
      :empty-description="emptyDescription"
    >
      <template #selector>
        <slot name="selector" />
      </template>
      <template #meta>
        <slot name="meta" />
      </template>
      <slot />
    </TransferTemplateSection>
  </a-modal>
</template>
