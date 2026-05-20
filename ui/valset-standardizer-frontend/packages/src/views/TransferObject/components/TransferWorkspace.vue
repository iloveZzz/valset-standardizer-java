<script setup lang="ts">
import { h, ref } from "vue";
import { Modal } from "ant-design-vue";
import { YButton, YCard, YTable } from "@yss-ui/components";
import { useTableHeight } from "@yss-ui/hooks";
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
import type { ObjectPage, TransferObjectTagViewDTO } from "../types";

const { page } = defineProps<{
  page: ObjectPage;
}>();

const tableAreaRef = ref<HTMLDivElement>();
const { tableHeight } = useTableHeight(tableAreaRef, {
  withPagination: true,
  withToolbar: true,
});

const VISIBLE_TAG_COUNT = 2;
const columns = useTransferObjectColumns();

const getTagKey = (tag: TransferObjectTagViewDTO, index: number) => {
  return tag.id || tag.tagId || `${tag.tagCode}-${tag.tagValue}-${index}`;
};

const getTagText = (tag: TransferObjectTagViewDTO) => {
  return page.formatTagLabel(tag.tagName);
};

const getVisibleTags = (tags: TransferObjectTagViewDTO[] | undefined) => {
  return (tags ?? []).slice(0, VISIBLE_TAG_COUNT);
};

const getHiddenTagCount = (tags: TransferObjectTagViewDTO[] | undefined) => {
  return Math.max((tags?.length ?? 0) - VISIBLE_TAG_COUNT, 0);
};

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
    <YCard class="workspace-query-card" :bordered="false" :padding="12">
      <div class="workspace-query-bar">
        <a-form
          layout="inline"
          class="workspace-query-form"
          @submit.prevent="page.runQuery"
        >
          <a-form-item label="文件状态">
            <a-select
              v-model:value="page.query.status"
              style="width: 180px"
              size="small"
              placeholder="全部"
              allow-clear
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="PENDING">待处理</a-select-option>
              <a-select-option value="RECEIVED">已收取</a-select-option>
              <a-select-option value="IDENTIFIED">已识别</a-select-option>
              <a-select-option value="ROUTED">已路由</a-select-option>
              <a-select-option value="DELIVERING">投递中</a-select-option>
              <a-select-option value="DELIVERED">已投递</a-select-option>
              <a-select-option value="ARCHIVED">已归档</a-select-option>
              <a-select-option value="SKIPPED">已跳过</a-select-option>
              <a-select-option value="QUARANTINED">已隔离</a-select-option>
              <a-select-option value="FAILED">失败</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="附件名称">
            <a-input
              v-model:value="page.query.originalName"
              style="width: 260px"
              size="small"
              placeholder="输入附件名称"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="业务日期">
            <a-date-picker
              v-model:value="page.query.businessDate"
              value-format="YYYY-MM-DD"
              style="width: 150px"
              size="small"
              placeholder="选择业务日期"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="收取日期">
            <a-date-picker
              v-model:value="page.query.receiveDate"
              value-format="YYYY-MM-DD"
              style="width: 150px"
              size="small"
              placeholder="选择收取日期"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="标签名称">
            <a-select
              v-model:value="page.query.tagIds"
              mode="multiple"
              style="width: 260px"
              size="small"
              placeholder="选择标签名称"
              allow-clear
              show-search
              option-filter-prop="label"
              @change="page.handleTagSelectChange"
              :options="
                page.tagOptions.map((tag) => ({
                  value: tag.tagId,
                  label: page.formatTagLabel(tag.tagName || tag.tagCode),
                }))
              "
            />
          </a-form-item>
        </a-form>
        <div class="workspace-query-actions">
          <a-space>
            <YButton type="primary" size="small" @click="page.runQuery">
              <template #icon><SearchOutlined /></template>
              查询对象
            </YButton>
            <YButton size="small" @click="page.resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置条件
            </YButton>
            <YButton
              :loading="page.retagLoading"
              size="small"
              @click="confirmRetag"
            >
              <template #icon><TagOutlined /></template>
              重新打标
            </YButton>
          </a-space>
        </div>
      </div>
    </YCard>

    <div ref="tableAreaRef" class="workspace-body">
      <YTable
        :columns="columns"
        :action-config="actionConfig"
        :data="page.tableData"
        :loading="page.loading || page.redeliverLoading || page.retagLoading"
        :max-height="tableHeight"
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
            <a-form
              layout="inline"
              class="workspace-table-toolbar-form"
              @submit.prevent="page.runQuery"
            >
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
          <div class="object-tag-list object-tag-list--table">
            <template v-if="row.tags?.length">
              <a-tag
                v-for="(tag, index) in getVisibleTags(row.tags)"
                :key="getTagKey(tag, index)"
                class="object-tag-chip"
                color="blue"
                :title="getTagText(tag)"
              >
                {{ getTagText(tag) }}
              </a-tag>
              <a-popover
                v-if="getHiddenTagCount(row.tags) >0"
                trigger="click"
                placement="topLeft"
                overlay-class-name="object-tag-popover"
              >
                <template #content>
                  <div class="object-tag-popover-list">
                    <a-tag
                      v-for="(tag, index) in row.tags"
                      :key="getTagKey(tag, index)"
                      class="object-tag-chip object-tag-chip--popover"
                      color="blue"
                    >
                      {{ getTagText(tag) }}
                    </a-tag>
                  </div>
                </template>
                <a-tag
                  class="object-tag-more"
                  color="blue"
                >
                  {{ `更多` }}
                </a-tag>
              </a-popover>
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
