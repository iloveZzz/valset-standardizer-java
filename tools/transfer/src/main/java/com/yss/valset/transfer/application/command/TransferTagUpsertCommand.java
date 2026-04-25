package com.yss.valset.transfer.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 标签新增或更新命令。
 */
@Data
public class TransferTagUpsertCommand {

    private String tagId;

    @NotBlank(message = "标签编码不能为空")
    private String tagCode;

    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    @NotBlank(message = "标签值不能为空")
    private String tagValue;

    private Boolean enabled = Boolean.TRUE;

    private Integer priority = 10;

    private String matchStrategy = "SCRIPT_AND_REGEX";

    private String scriptLanguage = "qlexpress4";

    private String scriptBody;

    private String regexPattern;

    private Map<String, Object> tagMeta;
}
