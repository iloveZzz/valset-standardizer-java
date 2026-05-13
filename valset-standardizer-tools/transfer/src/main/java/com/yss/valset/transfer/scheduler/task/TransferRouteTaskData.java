package com.yss.valset.transfer.scheduler.task;

/**
 * 文件路由任务入参。
 */
public class TransferRouteTaskData {

    private final String transferId;

    public TransferRouteTaskData(String transferId) {
        this.transferId = transferId;
    }



    public String transferId() {
        return transferId;
    }



    public String getTransferId() {
        return transferId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRouteTaskData other = (TransferRouteTaskData) o;
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(transferId);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRouteTaskData[");
        sb.append("transferId=").append(transferId);
        sb.append(']');
        return sb.toString();
    }


}
