package com.yss.valset.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.command.TransferRuleUpsertCommand;
import com.yss.valset.application.dto.TransferRuleMutationResponse;
import com.yss.valset.application.dto.TransferRuleViewDTO;
import com.yss.valset.application.service.TransferRuleManagementAppService;
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
 * 路由规则管理接口。
 */
@RestController
@RequestMapping("/api/transfer-rules")
public class TransferRuleController {

    private final TransferRuleManagementAppService transferRuleManagementAppService;

    public TransferRuleController(TransferRuleManagementAppService transferRuleManagementAppService) {
        this.transferRuleManagementAppService = transferRuleManagementAppService;
    }

    /**
     * 查询路由规则列表。
     *
     * @param ruleCode 规则编码
     * @param enabled 是否启用
     * @param limit 查询上限
     * @return 路由规则列表
     */
    @GetMapping
    @Operation(summary = "查询路由规则列表", description = "按规则编码和启用状态查询路由规则。")
    public MultiResult<TransferRuleViewDTO> listRules(@RequestParam(value = "ruleCode", required = false) String ruleCode,
                                                      @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                      @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferRuleManagementAppService.listRules(ruleCode, enabled, limit));
    }

    /**
     * 查询路由规则详情。
     *
     * @param ruleId 规则主键
     * @return 路由规则详情
     */
    @GetMapping("/{ruleId}")
    @Operation(summary = "查询路由规则详情")
    public SingleResult<TransferRuleViewDTO> getRule(@PathVariable String ruleId) {
        return SingleResult.of(transferRuleManagementAppService.getRule(ruleId));
    }

    /**
     * 查询路由规则对应表单模板名。
     *
     * @return 表单模板名称
     */
    @GetMapping("/template-name")
    @Operation(summary = "查询路由规则对应表单模板名", description = "返回前端应加载的路由规则表单模板名称。")
    public SingleResult<String> getTemplateName() {
        return SingleResult.of(TransferFormTemplateNames.TRANSFER_RULE);
    }

    /**
     * 创建路由规则。
     *
     * @param command 路由规则新增命令
     * @return 路由规则变更结果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建路由规则", description = "创建新的路由规则配置。")
    public SingleResult<TransferRuleMutationResponse> createRule(@Valid @RequestBody TransferRuleUpsertCommand command) {
        command.setRuleId(null);
        return SingleResult.of(transferRuleManagementAppService.upsertRule(command));
    }

    /**
     * 更新路由规则。
     *
     * @param ruleId 规则主键
     * @param command 路由规则更新命令
     * @return 路由规则变更结果
     */
    @PutMapping("/{ruleId}")
    @Operation(summary = "更新路由规则", description = "更新路由规则配置。")
    public SingleResult<TransferRuleMutationResponse> updateRule(@PathVariable String ruleId,
                                                                 @Valid @RequestBody TransferRuleUpsertCommand command) {
        command.setRuleId(ruleId);
        return SingleResult.of(transferRuleManagementAppService.upsertRule(command));
    }

    /**
     * 删除路由规则。
     *
     * @param ruleId 规则主键
     * @return 路由规则变更结果
     */
    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除路由规则", description = "按规则 id 删除路由规则。")
    public SingleResult<TransferRuleMutationResponse> deleteRule(@PathVariable String ruleId) {
        return SingleResult.of(transferRuleManagementAppService.deleteRule(ruleId));
    }
}
