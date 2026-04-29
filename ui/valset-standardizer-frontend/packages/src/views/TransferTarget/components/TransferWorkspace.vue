<script setup lang="ts">
import { YButton, YCard, YTable, YssFormily } from "@yss-ui/components";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import TransferTypeSelector from "../../TransferShared/components/TransferTypeSelector.vue";
import TransferTemplateDialog from "../../TransferShared/components/TransferTemplateDialog.vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferTargetColumns } from "../../TransferShared/hooks/useTransferTableColumns";
import type { TargetPage } from "../types";

const { page } = defineProps<{
  page: TargetPage;
}>();

const columns = useTransferTargetColumns();

const actionConfig = useTableActionConfig({
  width: 240,
  displayLimit: 3,
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
          <h2>文件投递目标配置</h2>
          <p>
            统一维护投递目标编码、名称、类型、目录根路径、子路径模板与扩展配置，面向收发分拣的出站投递管理。
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
              新建目标
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
        :row-config="{ keyField: 'targetId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="目标列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看详情或维护配置。`"
            :meta="`已启用 ${page.enabledCount} 条，覆盖 ${page.targetTypeCount} 种类型`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="目标类型">
                <a-select
                  v-model:value="page.query.targetType"
                  allow-clear
                  style="width: 168px"
                  placeholder="全部"
                >
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option
                    v-for="item in page.targetTypeOptions"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="目标编码">
                <a-input
                  v-model:value="page.query.targetCode"
                  style="width: 190px"
                  placeholder="输入目标编码"
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
            :loading="page.isEnabledUpdating(row.targetId)"
            :disabled="
              page.isEnabledUpdating(row.targetId) ||
              page.hasReferencedRoutes(row)
            "
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => page.toggleEnabled(row, checked === true)"
          />
        </template>
      </YTable>
    </div>

    <TransferTemplateDialog
      :open="page.formVisible"
      :title="page.formMode === 'create' ? '新建目标' : '编辑目标'"
      hint="目标类型变更后会自动刷新模板名；本地目录目标请单独填写目录根路径，子路径模板仅用于追加相对路径。"
      panel-title="目标模板表单"
      :panel-subtitle="page.templateNamePreview || '未选择目标类型'"
      :loading="page.templateLoading"
      :has-schema="Boolean(page.templateSchema)"
      empty-description="选择目标类型后将自动加载对应模板表单"
      :confirm-loading="page.formSubmitting"
      @ok="page.submitForm"
      @cancel="page.closeForm"
    >
      <template #selector>
        <TransferTypeSelector
          label="目标类型"
          :options="page.targetTypeOptions"
          v-model:modelValue="page.formState.targetType"
          :disabled="page.formMode === 'edit'"
          placeholder="请选择目标类型"
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
        :key="`${page.formMode}-${page.formState.targetType}-${page.templateNamePreview || 'template'}`"
        v-model="page.templateValues"
        :schema="page.templateSchema"
        :initial-values="page.templateInitialValues"
        :scope="page.templateScope"
        :mode="page.templateMode"
        :read-pretty="page.templateReadPretty"
        :detail-options="page.templateDetailOptions"
        :grid-defaults="page.templateGridDefaults"
      />
    </TransferTemplateDialog>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="目标详情"
      :width="640"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{
              page.selectedRow.targetName ||
              page.selectedRow.targetCode ||
              "目标详情"
            }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.selectedRow.targetType || "-" }} ·
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </div>
        </div>
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="目标编码">
            {{ page.selectedRow.targetCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="目标名称">
            {{ page.selectedRow.targetName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="目标类型">
            {{ page.selectedRow.targetType || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="模板名">
            {{ page.selectedRow.formTemplateName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="启用状态">
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </a-descriptions-item>
          <a-descriptions-item label="目标子路径模板">
            {{ page.selectedRow.targetPathTemplate || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div
          v-if="
            page.selectedRow.targetType === 'LOCAL_DIR' &&
            page.getLocalTargetDirectory(page.selectedRow)
          "
          class="detail-json-block"
        >
          <h4>目标目录根路径</h4>
          <pre>{{ page.getLocalTargetDirectory(page.selectedRow) }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>连接配置</h4>
          <pre>{{
            JSON.stringify(page.selectedRow.connectionConfig || {}, null, 2)
          }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>扩展信息</h4>
          <pre>{{
            JSON.stringify(page.selectedRow.targetMeta || {}, null, 2)
          }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
