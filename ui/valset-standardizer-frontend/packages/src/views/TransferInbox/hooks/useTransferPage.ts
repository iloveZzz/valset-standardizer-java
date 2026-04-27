import type { ComponentPublicInstance } from "vue";
import { computed, nextTick, reactive, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type {
  TransferObjectAnalysisViewDTO,
  TransferObjectMailFolderCountViewDTO,
  TransferObjectViewDTO,
} from "@/api/generated/valset/schemas";
import { getJavaSpringBootQuartzApi } from "@/api";
import { customInstance } from "@/api/mutator";
import { formatBytes, formatDateTime } from "@/utils/format";
import { unwrapSingleResult } from "@/utils/api-response";
import type {
  InboxAttachmentViewDTO,
  InboxFolderStat,
  InboxMailViewDTO,
  InboxPage,
  InboxQueryState,
} from "../types";

type InboxMailInfoViewDTO = Pick<
  TransferObjectViewDTO,
  | "transferId"
  | "mailId"
  | "mailFrom"
  | "mailTo"
  | "mailCc"
  | "mailBcc"
  | "mailSubject"
  | "mailBody"
  | "mailProtocol"
  | "mailFolder"
>;

type MailBodyMode = "fit" | "raw";

const api = getJavaSpringBootQuartzApi();
const LIST_PAGE_SIZE = 15;
const LIST_PREFETCH_THRESHOLD = 160;

const loadMailInfo = async (transferId: string) => {
  const response = await customInstance<{ data?: InboxMailInfoViewDTO }>({
    url: `/transfer-objects/${transferId}/mail-info`,
    method: "GET",
  });
  return unwrapSingleResult(response) as InboxMailInfoViewDTO | null;
};

const MAIL_BODY_FIT_STYLE = `
  :root {
    color-scheme: light;
  }

  html,
  body {
    margin: 0;
    padding: 0;
    min-height: 100%;
    background: transparent;
    color: rgba(15, 23, 42, 0.92);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
      "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    font-size: 14px;
    line-height: 1.78;
    word-break: break-word;
  }

  body {
    padding: 20px 24px 28px;
  }

  * {
    box-sizing: border-box;
  }

  img {
    max-width: 100%;
    height: auto;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    margin: 0 0 14px;
    table-layout: fixed;
  }

  th,
  td {
    border: 1px solid rgba(15, 23, 42, 0.08);
    padding: 8px 10px;
    vertical-align: top;
  }

  p {
    margin: 0 0 10px;
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    margin: 18px 0 10px;
    color: #0f172a;
    line-height: 1.35;
  }

  h1 {
    font-size: 24px;
  }

  h2 {
    font-size: 20px;
  }

  h3 {
    font-size: 18px;
  }

  h4 {
    font-size: 16px;
  }

  ul,
  ol {
    margin: 0 0 12px 20px;
    padding: 0;
  }

  li {
    margin: 4px 0;
  }

  blockquote {
    margin: 0 0 12px;
    padding: 10px 14px;
    border-left: 4px solid rgba(22, 119, 255, 0.28);
    background: rgba(22, 119, 255, 0.04);
    color: rgba(15, 23, 42, 0.75);
    border-radius: 10px;
  }

  hr {
    border: 0;
    border-top: 1px solid rgba(15, 23, 42, 0.1);
    margin: 16px 0;
  }

  a {
    color: #1677ff;
    text-decoration: none;
  }

  code {
    padding: 2px 6px;
    border-radius: 6px;
    background: rgba(15, 23, 42, 0.06);
    font-size: 0.95em;
  }

  pre {
    margin: 0 0 12px;
    padding: 14px 16px;
    border-radius: 14px;
    background: rgba(15, 23, 42, 0.05);
    color: rgba(15, 23, 42, 0.88);
    white-space: pre-wrap;
    word-break: break-word;
    overflow: auto;
  }

  figure {
    margin: 0 0 12px;
  }

  figcaption {
    margin-top: 6px;
    color: rgba(15, 23, 42, 0.56);
    font-size: 12px;
    line-height: 1.5;
    text-align: center;
  }

  aside {
    margin: 0 0 12px;
    padding: 10px 14px;
    border-radius: 12px;
    background: rgba(15, 23, 42, 0.035);
  }

  .MsoNormal {
    margin: 0 0 10px;
  }

  .mail-signature,
  .signature {
    margin-top: 18px;
    padding-top: 12px;
    border-top: 1px solid rgba(15, 23, 42, 0.08);
    color: rgba(15, 23, 42, 0.62);
    font-size: 12px;
  }

  .mail-detail-empty,
  .mail-detail-html-fallback {
    color: rgba(15, 23, 42, 0.6);
    font-size: 13px;
    line-height: 1.7;
  }
`;

const MAIL_BODY_RAW_STYLE = `
  html,
  body {
    margin: 0;
    padding: 0;
    min-height: 100%;
    background: #fff;
    color: rgba(15, 23, 42, 0.92);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
      "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
    font-size: 14px;
    line-height: 1.7;
    word-break: break-word;
  }

  body {
    padding: 18px 22px 24px;
  }

  * {
    box-sizing: border-box;
  }

  img {
    max-width: 100%;
    height: auto;
  }

  table {
    max-width: 100%;
  }
`;

const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const buildMailBodySrcdoc = (
  content?: string | null,
  mode: MailBodyMode = "fit",
) => {
  const raw = getText(content);
  const plainTextHtml = raw
    ? `<div class="mail-detail-html-fallback">${escapeHtml(raw).replace(/\n/g, "<br />")}</div>`
    : '<div class="mail-detail-empty">暂无正文</div>';
  const baseStyle = mode === "raw" ? MAIL_BODY_RAW_STYLE : MAIL_BODY_FIT_STYLE;
  const baseDoc = `<!doctype html><html><head><meta charset="UTF-8"><style>${baseStyle}</style></head><body><div class="mail-detail-html-root">${plainTextHtml}</div></body></html>`;
  if (!raw) {
    return baseDoc;
  }
  if (typeof window === "undefined" || typeof DOMParser === "undefined") {
    return baseDoc;
  }

  const parser = new DOMParser();
  const doc = parser.parseFromString(raw, "text/html");
  doc
    .querySelectorAll("script,iframe,object,embed,meta")
    .forEach((node) => node.remove());

  doc.querySelectorAll("*").forEach((node) => {
    [...node.attributes].forEach((attribute) => {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim().toLowerCase();
      if (name.startsWith("on")) {
        node.removeAttribute(attribute.name);
        return;
      }
      if (
        (name === "href" || name === "src" || name === "xlink:href") &&
        value.startsWith("javascript:")
      ) {
        node.removeAttribute(attribute.name);
      }
    });
  });

  const headHtml = doc.head?.innerHTML?.trim() ?? "";
  const bodyHtml = doc.body?.innerHTML?.trim() ?? "";
  const contentHtml = bodyHtml || plainTextHtml;
  const title = doc.title?.trim();
  return `<!doctype html><html><head><meta charset="UTF-8">${
    title ? `<title>${escapeHtml(title)}</title>` : ""
  }<style>${baseStyle}</style>${headHtml}</head><body><div class="mail-detail-html-root">${contentHtml}</div></body></html>`;
};

const defaultQuery = (): InboxQueryState => ({
  keyword: "",
  mailId: "",
  sourceCode: "",
  mailFolder: "",
  deliveryStatus: "",
});

const safeJson = (value: unknown) => {
  if (value === null || value === undefined || value === "") {
    return "{}";
  }
  if (typeof value === "string") {
    return value;
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return "{}";
  }
};

const getText = (value: unknown) => String(value ?? "").trim();

export const useTransferPage = (): { page: InboxPage } => {
  const query = reactive<InboxQueryState>(defaultQuery());
  const rows = ref<InboxMailViewDTO[]>([]);
  const total = ref(0);
  const pagination = ref({
    current: 1,
    pageSize: LIST_PAGE_SIZE,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ["15", "30", "45", "60"],
  });
  const analysis = ref<TransferObjectAnalysisViewDTO | null>(null);
  const selectedRow = ref<InboxMailViewDTO | null>(null);
  const loading = ref(false);
  const loadingMore = ref(false);
  const analysisLoading = ref(false);
  const detailLoading = ref(false);
  const downloadLoading = ref(false);
  const hasMoreRows = ref(true);
  const mailBodyMode = ref<MailBodyMode>("fit");
  const listContainerRef = ref<HTMLElement | null>(null);
  const sourceTotal = ref(0);
  let listRequestId = 0;
  let analysisRequestId = 0;

  const folderStats = computed<InboxFolderStat[]>(() => {
    const source = analysis.value?.sourceAnalyses?.[0]?.mailFolderCounts ?? [];
    return source.map((item: TransferObjectMailFolderCountViewDTO) => ({
      mailFolder: item.mailFolder,
      mailFolderLabel: item.mailFolderLabel || item.mailFolder || "未分类",
      count: Number(item.count ?? 0),
    }));
  });

  const matchesKeyword = (row: InboxMailViewDTO, keyword: string) => {
    if (!keyword) {
      return true;
    }
    const values = [
      row.mailFrom,
      row.mailTo,
      row.mailCc,
      row.mailBcc,
      row.mailSubject,
      row.mailBody,
      row.mailId,
      row.transferIds?.join(","),
      row.attachments?.map((item) => item.originalName || item.transferId).join(","),
      row.sourceCode,
      row.originalName,
      row.localTempPath,
      row.realStoragePath,
      row.mailFolder,
    ];
    return values.some((value) => getText(value).toLowerCase().includes(keyword));
  };

  const filteredRows = computed(() => {
    const keyword = getText(query.keyword).toLowerCase();
    const folder = getText(query.mailFolder);
    const deliveryStatus = getText(query.deliveryStatus).toUpperCase();
    const mailId = getText(query.mailId).toLowerCase();
    const sourceCode = getText(query.sourceCode).toLowerCase();

    return rows.value.filter((row) => {
      if (folder && getText(row.mailFolder) !== folder) {
        return false;
      }
      if (deliveryStatus) {
        const normalized = getText(row.deliveryStatus).toUpperCase();
        if (deliveryStatus === "已投递" || deliveryStatus === "DELIVERED") {
          if (normalized !== "已投递" && normalized !== "DELIVERED" && normalized !== "SUCCESS") {
            return false;
          }
        } else if (deliveryStatus === "未投递") {
          if (normalized === "已投递" || normalized === "DELIVERED" || normalized === "SUCCESS") {
            return false;
          }
        }
      }
      if (mailId && !getText(row.mailId).toLowerCase().includes(mailId)) {
        return false;
      }
      if (sourceCode && !getText(row.sourceCode).toLowerCase().includes(sourceCode)) {
        return false;
      }
      return matchesKeyword(row, keyword);
    });
  });

  const filteredTotal = computed(() => filteredRows.value.length);

  const ensureSelectedRow = () => {
    if (!filteredRows.value.length) {
      selectedRow.value = null;
      return;
    }
    if (
      selectedRow.value &&
      filteredRows.value.some(
        (row) => getRowKey(row) === getRowKey(selectedRow.value),
      )
    ) {
      return;
    }
    selectedRow.value = filteredRows.value[0];
  };

  const getRowKey = (row?: InboxMailViewDTO | null) =>
    getText(row?.primaryTransferId ?? row?.transferId ?? row?.mailId);

  const loadAnalysis = async () => {
    const requestId = ++analysisRequestId;
    analysisLoading.value = true;
    try {
      const res = await customInstance<{
        data?: TransferObjectAnalysisViewDTO;
      }>({
        url: buildInboxQueryUrl("/transfer-objects/mail-inbox/analysis", {
          sourceCode: query.sourceCode || undefined,
          mailId: query.mailId || undefined,
          deliveryStatus: query.deliveryStatus || undefined,
        }),
        method: "GET",
      });
      if (requestId !== analysisRequestId) {
        return;
      }
      analysis.value = unwrapSingleResult(res) ?? null;
    } catch (error) {
      if (requestId !== analysisRequestId) {
        return;
      }
      console.error("加载邮件收件箱分析失败:", error);
      message.error("加载邮件收件箱分析失败");
      analysis.value = null;
    } finally {
      if (requestId === analysisRequestId) {
        analysisLoading.value = false;
      }
    }
  };

  const resetListState = () => {
    rows.value = [];
    sourceTotal.value = 0;
    pagination.value.total = 0;
    pagination.value.current = 1;
    hasMoreRows.value = true;
  };

  const maybeAutoLoadMore = async () => {
    if (loading.value || loadingMore.value) {
      return;
    }
    await nextTick();
    const container = listContainerRef.value;
    if (!container || !hasMoreRows.value) {
      return;
    }
    if (container.scrollHeight <= container.clientHeight + 8) {
      await loadRows({ append: true });
      await maybeAutoLoadMore();
    }
  };

  const buildInboxQueryUrl = (
    path: string,
    params: Record<string, string | number | undefined>,
  ) => {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "") {
        return;
      }
      searchParams.set(key, String(value));
    });
    const query = searchParams.toString();
    return query ? `${path}?${query}` : path;
  };

  const loadRows = async (options: { append?: boolean } = {}) => {
    const append = options.append === true;
    const requestId = ++listRequestId;
    if (append) {
      loadingMore.value = true;
    } else {
      loading.value = true;
      resetListState();
    }
    try {
      const page = await customInstance<{
        data?: InboxMailViewDTO[];
        totalCount?: number;
      }>({
        url: buildInboxQueryUrl("/transfer-objects/mail-inbox", {
          sourceCode: query.sourceCode || undefined,
          mailId: query.mailId || undefined,
          deliveryStatus: query.deliveryStatus || undefined,
          pageIndex: append ? Math.floor(rows.value.length / LIST_PAGE_SIZE) : 0,
          pageSize: LIST_PAGE_SIZE,
        }),
        method: "GET",
      });
      if (requestId !== listRequestId) {
        return;
      }
      const nextRows = page.data ?? [];
      rows.value = append ? [...rows.value, ...nextRows] : nextRows;
      sourceTotal.value = Number(page.totalCount ?? 0);
      total.value = sourceTotal.value;
      pagination.value.total = sourceTotal.value;
      pagination.value.current = 1;
      hasMoreRows.value =
        rows.value.length < sourceTotal.value && nextRows.length === LIST_PAGE_SIZE;
    } catch (error) {
      if (requestId !== listRequestId) {
        return;
      }
      console.error("加载邮件收件箱失败:", error);
      message.error("加载邮件收件箱失败");
      if (!append) {
        resetListState();
      } else {
        hasMoreRows.value = false;
      }
    } finally {
      if (requestId === listRequestId) {
        loading.value = false;
        loadingMore.value = false;
      }
    }
  };

  const reloadInbox = async () => {
    await Promise.all([loadRows(), loadAnalysis()]);
    await maybeAutoLoadMore();
    ensureSelectedRow();
  };

  const refreshInbox = () => {
    void reloadInbox();
  };

  const resetQuery = () => {
    Object.assign(query, defaultQuery());
    void reloadInbox();
  };

  const selectFolder = (mailFolder?: string) => {
    query.mailFolder = mailFolder ?? "";
    ensureSelectedRow();
  };

  const selectDeliveryStatus = (deliveryStatus?: string) => {
    query.deliveryStatus = deliveryStatus ?? "";
    ensureSelectedRow();
  };

  const setListContainerRef = (el: Element | ComponentPublicInstance | null) => {
    listContainerRef.value = el instanceof HTMLElement ? el : null;
  };

  const loadMoreRows = () => {
    if (loading.value || loadingMore.value || !hasMoreRows.value) {
      return;
    }
    void loadRows({ append: true }).then(() => {
      void maybeAutoLoadMore();
    });
  };

  const handleListScroll = (event: Event) => {
    const target = event.currentTarget as HTMLElement | null;
    if (!target || loading.value || loadingMore.value || !hasMoreRows.value) {
      return;
    }
    if (
      target.scrollTop + target.clientHeight >=
      target.scrollHeight - LIST_PREFETCH_THRESHOLD
    ) {
      loadMoreRows();
    }
  };

  const loadDetail = async (row: InboxMailViewDTO) => {
    if (!row?.transferId) {
      selectedRow.value = row;
      return;
    }
    const primaryTransferId = getText(row.primaryTransferId ?? row.transferId);
    detailLoading.value = true;
    try {
      const detail = unwrapSingleResult(await api.getObject(primaryTransferId));
      const mailInfo = await loadMailInfo(primaryTransferId).catch(() => null);
      selectedRow.value = {
        ...row,
        ...(detail ?? {}),
        ...(mailInfo ?? {}),
      } as InboxMailViewDTO;
    } catch (error) {
      console.error("加载邮件详情失败:", error);
      message.error("加载邮件详情失败");
      selectedRow.value = row;
    } finally {
      detailLoading.value = false;
    }
  };

  const selectRow = (row: InboxMailViewDTO) => {
    void loadDetail(row);
  };

  const downloadRow = async (row?: InboxMailViewDTO | null) => {
    const targetRow = row ?? selectedRow.value;
    const transferId = getText(
      targetRow?.primaryTransferId ?? targetRow?.transferId,
    );
    if (!transferId) {
      message.warning("邮件缺少主键，无法下载");
      return;
    }
    if (downloadLoading.value) {
      return;
    }
    downloadLoading.value = true;
    try {
      const response = await customInstance<{
        data: Blob;
        headers?: Record<string, string>;
      }>({
        url: `/transfer-objects/${transferId}/download`,
        method: "GET",
        responseType: "blob",
      });
      const blob = response.data;
      const contentDisposition =
        response.headers?.["content-disposition"] ||
        response.headers?.["Content-Disposition"];
      const utf8FileName = contentDisposition?.match(
        /filename\*=UTF-8''([^;]+)/i,
      );
      const plainFileName = contentDisposition?.match(/filename="?([^";]+)"?/i);
      const fileName =
        (utf8FileName?.[1] &&
          decodeURIComponent(utf8FileName[1].trim())) ||
        plainFileName?.[1]?.trim() ||
        getText(targetRow?.originalName) ||
        "mail-attachment";
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = fileName;
      link.style.display = "none";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
      message.success("下载已开始");
    } catch (error) {
      console.error("下载邮件附件失败:", error);
      message.error("下载邮件附件失败");
    } finally {
      downloadLoading.value = false;
    }
  };

  const downloadAttachment = async (attachment: InboxAttachmentViewDTO) => {
    if (!attachment?.transferId) {
      message.warning("附件缺少主键，无法下载");
      return;
    }
    await downloadRow({
      transferId: attachment.transferId,
      originalName: attachment.originalName,
      localTempPath: attachment.localTempPath,
      realStoragePath: attachment.realStoragePath,
      mimeType: attachment.mimeType,
      sizeBytes: attachment.sizeBytes,
    } as InboxMailViewDTO);
  };

  const formatDeliveryStatus = (value?: string) => {
    const status = getText(value).toUpperCase();
    if (status === "已投递" || status === "DELIVERED" || status === "SUCCESS") {
      return "已投递";
    }
    return "未投递";
  };

  const getSenderInitial = (row?: InboxMailViewDTO | null) => {
    const source =
      getText(row?.mailFrom) ||
      getText(row?.sourceCode) ||
      getText(row?.originalName) ||
      "M";
    return source.charAt(0).toUpperCase();
  };

  const getPreviewText = (row?: InboxMailViewDTO | null) => {
    const text =
      getText(row?.mailBody) ||
      getText(row?.mailSubject) ||
      getText(row?.originalName) ||
      getText(row?.mailId) ||
      getText(row?.attachments?.[0]?.originalName) ||
      "暂无摘要";
    return text.length > 120 ? `${text.slice(0, 120)}...` : text;
  };

  const getFolderLabel = (value?: string | null) => {
    const text = getText(value);
    return text || "未分类";
  };

  const getAttachmentLabel = (row?: InboxMailViewDTO | null) => {
    if (!row) {
      return "无附件";
    }
    const attachments = row.attachments ?? [];
    if (attachments.length > 1) {
      const first = attachments[0];
      const name =
        getText(first.originalName) ||
        getText(first.localTempPath) ||
        getText(first.realStoragePath) ||
        "附件";
      return `${name} 等 ${attachments.length} 个附件`;
    }
    if (attachments.length === 1) {
      const item = attachments[0];
      return (
        getText(item.originalName) ||
        getText(item.localTempPath) ||
        getText(item.realStoragePath) ||
        "附件"
      );
    }
    if (getText(row.originalName)) {
      return row.originalName!;
    }
    if (getText(row.localTempPath)) {
      return row.localTempPath!;
    }
    if (getText(row.realStoragePath)) {
      return row.realStoragePath!;
    }
    return "无附件";
  };

  const setMailBodyMode = (mode: MailBodyMode) => {
    mailBodyMode.value = mode;
  };

  const renderMailBodySrcdoc = (value?: string | null) =>
    buildMailBodySrcdoc(value, mailBodyMode.value);

  watch(
    filteredRows,
    () => {
      ensureSelectedRow();
      void maybeAutoLoadMore();
    },
    { immediate: true },
  );

  void reloadInbox();

  const page = reactive({
    loading,
    loadingMore,
    analysisLoading,
    detailLoading,
    downloadLoading,
    hasMoreRows,
    mailBodyMode,
    rows,
    filteredRows,
    selectedRow,
    setListContainerRef,
    query,
    analysis,
    folderStats,
    total,
    filteredTotal,
    pagination,
    refreshInbox,
    resetQuery,
    selectFolder,
    selectDeliveryStatus,
    handleListScroll,
    selectRow,
    downloadRow,
    downloadAttachment,
    setMailBodyMode,
    formatDateTime,
    formatBytes: (value?: string | number | null) =>
      formatBytes(Number(value ?? 0)),
    formatDeliveryStatus,
    getSenderInitial,
    getRowKey,
    getPreviewText,
    getFolderLabel,
    getAttachmentLabel,
    renderMailBodySrcdoc,
    safeJson,
  }) as unknown as InboxPage;

  return {
    page,
  };
};
