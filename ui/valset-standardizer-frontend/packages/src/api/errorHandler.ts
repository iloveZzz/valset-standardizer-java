import { message } from "ant-design-vue";

/**
 * 处理业务逻辑错误
 * @param errorMessage 错误消息
 */
export const handleBusinessError = (errorMessage: string) => {
  message.error(errorMessage);
};
