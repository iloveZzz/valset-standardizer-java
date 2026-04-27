<script setup lang="ts">
import "./index.less";
import TransferWorkspace from "./components/TransferWorkspace.vue";
import { useTransferPage } from "./hooks/useTransferPage";

defineOptions({ name: "TransferInboxPage" });

const { page } = useTransferPage();
</script>

<template>
  <TransferWorkspace :page="page" />
</template>
