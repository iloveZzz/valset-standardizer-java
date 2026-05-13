package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件主对象重新投递明细。
 */
@Data
@Builder
public class TransferObjectRedeliverItemViewDTO {

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 路由主键。
     */
    private String routeId;

    /**
     * 是否成功。
     */
    private boolean success;

    /**
     * 处理说明。
     */
    private String message;
}
