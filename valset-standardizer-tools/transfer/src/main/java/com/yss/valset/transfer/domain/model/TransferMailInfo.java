package com.yss.valset.transfer.domain.model;

/**
 * 分拣对象关联的邮件信息。
 */
public record TransferMailInfo(
        String transferId,
        String mailId,
        String mailFrom,
        String mailTo,
        String mailCc,
        String mailBcc,
        String mailSubject,
        String mailBody,
        String mailProtocol,
        String mailFolder
) {
}
