package com.yss.valset.qlexpress.application.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * QLExpress 自定义函数新增/更新命令。
 */
@Data
public class QlexpressFunctionUpsertCommand {

    private String functionId;

    @NotBlank(message = "函数中文名称不能为空")
    private String functionCnName;

    @NotBlank(message = "函数名称不能为空")
    private String functionName;

    private String remark;

    @NotBlank(message = "函数脚本不能为空")
    private String scriptBody;

    private Boolean enabled;

    private Object extInfo;
}
