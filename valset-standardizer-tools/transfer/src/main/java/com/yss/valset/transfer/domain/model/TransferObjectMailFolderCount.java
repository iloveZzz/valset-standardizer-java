package com.yss.valset.transfer.domain.model;

/**
 * 邮件文件夹统计项。
 */
public record TransferObjectMailFolderCount(
        String mailFolder,
        Long count
) {
}
