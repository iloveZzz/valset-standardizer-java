package com.yss.valset.analysis.domain.model;

import java.util.List;

/**
 * 待解析任务分页结果。
 */
public record ParseQueuePage(
        List<ParseQueue> records,
        long total,
        int pageIndex,
        int pageSize
) {
}
