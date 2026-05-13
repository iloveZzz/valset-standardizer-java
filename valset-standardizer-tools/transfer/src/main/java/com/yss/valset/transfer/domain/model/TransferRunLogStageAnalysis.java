package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件收发运行日志阶段统计。
 */
public class TransferRunLogStageAnalysis {

    private final String runStage;
    private final long totalCount;
    private final List<TransferRunLogStatusCount> statusCounts;

    public TransferRunLogStageAnalysis(String runStage, long totalCount, List<TransferRunLogStatusCount> statusCounts) {
        this.runStage = runStage;
        this.totalCount = totalCount;
        this.statusCounts = statusCounts;
    }



    public String runStage() {
        return runStage;
    }

    public long totalCount() {
        return totalCount;
    }

    public List<TransferRunLogStatusCount> statusCounts() {
        return statusCounts;
    }



    public String getRunStage() {
        return runStage;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public List<TransferRunLogStatusCount> getStatusCounts() {
        return statusCounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRunLogStageAnalysis other = (TransferRunLogStageAnalysis) o;
        if (!java.util.Objects.equals(runStage, other.runStage)) {
            return false;
        }
        if (totalCount != other.totalCount) {
            return false;
        }
        if (!java.util.Objects.equals(statusCounts, other.statusCounts)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(runStage, totalCount, statusCounts);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRunLogStageAnalysis[");
        sb.append("runStage=").append(runStage);
        sb.append(", totalCount=").append(totalCount);
        sb.append(", statusCounts=").append(statusCounts);
        sb.append(']');
        return sb.toString();
    }



}
