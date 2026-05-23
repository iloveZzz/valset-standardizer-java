package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.transfer.application.dto.TransferFormTemplateViewDTO;
import com.yss.valset.transfer.application.dto.TransferFormTemplateGroupDTO;
import com.yss.valset.transfer.application.service.TransferFormTemplateQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer 表单接口。
 */
@RestController
@RequestMapping("/transfer-form-temps")
public class TransferFormTempController {

    private final TransferFormTemplateQueryService transferFormTemplateQueryService;

    public TransferFormTempController(TransferFormTemplateQueryService transferFormTemplateQueryService) {
        this.transferFormTemplateQueryService = transferFormTemplateQueryService;
    }

    /**
     * 查询 Transfer 表单列表。
     *
     * @return 表单列表
     */
    @GetMapping
    @Operation(summary = "查询 Transfer 表单列表", description = "返回来源、目标以及存储表单的 schema 和默认值，前端可直接用于生成表单。")
    public MultiResult<TransferFormTemplateViewDTO> listFormTemps() {
        return MultiResult.of(transferFormTemplateQueryService.listFormTemps());
    }

    /**
     * 查询 Transfer 表单分组。
     *
     * @return 表单分组列表
     */
    @GetMapping("/grouped")
    @Operation(summary = "查询 Transfer 表单分组", description = "返回按来源、目标和存储分组的表单，前端可直接用于菜单和表单生成。")
    public MultiResult<TransferFormTemplateGroupDTO> listGroupedFormTemps() {
        return MultiResult.of(transferFormTemplateQueryService.listGroupedFormTemps());
    }

    /**
     * 查询 Transfer 表单详情。
     *
     * @param name 表单名称
     * @return 表单详情
     */
    @GetMapping("/{name}")
    @Operation(summary = "查询 Transfer 表单详情", description = "按名称返回单个表单的 schema 和默认值。")
    public SingleResult<TransferFormTemplateViewDTO> getFormTemp(@PathVariable String name) {
        return SingleResult.of(transferFormTemplateQueryService.getFormTemp(name));
    }
}
