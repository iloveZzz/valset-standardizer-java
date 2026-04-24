/// <reference types="vite/client" />

declare module "*.vue" {
  import type { DefineComponent } from "vue";

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_SUB_APP_NAME: string;
  readonly VITE_ACTIVE_RULE?: string;
  readonly VITE_API_BASE_URL: string;
  readonly VITE_PROXY_TARGET?: string;
  readonly VITE_STANDALONE_DEPLOY?: string;
  readonly VITE_IS_JSP?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
