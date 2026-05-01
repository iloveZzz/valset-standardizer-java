<script setup lang="ts">
import { computed } from "vue";
import type { Component } from "vue";

type MenuItem = {
  title: string;
  path: string;
  icon: Component;
  children?: MenuItem[];
};

const props = defineProps<{
  items: MenuItem[];
  selectedKeys: string[];
}>();

defineEmits<{
  (event: "select", path: string): void;
}>();

const openKeys = computed(() =>
  props.items.filter((item) => item.children?.length).map((item) => item.path),
);
</script>

<template>
  <a-menu
    class="recursive-menu"
    :selected-keys="selectedKeys"
    :default-open-keys="openKeys"
    mode="inline"
    theme="light"
  >
    <template v-for="item in items" :key="item.path">
      <a-sub-menu v-if="item.children?.length" :key="item.path">
        <template #title>
          <component :is="item.icon" />
          <span>{{ item.title }}</span>
        </template>
        <a-menu-item
          v-for="child in item.children"
          :key="child.path"
          @click="$emit('select', child.path)"
        >
          <component :is="child.icon" />
          <span>{{ child.title }}</span>
        </a-menu-item>
      </a-sub-menu>
      <a-menu-item v-else :key="item.path" @click="$emit('select', item.path)">
        <component :is="item.icon" />
        <span>{{ item.title }}</span>
      </a-menu-item>
    </template>
  </a-menu>
</template>
