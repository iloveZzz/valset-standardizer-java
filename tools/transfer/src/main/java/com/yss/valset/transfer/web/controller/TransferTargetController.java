package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.command.TransferTargetUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTargetMutationResponse;
import com.yss.valset.transfer.application.dto.TransferTargetViewDTO;
import com.yss.valset.transfer.application.service.TransferTargetManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.model.TargetType;
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
 * 投递目标管理接口。
 */
@RestController
@RequestMapping("/api/transfer-targets")
public class TransferTargetController {

    private final TransferTargetManagementAppService transferTargetManagementAppService;

    public TransferTargetController(TransferTargetManagementAppService transferTargetManagementAppService) {
        this.transferTargetManagementAppService = transferTargetManagementAppService;
    }

    /**
     * 查询投递目标列表。
     *
     * @param targetType 目标类型
     * @param targetCode 目标编码
     * @param enabled 是否启用
     * @param limit 查询上限
     * @return 投递目标列表
     */
    @GetMapping
    @Operation(summary = "查询投递目标列表", description = "按目标类型、目标编码和启用状态查询投递目标。")
    public MultiResult<TransferTargetViewDTO> listTargets(@RequestParam(value = "targetType", required = false) String targetType,
                                                          @RequestParam(value = "targetCode", required = false) String targetCode,
                                                          @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                          @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferTargetManagementAppService.listTargets(targetType, targetCode, enabled, limit));
    }

    /**
     * 查询投递目标详情。
     *
     * @param targetId 目标主键
     * @return 投递目标详情
     */
    @GetMapping("/{targetId}")
    @Operation(summary = "查询投递目标详情")
    public SingleResult<TransferTargetViewDTO> getTarget(@PathVariable String targetId) {
        return SingleResult.of(transferTargetManagementAppService.getTarget(targetId));
    }

    /**
     * 查询投递目标对应表单模板名。
     *
     * @param targetType 目标类型
     * @return 表单模板名称
     */
    @GetMapping("/template-name")
    @Operation(summary = "查询投递目标对应表单模板名", description = "根据目标类型返回前端应加载的表单模板名称。")
    public SingleResult<String> getTemplateName(@RequestParam("targetType") TargetType targetType) {
        return SingleResult.of(TransferFormTemplateNames.targetTemplateName(targetType));
    }

    /**
     * 创建投递目标。
     *
     * @param command 投递目标新增命令
     * @return 投递目标变更结果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建投递目标", description = "创建新的邮件、S3、SFTP、本地目录或文件服务投递目标配置。")
    public SingleResult<TransferTargetMutationResponse> createTarget(@Valid @RequestBody TransferTargetUpsertCommand command) {
        command.setTargetId(null);
        return SingleResult.of(transferTargetManagementAppService.upsertTarget(command));
    }

    /**
     * 更新投递目标。
     *
     * @param targetId 目标主键
     * @param command 投递目标更新命令
     * @return 投递目标变更结果
     */
    @PutMapping("/{targetId}")
    @Operation(summary = "更新投递目标", description = "更新投递目标配置与连接参数。")
    public SingleResult<TransferTargetMutationResponse> updateTarget(@PathVariable String targetId,
                                                                     @Valid @RequestBody TransferTargetUpsertCommand command) {
        command.setTargetId(targetId);
        return SingleResult.of(transferTargetManagementAppService.upsertTarget(command));
    }

    /**
     * 删除投递目标。
     *
     * @param targetId 目标主键
     * @return 投递目标变更结果
     */
    @DeleteMapping("/{targetId}")
    @Operation(summary = "删除投递目标", description = "按目标 id 删除投递目标配置；如果已有分拣路由引用该目标，则拒绝删除。")
    public SingleResult<TransferTargetMutationResponse> deleteTarget(@PathVariable String targetId) {
        return SingleResult.of(transferTargetManagementAppService.deleteTarget(targetId));
    }
}
