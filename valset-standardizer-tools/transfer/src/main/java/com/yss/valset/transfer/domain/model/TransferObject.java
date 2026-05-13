package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.yss.valset.transfer.infrastructure.convertor.Default;

/**
 * 已收取的文件主对象。
 */
public class TransferObject {

    private final String transferId;
    private final String sourceId;
    private final String sourceType;
    private final String sourceCode;
    private final String originalName;
    private final String extension;
    private final String mimeType;
    private final Long sizeBytes;
    private final String fingerprint;
    private final String sourceRef;
    private final String mailId;
    private final String mailFrom;
    private final String mailTo;
    private final String mailCc;
    private final String mailBcc;
    private final String mailSubject;
    private final String mailBody;
    private final String mailProtocol;
    private final String mailFolder;
    private final String localTempPath;
    private final TransferStatus status;
    private final Instant receivedAt;
    private final Instant storedAt;
    private final LocalDate businessDate;
    private final String businessId;
    private final LocalDate receiveDate;
    private final String routeId;
    private final String errorMessage;
    private final ProbeResult probeResult;
    private final Map<String, Object> fileMeta;
    private final String realStoragePath;

    @Default
    public TransferObject(String transferId, String sourceId, String sourceType, String sourceCode, String originalName, String extension, String mimeType, Long sizeBytes, String fingerprint, String sourceRef, String mailId, String mailFrom, String mailTo, String mailCc, String mailBcc, String mailSubject, String mailBody, String mailProtocol, String mailFolder, String localTempPath, TransferStatus status, Instant receivedAt, Instant storedAt, LocalDate businessDate, String businessId, LocalDate receiveDate, String routeId, String errorMessage, ProbeResult probeResult, Map<String, Object> fileMeta, String realStoragePath) {
        this.transferId = transferId;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.sourceCode = sourceCode;
        this.originalName = originalName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.fingerprint = fingerprint;
        this.sourceRef = sourceRef;
        this.mailId = mailId;
        this.mailFrom = mailFrom;
        this.mailTo = mailTo;
        this.mailCc = mailCc;
        this.mailBcc = mailBcc;
        this.mailSubject = mailSubject;
        this.mailBody = mailBody;
        this.mailProtocol = mailProtocol;
        this.mailFolder = mailFolder;
        this.localTempPath = localTempPath;
        this.status = status;
        this.receivedAt = receivedAt;
        this.storedAt = storedAt;
        this.businessDate = businessDate;
        this.businessId = businessId;
        this.receiveDate = receiveDate;
        this.routeId = routeId;
        this.errorMessage = errorMessage;
        this.probeResult = probeResult;
        this.fileMeta = fileMeta;
        this.realStoragePath = realStoragePath;
    }



    public String transferId() {
        return transferId;
    }

    public String sourceId() {
        return sourceId;
    }

    public String sourceType() {
        return sourceType;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String originalName() {
        return originalName;
    }

    public String extension() {
        return extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public Long sizeBytes() {
        return sizeBytes;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public String sourceRef() {
        return sourceRef;
    }

    public String mailId() {
        return mailId;
    }

    public String mailFrom() {
        return mailFrom;
    }

    public String mailTo() {
        return mailTo;
    }

    public String mailCc() {
        return mailCc;
    }

    public String mailBcc() {
        return mailBcc;
    }

    public String mailSubject() {
        return mailSubject;
    }

    public String mailBody() {
        return mailBody;
    }

    public String mailProtocol() {
        return mailProtocol;
    }

    public String mailFolder() {
        return mailFolder;
    }

    public String localTempPath() {
        return localTempPath;
    }

    public TransferStatus status() {
        return status;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant storedAt() {
        return storedAt;
    }

    public LocalDate businessDate() {
        return businessDate;
    }

    public String businessId() {
        return businessId;
    }

    public LocalDate receiveDate() {
        return receiveDate;
    }

    public String routeId() {
        return routeId;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public ProbeResult probeResult() {
        return probeResult;
    }

    public Map<String, Object> fileMeta() {
        return fileMeta;
    }

    public String realStoragePath() {
        return realStoragePath;
    }



    public String getTransferId() {
        return transferId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getMailId() {
        return mailId;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public String getMailCc() {
        return mailCc;
    }

    public String getMailBcc() {
        return mailBcc;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public String getMailBody() {
        return mailBody;
    }

    public String getMailProtocol() {
        return mailProtocol;
    }

    public String getMailFolder() {
        return mailFolder;
    }

    public String getLocalTempPath() {
        return localTempPath;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getStoredAt() {
        return storedAt;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public String getBusinessId() {
        return businessId;
    }

    public LocalDate getReceiveDate() {
        return receiveDate;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ProbeResult getProbeResult() {
        return probeResult;
    }

    public Map<String, Object> getFileMeta() {
        return fileMeta;
    }

    public String getRealStoragePath() {
        return realStoragePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferObject other = (TransferObject) o;
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        if (!java.util.Objects.equals(originalName, other.originalName)) {
            return false;
        }
        if (!java.util.Objects.equals(extension, other.extension)) {
            return false;
        }
        if (!java.util.Objects.equals(mimeType, other.mimeType)) {
            return false;
        }
        if (!java.util.Objects.equals(sizeBytes, other.sizeBytes)) {
            return false;
        }
        if (!java.util.Objects.equals(fingerprint, other.fingerprint)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceRef, other.sourceRef)) {
            return false;
        }
        if (!java.util.Objects.equals(mailId, other.mailId)) {
            return false;
        }
        if (!java.util.Objects.equals(mailFrom, other.mailFrom)) {
            return false;
        }
        if (!java.util.Objects.equals(mailTo, other.mailTo)) {
            return false;
        }
        if (!java.util.Objects.equals(mailCc, other.mailCc)) {
            return false;
        }
        if (!java.util.Objects.equals(mailBcc, other.mailBcc)) {
            return false;
        }
        if (!java.util.Objects.equals(mailSubject, other.mailSubject)) {
            return false;
        }
        if (!java.util.Objects.equals(mailBody, other.mailBody)) {
            return false;
        }
        if (!java.util.Objects.equals(mailProtocol, other.mailProtocol)) {
            return false;
        }
        if (!java.util.Objects.equals(mailFolder, other.mailFolder)) {
            return false;
        }
        if (!java.util.Objects.equals(localTempPath, other.localTempPath)) {
            return false;
        }
        if (!java.util.Objects.equals(status, other.status)) {
            return false;
        }
        if (!java.util.Objects.equals(receivedAt, other.receivedAt)) {
            return false;
        }
        if (!java.util.Objects.equals(storedAt, other.storedAt)) {
            return false;
        }
        if (!java.util.Objects.equals(businessDate, other.businessDate)) {
            return false;
        }
        if (!java.util.Objects.equals(businessId, other.businessId)) {
            return false;
        }
        if (!java.util.Objects.equals(receiveDate, other.receiveDate)) {
            return false;
        }
        if (!java.util.Objects.equals(routeId, other.routeId)) {
            return false;
        }
        if (!java.util.Objects.equals(errorMessage, other.errorMessage)) {
            return false;
        }
        if (!java.util.Objects.equals(probeResult, other.probeResult)) {
            return false;
        }
        if (!java.util.Objects.equals(fileMeta, other.fileMeta)) {
            return false;
        }
        if (!java.util.Objects.equals(realStoragePath, other.realStoragePath)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(transferId, sourceId, sourceType, sourceCode, originalName, extension, mimeType, sizeBytes, fingerprint, sourceRef, mailId, mailFrom, mailTo, mailCc, mailBcc, mailSubject, mailBody, mailProtocol, mailFolder, localTempPath, status, receivedAt, storedAt, businessDate, businessId, receiveDate, routeId, errorMessage, probeResult, fileMeta, realStoragePath);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObject[");
        sb.append("transferId=").append(transferId);
        sb.append(", sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", originalName=").append(originalName);
        sb.append(", extension=").append(extension);
        sb.append(", mimeType=").append(mimeType);
        sb.append(", sizeBytes=").append(sizeBytes);
        sb.append(", fingerprint=").append(fingerprint);
        sb.append(", sourceRef=").append(sourceRef);
        sb.append(", mailId=").append(mailId);
        sb.append(", mailFrom=").append(mailFrom);
        sb.append(", mailTo=").append(mailTo);
        sb.append(", mailCc=").append(mailCc);
        sb.append(", mailBcc=").append(mailBcc);
        sb.append(", mailSubject=").append(mailSubject);
        sb.append(", mailBody=").append(mailBody);
        sb.append(", mailProtocol=").append(mailProtocol);
        sb.append(", mailFolder=").append(mailFolder);
        sb.append(", localTempPath=").append(localTempPath);
        sb.append(", status=").append(status);
        sb.append(", receivedAt=").append(receivedAt);
        sb.append(", storedAt=").append(storedAt);
        sb.append(", businessDate=").append(businessDate);
        sb.append(", businessId=").append(businessId);
        sb.append(", receiveDate=").append(receiveDate);
        sb.append(", routeId=").append(routeId);
        sb.append(", errorMessage=").append(errorMessage);
        sb.append(", probeResult=").append(probeResult);
        sb.append(", fileMeta=").append(fileMeta);
        sb.append(", realStoragePath=").append(realStoragePath);
        sb.append(']');
        return sb.toString();
    }



public TransferObject(String transferId,
                          String sourceId,
                          String sourceType,
                          String sourceCode,
                          String originalName,
                          String extension,
                          String mimeType,
                          Long sizeBytes,
                          String fingerprint,
                          String sourceRef,
                          String mailId,
                          String mailFrom,
                          String mailTo,
                          String mailCc,
                          String mailBcc,
                          String mailSubject,
                          String mailBody,
                          String mailProtocol,
                          String mailFolder,
                          String localTempPath,
                          TransferStatus status,
                          Instant receivedAt,
                          Instant storedAt,
                          String routeId,
                          String errorMessage,
                          ProbeResult probeResult,
                          Map<String, Object> fileMeta) {
        this(transferId, sourceId, sourceType, sourceCode, originalName, extension, mimeType, sizeBytes, fingerprint, sourceRef,
                mailId, mailFrom, mailTo, mailCc, mailBcc, mailSubject, mailBody, mailProtocol, mailFolder, localTempPath,
                status, receivedAt, storedAt, null, null, null, routeId, errorMessage, probeResult, fileMeta, null);
    }

    public TransferObject withLocalTempPath(String localTempPath) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withRealStoragePath(String realStoragePath) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withBusinessDate(LocalDate businessDate) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withBusinessId(String businessId) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withReceiveDate(LocalDate receiveDate) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withBusinessFields(LocalDate businessDate, String businessId, LocalDate receiveDate) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withRouteId(String routeId) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withStatus(TransferStatus status, String errorMessage) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withProbeResult(ProbeResult probeResult) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withFileMeta(Map<String, Object> fileMeta) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

    public TransferObject withMailInfo(TransferMailInfo mailInfo) {
        if (mailInfo == null) {
            return this;
        }
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailInfo.mailId(),
                mailInfo.mailFrom(),
                mailInfo.mailTo(),
                mailInfo.mailCc(),
                mailInfo.mailBcc(),
                mailInfo.mailSubject(),
                mailInfo.mailBody(),
                mailInfo.mailProtocol(),
                mailInfo.mailFolder(),
                localTempPath,
                status,
                receivedAt,
                storedAt,
                businessDate,
                businessId,
                receiveDate,
                routeId,
                errorMessage,
                probeResult,
                fileMeta,
                realStoragePath
        );
    }

}
