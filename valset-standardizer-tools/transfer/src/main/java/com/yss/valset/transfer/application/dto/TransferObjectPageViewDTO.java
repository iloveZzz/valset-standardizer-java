package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件主对象分页视图。
 */
@Data
@Builder
public class TransferObjectPageViewDTO {

    /**
     * 记录列表。
     */
    private List<TransferObjectViewDTO> records;

    /**
     * 总数。
     */
    private Long total;

    /**
     * 页码。
     */
    private Long pageIndex;

    /**
     * 每页条数。
     */
    private Long pageSize;
}
