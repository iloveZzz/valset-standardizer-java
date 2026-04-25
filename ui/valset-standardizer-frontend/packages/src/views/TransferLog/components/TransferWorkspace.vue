<script setup lang="ts">
import {
  YButton,
  YCard,
  YTable,
  type YTablePagination,
} from "@yss-ui/components";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons-vue";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferLogColumns } from "../../TransferShared/hooks/useTransferTableColumns";

type LogPage = {
  loading: boolean;
  rows: any[];
  tableData: any[];
  total: number;
  pageSize: number;
  pagination: YTablePagination;
  query: {
    routeId: string;
    transferId: string;
    targetCode: string;
    executeStatus: string;
  };
  errorCount: number;
  routeCount: number;
  transferCount: number;
  statusCount: number;
  detailVisible: boolean;
  selectedRow: any | null;
  openDetailDrawer: (row: any) => void;
  runQuery: () => void;
  resetQuery: () => void;
  closeDetail: () => void;
  formatStatus: (value: string | undefined) => string;
  safeJson: (value: unknown) => string;
  handlePageChange: (params: { current: number; pageSize: number }) => void;
};

const { page } = defineProps<{
  page: LogPage;
}>();

const columns = useTransferLogColumns();

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
    <YCard class="workspace-header" :bordered="false" :padding="12">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>文件投递</h2>
          <p>
            查询文件投递记录、执行状态、路由关联和请求/响应摘要，便于排查投递链路问题。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持按路由、文件和状态过滤</span>
            <span class="workspace-pill">详情查看请求与响应摘要</span>
            <span class="workspace-pill">聚焦结果查询，不提供编辑</span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton type="primary" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询结果
            </YButton>
            <YButton @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置条件
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
        :row-config="{ keyField: 'deliveryId' }"
        :checkbox-config="{ highlight: true }"
        :pageable="true"
        v-model:pagination="page.pagination"
        :toolbar-config="{ custom: false }"
        @page-change="page.handlePageChange"
      >
        <template #toolbar-left>
          <WorkspaceTableToolbar
            title="文件投递列表"
            :description="`总数 ${page.total} 条，点击操作按钮查看请求与响应摘要。`"
            :meta="`可查看 ${page.total} 条结果，错误记录 ${page.errorCount} 条`"
          >
            <a-form layout="inline" class="workspace-table-toolbar-form">
              <a-form-item label="路由ID">
                <a-input
                  v-model:value="page.query.routeId"
                  style="width: 160px"
                  placeholder="输入路由ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="分拣ID">
                <a-input
                  v-model:value="page.query.transferId"
                  style="width: 160px"
                  placeholder="输入分拣ID"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="目标编码">
                <a-input
                  v-model:value="page.query.targetCode"
                  style="width: 180px"
                  placeholder="输入目标编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="执行状态">
                <a-input
                  v-model:value="page.query.executeStatus"
                  style="width: 160px"
                  placeholder="例如 SUCCESS"
                  allow-clear
                />
              </a-form-item>
              <a-form-item class="workspace-table-toolbar-actions">
                <YButton type="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
          </WorkspaceTableToolbar>
        </template>

        <template #executeStatus="{ row }">
          <a-tag :color="row.executeStatus ? 'blue' : 'default'">
            {{ page.formatStatus(row.executeStatusLabel || row.executeStatus) }}
          </a-tag>
        </template>
      </YTable>
    </div>

    <a-drawer
      class="source-detail-drawer"
      :open="page.detailVisible"
      title="文件投递详情"
      :width="720"
      @close="page.closeDetail"
    >
      <template v-if="page.selectedRow">
        <div class="source-detail-banner">
          <div class="source-detail-banner-title">
            {{
              page.selectedRow.targetCode ||
              page.selectedRow.deliveryId ||
              "文件投递详情"
            }}
          </div>
          <div class="source-detail-banner-meta">
            {{
              page.formatStatus(
                page.selectedRow.executeStatusLabel ||
                  page.selectedRow.executeStatus,
              )
            }}
            ·
            {{ page.selectedRow.deliveredAt || "-" }}
          </div>
        </div>
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="投递ID">
            {{ page.selectedRow.deliveryId ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="路由ID">
            {{ page.selectedRow.routeId ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="分拣ID">
            {{ page.selectedRow.transferId ?? "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="目标编码">
            {{ page.selectedRow.targetCode || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="目标类型">
            {{ page.selectedRow.targetType || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="执行状态">
            {{
              page.formatStatus(
                page.selectedRow.executeStatusLabel ||
                  page.selectedRow.executeStatus,
              )
            }}
          </a-descriptions-item>
          <a-descriptions-item label="投递时间">
            {{ page.selectedRow.deliveredAt || "-" }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-json-block">
          <h4>请求摘要</h4>
          <pre>{{ page.safeJson(page.selectedRow.requestSnapshotJson) }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>响应摘要</h4>
          <pre>{{ page.safeJson(page.selectedRow.responseSnapshotJson) }}</pre>
        </div>
        <div class="detail-json-block">
          <h4>错误信息</h4>
          <pre>{{ page.selectedRow.errorMessage || "-" }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
