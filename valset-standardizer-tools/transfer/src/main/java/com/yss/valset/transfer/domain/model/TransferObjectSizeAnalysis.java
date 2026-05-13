package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件大小统计结果。
 */
public class TransferObjectSizeAnalysis {

    private final Long totalCount;
    private final Long totalSizeBytes;
    private final List<TransferObjectExtensionCount> extensionCounts;

    public TransferObjectSizeAnalysis(Long totalCount, Long totalSizeBytes, List<TransferObjectExtensionCount> extensionCounts) {
        this.totalCount = totalCount;
        this.totalSizeBytes = totalSizeBytes;
        this.extensionCounts = extensionCounts;
    }



    public Long totalCount() {
        return totalCount;
    }

    public Long totalSizeBytes() {
        return totalSizeBytes;
    }

    public List<TransferObjectExtensionCount> extensionCounts() {
        return extensionCounts;
    }



    public Long getTotalCount() {
        return totalCount;
    }

    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public List<TransferObjectExtensionCount> getExtensionCounts() {
        return extensionCounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferObjectSizeAnalysis other = (TransferObjectSizeAnalysis) o;
        if (!java.util.Objects.equals(totalCount, other.totalCount)) {
            return false;
        }
        if (!java.util.Objects.equals(totalSizeBytes, other.totalSizeBytes)) {
            return false;
        }
        if (!java.util.Objects.equals(extensionCounts, other.extensionCounts)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(totalCount, totalSizeBytes, extensionCounts);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectSizeAnalysis[");
        sb.append("totalCount=").append(totalCount);
        sb.append(", totalSizeBytes=").append(totalSizeBytes);
        sb.append(", extensionCounts=").append(extensionCounts);
        sb.append(']');
        return sb.toString();
    }



}
