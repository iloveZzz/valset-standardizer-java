package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件主对象统计分析结果。
 */
public class TransferObjectAnalysis {

    private final Long totalCount;
    private final Long taggedCount;
    private final Long untaggedCount;
    private final List<TransferObjectSourceAnalysis> sourceAnalyses;
    private final TransferObjectSizeAnalysis sizeAnalysis;

    public TransferObjectAnalysis(Long totalCount, Long taggedCount, Long untaggedCount, List<TransferObjectSourceAnalysis> sourceAnalyses, TransferObjectSizeAnalysis sizeAnalysis) {
        this.totalCount = totalCount;
        this.taggedCount = taggedCount;
        this.untaggedCount = untaggedCount;
        this.sourceAnalyses = sourceAnalyses;
        this.sizeAnalysis = sizeAnalysis;
    }



    public Long totalCount() {
        return totalCount;
    }

    public Long taggedCount() {
        return taggedCount;
    }

    public Long untaggedCount() {
        return untaggedCount;
    }

    public List<TransferObjectSourceAnalysis> sourceAnalyses() {
        return sourceAnalyses;
    }

    public TransferObjectSizeAnalysis sizeAnalysis() {
        return sizeAnalysis;
    }



    public Long getTotalCount() {
        return totalCount;
    }

    public Long getTaggedCount() {
        return taggedCount;
    }

    public Long getUntaggedCount() {
        return untaggedCount;
    }

    public List<TransferObjectSourceAnalysis> getSourceAnalyses() {
        return sourceAnalyses;
    }

    public TransferObjectSizeAnalysis getSizeAnalysis() {
        return sizeAnalysis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferObjectAnalysis other = (TransferObjectAnalysis) o;
        if (!java.util.Objects.equals(totalCount, other.totalCount)) {
            return false;
        }
        if (!java.util.Objects.equals(taggedCount, other.taggedCount)) {
            return false;
        }
        if (!java.util.Objects.equals(untaggedCount, other.untaggedCount)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceAnalyses, other.sourceAnalyses)) {
            return false;
        }
        if (!java.util.Objects.equals(sizeAnalysis, other.sizeAnalysis)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(totalCount, taggedCount, untaggedCount, sourceAnalyses, sizeAnalysis);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectAnalysis[");
        sb.append("totalCount=").append(totalCount);
        sb.append(", taggedCount=").append(taggedCount);
        sb.append(", untaggedCount=").append(untaggedCount);
        sb.append(", sourceAnalyses=").append(sourceAnalyses);
        sb.append(", sizeAnalysis=").append(sizeAnalysis);
        sb.append(']');
        return sb.toString();
    }



}
