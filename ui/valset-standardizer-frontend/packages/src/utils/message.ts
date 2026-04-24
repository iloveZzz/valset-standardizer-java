/**
 * 全局 Message 工具（去重版）
 * - 统一收口 ant-design-vue 的 message 调用
 * - 默认同类型消息使用固定 key，避免并发重复叠加
 * - 支持字符串与对象两种入参
 */
import { message as AntMessage } from "ant-design-vue";

type MessageLevel = "success" | "info" | "warning" | "error" | "loading";

type MessageInput =
  | string
  | {
      /** 消息内容（与旧项目保持兼容，支持 message/content 两种写法） */
      message?: string;
      content?: string;
      /** 自动关闭时间，单位秒 */
      duration?: number;
      /** 指定唯一 key，可覆盖默认去重 key */
      key?: string | number;
      /** 设为 false 关闭去重（默认开启） */
      dedupe?: boolean;
    };

type NormalizedOptions = {
  content: string;
  duration?: number;
  key?: string | number;
  dedupe?: boolean;
};

const DEFAULT_DURATION = 1.5; // 秒

// 为不同类型提供稳定的默认去重 key
const DEFAULT_KEYS: Record<MessageLevel, string> = {
  success: "GLOBAL_MSG_SUCCESS",
  info: "GLOBAL_MSG_INFO",
  warning: "GLOBAL_MSG_WARNING",
  error: "GLOBAL_MSG_ERROR",
  loading: "GLOBAL_MSG_LOADING",
};

const normalize = (input: MessageInput): NormalizedOptions => {
  if (typeof input === "string") {
    return { content: input };
  }
  return {
    content: input.content ?? input.message ?? "",
    duration: input.duration,
    key: input.key,
    dedupe: input.dedupe,
  };
};

const openByType = (type: MessageLevel, input: MessageInput) => {
  const { content, duration, key, dedupe } = normalize(input);
  const finalKey = dedupe === false ? key : (key ?? DEFAULT_KEYS[type]);
  // 使用 open + 固定 key，将并发的同类消息合并为一条
  return AntMessage.open({
    type,
    content,
    duration: duration ?? DEFAULT_DURATION,
    key: finalKey as any,
  });
};

export const customMessage = {
  open: (input: MessageInput & { type: MessageLevel }) =>
    openByType(input.type, input),
  success: (input: MessageInput) => openByType("success", input),
  info: (input: MessageInput) => openByType("info", input),
  warning: (input: MessageInput) => openByType("warning", input),
  error: (input: MessageInput) => openByType("error", input),
  loading: (input: MessageInput) => openByType("loading", input),
};
