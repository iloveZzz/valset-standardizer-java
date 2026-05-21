<script setup lang="ts">
import "./index.less";
import ProductInfoExtractionWorkspace from "./components/ProductInfoExtractionWorkspace.vue";
import { useProductInfoExtractionPage } from "./hooks/useProductInfoExtractionPage";

defineOptions({ name: "ProductInfoExtractionPage" });

const page = useProductInfoExtractionPage();
</script>

<template>
  <ProductInfoExtractionWorkspace :page="page" />
</template>
