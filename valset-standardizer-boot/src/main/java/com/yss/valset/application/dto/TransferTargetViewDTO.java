package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 投递目标视图。
 */
@Data
@Builder
public class TransferTargetViewDTO {

    /**
     * 投递目标主键。
     */
    private String targetId;
    /**
     * 投递目标编码。
     */
    private String targetCode;
    /**
     * 投递目标名称。
     */
    private String targetName;
    /**
     * 投递目标类型。
     */
    private String targetType;
    /**
     * 当前使用的目标表单模板名。
     */
    private String formTemplateName;
    /**
     * 是否启用。
     */
    private Boolean enabled;
    /**
     * 投递路径模板。
     */
    private String targetPathTemplate;
    /**
     * 连接配置。
     */
    private Map<String, Object> connectionConfig;
    /**
     * 目标扩展信息。
     */
    private Map<String, Object> targetMeta;
    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    /**
     * 修改时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
