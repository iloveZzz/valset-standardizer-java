import { computed } from "vue";
import { qiankunWindow } from "vite-plugin-qiankun/dist/helper";

/**
 * qiankun 微前端环境相关的 hook
 * @returns 返回 qiankun 环境检测相关的响应式数据和方法
 */
export function useQiankun() {
  /**
   * 检查是否在 qiankun 环境中运行
   */
  const isInQiankun = computed(() => {
    return qiankunWindow.__POWERED_BY_QIANKUN__ || false;
  });

  /**
   * 获取 qiankun 的全局配置
   */
  const qiankunConfig = computed(() => {
    if (isInQiankun.value) {
      return qiankunWindow;
    }
    return null;
  });

  return {
    isInQiankun,
    qiankunConfig,
  };
}
