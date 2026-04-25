package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.command.TransferTagTestCommand;
import com.yss.valset.transfer.application.command.TransferTagUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTagMutationResponse;
import com.yss.valset.transfer.application.dto.TransferTagTestResultDTO;
import com.yss.valset.transfer.application.dto.TransferTagViewDTO;
import com.yss.valset.transfer.application.service.TransferTagManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标签管理接口。
 */
@RestController
@RequestMapping("/api/transfer-tags")
public class TransferTagController {

    private final TransferTagManagementAppService transferTagManagementAppService;

    public TransferTagController(TransferTagManagementAppService transferTagManagementAppService) {
        this.transferTagManagementAppService = transferTagManagementAppService;
    }

    @GetMapping
    @Operation(summary = "分页查询标签列表", description = "按标签编码、匹配策略和启用状态分页查询标签配置。")
    public PageResult<TransferTagViewDTO> pageTags(@RequestParam(value = "tagCode", required = false) String tagCode,
                                                   @RequestParam(value = "matchStrategy", required = false) String matchStrategy,
                                                   @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                   @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferTagManagementAppService.pageTags(tagCode, matchStrategy, enabled, pageIndex, pageSize);
    }

    @GetMapping("/{tagId}")
    @Operation(summary = "查询标签详情")
    public SingleResult<TransferTagViewDTO> getTag(@PathVariable String tagId) {
        return SingleResult.of(transferTagManagementAppService.getTag(tagId));
    }

    @GetMapping("/template-name")
    @Operation(summary = "查询标签模板名", description = "返回前端应加载的标签表单模板名称。")
    public SingleResult<String> getTemplateName() {
        return SingleResult.of(TransferFormTemplateNames.TRANSFER_TAG);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建标签")
    public SingleResult<TransferTagMutationResponse> createTag(@Valid @RequestBody TransferTagUpsertCommand command) {
        command.setTagId(null);
        return SingleResult.of(transferTagManagementAppService.upsertTag(command));
    }

    @PutMapping("/{tagId}")
    @Operation(summary = "更新标签")
    public SingleResult<TransferTagMutationResponse> updateTag(@PathVariable String tagId,
                                                               @Valid @RequestBody TransferTagUpsertCommand command) {
        command.setTagId(tagId);
        return SingleResult.of(transferTagManagementAppService.upsertTag(command));
    }

    @DeleteMapping("/{tagId}")
    @Operation(summary = "删除标签")
    public SingleResult<TransferTagMutationResponse> deleteTag(@PathVariable String tagId) {
        return SingleResult.of(transferTagManagementAppService.deleteTag(tagId));
    }

    @PostMapping("/{tagId}/test")
    @Operation(summary = "试跑标签规则")
    public SingleResult<TransferTagTestResultDTO> testTag(@PathVariable String tagId,
                                                          @RequestBody(required = false) TransferTagTestCommand command) {
        return SingleResult.of(transferTagManagementAppService.testTag(tagId, command));
    }
}
