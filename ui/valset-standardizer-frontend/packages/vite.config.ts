import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import qiankun from "vite-plugin-qiankun";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const useDevMode = true;
const rootDir = dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, rootDir);
  const subAppName = env.VITE_SUB_APP_NAME || "yss-fs-frontend";
  const activeRule = env.VITE_ACTIVE_RULE || `/${subAppName}`;
  const apiBase = env.VITE_API_BASE_URL || "/api";
  const proxyTarget = env.VITE_PROXY_TARGET || "http://localhost:30066";
  const isProd = mode === "production";
  const isStandalone = env.VITE_STANDALONE_DEPLOY === "true";
  const cleanActiveRule = activeRule
    .replace(/^\//, "")
    .replace(/^subApp\//, "");
  const base = isProd && !isStandalone ? `/${cleanActiveRule}/` : "/";

  return {
    base,
    plugins: [vue(), qiankun(subAppName, { useDevMode })],
    resolve: {
      alias: {
        "@": resolve(rootDir, "src"),
        "@api": resolve(rootDir, "src/api"),
        "@components": resolve(rootDir, "src/components"),
        "@views": resolve(rootDir, "src/views"),
        "@utils": resolve(rootDir, "src/utils"),
        "@types": resolve(rootDir, "src/types"),
        "@hooks": resolve(rootDir, "src/hooks"),
        "@store": resolve(rootDir, "src/store"),
        "@styles": resolve(rootDir, "src/styles"),
      },
      dedupe: ["vue"],
    },
    css: {
      preprocessorOptions: {
        less: {
          additionalData: `@import "@/styles/variables.less";`,
          javascriptEnabled: true,
        },
      },
    },
    server: {
      port: 8082,
      host: "0.0.0.0",
      cors: true,
      origin: "http://localhost:8082",
      headers: {
        "Access-Control-Allow-Origin": "*",
      },
      proxy: {
        [apiBase]: {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp(`^${apiBase}`), ""),
        },
      },
    },
    optimizeDeps: {
      force: true,
      include: ["axios", "ant-design-vue"],
      exclude: ["@/api/generated/approval"],
    },
    build: {
      target: "es2020",
      modulePreload: { polyfill: false },
      rollupOptions: {
        output: {
          manualChunks: undefined,
        },
      },
    },
  };
});
