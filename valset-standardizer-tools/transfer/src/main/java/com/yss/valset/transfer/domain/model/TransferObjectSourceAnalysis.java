package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件主对象按来源类型统计结果。
 */
public class TransferObjectSourceAnalysis {

    private final String sourceType;
    private final Long totalCount;
    private final List<TransferObjectStatusCount> statusCounts;
    private final List<TransferObjectMailFolderCount> mailFolderCounts;
    private final Long undeliveredCount;

    public TransferObjectSourceAnalysis(String sourceType, Long totalCount, List<TransferObjectStatusCount> statusCounts, List<TransferObjectMailFolderCount> mailFolderCounts, Long undeliveredCount) {
        this.sourceType = sourceType;
        this.totalCount = totalCount;
        this.statusCounts = statusCounts;
        this.mailFolderCounts = mailFolderCounts;
        this.undeliveredCount = undeliveredCount;
    }



    public String sourceType() {
        return sourceType;
    }

    public Long totalCount() {
        return totalCount;
    }

    public List<TransferObjectStatusCount> statusCounts() {
        return statusCounts;
    }

    public List<TransferObjectMailFolderCount> mailFolderCounts() {
        return mailFolderCounts;
    }

    public Long undeliveredCount() {
        return undeliveredCount;
    }



    public String getSourceType() {
        return sourceType;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public List<TransferObjectStatusCount> getStatusCounts() {
        return statusCounts;
    }

    public List<TransferObjectMailFolderCount> getMailFolderCounts() {
        return mailFolderCounts;
    }

    public Long getUndeliveredCount() {
        return undeliveredCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferObjectSourceAnalysis other = (TransferObjectSourceAnalysis) o;
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(totalCount, other.totalCount)) {
            return false;
        }
        if (!java.util.Objects.equals(statusCounts, other.statusCounts)) {
            return false;
        }
        if (!java.util.Objects.equals(mailFolderCounts, other.mailFolderCounts)) {
            return false;
        }
        if (!java.util.Objects.equals(undeliveredCount, other.undeliveredCount)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(sourceType, totalCount, statusCounts, mailFolderCounts, undeliveredCount);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectSourceAnalysis[");
        sb.append("sourceType=").append(sourceType);
        sb.append(", totalCount=").append(totalCount);
        sb.append(", statusCounts=").append(statusCounts);
        sb.append(", mailFolderCounts=").append(mailFolderCounts);
        sb.append(", undeliveredCount=").append(undeliveredCount);
        sb.append(']');
        return sb.toString();
    }



}
