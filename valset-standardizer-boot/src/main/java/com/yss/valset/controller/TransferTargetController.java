package com.yss.valset.controller;

import com.yss.valset.application.command.TransferTargetUpsertCommand;
import com.yss.valset.application.dto.TransferTargetMutationResponse;
import com.yss.valset.application.dto.TransferTargetViewDTO;
import com.yss.valset.application.service.TransferTargetManagementAppService;
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

import java.util.List;

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

    @GetMapping
    @Operation(summary = "查询投递目标列表", description = "按目标类型、目标编码和启用状态查询投递目标。")
    public List<TransferTargetViewDTO> listTargets(@RequestParam(value = "targetType", required = false) String targetType,
                                                   @RequestParam(value = "targetCode", required = false) String targetCode,
                                                   @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        return transferTargetManagementAppService.listTargets(targetType, targetCode, enabled, limit);
    }

    @GetMapping("/{targetId}")
    @Operation(summary = "查询投递目标详情")
    public TransferTargetViewDTO getTarget(@PathVariable Long targetId) {
        return transferTargetManagementAppService.getTarget(targetId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建投递目标", description = "创建新的邮件、S3、SFTP 或文件服务投递目标配置。")
    public TransferTargetMutationResponse createTarget(@Valid @RequestBody TransferTargetUpsertCommand command) {
        command.setTargetId(null);
        return transferTargetManagementAppService.upsertTarget(command);
    }

    @PutMapping("/{targetId}")
    @Operation(summary = "更新投递目标", description = "更新投递目标配置与连接参数。")
    public TransferTargetMutationResponse updateTarget(@PathVariable Long targetId,
                                                       @Valid @RequestBody TransferTargetUpsertCommand command) {
        command.setTargetId(targetId);
        return transferTargetManagementAppService.upsertTarget(command);
    }

    @DeleteMapping("/{targetId}")
    @Operation(summary = "删除投递目标", description = "按目标 id 删除投递目标配置。")
    public TransferTargetMutationResponse deleteTarget(@PathVariable Long targetId) {
        return transferTargetManagementAppService.deleteTarget(targetId);
    }
}
