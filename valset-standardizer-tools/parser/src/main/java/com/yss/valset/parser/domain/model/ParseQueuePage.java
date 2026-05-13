package com.yss.valset.parser.domain.model;

import java.util.List;
import lombok.Value;

/**
 * 待解析任务分页结果。
 */
@Value
public class ParseQueuePage {

    List<ParseQueue> records;
    long total;
    int pageIndex;
    int pageSize;

    public List<ParseQueue> records() {
        return records;
    }

    public long total() {
        return total;
    }

    public int pageIndex() {
        return pageIndex;
    }

    public int pageSize() {
        return pageSize;
    }
}
