package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 文件路由查询视图。
 */
@Data
@Builder
public class TransferRouteViewDTO {

    /**
     * 路由主键。
     */
    private String routeId;

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
     * 规则主键。
     */
    private String ruleId;

    /**
     * 目标类型。
     */
    private String targetType;

    /**
     * 目标编码。
     */
    private String targetCode;

    /**
     * 目标路径。
     */
    private String targetPath;

    /**
     * 重命名模板。
     */
    private String renamePattern;

    /**
     * 路由状态。
     */
    private String routeStatus;

    /**
     * 路由扩展信息。
     */
    private Map<String, Object> routeMeta;
}
