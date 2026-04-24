package com.yss.valset.transfer.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 分拣路由新增或更新命令。
 */
@Data
public class TransferRouteUpsertCommand {

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
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /**
     * 来源编码。
     */
    @NotBlank(message = "来源编码不能为空")
    private String sourceCode;

    /**
     * 规则主键。
     */
    @NotNull(message = "规则主键不能为空")
    private String ruleId;

    /**
     * 目标类型。
     */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /**
     * 目标编码。
     */
    @NotBlank(message = "目标编码不能为空")
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
