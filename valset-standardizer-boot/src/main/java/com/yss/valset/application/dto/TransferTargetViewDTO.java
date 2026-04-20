package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 投递目标视图。
 */
@Data
@Builder
public class TransferTargetViewDTO {

    private Long targetId;
    private String targetCode;
    private String targetName;
    private String targetType;
    private Boolean enabled;
    private String targetPathTemplate;
    private Map<String, Object> connectionConfig;
    private Map<String, Object> targetMeta;
}
