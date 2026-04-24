<script setup lang="ts">
defineProps<{
  label: string;
  options: Array<{ label: string; value: string }>;
  modelValue: string;
  placeholder?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  (event: "update:modelValue", value: string): void;
}>();
</script>

<template>
  <a-form layout="horizontal">
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item :label="label" required>
          <a-select
            :value="modelValue"
            :placeholder="placeholder || '请选择'"
            :disabled="disabled"
            @update:value="emit('update:modelValue', String($event ?? ''))"
          >
            <a-select-option
              v-for="item in options"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>
