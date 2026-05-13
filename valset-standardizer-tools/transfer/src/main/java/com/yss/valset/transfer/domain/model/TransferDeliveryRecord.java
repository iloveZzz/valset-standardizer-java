package com.yss.valset.transfer.domain.model;

import java.time.LocalDateTime;

/**
 * 文件投递结果。
 */
public class TransferDeliveryRecord {

    private final String deliveryId;
    private final String routeId;
    private final String transferId;
    private final String targetType;
    private final String targetCode;
    private final String executeStatus;
    private final Integer retryCount;
    private final String requestSnapshotJson;
    private final String responseSnapshotJson;
    private final String errorMessage;
    private final LocalDateTime deliveredAt;

    public TransferDeliveryRecord(String deliveryId, String routeId, String transferId, String targetType, String targetCode, String executeStatus, Integer retryCount, String requestSnapshotJson, String responseSnapshotJson, String errorMessage, LocalDateTime deliveredAt) {
        this.deliveryId = deliveryId;
        this.routeId = routeId;
        this.transferId = transferId;
        this.targetType = targetType;
        this.targetCode = targetCode;
        this.executeStatus = executeStatus;
        this.retryCount = retryCount;
        this.requestSnapshotJson = requestSnapshotJson;
        this.responseSnapshotJson = responseSnapshotJson;
        this.errorMessage = errorMessage;
        this.deliveredAt = deliveredAt;
    }



    public String deliveryId() {
        return deliveryId;
    }

    public String routeId() {
        return routeId;
    }

    public String transferId() {
        return transferId;
    }

    public String targetType() {
        return targetType;
    }

    public String targetCode() {
        return targetCode;
    }

    public String executeStatus() {
        return executeStatus;
    }

    public Integer retryCount() {
        return retryCount;
    }

    public String requestSnapshotJson() {
        return requestSnapshotJson;
    }

    public String responseSnapshotJson() {
        return responseSnapshotJson;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime deliveredAt() {
        return deliveredAt;
    }



    public String getDeliveryId() {
        return deliveryId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public String getExecuteStatus() {
        return executeStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public String getRequestSnapshotJson() {
        return requestSnapshotJson;
    }

    public String getResponseSnapshotJson() {
        return responseSnapshotJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferDeliveryRecord other = (TransferDeliveryRecord) o;
        if (!java.util.Objects.equals(deliveryId, other.deliveryId)) {
            return false;
        }
        if (!java.util.Objects.equals(routeId, other.routeId)) {
            return false;
        }
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        if (!java.util.Objects.equals(targetType, other.targetType)) {
            return false;
        }
        if (!java.util.Objects.equals(targetCode, other.targetCode)) {
            return false;
        }
        if (!java.util.Objects.equals(executeStatus, other.executeStatus)) {
            return false;
        }
        if (!java.util.Objects.equals(retryCount, other.retryCount)) {
            return false;
        }
        if (!java.util.Objects.equals(requestSnapshotJson, other.requestSnapshotJson)) {
            return false;
        }
        if (!java.util.Objects.equals(responseSnapshotJson, other.responseSnapshotJson)) {
            return false;
        }
        if (!java.util.Objects.equals(errorMessage, other.errorMessage)) {
            return false;
        }
        if (!java.util.Objects.equals(deliveredAt, other.deliveredAt)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(deliveryId, routeId, transferId, targetType, targetCode, executeStatus, retryCount, requestSnapshotJson, responseSnapshotJson, errorMessage, deliveredAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferDeliveryRecord[");
        sb.append("deliveryId=").append(deliveryId);
        sb.append(", routeId=").append(routeId);
        sb.append(", transferId=").append(transferId);
        sb.append(", targetType=").append(targetType);
        sb.append(", targetCode=").append(targetCode);
        sb.append(", executeStatus=").append(executeStatus);
        sb.append(", retryCount=").append(retryCount);
        sb.append(", requestSnapshotJson=").append(requestSnapshotJson);
        sb.append(", responseSnapshotJson=").append(responseSnapshotJson);
        sb.append(", errorMessage=").append(errorMessage);
        sb.append(", deliveredAt=").append(deliveredAt);
        sb.append(']');
        return sb.toString();
    }



}
