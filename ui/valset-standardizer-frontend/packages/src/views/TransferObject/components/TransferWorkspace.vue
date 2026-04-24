<script setup lang="ts">
import { YButton, YCard, YTable } from "@yss-ui/components";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons-vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferObjectColumns } from "../../TransferShared/hooks/useTransferTableColumns";
import type { ObjectPage } from "../types";

const { page } = defineProps<{
  page: ObjectPage;
}>();

const columns = useTransferObjectColumns();

const actionConfig = useTableActionConfig({
  width: 120,
  displayLimit: 1,
  buttons: [
    {
      text: "详情",
      key: "detail",
      type: "link",
      clickFn: ({ row }: any) => page.openDetailDrawer(row),
    },
  ],
});
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="20">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h1>分拣对象查询</h1>
          <p>
            查询文件主对象清单、来源信息、文件状态和邮件元数据，便于定位文件从收取到入库的链路。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持按来源、状态、指纹和邮件筛选</span>
            <span class="workspace-pill">详情查看完整文件元数据</span>
            <span class="workspace-pill">只读查询，无编辑操作</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton theme="primary" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询对象
            </YButton>
            <YButton @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置条件
            </YButton>
          </div>
        </div>
      </div>
      <div class="workspace-summary">
        <div class="workspace-summary-title">
          <strong>来源类型统计</strong>
          <span>按来源类型展示文件状态个数，点击卡片或状态可直接筛选</span>
        </div>
        <a-spin :spinning="page.analysisLoading">
          <div class="workspace-analysis-grid">
            <div
              v-for="sourceItem in page.analysis.sourceAnalyses"
              :key="sourceItem.sourceType"
              class="analysis-card-shell"
              @click="page.applySourceFilter(sourceItem.sourceType)"
            >
              <YCard class="analysis-card" :bordered="false" :padding="18">
                <div class="analysis-card-header">
                  <div>
                    <div class="analysis-card-label">
                      {{ page.formatSourceTypeLabel(sourceItem.sourceType) }}
                    </div>
                    <div class="analysis-card-desc">来源类型文件状态统计</div>
                  </div>
                  <a-tag color="blue">{{ sourceItem.totalCount }} 条</a-tag>
                </div>
                <div class="analysis-card-status-list">
                  <button
                    v-for="statusItem in sourceItem.statusCounts"
                    :key="`${sourceItem.sourceType}-${statusItem.status}`"
                    type="button"
                    class="analysis-status-chip"
                    @click.stop="
                      page.applySourceStatusFilter(
                        sourceItem.sourceType,
                        statusItem.status,
                      )
                    "
                  >
                    <span class="analysis-status-chip-label">
                      {{ statusItem.statusLabel }}
                    </span>
                    <span class="analysis-status-chip-value">
                      {{ statusItem.count }}
                    </span>
                  </button>
                  <div
                    v-if="!sourceItem.statusCounts.length"
                    class="analysis-card-empty"
                  >
                    当前筛选下暂无对象
                  </div>
                </div>
              </YCard>
            </div>
          </div>
        </a-spin>
      </div>
    </YCard>

    <div class="workspace-body">
      <YTable
        :columns="columns"
        :action-config="actionConfig"
        :data="page.tableData"
        :loading="page.loading"
        :autoFlexColumn="true"
        :row-config="{ keyField: 'transferId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="主对象列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看完整详情。`"
            :meta="`当前页 ${page.tableData.length} 条，异常记录 ${page.errorCount} 条`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="来源ID">
                <a-input
                  v-model:value="page.query.sourceId"
                  style="width: 140px"
                  placeholder="来源ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="路由ID">
                <a-input
                  v-model:value="page.query.routeId"
                  style="width: 140px"
                  placeholder="路由ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="来源类型">
                <a-input
                  v-model:value="page.query.sourceType"
                  style="width: 160px"
                  placeholder="来源类型"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="来源编码">
                <a-input
                  v-model:value="page.query.sourceCode"
                  style="width: 180px"
                  placeholder="来源编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="文件状态">
                <a-input
                  v-model:value="page.query.status"
                  style="width: 160px"
                  placeholder="例如 SUCCESS"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="邮件ID">
                <a-input
                  v-model:value="page.query.mailId"
                  style="width: 180px"
                  placeholder="邮件唯一标识"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="文件指纹">
                <a-input
                  v-model:value="page.query.fingerprint"
                  style="width: 200px"
                  placeholder="文件指纹"
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

        <template #status="{ row }">
          <a-tag :color="row.status ? 'blue' : 'default'">
            {{ page.formatStatus(row.status) }}
          </a-tag>
        </template>
        <template #sizeBytes="{ row }">
          {{ row.sizeBytes ?? 0 }}
        </template>
      </YTable>
    </div>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="主对象详情"
      :width="760"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{
              page.selectedRow.originalName ||
              page.selectedRow.transferId ||
              "主对象详情"
            }}
          </div>
          <div class="source-detail-banner-meta">
            {{ page.formatStatus(page.selectedRow.status) }} ·
            {{ page.selectedRow.sourceCode || "-" }}
          </div>
        </div>
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="文件主键">
            {{ page.selectedRow.transferId ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="来源编码">
            {{ page.selectedRow.sourceCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="来源主键">
            {{ page.selectedRow.sourceId ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="路由主键">
            {{ page.selectedRow.routeId ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="来源类型">
            {{ page.selectedRow.sourceType || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="文件状态">
            {{ page.formatStatus(page.selectedRow.status) }}
          </a-descriptions-item>
          <a-descriptions-item label="附件名称">
            {{ page.selectedRow.originalName || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="文件大小">
            {{ page.selectedRow.sizeBytes ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="指纹">
            {{ page.selectedRow.fingerprint || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="收取时间">
            {{ page.selectedRow.receivedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="落库时间">
            {{ page.selectedRow.storedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="本地临时路径">
            {{ page.selectedRow.localTempPath || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="邮件ID">
            {{ page.selectedRow.mailId || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-json-block">
          <h4>文件元数据</h4>
          <pre>{{ page.safeJson(page.selectedRow.fileMetaJson) }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>邮件信息</h4>
          <pre>{{
            page.safeJson({
              mailFrom: page.selectedRow.mailFrom,
              mailTo: page.selectedRow.mailTo,
              mailCc: page.selectedRow.mailCc,
              mailBcc: page.selectedRow.mailBcc,
              mailSubject: page.selectedRow.mailSubject,
              mailBody: page.selectedRow.mailBody,
              mailFolder: page.selectedRow.mailFolder,
              mailProtocol: page.selectedRow.mailProtocol,
              mailId: page.selectedRow.mailId,
            })
          }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>错误信息</h4>
          <pre>{{ page.selectedRow.errorMessage || "-" }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
