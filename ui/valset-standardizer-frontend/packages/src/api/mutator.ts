import axios, { type AxiosRequestConfig } from "axios";
import JSONbig from "json-bigint";
import { handleErrorResponse } from "@/utils";
import { handleBusinessError } from "./errorHandler";

// 创建支持大数字的解析器(关键:storeAsString=true 将大数字转为字符串)
const JSONbigString = JSONbig({ storeAsString: true });

// 获取环境变量，兼容 Node.js 环境（Orval 生成时）和浏览器环境（运行时）
const getBaseURL = () => {
  // 优先尝试使用 process.env（Node.js 环境，Orval 生成时）
  if (
    typeof process !== "undefined" &&
    process.env &&
    process.env.VITE_API_BASE_URL
  ) {
    return process.env.VITE_API_BASE_URL;
  }
  // 在浏览器环境中使用 import.meta.env（运行时）
  try {
    // @ts-ignore - import.meta 在 es2015 目标环境中可能不可用，但运行时可用
    if (typeof import.meta !== "undefined" && import.meta.env) {
      // @ts-ignore
      return import.meta.env.VITE_API_BASE_URL || "/api";
    }
  } catch (e) {
    // 如果 import.meta 不可用，忽略错误
  }
  return "/api";
};
/** 创建 axios 实例 */
const axiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 全局配置 transformResponse 处理大数字
const originalTransformResponse = axiosInstance.defaults.transformResponse;
axiosInstance.defaults.transformResponse = [
  (data, headers) => {
    try {
      // 仅处理字符串类型的 JSON 响应
      if (typeof data === "string") {
        // 检查响应头，确保是 JSON 类型
        const contentType =
          headers?.["content-type"] || headers?.["Content-Type"] || "";
        if (
          contentType.includes("application/json") ||
          contentType.includes("text/json")
        ) {
          return JSONbigString.parse(data);
        }
      }
      return data; // 非字符串或非JSON数据原样返回
    } catch (e) {
      // 解析失败时返回原始数据
      return data;
    }
  },
  ...(Array.isArray(originalTransformResponse)
    ? originalTransformResponse
    : originalTransformResponse
      ? [originalTransformResponse]
      : []),
];

// 请求拦截器
axiosInstance.interceptors.request.use(
  (config: any) => {
    // /oauth2/token 接口不需要携带 token
    const requestUrl = config?.url || "";
    if (requestUrl.includes("/oauth2/token")) {
      return config;
    }

    // 其他接口添加认证 token
    const token = localStorage.getItem("access_token");
    if (token) {
      // 去除 token 首尾可能存在的双引号
      const cleanToken = token.replace(/^"|"$/g, "");
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${cleanToken}`;
    }

    return config;
  },
  (error: any) => {
    console.error("❌ API Request Error:", error);
    return Promise.reject(error);
  },
);

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response) => {
    const { data, headers } = response;
    const isBlob = data instanceof Blob;

    // 优先处理业务逻辑错误 (ResultVO)
    // 如果 response.data 是对象且包含 success 字段，并且 success === false
    // 注意：有些接口可能不返回 ResultVO，所以需要严格判断
    if (
      data &&
      typeof data === "object" &&
      "success" in data &&
      data.success === false
    ) {
      const errorMessage = data.message || "请求失败";
      handleBusinessError(errorMessage);
      // 返回 reject，中断后续 then 链
      return Promise.reject(new Error(errorMessage));
    }

    return isBlob ? { data, headers } : data;
  },
  async (error) => await handleErrorResponse(error),
);

/**
 * Orval 的 customInstance 函数
 * @description 用于 Orval 生成的 API 客户端调用
 */
export const customInstance = <T = any>(
  config: AxiosRequestConfig,
): Promise<T> => {
  // 处理 DELETE 请求的 params
  // DELETE 请求默认会将 params 放到 URL query 中，但后端期望在 request body 中
  if (config.method?.toUpperCase() === "DELETE" && config.params) {
    config.data = config.params;
    delete config.params;
  }

  return axiosInstance(config) as unknown as Promise<T>;
};

// 默认导出
export default customInstance;
