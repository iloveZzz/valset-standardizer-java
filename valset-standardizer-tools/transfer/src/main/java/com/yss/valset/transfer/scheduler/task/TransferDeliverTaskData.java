package com.yss.valset.transfer.scheduler.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 文件投递任务入参。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDeliverTaskData {

    private final String routeId;
    private final String transferId;
    private final int retryCount;

    @JsonCreator
    public TransferDeliverTaskData(
            @JsonProperty("routeId") String routeId,
            @JsonProperty("transferId") String transferId,
            @JsonProperty("retryCount") int retryCount) {
        this.routeId = routeId;
        this.transferId = transferId;
        this.retryCount = retryCount;
    }



    public String routeId() {
        return routeId;
    }

    public String transferId() {
        return transferId;
    }

    public int retryCount() {
        return retryCount;
    }



    public String getRouteId() {
        return routeId;
    }

    public String getTransferId() {
        return transferId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferDeliverTaskData other = (TransferDeliverTaskData) o;
        if (!java.util.Objects.equals(routeId, other.routeId)) {
            return false;
        }
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        if (retryCount != other.retryCount) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(routeId, transferId, retryCount);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferDeliverTaskData[");
        sb.append("routeId=").append(routeId);
        sb.append(", transferId=").append(transferId);
        sb.append(", retryCount=").append(retryCount);
        sb.append(']');
        return sb.toString();
    }


}
