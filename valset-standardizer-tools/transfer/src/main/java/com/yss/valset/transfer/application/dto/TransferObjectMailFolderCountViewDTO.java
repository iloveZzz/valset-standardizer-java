package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 邮件文件夹统计视图。
 */
@Data
@Builder
public class TransferObjectMailFolderCountViewDTO {

    /**
     * 邮件文件夹。
     */
    private String mailFolder;

    /**
     * 邮件文件夹标签。
     */
    private String mailFolderLabel;

    /**
     * 数量。
     */
    private Long count;
}
