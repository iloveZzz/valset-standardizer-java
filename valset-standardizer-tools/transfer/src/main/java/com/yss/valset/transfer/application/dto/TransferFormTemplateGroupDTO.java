package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Transfer 表单模板分组视图。
 */
@Data
@Builder
public class TransferFormTemplateGroupDTO {

    /**
     * 分组编码。
     */
    private String category;
    /**
     * 分组名称。
     */
    private String categoryName;
    /**
     * 当前分组下的模板列表。
     */
    private List<TransferFormTemplateViewDTO> templates;
}
