package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.dto.TransferFormTemplateViewDTO;
import com.yss.valset.transfer.application.dto.TransferFormTemplateGroupDTO;
import com.yss.valset.transfer.application.service.TransferFormTemplateQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer 表单模板接口。
 */
@RestController
@RequestMapping("/transfer-form-templates")
public class TransferFormTemplateController {

    private final TransferFormTemplateQueryService transferFormTemplateQueryService;

    public TransferFormTemplateController(TransferFormTemplateQueryService transferFormTemplateQueryService) {
        this.transferFormTemplateQueryService = transferFormTemplateQueryService;
    }

    /**
     * 查询 Transfer 表单模板列表。
     *
     * @return 表单模板列表
     */
    @GetMapping
    @Operation(summary = "查询 Transfer 表单模板列表", description = "返回来源、目标以及存储模板的 schema 和默认值，前端可直接用于生成表单。")
    public MultiResult<TransferFormTemplateViewDTO> listTemplates() {
        return MultiResult.of(transferFormTemplateQueryService.listTemplates());
    }

    /**
     * 查询 Transfer 表单模板分组。
     *
     * @return 表单模板分组列表
     */
    @GetMapping("/grouped")
    @Operation(summary = "查询 Transfer 表单模板分组", description = "返回按来源、目标和存储分组的表单模板，前端可直接用于菜单和表单生成。")
    public MultiResult<TransferFormTemplateGroupDTO> listGroupedTemplates() {
        return MultiResult.of(transferFormTemplateQueryService.listGroupedTemplates());
    }

    /**
     * 查询 Transfer 表单模板详情。
     *
     * @param name 模板名称
     * @return 表单模板详情
     */
    @GetMapping("/{name}")
    @Operation(summary = "查询 Transfer 表单模板详情", description = "按模板名称返回单个表单模板的 schema 和默认值。")
    public SingleResult<TransferFormTemplateViewDTO> getTemplate(@PathVariable String name) {
        return SingleResult.of(transferFormTemplateQueryService.getTemplate(name));
    }
}
