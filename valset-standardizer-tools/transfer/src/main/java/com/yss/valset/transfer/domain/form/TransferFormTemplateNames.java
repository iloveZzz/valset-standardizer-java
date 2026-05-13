package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;

/**
 * Transfer 表单模板名称常量与映射工具。
 */
public final class TransferFormTemplateNames {

    public static final String TRANSFER_SOURCE_LOCAL = "transfer_source_local";
    public static final String TRANSFER_SOURCE_EMAIL = "transfer_source_email";
    public static final String TRANSFER_SOURCE_S3 = "transfer_source_s3";
    public static final String TRANSFER_SOURCE_SFTP = "transfer_source_sftp";
    public static final String TRANSFER_SOURCE_HTTP = "transfer_source_http";

    public static final String TRANSFER_TARGET_EMAIL = "transfer_target_email";
    public static final String TRANSFER_TARGET_S3 = "transfer_target_s3";
    public static final String TRANSFER_TARGET_SFTP = "transfer_target_sftp";
    public static final String TRANSFER_TARGET_LOCAL = "transfer_target_local";
    public static final String TRANSFER_TARGET_FILESYS = "transfer_target_filesys";
    public static final String TRANSFER_RULE = "transfer_rule";
    public static final String TRANSFER_ROUTE = "transfer_route";
    public static final String TRANSFER_TAG = "transfer_tag";
    public static final String TRANSFER_TAG_BUSINESS_DATE = "transfer_tag_business_date";

    private TransferFormTemplateNames() {
    }

    /**
     * 根据来源类型返回表单模板名称。
     *
     * @param sourceType 来源类型
     * @return 表单模板名称，来源类型为空时返回 null
     */
    public static String sourceTemplateName(SourceType sourceType) {
        if (sourceType == null) {
            return null;
        }
        switch (sourceType) {
            case LOCAL_DIR:
                return TRANSFER_SOURCE_LOCAL;
            case EMAIL:
                return TRANSFER_SOURCE_EMAIL;
            case S3:
                return TRANSFER_SOURCE_S3;
            case SFTP:
                return TRANSFER_SOURCE_SFTP;
            case HTTP:
                return TRANSFER_SOURCE_HTTP;
            default:
                return null;
        }
    }

    /**
     * 根据目标类型返回表单模板名称。
     *
     * @param targetType 目标类型
     * @return 表单模板名称，目标类型为空时返回 null
     */
    public static String targetTemplateName(TargetType targetType) {
        if (targetType == null) {
            return null;
        }
        switch (targetType) {
            case EMAIL:
                return TRANSFER_TARGET_EMAIL;
            case S3:
                return TRANSFER_TARGET_S3;
            case SFTP:
                return TRANSFER_TARGET_SFTP;
            case LOCAL_DIR:
                return TRANSFER_TARGET_LOCAL;
            case FILESYS:
                return TRANSFER_TARGET_FILESYS;
            default:
                return null;
        }
    }
}
