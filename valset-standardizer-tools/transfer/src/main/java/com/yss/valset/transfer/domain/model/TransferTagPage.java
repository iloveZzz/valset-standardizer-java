package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 标签分页结果。
 */
public class TransferTagPage {

    private final List<TransferTagDefinition> records;
    private final long total;
    private final long pageIndex;
    private final long pageSize;

    public TransferTagPage(List<TransferTagDefinition> records, long total, long pageIndex, long pageSize) {
        this.records = records;
        this.total = total;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }



    public List<TransferTagDefinition> records() {
        return records;
    }

    public long total() {
        return total;
    }

    public long pageIndex() {
        return pageIndex;
    }

    public long pageSize() {
        return pageSize;
    }



    public List<TransferTagDefinition> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPageIndex() {
        return pageIndex;
    }

    public long getPageSize() {
        return pageSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferTagPage other = (TransferTagPage) o;
        if (!java.util.Objects.equals(records, other.records)) {
            return false;
        }
        if (total != other.total) {
            return false;
        }
        if (pageIndex != other.pageIndex) {
            return false;
        }
        if (pageSize != other.pageSize) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(records, total, pageIndex, pageSize);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferTagPage[");
        sb.append("records=").append(records);
        sb.append(", total=").append(total);
        sb.append(", pageIndex=").append(pageIndex);
        sb.append(", pageSize=").append(pageSize);
        sb.append(']');
        return sb.toString();
    }



}
