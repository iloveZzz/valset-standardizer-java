<script setup lang="ts">
import type { Component } from "vue";

type MenuItem = {
  title: string;
  path: string;
  icon: Component;
};

defineProps<{
  items: MenuItem[];
  selectedKeys: string[];
}>();

defineEmits<{
  (event: "select", path: string): void;
}>();
</script>

<template>
  <a-menu class="recursive-menu" :selected-keys="selectedKeys" mode="inline" theme="light">
    <a-menu-item v-for="item in items" :key="item.path" @click="$emit('select', item.path)">
      <component :is="item.icon" />
      <span>{{ item.title }}</span>
    </a-menu-item>
  </a-menu>
</template>
