/**
 * 样式管理器：解决 qiankun 微前端环境下 vxe-table 样式冲突问题
 * 问题：Vue3 子应用使用 vxe-table v4，Vue2.6 子应用使用 v3，样式冲突
 * 方案：只禁用 vxe-table 相关样式，其他样式保持不变
 */
export class StyleManager {
  private vxeTableStyles: HTMLStyleElement[] = [];

  /** vxe-table 相关的样式关键词 */
  private readonly vxeKeywords = [
    "vxe-table",
    "vxe-ui",
    "vxe-grid",
    "vxe-cell",
    "vxe-body",
    "vxe-header",
  ];

  constructor(appName: string) {
    void appName;
  }

  /**
   * 检查样式是否与 vxe-table 相关
   */
  private isVxeTableStyle(style: HTMLStyleElement): boolean {
    const devId = style.getAttribute("data-vite-dev-id") || "";
    const textContent = style.textContent || "";

    // 检查 data-vite-dev-id 是否包含 vxe 相关路径
    if (devId.includes("vxe-table") || devId.includes("vxe-pc-ui")) {
      return true;
    }

    // 检查样式内容是否包含 vxe 相关选择器
    return this.vxeKeywords.some((keyword) => textContent.includes(keyword));
  }

  /**
   * 记录当前 head 中所有 vxe-table 相关的样式标签
   * 开发模式：匹配 style[data-vite-dev-id]
   * 生产模式：匹配所有 style 标签并检查内容
   */
  recordStyles() {
    // 开发模式：Vite 注入的样式带有 data-vite-dev-id 属性
    const viteStyles = document.querySelectorAll("style[data-vite-dev-id]");

    if (viteStyles.length > 0) {
      // 开发模式
      this.vxeTableStyles = Array.from(viteStyles).filter((style) =>
        this.isVxeTableStyle(style as HTMLStyleElement),
      ) as HTMLStyleElement[];
    } else {
      // 生产模式：检查所有 head 中的 style 标签
      const allStyles = document.querySelectorAll("style");
      this.vxeTableStyles = Array.from(allStyles).filter((style) =>
        this.isVxeTableStyle(style as HTMLStyleElement),
      ) as HTMLStyleElement[];
    }
  }

  /**
   * 禁用 vxe-table 相关样式（子应用卸载或切换时调用）
   */
  disableStyles() {
    this.vxeTableStyles.forEach((style) => {
      style.disabled = true;
    });
  }

  /**
   * 恢复 vxe-table 相关样式（子应用挂载或激活时调用）
   */
  enableStyles() {
    this.vxeTableStyles.forEach((style) => {
      style.disabled = false;
    });
  }
}

/**
 * 创建样式管理器实例并注册事件监听
 * @param appName 子应用名称
 * @returns StyleManager 实例
 */
export function createStyleManager(appName: string): StyleManager {
  const styleManager = new StyleManager(appName);

  // 监听主应用的切换事件，动态禁用/启用 vxe-table 样式
  const handleAppDeactivate = (event: CustomEvent) => {
    if (event.detail?.appName === appName) {
      styleManager.disableStyles();
    }
  };

  const handleAppActivate = (event: CustomEvent) => {
    if (event.detail?.appName === appName) {
      styleManager.enableStyles();
    }
  };

  // 注册事件监听
  window.addEventListener(
    "qiankun-app-deactivate",
    handleAppDeactivate as unknown as EventListenerOrEventListenerObject,
  );
  window.addEventListener(
    "qiankun-app-activate",
    handleAppActivate as unknown as EventListenerOrEventListenerObject,
  );

  return styleManager;
}
