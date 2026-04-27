<script setup lang="ts">
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import { YButton, YCard, YssFormily, YTable } from "@yss-ui/components";
import TransferTypeSelector from "../../TransferShared/components/TransferTypeSelector.vue";
import MailConditionBuilder from "../../TransferShared/components/MailConditionBuilder.vue";
import TransferTemplateDialog from "../../TransferShared/components/TransferTemplateDialog.vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferSourceColumns } from "../../TransferShared/hooks/useTransferTableColumns";
import { formatDateTime } from "@/utils/format";
import type { SourcePage } from "../types";

const { page } = defineProps<{
  page: SourcePage;
}>();

const columns = useTransferSourceColumns();
const checkpointColumns = [
  {
    title: "检查点键",
    dataIndex: "checkpointKey",
    key: "checkpointKey",
    width: 160,
  },
  {
    title: "检查点值",
    dataIndex: "checkpointValue",
    key: "checkpointValue",
    ellipsis: true,
  },
  {
    title: "创建时间",
    dataIndex: "createdAt",
    key: "createdAt",
    width: 170,
  },
  {
    title: "修改时间",
    dataIndex: "updatedAt",
    key: "updatedAt",
    width: 170,
  },
];
const checkpointItemColumns = [
  {
    title: "去重键",
    dataIndex: "itemKey",
    key: "itemKey",
    width: 220,
    ellipsis: true,
  },
  {
    title: "名称",
    dataIndex: "itemName",
    key: "itemName",
    width: 180,
    ellipsis: true,
  },
  {
    title: "引用",
    dataIndex: "itemRef",
    key: "itemRef",
    width: 220,
    ellipsis: true,
  },
  {
    title: "触发",
    dataIndex: "triggerType",
    key: "triggerType",
    width: 90,
  },
  {
    title: "处理时间",
    dataIndex: "processedAt",
    key: "processedAt",
    width: 170,
  },
];

const actionConfig = useTableActionConfig({
  width: 300,
  displayLimit: 4,
  buttons: [
    {
      text: "详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: any) => page.openDetailDrawer(row),
    },
    {
      text: "修改",
      key: "edit",
      type: "link",
      disabledFn: ({ row }: any) => Boolean(row?.enabled),
      clickFn: ({ row }: any) => page.openEditDialog(row),
    },
    {
      text: "删除",
      key: "delete",
      type: "link",
      disabledFn: ({ row }: any) => Boolean(row?.enabled),
      clickFn: ({ row }: any) => page.confirmDelete(row),
    },
  ],
});
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="12">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>文件来源接口配置</h2>
          <p>
            统一维护来源编码、名称、类型与扩展配置，适用于收发分拣的来源接入管理。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持查询 / 新建 / 修改 / 删除</span>
            <span class="workspace-pill">类型预览随表单同步刷新</span>
            <span class="workspace-pill">详情抽屉展示完整 JSON</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton type="primary" @click="page.openCreateDialog">
              <template #icon><PlusOutlined /></template>
              新建来源
            </YButton>
            <YButton @click="page.runQuery">
              <template #icon><ReloadOutlined /></template>
              刷新列表
            </YButton>
          </div>
        </div>
      </div>
    </YCard>

    <div class="workspace-body">
      <YTable
        :columns="columns"
        :action-config="actionConfig"
        :data="page.tableData"
        :loading="page.loading"
        :row-config="{ keyField: 'sourceId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="来源列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看详情或维护配置。`"
            :meta="`已启用 ${page.enabledCount} 条，覆盖 ${page.sourceTypeCount} 种类型`"
          >
            <a-form layout="inline">
              <a-form-item label="来源类型">
                <a-select
                  v-model:value="page.query.sourceType"
                  allow-clear
                  style="width: 168px"
                  placeholder="全部"
                >
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option
                    v-for="item in page.sourceTypeOptions"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="来源编码">
                <a-input
                  v-model:value="page.query.sourceCode"
                  style="width: 190px"
                  placeholder="输入来源编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="来源名称">
                <a-input
                  v-model:value="page.query.sourceName"
                  style="width: 190px"
                  placeholder="输入来源名称"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="启用状态">
                <a-select
                  v-model:value="page.query.enabled"
                  style="width: 132px"
                  placeholder="全部"
                >
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="true">启用</a-select-option>
                  <a-select-option value="false">停用</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item class="workspace-table-toolbar-actions">
                <YButton type="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>

        <template #enabled="{ row }">
          <a-switch
            :checked="Boolean(row.enabled)"
            :loading="page.isEnabledUpdating(row.sourceId)"
            :disabled="page.isEnabledUpdating(row.sourceId) || page.hasEnabledRoutes(row)"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => page.toggleEnabled(row, checked === true)"
          />
        </template>
      </YTable>
    </div>

    <TransferTemplateDialog
      :open="page.formVisible"
      :title="page.formMode === 'create' ? '新建来源' : '编辑来源'"
      hint="来源类型变更后会自动刷新模板名，便于提前核对后端约定。"
      panel-title="文件来源通道"
      :panel-subtitle="page.templateNamePreview || '未选择来源类型'"
      :loading="page.templateLoading"
      :has-schema="Boolean(page.templateSchema)"
      empty-description="选择来源类型后将自动加载对应模板表单"
      :confirm-loading="page.formSubmitting"
      @ok="page.submitForm"
      @cancel="page.closeForm"
    >
      <template v-if="page.formMode === 'edit' && page.editingRow?.enabledRouteCount > 0">
        <a-alert
          type="warning"
          show-icon
          style="margin-bottom: 16px"
          :message="`该来源已存在 ${page.editingRow.enabledRouteCount} 条启用中的路由配置，无法修改启用状态。若要切换来源启用状态，请先处理相关路由配置。`"
        />
      </template>
      <template #selector>
        <TransferTypeSelector
          label="来源类型"
          :options="page.sourceTypeOptions"
          v-model:modelValue="page.formState.sourceType"
          :disabled="page.formMode === 'edit'"
          placeholder="请选择来源类型"
        />
      </template>
      <template #meta>
        <span v-if="page.templateVersion">版本 {{ page.templateVersion }}</span>
        <span v-if="page.templateDescription">{{
          page.templateDescription
        }}</span>
      </template>
      <YssFormily
        :ref="page.setTemplateFormRef"
        :key="`${page.formMode}-${page.formState.sourceType}-${page.templateNamePreview || 'template'}`"
        v-model="page.templateValues"
        :schema="page.templateSchema"
        :initial-values="page.templateInitialValues"
        :scope="page.templateScope"
        :mode="page.templateMode"
        :read-pretty="page.templateReadPretty"
        :detail-options="page.templateDetailOptions"
        :grid-defaults="page.templateGridDefaults"
      >
        <template #mailCondition>
          <MailConditionBuilder v-model="page.templateValues.mailCondition" />
        </template>
      </YssFormily>
    </TransferTemplateDialog>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="来源详情"
      :width="980"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{
              page.selectedRow.sourceName ||
              page.selectedRow.sourceCode ||
              "来源详情"
            }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.selectedRow.sourceType || "-" }} ·
            {{ page.formatEnabled(page.selectedRow.enabled) }}
            · {{ page.formatIngestStatus(page.selectedRow.ingestStatus) }}
          </div>
        </div>
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="来源编码">
            {{ page.selectedRow.sourceCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="来源名称">
            {{ page.selectedRow.sourceName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="来源类型">
            {{ page.selectedRow.sourceType || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="模板名">
            {{ page.selectedRow.formTemplateName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="启用状态">
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </a-descriptions-item>
          <a-descriptions-item label="启用路由数">
            {{ page.selectedRow.enabledRouteCount ?? 0 }}
          </a-descriptions-item>
          <a-descriptions-item label="收取状态">
            {{ page.formatIngestStatus(page.selectedRow.ingestStatus) }}
          </a-descriptions-item>
          <a-descriptions-item label="收取开始时间">
            {{ formatDateTime(page.selectedRow.ingestStartedAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="收取结束时间">
            {{ formatDateTime(page.selectedRow.ingestFinishedAt) }}
          </a-descriptions-item>
        </a-descriptions>

        <a-spin :spinning="page.checkpointLoading">
          <div class="detail-json-block">
            <h4>当前检查点</h4>
            <a-table
              :columns="checkpointColumns"
              :data-source="page.checkpointRows"
              :pagination="false"
              size="small"
              row-key="checkpointId"
              bordered
            />
          </div>
          <div class="detail-json-block">
            <h4>最近处理记录</h4>
            <a-table
              :columns="checkpointItemColumns"
              :data-source="page.checkpointItemRows"
              :pagination="false"
              size="small"
              row-key="checkpointItemId"
              bordered
            />
          </div>
        </a-spin>

        <div class="detail-json-block">
          <h4>连接配置</h4>
          <pre>{{
            JSON.stringify(page.selectedRow.connectionConfig || {}, null, 2)
          }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>扩展信息</h4>
          <pre>{{
            JSON.stringify(page.selectedRow.sourceMeta || {}, null, 2)
          }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
