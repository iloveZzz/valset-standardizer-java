<script setup lang="ts">
import {
  YButton,
  YCard,
  YMonaco,
  YssFormily,
  YTable,
} from "@yss-ui/components";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import TransferTemplateDialog from "../../TransferShared/components/TransferTemplateDialog.vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferRuleColumns } from "../../TransferShared/hooks/useTransferTableColumns";
import type { RulePage } from "../types";

const { page } = defineProps<{
  page: RulePage;
}>();

const columns = useTransferRuleColumns();

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
      clickFn: ({ row }: any) => page.openEditDialog(row),
    },
    {
      text: "删除",
      key: "delete",
      type: "link",
      clickFn: ({ row }: any) => page.confirmDelete(row),
    },
  ],
});
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="20">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h1>文件分拣规则配置</h1>
          <p>
            统一维护分拣规则编码、名称、版本、脚本与路由扩展信息，模板固定加载
            transfer_rule。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持查询 / 新建 / 修改 / 删除</span>
            <span class="workspace-pill">模板名固定 transfer_rule</span>
            <span class="workspace-pill">详情抽屉展示脚本与 JSON</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton theme="primary" @click="page.openCreateDialog">
              <template #icon><PlusOutlined /></template>
              新建规则
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
        :row-config="{ keyField: 'ruleId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="规则列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看详情或维护配置。`"
            :meta="`已启用 ${page.enabledCount} 条，覆盖 ${page.strategyCount} 种策略`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="规则编码">
                <a-input
                  v-model:value="page.query.ruleCode"
                  style="width: 220px"
                  placeholder="输入规则编码"
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
                <YButton theme="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>

        <template #enabled="{ row }">
          <a-switch
            :checked="Boolean(row.enabled)"
            :loading="page.isEnabledUpdating(row.ruleId)"
            :disabled="page.isEnabledUpdating(row.ruleId)"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => page.toggleEnabled(row, checked === true)"
          />
        </template>
        <template #priority="{ row }">
          {{ row.priority ?? "-" }}
        </template>
      </YTable>
    </div>

    <TransferTemplateDialog
      :open="page.formVisible"
      :title="page.formMode === 'create' ? '新建规则' : '编辑规则'"
      hint="规则新增窗体固定加载 transfer_rule 模板，无需选择来源类型。"
      panel-title="文件分拣规则模板"
      :panel-subtitle="page.templateNamePreview || 'transfer_rule'"
      :loading="page.templateLoading"
      :has-schema="Boolean(page.templateSchema)"
      empty-description="规则模板尚未加载成功，请稍后重试"
      :confirm-loading="page.formSubmitting"
      @ok="page.submitForm"
      @cancel="page.closeForm"
    >
      <template #meta>
        <span v-if="page.templateVersion">版本 {{ page.templateVersion }}</span>
        <span v-if="page.templateDescription">{{
          page.templateDescription
        }}</span>
      </template>
      <YssFormily
        :ref="page.setTemplateFormRef"
        :key="`${page.formMode}-${page.templateNamePreview || 'transfer_rule'}`"
        v-model="page.templateValues"
        :schema="page.templateSchema"
        :initial-values="page.templateInitialValues"
        :scope="page.templateScope"
        :mode="page.templateMode"
        :read-pretty="page.templateReadPretty"
        :detail-options="page.templateDetailOptions"
        :grid-defaults="page.templateGridDefaults"
      >
        <template #scriptBody="{ value, onChange }">
          <YMonaco
            :model-value="value"
            :language="page.resolveScriptEditorLanguage(page.templateValues.scriptLanguage)"
            height="360px"
            @update:model-value="onChange"
          />
        </template>
      </YssFormily>
    </TransferTemplateDialog>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="规则详情"
      :width="700"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{
              page.selectedRow.ruleName ||
              page.selectedRow.ruleCode ||
              "规则详情"
            }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.selectedRow.ruleVersion || "-" }} ·
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </div>
        </div>
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="规则编码">
            {{ page.selectedRow.ruleCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="规则名称">
            {{ page.selectedRow.ruleName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="规则版本">
            {{ page.selectedRow.ruleVersion || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="模板名">
            {{ page.selectedRow.formTemplateName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="启用状态">
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </a-descriptions-item>
          <a-descriptions-item label="匹配策略">
            {{ page.selectedRow.matchStrategy || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="脚本语言">
            {{ page.selectedRow.scriptLanguage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="优先级">
            {{ page.selectedRow.priority ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="生效开始时间">
            {{ page.selectedRow.effectiveFrom || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="生效结束时间">
            {{ page.selectedRow.effectiveTo || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-json-block">
          <h4>规则脚本</h4>
          <pre>{{ page.selectedRow.scriptBody || "" }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>扩展信息</h4>
          <pre>{{
            JSON.stringify(page.selectedRow.ruleMeta || {}, null, 2)
          }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
