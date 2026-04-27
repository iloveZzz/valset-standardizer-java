package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件主对象重新打标明细。
 */
@Data
@Builder
public class TransferObjectRetagItemViewDTO {

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 是否成功。
     */
    private boolean success;

    /**
     * 命中标签数量。
     */
    private Integer tagCount;

    /**
     * 处理说明。
     */
    private String message;
}
