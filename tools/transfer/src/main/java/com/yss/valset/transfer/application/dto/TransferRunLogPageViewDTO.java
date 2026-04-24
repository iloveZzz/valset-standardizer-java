package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件收发运行日志分页视图。
 */
@Data
@Builder
public class TransferRunLogPageViewDTO {

    /**
     * 当前页码。
     */
    private Long pageIndex;

    /**
     * 每页条数。
     */
    private Long pageSize;

    /**
     * 总记录数。
     */
    private Long total;

    /**
     * 日志记录列表。
     */
    private List<TransferRunLogViewDTO> records;
}
