package com.yss.valset.transfer.infrastructure.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 邮件收件箱分组数据传输对象。
 */
public class MailInboxGroupDTO {

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 邮件分组键。
     */
    private String mailKey;

    /**
     * 来源主键。
     */
    private String sourceId;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源编码。
     */
    private String sourceCode;

    /**
     * 原始文件名。
     */
    private String originalName;

    /**
     * 文件扩展名。
     */
    private String extension;

    /**
     * 文件类型。
     */
    private String mimeType;

    /**
     * 文件大小，单位字节。
     */
    private Long sizeBytes;

    /**
     * 文件指纹。
     */
    private String fingerprint;

    /**
     * 来源引用标识。
     */
    private String sourceRef;

    /**
     * 本地临时文件路径。
     */
    private String localTempPath;

    /**
     * 真实文件存储地址。
     */
    private String realStoragePath;

    /**
     * 文件状态。
     */
    private String status;

    /**
     * 收取时间。
     */
    private LocalDateTime receivedAt;

    /**
     * 落库时间。
     */
    private LocalDateTime storedAt;

    /**
     * 业务日期。
     */
    private LocalDate businessDate;

    /**
     * 业务标识。
     */
    private String businessId;

    /**
     * 收取日期。
     */
    private LocalDate receiveDate;

    /**
     * 路由主键。
     */
    private String routeId;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 探测结果 JSON。
     */
    private String probeResultJson;

    /**
     * 文件元数据 JSON。
     */
    private String fileMetaJson;

    /**
     * 邮件唯一标识。
     */
    private String mailId;

    /**
     * 邮件发件人。
     */
    private String mailFrom;

    /**
     * 邮件收件人。
     */
    private String mailTo;

    /**
     * 邮件抄送人。
     */
    private String mailCc;

    /**
     * 邮件密送人。
     */
    private String mailBcc;

    /**
     * 邮件主题。
     */
    private String mailSubject;

    /**
     * 邮件正文。
     */
    private String mailBody;

    /**
     * 邮件协议。
     */
    private String mailProtocol;

    /**
     * 邮件文件夹。
     */
    private String mailFolder;

    /**
     * 是否已投递。
     */
    private Integer delivered;

    /**
     * 是否已打标。
     */
    private Integer tagged;

    /**
     * 行号（用于分组）。
     */
    private Long rowNum;

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getMailKey() {
        return mailKey;
    }

    public void setMailKey(String mailKey) {
        this.mailKey = mailKey;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getLocalTempPath() {
        return localTempPath;
    }

    public void setLocalTempPath(String localTempPath) {
        this.localTempPath = localTempPath;
    }

    public String getRealStoragePath() {
        return realStoragePath;
    }

    public void setRealStoragePath(String realStoragePath) {
        this.realStoragePath = realStoragePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(LocalDateTime storedAt) {
        this.storedAt = storedAt;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public LocalDate getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(LocalDate receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProbeResultJson() {
        return probeResultJson;
    }

    public void setProbeResultJson(String probeResultJson) {
        this.probeResultJson = probeResultJson;
    }

    public String getFileMetaJson() {
        return fileMetaJson;
    }

    public void setFileMetaJson(String fileMetaJson) {
        this.fileMetaJson = fileMetaJson;
    }

    public String getMailId() {
        return mailId;
    }

    public void setMailId(String mailId) {
        this.mailId = mailId;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    public String getMailCc() {
        return mailCc;
    }

    public void setMailCc(String mailCc) {
        this.mailCc = mailCc;
    }

    public String getMailBcc() {
        return mailBcc;
    }

    public void setMailBcc(String mailBcc) {
        this.mailBcc = mailBcc;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    public String getMailBody() {
        return mailBody;
    }

    public void setMailBody(String mailBody) {
        this.mailBody = mailBody;
    }

    public String getMailProtocol() {
        return mailProtocol;
    }

    public void setMailProtocol(String mailProtocol) {
        this.mailProtocol = mailProtocol;
    }

    public String getMailFolder() {
        return mailFolder;
    }

    public void setMailFolder(String mailFolder) {
        this.mailFolder = mailFolder;
    }

    public Integer getDelivered() {
        return delivered;
    }

    public void setDelivered(Integer delivered) {
        this.delivered = delivered;
    }

    public Integer getTagged() {
        return tagged;
    }

    public void setTagged(Integer tagged) {
        this.tagged = tagged;
    }

    public Long getRowNum() {
        return rowNum;
    }

    public void setRowNum(Long rowNum) {
        this.rowNum = rowNum;
    }
}
