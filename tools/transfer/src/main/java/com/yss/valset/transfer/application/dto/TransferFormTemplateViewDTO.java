package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

import com.yss.valset.transfer.domain.form.model.YssFormDefinition;

/**
 * Transfer 表单模板视图。
 */
@Data
@Builder
public class TransferFormTemplateViewDTO {

    /**
     * 模板名称。
     */
    private String name;
    /**
     * 模板说明。
     */
    private String description;
    /**
     * 模板分类。
     */
    private String category;
    /**
     * 模板版本。
     */
    private String version;
    /**
     * 表单定义。
     */
    private YssFormDefinition formDefinition;
    /**
     * 默认初始值。
     */
    private Map<String, Object> initialValues;
}
