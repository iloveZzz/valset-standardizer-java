package com.yss.valset.transfer.application.command;

import lombok.Data;

/**
 * 文件主对象重新打标命令。
 */
@Data
public class TransferObjectRetagCommand {

    /**
     * 来源主键。
     */
    private String sourceId;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源编码。
     */
    private String sourceCode;

    /**
     * 文件状态。
     */
    private String status;

    /**
     * 邮件唯一标识。
     */
    private String mailId;

    /**
     * 文件指纹。
     */
    private String fingerprint;

    /**
     * 路由主键。
     */
    private String routeId;

    /**
     * 标签主键。
     */
    private String tagId;

    /**
     * 标签编码。
     */
    private String tagCode;

    /**
     * 标签值。
     */
    private String tagValue;
}
