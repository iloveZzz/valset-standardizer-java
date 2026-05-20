<script setup lang="ts">
import "./index.less";
import "@vue-flow/core/dist/style.css";
import "@vue-flow/core/dist/theme-default.css";
import "@vue-flow/controls/dist/style.css";
import { computed, h, ref } from "vue";
import dayjs, { type Dayjs } from "dayjs";
import { Modal } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  ReloadOutlined,
} from "@ant-design/icons-vue";
import {
  YButton,
  YCard,
  YTable,
  type YTableColumn,
} from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
import WorkflowFlowCanvas from "./components/WorkflowFlowCanvas.vue";
import { useWorkflowMonitorPage } from "./hooks/useWorkflowMonitorPage";
import type { WorkflowMonitorInstanceRow } from "./types";

defineOptions({ name: "EtlWorkflowMonitorPage" });

const { page } = useWorkflowMonitorPage();

const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: false,
});

const TIME_RANGE_FORMAT = "YYYY-MM-DD HH:mm:ss";

const parseDateTime = (value?: string | null) => {
  const text = String(value ?? "").trim();
  if (!text) {
    return null;
  }
  const parsed = dayjs(text);
  return parsed.isValid() ? parsed : null;
};

const triggerTimeRange = computed<[Dayjs, Dayjs] | undefined>({
  get: () => {
    const startTime = parseDateTime(page.query.triggerTimeFrom);
    const endTime = parseDateTime(page.query.triggerTimeTo);
    if (!startTime || !endTime) {
      return undefined;
    }
    return [startTime, endTime] as [Dayjs, Dayjs];
  },
  set: (value: [Dayjs, Dayjs] | undefined) => {
    if (!value || value.length !== 2) {
      page.query.triggerTimeFrom = "";
      page.query.triggerTimeTo = "";
      return;
    }
    page.query.triggerTimeFrom = value[0].format(TIME_RANGE_FORMAT);
    page.query.triggerTimeTo = value[1].format(TIME_RANGE_FORMAT);
  },
});

const columns = computed<YTableColumn[]>(() => [
  { field: "workflowName", title: "工作流名称", minWidth: 190, ellipsis: true },
  { field: "instanceId", title: "实例 ID", minWidth: 180, ellipsis: true },
  { field: "status", title: "状态", width: 110 },
  { field: "startTime", title: "开始时间", width: 170 },
]);

const emptyText = computed(() =>
  page.selectedInstance
    ? "当前实例暂无可展示的链路数据"
    : "请选择左侧工作流实例查看全链路拓扑",
);

const formatStatusColor = (value?: string) => {
  const status = String(value ?? "").trim().toUpperCase();
  if (status === "SUCCEEDED" || status === "SUCCESS") return "green";
  if (["RUNNING", "SUBMITTED", "RETRYING"].includes(status)) return "processing";
  if (status === "FAILED" || status === "FAILURE") return "red";
  if (status === "STOPPED" || status === "PAUSE") return "orange";
  return "default";
};

const confirmAction = (
  title: string,
  content: string,
  onOk: () => Promise<void>,
  danger = false,
) => {
  Modal.confirm({
    title,
    content,
    icon: h(ExclamationCircleOutlined),
    okText: "确定",
    cancelText: "取消",
    okButtonProps: { danger, loading: page.actionLoading },
    onOk,
  });
};

const selectedInstanceName = computed(
  () => page.selectedInstance?.workflowLabel || "未选择实例",
);

const selectRow = (row: WorkflowMonitorInstanceRow) => {
  void page.selectInstance(row);
};
</script>

<template>
  <div class="workflow-monitor-page">
    <YCard class="workflow-monitor-header" :bordered="false" :padding="12">
      <div class="workflow-monitor-header__copy">
        <h2>工作流全链路监控</h2>
        <p>以实例为入口串联触发、阶段、任务与输出结果，运行中链路通过 Vue Flow 动画边展示数据流动。</p>
      </div>
      <div class="workflow-monitor-header__actions">
        <YButton
          size="small"
          :loading="page.listLoading || page.loading"
          @click="page.refreshList"
        >
          <template #icon><ReloadOutlined /></template>
          刷新实例
        </YButton>
      </div>
    </YCard>

    <div class="workflow-monitor-layout">
      <YCard class="workflow-monitor-sidebar" :bordered="false" :padding="12">
        <div class="workflow-monitor-query">
          <a-form layout="vertical">
            <a-form-item label="工作流名称">
              <a-input
                v-model:value="page.query.workflowName"
                size="small"
                allow-clear
                placeholder="输入工作流名称"
              />
            </a-form-item>
            <a-form-item label="状态">
              <a-select
                v-model:value="page.query.status"
                size="small"
                :options="page.statusOptions"
                allow-clear
                placeholder="选择状态"
              />
            </a-form-item>
            <a-form-item label="触发时间">
              <a-range-picker
                v-model:value="triggerTimeRange"
                size="small"
                allow-clear
                show-time
                format="YYYY-MM-DD HH:mm:ss"
                :placeholder="['开始时间', '结束时间']"
              />
            </a-form-item>
            <div class="workflow-monitor-query__actions">
              <YButton size="small" type="primary" @click="page.runQuery">
                查询
              </YButton>
              <YButton size="small" @click="page.resetQuery">重置</YButton>
            </div>
          </a-form>
        </div>

        <div ref="tableAreaRef" class="workflow-monitor-instance-table">
          <YTable
            :columns="columns"
            :data="page.tableData"
            :loading="page.listLoading || page.loading"
            :max-height="tableHeight"
            :border="false"
            :pageable="true"
            :autoFlexColumn="false"
            :row-config="{ keyField: 'instanceId', isCurrent: true }"
            v-model:pagination="page.pagination"
            @page-change="page.handlePageChange"
            @cell-click="({ row }) => selectRow(row)"
          >
            <template #status="{ row }">
              <a-tag :color="formatStatusColor(row.status)">
                {{ row.statusLabel }}
              </a-tag>
            </template>
          </YTable>
        </div>
      </YCard>

      <YCard class="workflow-monitor-main" :bordered="false" :padding="0">
        <div class="workflow-monitor-toolbar">
          <div class="workflow-monitor-toolbar__copy">
            <h3>{{ selectedInstanceName }}</h3>
            <p>{{ page.graphSummary }}</p>
          </div>
          <div class="workflow-monitor-toolbar__actions">
            <a-switch
              size="small"
              :checked="page.autoRefresh"
              :disabled="!page.canAutoRefresh"
              checked-children="自动"
              un-checked-children="手动"
              @change="(checked) => page.toggleAutoRefresh(Boolean(checked))"
            />
            <YButton
              size="small"
              :loading="page.graphLoading"
              @click="page.refreshGraph"
            >
              刷新链路
            </YButton>
            <YButton
              v-if="page.canRerunInstance()"
              size="small"
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '重跑工作流实例',
                  `将重跑 ${page.selectedInstanceId}，是否继续？`,
                  page.rerunInstance,
                )
              "
            >
              重跑
            </YButton>
            <YButton
              v-if="page.canRerunFailedTasks()"
              size="small"
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '重跑失败任务',
                  `将重跑 ${page.selectedInstanceId} 的失败任务，是否继续？`,
                  page.rerunFailedTasks,
                )
              "
            >
              重跑失败任务
            </YButton>
            <YButton
              v-if="page.canStopInstance()"
              size="small"
              danger
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '停止工作流实例',
                  `将停止 ${page.selectedInstanceId}，是否继续？`,
                  page.stopInstance,
                  true,
                )
              "
            >
              停止
            </YButton>
            <YButton
              v-if="page.canPauseInstance()"
              size="small"
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '暂停工作流实例',
                  `将暂停 ${page.selectedInstanceId}，是否继续？`,
                  page.pauseInstance,
                )
              "
            >
              暂停
            </YButton>
            <YButton
              v-if="page.canResumeInstance()"
              size="small"
              :loading="page.actionLoading"
              @click="
                confirmAction(
                  '恢复运行工作流实例',
                  `将恢复 ${page.selectedInstanceId}，是否继续？`,
                  page.resumeInstance,
                )
              "
            >
              恢复运行
            </YButton>
          </div>
        </div>

        <WorkflowFlowCanvas
          :nodes="page.nodes"
          :edges="page.edges"
          :loading="page.graphLoading"
          :empty-text="emptyText"
          @node-click="page.openNodeDetail"
        />
      </YCard>
    </div>

    <a-drawer
      class="workflow-monitor-detail-drawer"
      :open="page.detailVisible"
      title="链路节点详情"
      :width="780"
      :destroyOnClose="true"
      :bodyStyle="{ padding: '14px' }"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedDetail">
        <div class="workflow-monitor-detail">
          <div class="workflow-monitor-detail__banner">
            <div>
              <h3>{{ page.selectedDetail.node.title }}</h3>
              <p>{{ page.selectedDetail.node.subtitle || "-" }}</p>
            </div>
            <a-tag :color="formatStatusColor(page.selectedDetail.node.status)">
              {{ page.selectedDetail.node.statusLabel }}
            </a-tag>
          </div>

          <a-descriptions bordered size="small" :column="2">
            <a-descriptions-item label="节点类型">
              {{ page.selectedDetail.node.kind }}
            </a-descriptions-item>
            <a-descriptions-item label="时间">
              {{ page.selectedDetail.node.timeLabel || "-" }}
            </a-descriptions-item>
            <a-descriptions-item label="摘要">
              {{ page.selectedDetail.node.meta || "-" }}
            </a-descriptions-item>
            <a-descriptions-item label="消息">
              {{ page.selectedDetail.node.message || "-" }}
            </a-descriptions-item>
          </a-descriptions>

          <div v-if="page.selectedDetail.node.stageLog" class="workflow-monitor-detail__block">
            <h4>阶段日志 Payload</h4>
            <pre>{{ page.formatJson(page.selectedDetail.node.stageLog.payload) }}</pre>
          </div>

          <div v-if="page.selectedDetail.node.taskRow" class="workflow-monitor-detail__block">
            <h4>任务参数</h4>
            <pre>{{ page.selectedDetail.node.taskRow.taskParams || "{}" }}</pre>
          </div>

          <div v-if="page.selectedDetail.node.kind === 'task'" class="workflow-monitor-detail__block">
            <h4>任务日志</h4>
            <a-spin :spinning="page.taskLogLoading">
              <pre>{{ page.selectedDetail.taskLog || "暂无日志" }}</pre>
            </a-spin>
          </div>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
