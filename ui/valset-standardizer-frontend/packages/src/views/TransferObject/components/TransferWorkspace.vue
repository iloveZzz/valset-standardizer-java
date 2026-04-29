<script setup lang="ts">
import { h } from "vue";
import { Modal } from "ant-design-vue";
import { YButton, YCard, YTable } from "@yss-ui/components";
import {
  ExclamationCircleOutlined,
  DownloadOutlined,
  TagOutlined,
  ReloadOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import { formatDate, formatDateTime } from "@/utils/format";
import WorkspaceTableToolbar from "../../TransferShared/components/WorkspaceTableToolbar.vue";
import { useTableActionConfig } from "../../TransferShared/hooks/useTableActionConfig";
import { useTransferObjectColumns } from "../../TransferShared/hooks/useTransferTableColumns";
import type { ObjectPage } from "../types";

const { page } = defineProps<{
  page: ObjectPage;
}>();

const columns = useTransferObjectColumns();

const confirmRedeliver = (row: any) => {
  Modal.confirm({
    title: "重新投递",
    content: "将对该分拣对象执行重新投递，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: "确定重投",
    cancelText: "取消",
    okButtonProps: {
      danger: true,
      loading: page.redeliverLoading,
    },
    onOk: () => page.redeliverObject(row),
  });
};

const confirmRetag = () => {
  Modal.confirm({
    title: "重新打标",
    content: "将按当前筛选条件对全部分拣对象重新识别并覆盖已有标签，是否继续？",
    icon: h(ExclamationCircleOutlined),
    okText: "确定打标",
    cancelText: "取消",
    okButtonProps: {
      danger: true,
    },
    onOk: () => page.retagObjects(),
  });
};

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
      text: "下载",
      key: "download",
      type: "link",
      disabledFn: ({ row }: any) => !String(row?.localTempPath ?? "").trim(),
      clickFn: ({ row }: any) => page.downloadObject(row),
    },
    {
      text: "重新投递",
      key: "redeliver",
      type: "link",
      disabledFn: ({ row }: any) =>
        page.formatDeliveryStatus(row.deliveryStatus) === "已投递" ||
        !String(row?.routeId ?? "").trim(),
      clickFn: ({ row }: any) => confirmRedeliver(row),
    },
  ],
});
</script>

<template>
  <div class="transfer-workspace">
    <YCard class="workspace-header" :bordered="false" :padding="12">
      <div class="workspace-header-inner">
        <div class="workspace-header-copy">
          <h2>分拣对象</h2>
          <p>
            查询文件主对象清单、来源信息、文件状态和邮件元数据，便于定位文件从收取到入库的链路。
          </p>
          <div class="workspace-header-pills">
            <span class="workspace-pill">支持按来源、状态、指纹和邮件筛选</span>
            <span class="workspace-pill">详情查看完整文件元数据</span>
            <span class="workspace-pill">只读查询，无编辑操作</span>
          </div>
          <div class="workspace-tag-filters">
            <span class="workspace-tag-filters-label">快捷标签筛选</span>
            <a-tag
              v-for="filter in page.tagFilters"
              :key="filter.tagId || `${filter.tagCode}-${filter.tagValue}`"
              class="workspace-tag-filter-chip"
              color="cyan"
              @click="page.applyTagFilter(filter)"
            >
              {{ filter.tagName || filter.tagCode || filter.tagValue || "-" }}
              <span v-if="filter.tagValue">：{{ filter.tagValue }}</span>
              <span class="workspace-tag-filter-count"
                >({{ filter.count }})</span
              >
            </a-tag>
            <a-button
              v-if="
                page.query.tagCode || page.query.tagValue || page.query.tagId
              "
              size="small"
              type="link"
              class="workspace-tag-clear"
              @click="page.clearTagFilter"
            >
              清除标签
            </a-button>
            <span v-if="!page.tagFilters.length" class="workspace-tag-empty">
              当前页暂无标签可筛选
            </span>
          </div>
        </div>
        <div class="workspace-header-actions">
          <div class="workspace-header-buttons">
            <YButton type="primary" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询对象
            </YButton>
            <YButton @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置条件
            </YButton>
            <YButton :loading="page.retagLoading" @click="confirmRetag">
              <template #icon><TagOutlined /></template>
              重新打标
            </YButton>
          </div>
        </div>
      </div>
      <div class="workspace-summary">
        <div class="workspace-summary-title"></div>
        <a-spin :spinning="page.analysisLoading">
          <div class="workspace-analysis-layout">
            <div class="workspace-analysis-grid workspace-analysis-grid--fill">
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
                  <div
                    class="analysis-card-status-list analysis-card-status-list--nowrap"
                  >
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
                    <button
                      v-if="
                        page.formatSourceTypeLabel(sourceItem.sourceType) ===
                        'EMAIL'
                      "
                      type="button"
                      class="analysis-status-chip analysis-status-chip--default"
                      @click.stop="
                        page.applyDeliveryStatusFilter(
                          sourceItem.sourceType,
                          '未投递',
                        )
                      "
                    >
                      <span class="analysis-status-chip-label">未投递</span>
                      <span class="analysis-status-chip-value">
                        {{ sourceItem.undeliveredCount }}
                      </span>
                    </button>
                    <span
                      v-for="folderItem in sourceItem.mailFolderCounts"
                      :key="`${sourceItem.sourceType}-${folderItem.mailFolder}`"
                      class="analysis-status-chip analysis-status-chip--default"
                    >
                      <span class="analysis-status-chip-label">
                        {{ folderItem.mailFolderLabel }}
                      </span>
                      <span class="analysis-status-chip-value">
                        {{ folderItem.count }}
                      </span>
                    </span>
                    <div
                      v-if="!sourceItem.statusCounts.length"
                      class="analysis-card-empty"
                    >
                      当前筛选下暂无对象
                    </div>
                  </div>
                </YCard>
              </div>
              <div class="analysis-card-shell analysis-card-shell--static">
                <YCard
                  class="analysis-card workspace-size-card"
                  :bordered="false"
                  :padding="18"
                >
                  <div class="analysis-card-header">
                    <div>
                      <div class="analysis-card-label">
                        收取文件大小统计：{{
                          page.formatBytes(
                            page.analysis.sizeAnalysis.totalSizeBytes,
                          )
                        }}
                      </div>
                      <div class="analysis-card-desc">
                        共
                        {{ page.analysis.sizeAnalysis.totalCount }}
                        个文件，按后缀统计展示
                      </div>
                    </div>
                    <a-tag color="blue"
                      >{{ page.analysis.sizeAnalysis.totalCount }} 条</a-tag
                    >
                  </div>
                  <div
                    class="analysis-card-status-list analysis-card-status-list--nowrap"
                  >
                    <span
                      v-for="extensionItem in page.analysis.sizeAnalysis
                        .extensionCounts"
                      :key="
                        extensionItem.extension || extensionItem.extensionLabel
                      "
                      class="analysis-status-chip analysis-status-chip--default"
                    >
                      <span class="analysis-status-chip-label">
                        {{ extensionItem.extensionLabel }}
                      </span>
                      <span class="analysis-status-chip-value">
                        {{ extensionItem.count }}
                      </span>
                    </span>
                    <div
                      v-if="!page.analysis.sizeAnalysis.extensionCounts.length"
                      class="analysis-card-empty"
                    >
                      当前筛选下暂无后缀统计
                    </div>
                  </div>
                </YCard>
              </div>
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
        :loading="page.loading || page.redeliverLoading || page.retagLoading"
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
              <a-form-item label="标签编码">
                <a-input
                  v-model:value="page.query.tagCode"
                  style="width: 180px"
                  placeholder="标签编码"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="标签值">
                <a-input
                  v-model:value="page.query.tagValue"
                  style="width: 160px"
                  placeholder="标签值"
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
                <YButton type="primary" @click="page.runQuery">查询</YButton>
                <YButton @click="page.resetQuery">重置</YButton>
              </a-form-item>
            </a-form>
            <div
              v-if="
                page.query.tagCode || page.query.tagValue || page.query.tagId
              "
              class="workspace-active-tag-filter"
            >
              当前标签筛选：
              <a-tag color="cyan">
                {{ page.query.tagCode || page.query.tagId || "标签" }}
                <span v-if="page.query.tagValue"
                  >：{{ page.query.tagValue }}</span
                >
              </a-tag>
              <a-button type="link" @click="page.clearTagFilter">清除</a-button>
            </div>
          </WorkspaceTableToolbar>
        </template>

        <template #status="{ row }">
          <a-tag :color="row.status ? 'blue' : 'default'">
            {{ page.formatStatus(row.status) }}
          </a-tag>
        </template>
        <template #deliveryStatus="{ row }">
          <a-tag
            :color="
              page.formatDeliveryStatus(row.deliveryStatus) === '已投递'
                ? 'green'
                : 'default'
            "
          >
            {{ page.formatDeliveryStatus(row.deliveryStatus) }}
          </a-tag>
        </template>
        <template #errorMessage="{ row }">
          <a-popover
            v-if="String(row.errorMessage ?? '').trim()"
            trigger="click"
            placement="topLeft"
          >
            <template #content>
              <pre class="error-message-popover-pre">{{
                String(row.errorMessage ?? "").trim()
              }}</pre>
            </template>
            <ExclamationCircleOutlined class="error-message-icon" />
          </a-popover>
        </template>
        <template #sizeBytes="{ row }">
          {{ row.sizeBytes ?? 0 }}
        </template>
        <template #tags="{ row }">
          <div class="object-tag-list">
            <template v-if="row.tags?.length">
              <a-tag
                v-for="tag in row.tags.slice(0, 3)"
                :key="tag.id || `${tag.tagCode}-${tag.tagValue}`"
                color="blue"
              >
                {{ tag.tagName || tag.tagCode || tag.tagValue || "-" }}
              </a-tag>
              <a-tag v-if="row.tags.length > 3" color="default">
                +{{ row.tags.length - 3 }}
              </a-tag>
            </template>
            <span v-else class="object-tag-empty">-</span>
          </div>
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
        <div class="source-detail-actions">
          <YButton
            :loading="page.downloadLoading"
            :disabled="!page.selectedRow.localTempPath"
            @click="page.downloadObject(page.selectedRow)"
          >
            <template #icon><DownloadOutlined /></template>
            下载文件
          </YButton>
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
          <a-descriptions-item label="业务日期">
            {{ formatDate(page.selectedRow.businessDate) }}
          </a-descriptions-item>
          <a-descriptions-item label="业务ID">
            {{ page.selectedRow.businessId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="收取日期">
            {{ formatDate(page.selectedRow.receiveDate) }}
          </a-descriptions-item>
          <a-descriptions-item label="指纹">
            {{ page.selectedRow.fingerprint || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="收取时间">
            {{ formatDateTime(page.selectedRow.receivedAt) }}
          </a-descriptions-item>
          <a-descriptions-item label="落库时间">
            {{ page.selectedRow.storedAt || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="本地临时路径">
            {{ page.selectedRow.localTempPath || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="真实存储地址">
            {{ page.selectedRow.realStoragePath || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="邮件ID">
            {{ page.selectedRow.mailId || "-" }}
          </a-descriptions-item>
          <a-descriptions-item label="标签">
            <div class="object-tag-list object-tag-list--wrap">
              <template v-if="page.selectedRow.tags?.length">
                <a-tag
                  v-for="tag in page.selectedRow.tags"
                  :key="tag.id || `${tag.tagCode}-${tag.tagValue}`"
                  color="blue"
                >
                  {{ page.formatTagLabel(tag.tagName || tag.tagCode) }}
                  <span v-if="tag.tagValue">：{{ tag.tagValue }}</span>
                </a-tag>
              </template>
              <span v-else>-</span>
            </div>
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-json-block">
          <h4>错误信息</h4>
          <pre>{{ page.selectedRow.errorMessage || "-" }}</pre>
        </div>
      </template>
    </a-drawer>
  </div>
</template>
