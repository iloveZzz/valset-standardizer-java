package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件主对象邮件信息视图。
 */
@Data
@Builder
public class TransferMailInfoViewDTO {

    /**
     * 文件主键。
     */
    private String transferId;

    /**
     * 邮件唯一标识。
     */
    private String mailId;

    /**
     * 邮件发件人。
     */
    private String mailFrom;

    /**
     * 邮件收件人。
     */
    private String mailTo;

    /**
     * 邮件抄送人。
     */
    private String mailCc;

    /**
     * 邮件密送人。
     */
    private String mailBcc;

    /**
     * 邮件主题。
     */
    private String mailSubject;

    /**
     * 邮件正文。
     */
    private String mailBody;

    /**
     * 邮件协议。
     */
    private String mailProtocol;

    /**
     * 邮件文件夹。
     */
    private String mailFolder;
}
