package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件收发运行日志重投递明细。
 */
@Data
@Builder
public class TransferRunLogRedeliverItemViewDTO {

    /**
     * 运行日志主键。
     */
    private String runLogId;

    /**
     * 分拣对象主键。
     */
    private String transferId;

    /**
     * 路由主键。
     */
    private String routeId;

    /**
     * 运行阶段。
     */
    private String runStage;

    /**
     * 运行状态。
     */
    private String runStatus;

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 明细消息。
     */
    private String message;
}
