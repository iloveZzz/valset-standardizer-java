<script setup lang="ts">
import {
  ArrowDownOutlined,
  ClearOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
} from "@ant-design/icons-vue";
import { YButton, YMonaco } from "@yss-ui/components";
import type { RunLogPage } from "../types";

const { page } = defineProps<{
  page: RunLogPage;
}>();

const handleNodeChange = (value: unknown) => {
  page.changeNode(typeof value === "string" ? value : "");
};
</script>

<template>
  <div class="transfer-run-log-page">
    <div class="system-output-log-toolbar">
      <div class="system-output-log-title">
        <h2>运行日志</h2>
        <span>系统输出日志</span>
      </div>
      <a-space wrap>
        <a-select
          :value="page.selectedNodeId"
          :loading="page.nodeLoading"
          :options="page.nodeOptions"
          class="system-output-log-node-select"
          placeholder="选择日志节点"
          @change="handleNodeChange"
        />
        <a-tag color="blue">日志 {{ page.logCount }}</a-tag>
        <a-tag color="green">行数 {{ page.totalLines }}</a-tag>
        <a-tag v-if="page.isStreaming" color="orange">
          <span class="blinking-dot">●</span>
          实时流式
        </a-tag>
        <YButton
          v-if="!page.isStreaming"
          type="primary"
          size="small"
          @click="page.startLogStream"
        >
          <template #icon><PlayCircleOutlined /></template>
          开始实时日志
        </YButton>
        <YButton v-else danger size="small" @click="page.stopLogStream">
          <template #icon><PauseCircleOutlined /></template>
          停止日志流
        </YButton>
        <YButton
          size="small"
          :loading="page.cleanupLoading"
          @click="page.clearLogs"
        >
          <template #icon><ClearOutlined /></template>
          清空日志
        </YButton>
        <YButton size="small" @click="page.scrollToBottom">
          <template #icon><ArrowDownOutlined /></template>
          滚动到底部
        </YButton>
      </a-space>
    </div>

    <div class="system-output-log-editor">
      <YMonaco
        :ref="page.setMonacoRef"
        :model-value="''"
        :log-mode="true"
        :max-lines="10000"
        height="100%"
        :scroll-threshold="10"
        :auto-scroll="true"
        language="shell"
        :readonly="true"
        :options="{
          fontSize: 13,
          lineNumbers: 'on',
          scrollBeyondLastLine: false,
          minimap: { enabled: false },
          wordWrap: 'off',
        }"
        @line-exceed="page.handleLineExceed"
      />
    </div>
  </div>
</template>
