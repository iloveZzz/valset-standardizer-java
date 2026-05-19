<script setup lang="ts">
import {
  YButton,
  YCard,
  YMonaco,
  YssFormily,
  YTable,
  type YTableColumn,
} from "@yss-ui/components";
import {
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import TransferTemplateDialog from "../../TransferShared/components/TransferTemplateDialog.vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import type { QlexpressFunctionPageState } from "../types";

const { page } = defineProps<{
  page: QlexpressFunctionPageState;
}>();

const actionConfig = useTableActionConfig({
  width: 300,
  displayLimit: 5,
  buttons: [
    {
      text: "详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: any) => page.openDetailDrawer(row),
    },
    {
      text: "调试",
      key: "debug",
      type: "link",
      clickFn: ({ row }: any) => page.openDebugDrawer(row),
    },
    {
      text: "修改",
      key: "edit",
      type: "link",
      disabledFn: ({ row }: any) => page.isActive(row),
      clickFn: ({ row }: any) => page.openEditDialog(row),
    },
    {
      text: "删除",
      key: "delete",
      type: "link",
      disabledFn: ({ row }: any) => page.isActive(row),
      clickFn: ({ row }: any) => page.confirmDelete(row),
    },
  ],
});

const columns: YTableColumn[] = [
  { field: "functionCnName", title: "函数中文名称", width: 180 },
  { field: "functionName", title: "函数名称", width: 180 },
  { field: "enabled", title: "启用", width: 110 },
  { field: "remark", title: "备注", minWidth: 220 },
  { field: "updatedAt", title: "更新时间", width: 180 },
];
</script>

<template>
  <div class="qlexpress-function-page">
    <YCard class="workspace-query-card" :bordered="false" :padding="12">
      <div class="workspace-query-bar">
        <a-form layout="inline">
          <a-form-item label="函数中文名称">
            <a-input
              v-model:value="page.query.functionCnName"
              allow-clear
              placeholder="请输入函数中文名称"
            />
          </a-form-item>
          <a-form-item label="函数名称">
            <a-input
              v-model:value="page.query.functionName"
              allow-clear
              placeholder="请输入函数名称"
            />
          </a-form-item>
          <a-form-item label="启用状态">
            <a-select
              v-model:value="page.query.enabled"
              allow-clear
              style="width: 140px"
              placeholder="全部"
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="true">启用</a-select-option>
              <a-select-option value="false">停用</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <div class="workspace-query-actions">
          <a-space>
            <YButton size="small" type="primary" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询
            </YButton>
            <YButton size="small" @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置
            </YButton>
            <YButton type="primary" size="small" @click="page.openCreateDialog">
              <template #icon><PlusOutlined /></template>
              新增函数
            </YButton>
          </a-space>
        </div>
      </div>
    </YCard>

    <div class="workspace-body">
      <YTable
        :columns="columns"
        :action-config="actionConfig"
        :data="page.tableData"
        :loading="page.loading"
        :row-config="{ keyField: 'functionId' }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #enabled="{ row }">
          <a-switch
            :checked="Boolean(row.enabled)"
            :loading="page.isOperating(row.functionId)"
            :disabled="page.isOperating(row.functionId)"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => page.toggleEnabled(row, checked === true)"
          />
        </template>
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="QLExpress 函数列表"
            :description="`总数 ${page.total} 条，已启用函数不可修改或删除。`"
            meta="动态函数会同步注册到 QLExpress Runner"
          />
        </template>
        <template #toolbar-right>
          <a-space>
            <YButton size="small" @click="page.openDebugDrawer(null)">
              调试临时脚本
            </YButton>
          </a-space>
        </template>
      </YTable>
    </div>

    <TransferTemplateDialog
      :open="page.formVisible"
      :title="page.formMode === 'create' ? '新建函数' : '编辑函数'"
      hint="函数脚本使用完整 QLExpress 函数定义，启用后会注册到 Express4Runner。"
      panel-title="函数配置"
      panel-subtitle="维护函数名称、脚本和扩展信息"
      :loading="false"
      :has-schema="Boolean(page.templateSchema)"
      empty-description="函数表单尚未加载成功，请稍后重试"
      :confirm-loading="page.formSubmitting"
      @ok="page.submitForm"
      @cancel="page.closeForm"
    >
      <YssFormily
        :ref="page.setTemplateFormRef"
        :key="`${page.formMode}-${page.templateValues.functionId || 'new'}`"
        v-model="page.templateValues"
        :schema="page.templateSchema"
        :initial-values="page.templateInitialValues"
        :scope="page.templateScope"
        :mode="page.templateMode"
        :read-pretty="page.templateReadPretty"
        :detail-options="page.templateDetailOptions"
        :grid-defaults="page.templateGridDefaults"
      >
        <template #scriptBody>
          <YMonaco
            v-model:model-value="page.templateValues.scriptBody"
            :language="page.resolveScriptEditorLanguage('qlexpress4')"
            height="360px"
          />
        </template>
        <template #extInfo>
          <a-textarea
            v-model:value="page.templateValues.extInfo"
            :rows="4"
            placeholder="函数扩展信息 JSON"
          />
        </template>
      </YssFormily>
    </TransferTemplateDialog>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="函数详情"
      :width="760"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="函数中文名称">
            {{ page.selectedRow.functionCnName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="函数名称">
            {{ page.selectedRow.functionName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="启用状态">
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </a-descriptions-item>
          <a-descriptions-item label="备注">
            {{ page.selectedRow.remark || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ page.selectedRow.createdAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ page.selectedRow.updatedAt || "-" }}
          </a-descriptions-item>
        </a-descriptions>
        <div class="detail-json-block">
          <h4>函数脚本</h4>
          <pre>{{ page.selectedRow.scriptBody || "" }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>函数扩展信息</h4>
          <pre>{{
            typeof page.selectedRow.extInfo === "string"
              ? page.selectedRow.extInfo
              : JSON.stringify(page.selectedRow.extInfo || {}, null, 2)
          }}</pre>
        </div>
      </template>
    </a-drawer>

    <a-drawer
      class="source-detail-drawer"
      :open="page.debugVisible"
      :title="page.debugTarget ? '函数在线调试' : '临时脚本调试'"
      :width="780"
      @close="page.closeDebug"
    >
      <a-form layout="vertical">
        <a-alert
          v-if="page.debugTarget"
          type="info"
          show-icon
          :message="`当前调试函数：${page.debugTarget.functionName || '-'}`"
          style="margin-bottom: 12px"
        />
        <a-form-item label="调试表达式">
          <a-input
            v-model:value="page.debugState.debugExpression"
            placeholder="例如：customContains(fileName, '估值')"
          />
        </a-form-item>
        <a-form-item label="上下文 JSON">
          <a-textarea
            v-model:value="page.debugState.contextText"
            :rows="8"
            placeholder='例如：{"fileName":"估值表.xlsx"}'
          />
        </a-form-item>
        <a-space>
          <YButton
            type="primary"
            :loading="page.debugSubmitting"
            @click="page.submitDebug"
          >
            执行调试
          </YButton>
          <YButton @click="page.closeDebug">关闭</YButton>
        </a-space>
      </a-form>

      <div v-if="page.debugResult" class="detail-json-block">
        <h4>调试结果</h4>
        <pre>{{ JSON.stringify(page.debugResult, null, 2) }}</pre>
      </div>
    </a-drawer>
  </div>
</template>
