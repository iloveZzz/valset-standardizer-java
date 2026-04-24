import { defineStore } from "pinia";
import { computed, ref, watch } from "vue";
import { theme as antdTheme } from "ant-design-vue";

type ThemePersist = {
  primaryColor: string;
  isDarkMode: boolean;
  isCompactMode: boolean;
};

const STORAGE_KEY = "yss_fs_theme_v1";

export const useThemeStore = defineStore("theme", () => {
  const primaryColor = ref("#1677ff");
  const isDarkMode = ref(false);
  const isCompactMode = ref(false);

  const algorithms = computed(() => {
    const list: any[] = [];
    if (isDarkMode.value) list.push(antdTheme.darkAlgorithm);
    if (isCompactMode.value) list.push(antdTheme.compactAlgorithm);
    return list;
  });

  const themeConfig = computed(() => ({
    token: {
      colorPrimary: primaryColor.value,
      colorInfo: primaryColor.value,
      colorBgLayout: isDarkMode.value ? "#0f172a" : "#f5f7fb",
      colorBgContainer: isDarkMode.value ? "rgba(15, 23, 42, 0.88)" : "rgba(255, 255, 255, 0.88)",
      colorBorderSecondary: isDarkMode.value ? "rgba(255, 255, 255, 0.08)" : "rgba(15, 23, 42, 0.08)",
      borderRadius: 10,
      borderRadiusLG: 14,
      fontSize: 14,
      controlHeight: 36,
      boxShadowSecondary: isDarkMode.value
        ? "0 16px 40px rgba(0, 0, 0, 0.28)"
        : "0 12px 32px rgba(15, 23, 42, 0.08)",
    },
    algorithm: algorithms.value,
  }));

  const persist = () => {
    const data: ThemePersist = {
      primaryColor: primaryColor.value,
      isDarkMode: isDarkMode.value,
      isCompactMode: isCompactMode.value,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  };

  const applyCssVars = () => {
    const root = document.documentElement;
    root.style.setProperty("--app-accent", primaryColor.value);
    root.style.setProperty("--app-accent-soft", `${primaryColor.value}1f`);
    root.style.setProperty("--app-bg", isDarkMode.value ? "#0f172a" : "#f5f7fb");
    root.style.setProperty("--app-surface", isDarkMode.value ? "rgba(15, 23, 42, 0.72)" : "rgba(255, 255, 255, 0.88)");
    root.style.setProperty("--app-surface-strong", isDarkMode.value ? "rgba(15, 23, 42, 0.92)" : "rgba(255, 255, 255, 0.96)");
    root.style.setProperty("--app-border", isDarkMode.value ? "rgba(255, 255, 255, 0.08)" : "rgba(15, 23, 42, 0.08)");
    root.style.setProperty(
      "--app-text",
      isDarkMode.value ? "#f8fafc" : "#0f172a",
    );
    root.style.setProperty(
      "--app-text-secondary",
      isDarkMode.value ? "rgba(248, 250, 252, 0.68)" : "rgba(15, 23, 42, 0.68)",
    );
    root.style.setProperty("--app-sidebar", isDarkMode.value ? "#020617" : "rgba(255, 255, 255, 0.92)");
    root.style.setProperty("--app-sidebar-soft", isDarkMode.value ? "rgba(255, 255, 255, 0.08)" : "rgba(15, 23, 42, 0.04)");
  };

  const init = () => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const saved = JSON.parse(raw) as ThemePersist;
        primaryColor.value = saved.primaryColor || primaryColor.value;
        isDarkMode.value = Boolean(saved.isDarkMode);
        isCompactMode.value = Boolean(saved.isCompactMode);
      }
    } catch {
      // Ignore malformed storage payloads.
    }
    applyCssVars();
  };

  const setPrimaryColor = (value: string) => {
    primaryColor.value = value;
  };

  const toggleDarkMode = () => {
    isDarkMode.value = !isDarkMode.value;
  };

  const toggleCompactMode = () => {
    isCompactMode.value = !isCompactMode.value;
  };

  watch([primaryColor, isDarkMode, isCompactMode], () => {
    persist();
    applyCssVars();
  });

  return {
    primaryColor,
    isDarkMode,
    isCompactMode,
    themeConfig,
    init,
    setPrimaryColor,
    toggleDarkMode,
    toggleCompactMode,
  };
});
