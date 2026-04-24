package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析规则发布日志视图。
 */
@Data
@Builder
public class ParseRulePublishLogViewDTO {
    /**
     * 发布日志主键。
     */
    private String id;
    /**
     * 模板主键。
     */
    private String profileId;
    /**
     * 发布版本号。
     */
    private String version;
    /**
     * 发布状态。
     */
    private String publishStatus;
    /**
     * 发布时间。
     */
    private LocalDateTime publishTime;
    /**
     * 发布人。
     */
    private String publisher;
    /**
     * 发布说明。
     */
    private String publishComment;
    /**
     * 校验结果 JSON。
     */
    private String validationResultJson;
    /**
     * 回滚来源版本。
     */
    private String rollbackFromVersion;
}
