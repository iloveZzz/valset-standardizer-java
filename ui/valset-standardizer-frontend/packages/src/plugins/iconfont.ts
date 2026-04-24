import type { App } from "vue";
import { createFromIconfontCN } from "@ant-design/icons-vue";

export type IconfontOptions = {
  scriptUrls: string[];
  componentName?: string;
  extraCommonProps?: Record<string, any>;
};

// 统一注册基于 Iconfont(Symbol) 的全局图标组件。
// 默认优先使用外网 CDN，其次使用本地离线文件作为回退。
export const setupIconfont = (app: App, options?: Partial<IconfontOptions>) => {
  const defaultUrls = [
    "https://at.alicdn.com/t/c/font_3948833_yhvtfgdjw7.js",
    "/iconfont/local-iconfont.js",
  ];

  const scriptUrls =
    options?.scriptUrls && options.scriptUrls.length > 0
      ? options.scriptUrls
      : defaultUrls;

  const IconFont = createFromIconfontCN({
    scriptUrl: scriptUrls,
    extraCommonProps: options?.extraCommonProps,
  });

  app.component(options?.componentName ?? "YIcon", IconFont);
  return IconFont;
};
