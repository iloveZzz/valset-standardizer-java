import { createRouter, createWebHashHistory, createWebHistory } from "vue-router";
import { routes } from "./routes";

export function createAppRouter(options?: {
  isJsp?: boolean;
  base?: string;
}) {
  const isJsp = options?.isJsp ?? false;
  const base = options?.base ?? "/";

  const history = isJsp ? createWebHashHistory() : createWebHistory(base);

  return createRouter({
    history,
    routes,
  });
}

export { routes };
