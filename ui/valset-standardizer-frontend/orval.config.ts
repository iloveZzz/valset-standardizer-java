import { defineConfig } from "orval";

export default defineConfig({
  // 微应用相关API
  valset: {
    input: {
      target: "./openapi/openapi.json",
      override: {
        transformer: "./scripts/api-transformer.cjs",
      },
    },
    output: {
      target: "./packages/src/api/generated/valset/index.ts",
      schemas: "./packages/src/api/generated/valset/schemas",
      client: "axios",
      prettier: true,
      override: {
        mutator: {
          path: "./packages/src/api/mutator.ts",
          name: "customInstance",
        },
        title: (title) => `${title}Api`,
        // 自定义操作名称，简化命名
        operationName: (operation: any, route: string, verb: string) => {
          // 使用原始的 operationId 作为基础，如果存在的话
          if (operation.operationId) {
            // 直接使用 operationId，确保唯一性
            return operation.operationId;
          }

          // 如果没有 operationId，则根据路由生成
          const cleanRoute = route.replace(/^\/api/, "");
          const pathSegments = cleanRoute.split("/").filter(Boolean);
          // 处理特殊路径参数
          const processedSegments = pathSegments.map((segment, index) => {
            if (segment.startsWith("{") && segment.endsWith("}")) {
              return "ById";
            }
            return segment.charAt(0).toUpperCase() + segment.slice(1);
          });
          // 生成操作名称，确保唯一性
          const operationName =
            verb.charAt(0).toUpperCase() +
            verb.slice(1) +
            processedSegments.join("");
          return operationName;
        },
        // 简化类型命名
        components: {
          schemas: {
            suffix: "",
          },
          responses: {
            suffix: "Response",
          },
          parameters: {
            suffix: "Params",
          },
          requestBodies: {
            suffix: "Request",
          },
        },
      },
    },
  },
});
