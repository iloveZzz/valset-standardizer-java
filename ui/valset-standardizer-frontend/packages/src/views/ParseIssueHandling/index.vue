<script setup lang="ts">
import "./index.less";
import "../ProductInfoExtraction/index.less";
import { onMounted } from "vue";
import { useRoute } from "vue-router";
import ParseIssueHandlingWorkspace from "./components/ParseIssueHandlingWorkspace.vue";
import { useParseIssueHandlingPage } from "./hooks/useParseIssueHandlingPage";
import type { ParseIssueHandlingTab } from "./types";

defineOptions({ name: "ParseIssueHandlingPage" });

const page = useParseIssueHandlingPage();
const route = useRoute();
const tabKeys: ParseIssueHandlingTab[] = [
  "fileParseSource",
  "fileParseRule",
  "productMatchRule",
  "productInfoExtraction",
];

onMounted(() => {
  const tab = String(route.query.tab ?? "");
  if (tabKeys.includes(tab as ParseIssueHandlingTab)) {
    page.setActiveTab(tab as ParseIssueHandlingTab);
  }
});
</script>

<template>
  <ParseIssueHandlingWorkspace :page="page" />
</template>
