<script setup lang="ts">
import SharedTransferWorkspace from "@/views/TransferOverview/components/TransferWorkspace.vue";

defineOptions({ name: "TransferGuideWorkspace" });

defineProps<{
  page: any;
}>();
</script>

<template>
  <SharedTransferWorkspace :page="page" />
</template>
