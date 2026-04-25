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
import type { YTableColumn } from "@yss-ui/components";
import type { TagPage } from "../types";

const { page } = defineProps<{
  page: TagPage;
}>();

const actionConfig = useTableActionConfig({
  width: 280,
  displayLimit: 4,
  buttons: [
    {
      text: "详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: any) => page.openDetailDrawer(row),
    },
    {
      text: "试跑",
      key: "test",
      type: "link",
      clickFn: ({ row }: any) => page.openTestDrawer(row),
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

const columns: YTableColumn[] = [
  {
    type: "checkbox",
    width: 50,
    align: "center" as const,
    fixed: "left" as const,
  },
  { field: "tagCode", title: "标签编码", width: 180 },
  { field: "tagName", title: "标签名称", width: 220 },
  { field: "tagValue", title: "标签值", width: 160 },
  { field: "matchStrategy", title: "匹配策略", width: 160 },
  { field: "priority", title: "优先级", width: 100 },
  { field: "enabled", title: "启用", width: 100 },
  { field: "updatedAt", title: "更新时间", width: 180 },
];
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="20">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h1>标签管理</h1>
          <p>
            配置标签名称、标签值、QLExpress4
            规则与正则表达式，并在对象收集完成后自动打标。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持查询 / 新建 / 修改 / 删除</span>
            <span class="workspace-pill">模板名固定 transfer_tag</span>
            <span class="workspace-pill">支持试跑验证</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton theme="primary" @click="page.openCreateDialog">
              <template #icon><PlusOutlined /></template>
              新建标签
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
        :row-config="{ keyField: 'tagId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #enabled="{ row }">
          <a-switch
            :checked="Boolean(row.enabled)"
            :loading="page.isEnabledUpdating(row.tagId)"
            :disabled="page.isEnabledUpdating(row.tagId)"
            checked-children="启用"
            un-checked-children="停用"
            @change="(checked) => page.toggleEnabled(row, checked === true)"
          />
        </template>
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="标签列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看详情或执行试跑。`"
            :meta="`当前模板 ${page.templateNamePreview || 'transfer_tag'}`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="标签编码">
                <a-input
                  v-model:value="page.query.tagCode"
                  style="width: 220px"
                  placeholder="输入标签编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="匹配策略">
                <a-select
                  v-model:value="page.query.matchStrategy"
                  style="width: 160px"
                  placeholder="全部"
                >
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="SCRIPT_RULE"
                    >脚本规则</a-select-option
                  >
                  <a-select-option value="REGEX_RULE">正则规则</a-select-option>
                  <a-select-option value="SCRIPT_AND_REGEX"
                    >脚本且正则</a-select-option
                  >
                  <a-select-option value="SCRIPT_OR_REGEX"
                    >脚本或正则</a-select-option
                  >
                </a-select>
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
      </YTable>
    </div>

    <TransferTemplateDialog
      :open="page.formVisible"
      :title="page.formMode === 'create' ? '新建标签' : '编辑标签'"
      hint="标签新增窗体固定加载 transfer_tag 模板，无需选择来源类型。"
      panel-title="标签配置"
      panel-subtitle="维护标签编码、名称、值和命中规则"
      :loading="page.templateLoading"
      :has-schema="Boolean(page.templateSchema)"
      empty-description="标签模板尚未加载成功，请稍后重试"
      :confirm-loading="page.formSubmitting"
      @ok="page.submitForm"
      @cancel="page.closeForm"
    >
      <YssFormily
        :ref="page.setTemplateFormRef"
        :key="`${page.formMode}-${page.templateNamePreview || 'transfer_tag'}`"
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
          <div class="workspace-script-helper">
            <div class="workspace-script-helper-title">推荐脚本</div>
            <pre>{{ page.templateInitialValues.scriptBody || "" }}</pre>
            <div class="workspace-script-helper-actions">
              <YButton size="small" @click="page.resetScriptBody">
                恢复默认脚本
              </YButton>
            </div>
          </div>
          <YMonaco
            v-model:model-value="page.templateValues.scriptBody"
            :language="
              page.resolveScriptEditorLanguage(
                page.templateValues.scriptLanguage,
              )
            "
            height="320px"
          />
        </template>
        <template #regexPattern>
          <a-textarea
            v-model:value="page.templateValues.regexPattern"
            :rows="4"
            placeholder="例如：^.*\\.(xlsx|xls)$"
          />
        </template>
        <template #tagMeta>
          <a-textarea
            v-model:value="page.templateValues.tagMeta"
            :rows="4"
            placeholder="标签扩展 JSON"
          />
        </template>
      </YssFormily>
    </TransferTemplateDialog>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="标签详情"
      :width="720"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="标签编码">
            {{ page.selectedRow.tagCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="标签名称">
            {{ page.selectedRow.tagName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="标签值">
            {{ page.selectedRow.tagValue || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="启用状态">
            {{ page.formatEnabled(page.selectedRow.enabled) }}
          </a-descriptions-item>
          <a-descriptions-item label="匹配策略">
            {{ page.formatMatchStrategy(page.selectedRow.matchStrategy) }}
          </a-descriptions-item>
          <a-descriptions-item label="脚本语言">
            {{ page.selectedRow.scriptLanguage || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ page.selectedRow.createdAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ page.selectedRow.updatedAt || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-json-block">
          <h4>脚本内容</h4>
          <pre>{{ page.selectedRow.scriptBody || "" }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>正则表达式</h4>
          <pre>{{ page.selectedRow.regexPattern || "" }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>扩展信息</h4>
          <pre>{{
            typeof page.selectedRow.tagMeta === "string"
              ? page.selectedRow.tagMeta
              : JSON.stringify(page.selectedRow.tagMeta || {}, null, 2)
          }}</pre>
        </div>
      </template>
    </a-drawer>

    <a-drawer
      class="source-detail-drawer"
      :open="page.testVisible"
      title="标签试跑"
      :width="720"
      @close="page.closeTest"
    >
      <template v-if="page.testTag">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{ page.testTag.tagName || page.testTag.tagCode || "标签试跑" }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.formatMatchStrategy(page.testTag.matchStrategy) }} ·
            {{ page.formatEnabled(page.testTag.enabled) }}
          </div>
        </div>
        <a-form layout="vertical">
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="来源类型">
                <a-input
                  v-model:value="page.testState.sourceType"
                  placeholder="例如 EMAIL"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="来源编码">
                <a-input
                  v-model:value="page.testState.sourceCode"
                  placeholder="例如 mail-001"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="文件名">
                <a-input
                  v-model:value="page.testState.fileName"
                  placeholder="例如 report.xlsx"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="MIME 类型">
                <a-input
                  v-model:value="page.testState.mimeType"
                  placeholder="例如 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="发件人">
                <a-input
                  v-model:value="page.testState.sender"
                  placeholder="例如 finance@example.com"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="主题">
                <a-input
                  v-model:value="page.testState.subject"
                  placeholder="例如 月报附件"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="路径">
            <a-input
              v-model:value="page.testState.path"
              placeholder="例如 /transfer/inbox/report.xlsx"
            />
          </a-form-item>
          <a-form-item label="邮箱文件夹">
            <a-input
              v-model:value="page.testState.mailFolder"
              placeholder="例如 INBOX"
            />
          </a-form-item>
          <a-form-item label="正文">
            <a-textarea
              v-model:value="page.testState.body"
              :rows="4"
              placeholder="可输入邮件正文或说明文本"
            />
          </a-form-item>
          <a-form-item label="扩展属性 JSON">
            <a-textarea
              v-model:value="page.testState.attributesText"
              :rows="5"
              placeholder='例如：{"attachmentIndex":1,"fileType":"xlsx"}'
            />
          </a-form-item>
          <a-space>
            <YButton
              theme="primary"
              :loading="page.testSubmitting"
              @click="page.submitTest"
              >执行试跑</YButton
            >
            <YButton @click="page.closeTest">关闭</YButton>
          </a-space>
        </a-form>

        <div v-if="page.testResult" class="detail-json-block">
          <h4>试跑结果</h4>
          <pre>{{ JSON.stringify(page.testResult, null, 2) }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
