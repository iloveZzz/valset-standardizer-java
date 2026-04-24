import { computed } from "vue";
import type { RouteRecordRaw } from "vue-router";

export function useKeepAlive(routes: RouteRecordRaw[]) {
  const getKeepAliveRoutes = (routeList: RouteRecordRaw[]): string[] => {
    const result: string[] = [];

    routeList.forEach((route) => {
      if (route.meta && route.meta.keepAlive === true && route.name) {
        result.push(route.name as string);
      }

      if (route.children && route.children.length > 0) {
        result.push(...getKeepAliveRoutes(route.children));
      }
    });

    return result;
  };

  const cachedRoutes = computed(() => getKeepAliveRoutes(routes));

  return {
    cachedRoutes,
  };
}
