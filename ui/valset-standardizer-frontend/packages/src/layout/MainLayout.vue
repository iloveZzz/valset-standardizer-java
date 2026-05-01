<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { workspaceNav } from "@/router/routes";
import { anonymousAccount } from "@/constants/anonymousAccount";
import RecursiveMenu from "@/components/RecursiveMenu.vue";

defineProps<{
  selectedKeys: string[];
  cachedRoutes: string[];
}>();

const router = useRouter();
const route = useRoute();

const goTo = (path: string) => {
  if (route.path !== path) {
    router.push(path);
  }
};
</script>

<template>
  <a-layout class="workspace-shell app-shell">
    <a-layout-sider
      width="272"
      breakpoint="lg"
      collapsed-width="0"
      class="workspace-sider shell-sider"
    >
      <div class="brand-brand">
        <div class="brand-mark">转换</div>
        <div class="brand-copy">
          <div class="brand-title">Transfer</div>
          <div class="brand-subtitle">YSS 文件收发管理</div>
        </div>
      </div>

      <div class="nav-group">
        <div class="nav-label">主导航</div>
        <RecursiveMenu
          :items="workspaceNav"
          :selected-keys="selectedKeys"
          @select="goTo"
        />
      </div>

      <div class="sider-footer-info">
        <div class="workspace-user">
          <div class="workspace-avatar">AN</div>
          <div>
            <strong>{{ anonymousAccount.name }}</strong>
            <p>匿名工作区</p>
          </div>
        </div>
      </div>
    </a-layout-sider>

    <a-layout class="workspace-content shell-content">
      <a-layout-content class="workspace-main shell-main">
        <div class="workspace-page-frame">
          <router-view v-slot="{ Component, route: currentRoute }">
            <transition name="workspace-fade" mode="out-in">
              <keep-alive :include="cachedRoutes">
                <component
                  :is="Component"
                  :key="
                    currentRoute.meta.keepAlive
                      ? undefined
                      : currentRoute.fullPath
                  "
                />
              </keep-alive>
            </transition>
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped lang="less">
.app-shell {
  min-height: 100vh;
  height: 100%;
  background: var(--bg-color);
}

.shell-sider {
  display: flex;
  flex-direction: column;
  padding: 18px 14px 14px;
  background: var(--app-sidebar);
  border-right: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06);
}

.brand-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #1677ff, #69b1ff);
}

.brand-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.brand-subtitle {
  color: rgba(15, 23, 42, 0.55);
  font-size: 12px;
}

.nav-group {
  margin-top: 18px;
}

.nav-label {
  margin: 0 0 10px;
  padding: 0 6px;
  color: rgba(15, 23, 42, 0.45);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.workspace-menu,
.recursive-menu {
  border-inline-end: none;
  background: transparent;
}

.workspace-menu :deep(.ant-menu-item),
.recursive-menu :deep(.ant-menu-item),
.recursive-menu :deep(.ant-menu-submenu-title) {
  display: flex;
  align-items: center;
  margin-inline: 0;
  width: 100%;
  height: 48px;
  gap: 12px;
  color: rgba(15, 23, 42, 0.72);
  font-weight: 600;
  border-radius: 14px;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.workspace-menu :deep(.ant-menu-item:hover),
.recursive-menu :deep(.ant-menu-item:hover),
.recursive-menu :deep(.ant-menu-submenu-title:hover) {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.06);
}

.workspace-menu :deep(.ant-menu-item .anticon),
.recursive-menu :deep(.ant-menu-item .anticon),
.recursive-menu :deep(.ant-menu-submenu-title .anticon) {
  font-size: 18px;
  color: inherit;
}

.workspace-menu :deep(.ant-menu-item-selected),
.recursive-menu :deep(.ant-menu-item-selected) {
  color: #1677ff !important;
  background: rgba(22, 119, 255, 0.1) !important;
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.12);
  transform: translateX(1px);
}

.workspace-menu :deep(.ant-menu-item-selected::after),
.recursive-menu :deep(.ant-menu-item-selected::after) {
  border-right: none !important;
}

.workspace-menu :deep(.ant-menu-item-selected .anticon),
.recursive-menu :deep(.ant-menu-item-selected .anticon) {
  color: #4f46e5;
}

.sider-footer-info {
  padding-top: 18px;
}

.workspace-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.workspace-avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 800;
  background: rgba(22, 119, 255, 0.12);
}

.workspace-user strong {
  display: block;
  color: #0f172a;
  font-size: 13px;
}

.workspace-user p {
  margin: 2px 0 0;
  color: rgba(15, 23, 42, 0.58);
  font-size: 12px;
}

.shell-content {
  min-height: 0;
  min-width: 0;
  height: 100%;
  background: transparent;
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: auto;
  padding: 18px 24px 0;
  line-height: 1;
  background: transparent;
}

.header-left {
  flex: 1;
  min-width: 0;
}

.search-box {
  max-width: 360px;
  border-radius: 999px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shell-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 18px 24px 24px;
  background: #fff;
}

.workspace-page-frame {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 0;
}

.workspace-fade-enter-active,
.workspace-fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.workspace-fade-enter-from,
.workspace-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (max-width: 992px) {
  .shell-header,
  .shell-main {
    padding-inline: 16px;
  }

  .workspace-page-frame {
    min-height: 0;
  }
}
</style>
