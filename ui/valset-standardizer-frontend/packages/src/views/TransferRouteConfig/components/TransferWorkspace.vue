<script setup lang="ts">
import { YButton, YCard, YTable } from "@yss-ui/components";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferRouteConfigColumns } from "../hooks/useTransferTableColumns";
import type { RouteConfigPage } from "../types";

const { page } = defineProps<{
  page: RouteConfigPage;
}>();

const columns = useTransferRouteConfigColumns();

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
          <h1>分拣路由映射配置</h1>
          <p>
            统一维护来源、规则与目标之间的路由映射，新增页使用普通 Ant Design
            Vue 表单完成配置。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">来源 → 路由映射 → 目标</span>
            <span class="workspace-pill">支持查询 / 新建 / 修改 / 删除</span>
            <span class="workspace-pill">详情抽屉展示流向与 JSON</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton theme="primary" @click="page.openCreateDialog">
              <template #icon><PlusOutlined /></template>
              新建路由
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
        :row-config="{ keyField: 'routeId' }"
        :pageable="true"
        :cell-config="{ height: 162 }"
        :header-height="40"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="路由映射列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看详情或维护配置。`"
            meta="固定使用 transfer_route 配置"
          >
            <div class="route-table-legend">
              <div
                v-for="item in page.flowTableLegend"
                :key="item.label"
                class="route-table-legend-item"
                :class="`route-table-legend-item--${item.tone}`"
              >
                <span>{{ item.label }}</span>
                <small>{{ item.hint }}</small>
              </div>
            </div>

            <a-form layout="inline" class="workspace-table-toolbar-form">
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
                  style="width: 180px"
                  placeholder="输入来源编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="规则主键">
                <a-input
                  v-model:value="page.query.ruleId"
                  style="width: 140px"
                  placeholder="输入规则ID"
                  allow-clear
                />
              </a-form-item>
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
                  style="width: 180px"
                  placeholder="输入目标编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item class="workspace-table-toolbar-actions">
                <YButton theme="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>

        <template #flowSummary="{ row }">
          <div
            class="route-summary-cell"
            :class="{
              'route-summary-cell--active':
                page.selectedRow?.routeId === row.routeId,
            }"
            role="button"
            tabindex="0"
            @click.stop="page.openDetailDrawer(row)"
            @keydown.enter.stop="page.openDetailDrawer(row)"
          >
            <div class="route-summary-segment route-summary-segment--source">
              <span>来源</span>
              <strong>{{ row.sourceCode || "-" }}</strong>
              <small>{{ row.sourceType || "-" }}</small>
              <a-button
                type="link"
                size="small"
                class="route-copy-button"
                @click.stop="page.copyRouteField(row.sourceCode, '来源编码')"
              >
                复制编码
              </a-button>
            </div>
            <div class="route-summary-segment route-summary-segment--route">
              <span>规则匹配</span>
              <strong>{{ row.ruleId ?? "-" }}</strong>
              <small>{{ row.renamePattern || "默认命名" }}</small>
              <a-button
                type="link"
                size="small"
                class="route-copy-button"
                @click.stop="page.copyRouteField(row.ruleId, '规则ID')"
              >
                复制规则
              </a-button>
            </div>
            <div class="route-summary-segment route-summary-segment--target">
              <span>目标</span>
              <strong>{{ row.targetCode || "-" }}</strong>
              <small>{{ row.targetPath || "-" }}</small>
              <a-button
                type="link"
                size="small"
                class="route-copy-button"
                @click.stop="page.copyRouteField(row.targetPath, '目标路径')"
              >
                复制路径
              </a-button>
            </div>
          </div>
        </template>

        <template #sourceCode="{ row }">
          <div
            class="route-cell route-cell--source"
            :class="{
              'route-cell--active': page.selectedRow?.routeId === row.routeId,
            }"
          >
            <strong>{{ row.sourceCode || "-" }}</strong>
            <span>{{ row.sourceType || "-" }}</span>
          </div>
        </template>

        <template #ruleId="{ row }">
          <div
            class="route-cell route-cell--route"
            :class="{
              'route-cell--active': page.selectedRow?.routeId === row.routeId,
            }"
          >
            <strong>{{ row.ruleId ?? "-" }}</strong>
            <span>{{ page.formatRouteMetaSummary(row) }}</span>
          </div>
        </template>

        <template #targetCode="{ row }">
          <div
            class="route-cell route-cell--target"
            :class="{
              'route-cell--active': page.selectedRow?.routeId === row.routeId,
            }"
          >
            <strong>{{ row.targetCode || "-" }}</strong>
            <span>{{ row.targetType || "-" }}</span>
          </div>
        </template>

        <template #targetPath="{ row }">
          <div
            class="route-path-cell"
            :class="{
              'route-path-cell--active':
                page.selectedRow?.routeId === row.routeId,
            }"
          >
            <strong>{{ row.targetPath || "-" }}</strong>
            <span>{{ row.renamePattern || "默认命名" }}</span>
          </div>
        </template>
      </YTable>
    </div>

    <a-modal
      class="source-modal"
      :open="page.formVisible"
      :title="page.formMode === 'create' ? '新建路由配置' : '编辑路由配置'"
      :width="1040"
      :confirm-loading="page.formSubmitting"
      destroy-on-close
      @ok="page.submitForm"
      @cancel="page.closeForm"
    >
      <div class="source-form-banner route-form-banner">
        <div>
          <div class="source-form-banner-label">路由配置</div>
          <div class="source-form-banner-value">
            {{ page.formMode === "create" ? "新建路由" : "编辑路由" }}
          </div>
        </div>
        <div class="source-form-banner-hint">
          选择来源、目标和分拣规则后，系统会自动回显编码、类型和路径，便于快速核对。
        </div>
      </div>

      <a-form
        :ref="page.setFormRef"
        :model="page.formState"
        layout="vertical"
        class="source-form route-config-form"
      >
        <div class="route-config-section">
          <div class="route-config-section-header">
            <h3>来源信息</h3>
            <p>选择来源规则后回显来源编码与来源类型。</p>
          </div>
          <div class="route-config-grid route-config-grid--source">
            <a-form-item
              label="来源规则"
              name="sourceCode"
              :rules="[
                {
                  required: true,
                  message: '请选择来源规则',
                  trigger: 'change',
                },
              ]"
            >
              <a-select
                v-model:value="page.formState.sourceCode"
                :options="page.sourceOptions"
                :loading="page.selectLoading"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="请选择来源规则"
                @change="page.handleSourceChange"
              />
            </a-form-item>
            <a-form-item label="来源编码">
              <a-input :value="page.formState.sourceCode" disabled />
            </a-form-item>
            <a-form-item label="来源类型">
              <a-input :value="page.formState.sourceType" disabled />
            </a-form-item>
          </div>
        </div>

        <div class="route-config-section">
          <div class="route-config-section-header">
            <h3>目标信息</h3>
            <p>选择目标规则后回显目标编码、类型和路径。</p>
          </div>
          <div class="route-config-grid route-config-grid--target">
            <a-form-item
              label="目标规则"
              name="targetCode"
              :rules="[
                {
                  required: true,
                  message: '请选择目标规则',
                  trigger: 'change',
                },
              ]"
            >
              <a-select
                v-model:value="page.formState.targetCode"
                :options="page.targetOptions"
                :loading="page.selectLoading"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="请选择目标规则"
                @change="page.handleTargetChange"
              />
            </a-form-item>
            <a-form-item label="目标编码">
              <a-input :value="page.formState.targetCode" disabled />
            </a-form-item>
            <a-form-item label="目标类型">
              <a-input :value="page.formState.targetType" disabled />
            </a-form-item>
            <a-form-item label="目标路径" class="route-config-path-item">
              <a-textarea
                :value="page.formState.targetPath"
                :rows="2"
                disabled
              />
            </a-form-item>
          </div>
        </div>

        <div class="route-config-section">
          <div class="route-config-section-header">
            <h3>分拣规则</h3>
            <p>选择分拣规则后回显规则 ID，并填写重命名模板。</p>
          </div>
          <div class="route-config-grid route-config-grid--rule">
            <a-form-item
              label="分拣规则"
              name="ruleId"
              :rules="[
                {
                  required: true,
                  message: '请选择分拣规则',
                  trigger: 'change',
                },
              ]"
            >
              <a-select
                v-model:value="page.formState.ruleId"
                :options="page.ruleOptions"
                :loading="page.selectLoading"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="请选择分拣规则"
                @change="page.handleRuleChange"
              />
            </a-form-item>
            <a-form-item label="规则ID">
              <a-input
                :value="
                  page.formState.ruleId ? String(page.formState.ruleId) : ''
                "
                disabled
              />
            </a-form-item>
            <a-form-item label="重命名模板">
              <a-input
                v-model:value="page.formState.renamePattern"
                placeholder="请输入重命名模板"
                allow-clear
              />
            </a-form-item>
          </div>
        </div>
      </a-form>
    </a-modal>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="路由映射详情"
      :width="'60vw'"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{
              page.selectedRow.sourceCode ||
              page.selectedRow.targetCode ||
              page.selectedRow.routeId ||
              "路由详情"
            }}
          </div>
          <div class="source-detail-banner-meta">
            配置详情
          </div>
        </div>

        <div class="route-detail-flow">
          <div class="route-detail-stage route-detail-stage--source">
            <span>来源</span>
            <strong>{{ page.flowPreview.sourceTitle }}</strong>
            <small>{{ page.flowPreview.sourceMeta }}</small>
          </div>
          <div class="route-detail-arrow" aria-hidden="true">→</div>
          <div class="route-detail-stage route-detail-stage--route">
            <span>规则匹配</span>
            <strong>{{ page.flowPreview.ruleTitle }}</strong>
            <small>{{ page.flowPreview.ruleMeta }}</small>
          </div>
          <div class="route-detail-arrow" aria-hidden="true">→</div>
          <div class="route-detail-stage route-detail-stage--target">
            <span>目标</span>
            <strong>{{ page.flowPreview.targetTitle }}</strong>
            <small>{{ page.flowPreview.targetMeta }}</small>
          </div>
        </div>

        <div class="route-detail-panels">
          <section class="route-detail-panel">
            <div class="route-detail-panel-header">
              <h4>来源信息</h4>
              <a-button
                type="link"
                size="small"
                class="route-copy-button"
                @click="
                  page.copyRouteField(page.selectedRow.sourceCode, '来源编码')
                "
              >
                复制来源编码
              </a-button>
            </div>
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="来源主键">
                {{ page.selectedRow.sourceId ?? "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="来源类型">
                {{ page.selectedRow.sourceType || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="来源编码">
                {{ page.selectedRow.sourceCode || "-" }}
              </a-descriptions-item>
            </a-descriptions>
          </section>

          <section class="route-detail-panel">
            <div class="route-detail-panel-header">
              <h4>路由匹配</h4>
              <a-button
                type="link"
                size="small"
                class="route-copy-button"
                @click="page.copyRouteField(page.selectedRow.ruleId, '规则ID')"
              >
                复制规则ID
              </a-button>
            </div>
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="路由主键">
                {{ page.selectedRow.routeId ?? "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="规则主键">
                {{ page.selectedRow.ruleId ?? "-" }}
              </a-descriptions-item>
            </a-descriptions>
          </section>

          <section class="route-detail-panel">
            <div class="route-detail-panel-header">
              <h4>目标信息</h4>
              <a-button
                type="link"
                size="small"
                class="route-copy-button"
                @click="
                  page.copyRouteField(page.selectedRow.targetPath, '目标路径')
                "
              >
                复制目标路径
              </a-button>
            </div>
            <a-descriptions bordered :column="1" size="small">
              <a-descriptions-item label="目标类型">
                {{ page.selectedRow.targetType || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="目标编码">
                {{ page.selectedRow.targetCode || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="目标路径">
                {{ page.selectedRow.targetPath || "-" }}
              </a-descriptions-item>
              <a-descriptions-item label="重命名模板">
                {{ page.selectedRow.renamePattern || "-" }}
              </a-descriptions-item>
            </a-descriptions>
          </section>
        </div>

        <div class="detail-json-block">
          <h4>映射扩展信息</h4>
          <pre>{{
            JSON.stringify(page.selectedRow.routeMeta || {}, null, 2)
          }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
