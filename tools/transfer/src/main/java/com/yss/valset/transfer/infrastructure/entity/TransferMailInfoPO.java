package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分拣对象关联的邮件信息持久化实体。
 */
@Data
@TableName("t_transfer_mail_info")
public class TransferMailInfoPO {

    /**
     * 文件主键。
     */
    @TableId("transfer_id")
    private String transferId;

    /**
     * 邮件唯一标识。
     */
    @TableField("mail_id")
    private String mailId;

    /**
     * 邮件发件人。
     */
    @TableField("mail_from")
    private String mailFrom;

    /**
     * 邮件收件人。
     */
    @TableField("mail_to")
    private String mailTo;

    /**
     * 邮件抄送人。
     */
    @TableField("mail_cc")
    private String mailCc;

    /**
     * 邮件密送人。
     */
    @TableField("mail_bcc")
    private String mailBcc;

    /**
     * 邮件主题。
     */
    @TableField("mail_subject")
    private String mailSubject;

    /**
     * 邮件正文。
     */
    @TableField("mail_body")
    private String mailBody;

    /**
     * 邮件协议。
     */
    @TableField("mail_protocol")
    private String mailProtocol;

    /**
     * 邮件文件夹。
     */
    @TableField("mail_folder")
    private String mailFolder;
}
