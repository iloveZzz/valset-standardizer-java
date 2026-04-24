<script setup lang="ts">
import { computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import MainLayout from "@/layout/MainLayout.vue";
import { useQiankun } from "@/hooks/useQiankun";
import { useRouteMenu } from "@/hooks/useRouteMenu";
import { useKeepAlive } from "@/hooks/useKeepAlive";
import { routes } from "@/router/routes";
import { useThemeStore } from "@/store/theme";

const route = useRoute();
const { isInQiankun } = useQiankun();
const themeStore = useThemeStore();
const isDev = import.meta.env.MODE === "development";
const { selectedKeys } = useRouteMenu(routes);
const { cachedRoutes } = useKeepAlive(routes);

const useShell = computed(() => !isInQiankun.value && isDev && route.meta.layout !== false);

const updateDocumentTitle = () => {
  const title = String(route.meta.title || "Transfer");
  document.title = title === "Transfer" ? title : `${title} - Transfer`;
};

onMounted(() => {
  themeStore.init();
  updateDocumentTitle();
});

watch(
  () => route.fullPath,
  () => {
    updateDocumentTitle();
  },
);
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="themeStore.themeConfig">
    <div class="workspace-page">
      <MainLayout
        v-if="useShell"
        :selected-keys="selectedKeys"
        :cached-routes="cachedRoutes"
      />
      <router-view v-else v-slot="{ Component, route: currentRoute }">
        <keep-alive :include="cachedRoutes">
          <component
            :is="Component"
            :key="currentRoute.meta.keepAlive ? undefined : currentRoute.fullPath"
          />
        </keep-alive>
      </router-view>
    </div>
  </a-config-provider>
</template>
