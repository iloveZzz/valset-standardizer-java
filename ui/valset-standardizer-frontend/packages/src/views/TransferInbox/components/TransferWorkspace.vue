<script setup lang="ts">
import { DownloadOutlined, EyeOutlined, PaperClipOutlined } from "@ant-design/icons-vue";
import { YButton, YCard } from "@yss-ui/components";
import type { InboxPage } from "../types";

const { page } = defineProps<{
  page: InboxPage;
}>();
</script>

<template>
  <div class="mail-inbox-page">
    <div class="mail-inbox-body">
      <section class="mail-inbox-sidebar">
        <YCard class="mail-inbox-panel" :bordered="false" :padding="14">
          <div class="mail-inbox-panel-head">
            <div>
              <h3>所有邮件</h3>
              <p>集中显示每个类别的邮件，以便在收件箱中快速浏览。</p>
            </div>
            <a-tag color="blue">{{ page.filteredTotal }} 封</a-tag>
          </div>

          <div class="mail-inbox-folder-strip">
            <button
              type="button"
              class="mail-inbox-folder-chip"
              :class="{
                'mail-inbox-folder-chip--active': !page.query.mailFolder,
              }"
              @click="page.selectFolder()"
            >
              <span>全部邮件</span>
              <strong>{{ page.filteredTotal }}</strong>
            </button>
            <button
              v-for="folder in page.folderStats"
              :key="folder.mailFolder || folder.mailFolderLabel"
              type="button"
              class="mail-inbox-folder-chip"
              :class="{
                'mail-inbox-folder-chip--active':
                  page.query.mailFolder === folder.mailFolder,
              }"
              @click="page.selectFolder(folder.mailFolder)"
            >
              <span>{{ folder.mailFolderLabel }}</span>
              <strong>{{ folder.count }}</strong>
            </button>
          </div>

          <div
            class="mail-inbox-list"
            v-if="page.filteredRows.length"
            :ref="page.setListContainerRef"
            @scroll.passive="page.handleListScroll"
          >
            <button
              v-for="row in page.filteredRows"
              :key="row.transferId"
              type="button"
              class="mail-inbox-item"
              :class="{
                'mail-inbox-item--selected':
                  page.selectedRow?.transferId === row.transferId,
              }"
              @click="page.selectRow(row)"
            >
              <div class="mail-inbox-item-avatar">
                {{ page.getSenderInitial(row) }}
              </div>
              <div class="mail-inbox-item-body">
                <div class="mail-inbox-item-head">
                  <strong>{{ row.mailFrom || row.sourceCode || "-" }}</strong>
                  <span>{{ page.formatDateTime(row.receivedAt) }}</span>
                </div>
                <div class="mail-inbox-item-subject">
                  {{ row.mailSubject || row.originalName || "无主题" }}
                </div>
                <div class="mail-inbox-item-preview">
                  {{ page.getPreviewText(row) }}
                </div>
                <div class="mail-inbox-item-footer">
                  <a-tag color="blue">
                    {{ page.getFolderLabel(row.mailFolder) }}
                  </a-tag>
                  <span
                    v-if="row.attachments?.length"
                    class="mail-inbox-item-attachment"
                  >
                    <PaperClipOutlined />
                    {{ page.getAttachmentLabel(row) }}
                  </span>
                </div>
              </div>
            </button>
            <div v-if="page.loadingMore" class="mail-inbox-list-footer">
              正在加载更多邮件...
            </div>
            <div
              v-else-if="page.hasMoreRows"
              class="mail-inbox-list-footer mail-inbox-list-footer--hint"
            >
              下滑继续加载更多邮件
            </div>
          </div>

          <a-empty v-else description="当前没有匹配的邮件" />
        </YCard>
      </section>

      <section class="mail-inbox-main">
        <YCard class="mail-detail-card" :bordered="false" :padding="18">
          <a-spin :spinning="page.detailLoading">
            <template v-if="page.selectedRow">
              <div class="mail-detail-scroll">
                <div class="mail-detail-head">
                  <div class="mail-detail-avatar">
                    {{ page.getSenderInitial(page.selectedRow) }}
                  </div>
                  <div class="mail-detail-meta">
                    <div class="mail-detail-from">
                      {{
                        page.selectedRow.mailFrom ||
                        page.selectedRow.sourceCode ||
                        "-"
                      }}
                    </div>
                    <div class="mail-detail-subject">
                      {{
                        page.selectedRow.mailSubject ||
                        page.selectedRow.originalName ||
                        "无主题"
                      }}
                    </div>
                    <div class="mail-detail-lines">
                      <span>收件人：{{ page.selectedRow.mailTo || "-" }}</span>
                      <span v-if="page.selectedRow.mailCc"
                        >抄送：{{ page.selectedRow.mailCc }}</span
                      >
                      <span v-if="page.selectedRow.mailFolder"
                        >文件夹：{{
                          page.getFolderLabel(page.selectedRow.mailFolder)
                        }}</span
                      >
                      <span v-if="page.selectedRow.attachmentCount">
                        附件：{{ page.selectedRow.attachmentCount }} 个
                      </span>
                    </div>
                    <div
                      v-if="
                        page.selectedRow.attachments &&
                        page.selectedRow.attachments.length > 0
                      "
                      class="mail-detail-attachment-list"
                    >
                      <button
                        v-for="attachment in page.selectedRow.attachments"
                        :key="attachment.transferId"
                        type="button"
                        class="mail-detail-attachment-chip"
                        @click="page.downloadAttachment(attachment)"
                      >
                        <PaperClipOutlined />
                        <span>{{
                          attachment.originalName ||
                          attachment.realStoragePath ||
                          attachment.localTempPath ||
                          attachment.transferId ||
                          "附件"
                        }}</span>
                      </button>
                    </div>
                  </div>
                  <div class="mail-detail-actions">
                    <YButton @click="page.downloadRow(page.selectedRow)">
                      <template #icon><DownloadOutlined /></template>
                      下载文件
                    </YButton>
                    <a-button shape="circle" type="text">
                      <template #icon><EyeOutlined /></template>
                    </a-button>
                  </div>
                </div>

                <div class="mail-detail-toolbar">
                  <div class="mail-detail-toolbar-info">
                    <span class="mail-detail-toolbar-label">邮件正文</span>
                    <strong>正文显示模式</strong>
                  </div>
                  <div class="mail-detail-toolbar-actions">
                    <a-button
                      :type="page.mailBodyMode === 'fit' ? 'primary' : 'default'"
                      @click="page.setMailBodyMode('fit')"
                    >
                      适配阅读
                    </a-button>
                    <a-button
                      :type="page.mailBodyMode === 'raw' ? 'primary' : 'default'"
                      @click="page.setMailBodyMode('raw')"
                    >
                      原始 HTML
                    </a-button>
                  </div>
                </div>

                <div class="mail-detail-content">
                  <div class="mail-detail-body">
                    <iframe
                      class="mail-detail-html-frame"
                      :srcdoc="
                        page.renderMailBodySrcdoc(page.selectedRow.mailBody)
                      "
                      title="邮件正文"
                      sandbox=""
                    />
                  </div>
                </div>
              </div>
            </template>
            <a-empty v-else description="请选择一封邮件查看详情" />
          </a-spin>
        </YCard>
      </section>
    </div>
  </div>
</template>
