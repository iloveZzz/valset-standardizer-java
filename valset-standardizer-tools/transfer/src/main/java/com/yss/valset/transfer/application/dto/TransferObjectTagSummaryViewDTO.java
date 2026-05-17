package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 标签识别结果汇总视图。
 */
@Data
@Builder
public class TransferObjectTagSummaryViewDTO implements java.io.Serializable {

    /**
     * 标签编码。
     */
    private String tagCode;

    /**
     * 标签名称。
     */
    private String tagName;

    /**
     * 命中数量。
     */
    private Long tagCount;
}
