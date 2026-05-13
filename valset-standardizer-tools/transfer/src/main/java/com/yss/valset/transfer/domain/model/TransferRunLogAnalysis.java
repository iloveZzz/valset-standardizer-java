package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件收发运行日志统计分析结果。
 */
public class TransferRunLogAnalysis {

    private final long totalCount;
    private final List<TransferRunLogStageAnalysis> stageAnalyses;

    public TransferRunLogAnalysis(long totalCount, List<TransferRunLogStageAnalysis> stageAnalyses) {
        this.totalCount = totalCount;
        this.stageAnalyses = stageAnalyses;
    }



    public long totalCount() {
        return totalCount;
    }

    public List<TransferRunLogStageAnalysis> stageAnalyses() {
        return stageAnalyses;
    }



    public long getTotalCount() {
        return totalCount;
    }

    public List<TransferRunLogStageAnalysis> getStageAnalyses() {
        return stageAnalyses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRunLogAnalysis other = (TransferRunLogAnalysis) o;
        if (totalCount != other.totalCount) {
            return false;
        }
        if (!java.util.Objects.equals(stageAnalyses, other.stageAnalyses)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(totalCount, stageAnalyses);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRunLogAnalysis[");
        sb.append("totalCount=").append(totalCount);
        sb.append(", stageAnalyses=").append(stageAnalyses);
        sb.append(']');
        return sb.toString();
    }



}
