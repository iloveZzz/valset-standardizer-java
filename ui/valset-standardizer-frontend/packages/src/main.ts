import { createApp } from "vue";
import { createPinia } from "pinia";
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import "ant-design-vue/dist/reset.css";
import Antd from "ant-design-vue";
import "@yss-ui/components/style.css";
import App from "./App.vue";
import { createAppRouter } from "./router";
import "@/styles/index.less";
import {
  renderWithQiankun,
  qiankunWindow,
} from "vite-plugin-qiankun/dist/helper";

dayjs.locale("zh-cn");

const isJspBuild =
  import.meta.env.VITE_IS_JSP === "true" || import.meta.env.MODE.includes("jsp");

let app: ReturnType<typeof createApp> | null = null;
let activeRouter = createAppRouter({
  isJsp: isJspBuild,
  base:
    import.meta.env.VITE_STANDALONE_DEPLOY === "true"
      ? "/"
      : `/${import.meta.env.VITE_SUB_APP_NAME || ""}/`,
});

function render(props: any = {}) {
  const container = props.container;
  const mountTarget = container
    ? container.querySelector("#app") || container
    : "#app";

  if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
    activeRouter = createAppRouter({
      isJsp: isJspBuild,
      base: "/",
    });
  } else {
    activeRouter = createAppRouter({
      isJsp: false,
      base: `/${
        import.meta.env.VITE_ACTIVE_RULE?.replace(/^\//, "") ||
        import.meta.env.VITE_SUB_APP_NAME ||
        "yss-fs-frontend"
      }/`,
    });
  }

  app = createApp(App);
  app.use(createPinia());
  app.use(activeRouter);
  app.use(Antd);
  app.mount(mountTarget as any);
}

renderWithQiankun({
  mount(props) {
    render(props);
  },
  bootstrap() {},
  unmount() {
    app?.unmount();
    app = null;
  },
  update() {},
});

if (!qiankunWindow.__POWERED_BY_QIANKUN__) {
  render();
}
