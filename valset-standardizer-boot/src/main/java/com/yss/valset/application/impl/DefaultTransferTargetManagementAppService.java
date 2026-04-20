package com.yss.valset.application.impl;

import com.yss.valset.application.command.TransferTargetUpsertCommand;
import com.yss.valset.application.dto.TransferTargetMutationResponse;
import com.yss.valset.application.dto.TransferTargetViewDTO;
import com.yss.valset.application.service.TransferTargetManagementAppService;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 默认投递目标管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferTargetManagementAppService implements TransferTargetManagementAppService {

    private final TransferTargetGateway transferTargetGateway;

    @Override
    public List<TransferTargetViewDTO> listTargets(String targetType, String targetCode, Boolean enabled, Integer limit) {
        return transferTargetGateway.listTargets(targetType, targetCode, enabled, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public TransferTargetViewDTO getTarget(Long targetId) {
        return toView(transferTargetGateway.findById(targetId)
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetId=" + targetId)));
    }

    @Override
    public TransferTargetMutationResponse upsertTarget(TransferTargetUpsertCommand command) {
        boolean createMode = command.getTargetId() == null;
        TargetType targetType = TargetType.valueOf(command.getTargetType());
        TransferTarget transferTarget = new TransferTarget(
                command.getTargetId(),
                command.getTargetCode(),
                command.getTargetName(),
                targetType,
                Boolean.TRUE.equals(command.getEnabled()),
                command.getTargetPathTemplate(),
                command.getConnectionConfig() == null ? Map.of() : command.getConnectionConfig(),
                command.getTargetMeta() == null ? Map.of() : command.getTargetMeta()
        );
        TransferTarget saved = transferTargetGateway.save(transferTarget);
        return TransferTargetMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("投递目标保存成功")
                .target(toView(saved))
                .build();
    }

    @Override
    public TransferTargetMutationResponse deleteTarget(Long targetId) {
        TransferTarget existing = transferTargetGateway.findById(targetId)
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetId=" + targetId));
        transferTargetGateway.deleteById(targetId);
        return TransferTargetMutationResponse.builder()
                .operation("delete")
                .message("投递目标删除成功")
                .target(toView(existing))
                .build();
    }

    private TransferTargetViewDTO toView(TransferTarget target) {
        return TransferTargetViewDTO.builder()
                .targetId(target.targetId())
                .targetCode(target.targetCode())
                .targetName(target.targetName())
                .targetType(target.targetType() == null ? null : target.targetType().name())
                .enabled(target.enabled())
                .targetPathTemplate(target.targetPathTemplate())
                .connectionConfig(target.connectionConfig())
                .targetMeta(target.targetMeta())
                .build();
    }
}
