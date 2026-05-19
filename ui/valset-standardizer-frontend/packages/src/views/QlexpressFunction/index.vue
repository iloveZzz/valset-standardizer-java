<script setup lang="ts">
import "./index.less";
import QlexpressFunctionWorkspace from "./components/QlexpressFunctionWorkspace.vue";
import { useQlexpressFunctionPage } from "./hooks/useQlexpressFunctionPage";

defineOptions({ name: "QlexpressFunctionPage" });

const page = useQlexpressFunctionPage();
</script>

<template>
  <QlexpressFunctionWorkspace :page="page" />
</template>
