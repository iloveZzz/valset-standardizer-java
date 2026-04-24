import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { viteMockServe } from "vite-plugin-mock";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv("production.jsp", rootDir);
  const apiBase = env.VITE_API_BASE_URL || "/api";
  const proxyTarget = env.VITE_PROXY_TARGET || "http://localhost:30067";
  const isProd = mode === "production";

  return {
    base: "./",
    plugins: [
      vue(),
      viteMockServe({
        mockPath: "mock",
        enable: !isProd,
        logger: true,
        watchFiles: true,
      }),
    ],
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
      port: 8081,
      host: "0.0.0.0",
      cors: true,
      origin: "http://localhost:8081",
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
      outDir: "dist-jsp",
      assetsDir: "static",
      assetsInlineLimit: 4096,
      target: "es2020",
      emptyOutDir: true,
      sourcemap: false,
      rollupOptions: {
        output: {
          chunkFileNames: "static/js/[name].js",
          entryFileNames: "static/js/index.js",
          assetFileNames: (assetInfo) => {
            if (assetInfo.name?.endsWith(".css")) return "static/css/index.css";
            if (/\.(png|jpe?g|gif|svg|webp|ico)$/.test(assetInfo.name || "")) {
              return "static/img/[name][extname]";
            }
            if (/\.(woff2?|eot|ttf|otf)$/.test(assetInfo.name || "")) {
              return "static/fonts/[name][extname]";
            }
            return "static/[name][extname]";
          },
          manualChunks: undefined,
        },
      },
    },
  };
});
