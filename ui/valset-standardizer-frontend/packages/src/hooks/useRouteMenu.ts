import { computed } from "vue";
import { useRoute } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

export function useRouteMenu(routes: RouteRecordRaw[]) {
  const route = useRoute();

  const selectedKeys = computed(() => {
    return route.path ? [route.path] : [];
  });

  const menuRoutes = computed(() => routes.filter((item) => item.name));

  return {
    selectedKeys,
    menuRoutes,
    currentRoute: route,
  };
}
