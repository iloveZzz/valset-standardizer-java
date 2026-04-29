package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferTargetUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTargetMutationResponse;
import com.yss.valset.transfer.application.dto.TransferTargetViewDTO;
import com.yss.valset.transfer.application.service.TransferTargetManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.infrastructure.convertor.TransferSecretCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认投递目标管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferTargetManagementAppService implements TransferTargetManagementAppService {

    private final TransferTargetGateway transferTargetGateway;
    private final TransferRouteGateway transferRouteGateway;
    private final TransferSecretCodec transferSecretCodec;

    @Override
    public List<TransferTargetViewDTO> listTargets(String targetType, String targetCode, Boolean enabled, Integer limit) {
        return transferTargetGateway.listTargets(targetType, targetCode, enabled, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public TransferTargetViewDTO getTarget(String targetId) {
        return toView(transferTargetGateway.findById(parseLong(targetId))
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetId=" + targetId)));
    }

    @Override
    public TransferTargetMutationResponse upsertTarget(TransferTargetUpsertCommand command) {
        boolean createMode = command.getTargetId() == null;
        TargetType targetType = TargetType.valueOf(command.getTargetType());
        TransferTarget existing = command.getTargetId() == null ? null : transferTargetGateway.findById(parseLong(command.getTargetId()))
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetId=" + command.getTargetId()));
        long referencedRouteCount = existing == null ? 0L : transferRouteGateway.countByTargetCode(existing.targetCode());
        if (existing != null && Boolean.TRUE.equals(existing.enabled()) && Boolean.FALSE.equals(command.getEnabled()) && referencedRouteCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "存在 " + referencedRouteCount + " 条分拣路由配置引用了该目标（targetId=" + command.getTargetId() + "，targetCode=" + existing.targetCode() + "），请先解除路由配置后再停用"
            );
        }
        Map<String, Object> connectionConfig = mergeSensitiveConfig(
                existing == null ? null : existing.connectionConfig(),
                command.getConnectionConfig()
        );
        Map<String, Object> targetMeta = command.getTargetMeta() == null ? Map.of() : command.getTargetMeta();
        TransferTarget transferTarget = new TransferTarget(
                parseLong(command.getTargetId()) == null ? null : parseLong(command.getTargetId()),
                command.getTargetCode(),
                command.getTargetName(),
                targetType,
                Boolean.TRUE.equals(command.getEnabled()),
                command.getTargetPathTemplate(),
                connectionConfig,
                targetMeta,
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt()
        );
        TransferTarget saved = transferTargetGateway.save(transferTarget);
        return TransferTargetMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("投递目标保存成功")
                .formTemplateName(TransferFormTemplateNames.targetTemplateName(saved.targetType()))
                .target(toView(saved))
                .build();
    }

    @Override
    public TransferTargetMutationResponse deleteTarget(String targetId) {
        TransferTarget existing = transferTargetGateway.findById(parseLong(targetId))
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetId=" + targetId));
        long referencedRouteCount = transferRouteGateway.countByTargetCode(existing.targetCode());
        if (referencedRouteCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "存在 " + referencedRouteCount + " 条分拣路由配置引用了该目标（targetId=" + targetId + "，targetCode=" + existing.targetCode() + "），请先解除路由配置后再删除"
            );
        }
        transferTargetGateway.deleteById(parseLong(targetId));
        return TransferTargetMutationResponse.builder()
                .operation("delete")
                .message("投递目标删除成功")
                .formTemplateName(TransferFormTemplateNames.targetTemplateName(existing.targetType()))
                .target(toView(existing))
                .build();
    }

    private TransferTargetViewDTO toView(TransferTarget target) {
        return TransferTargetViewDTO.builder()
                .targetId(target.targetId() == null ? null : String.valueOf(target.targetId()))
                .targetCode(target.targetCode())
                .targetName(target.targetName())
                .targetType(target.targetType() == null ? null : target.targetType().name())
                .formTemplateName(TransferFormTemplateNames.targetTemplateName(target.targetType()))
                .enabled(target.enabled())
                .referencedRouteCount(target.targetCode() == null ? 0L : transferRouteGateway.countByTargetCode(target.targetCode()))
                .targetPathTemplate(target.targetPathTemplate())
                .connectionConfig(transferSecretCodec.maskMap(target.connectionConfig()))
                .targetMeta(transferSecretCodec.maskMap(target.targetMeta()))
                .createdAt(target.createdAt() == null ? null : java.time.LocalDateTime.ofInstant(target.createdAt(), java.time.ZoneId.systemDefault()))
                .updatedAt(target.updatedAt() == null ? null : java.time.LocalDateTime.ofInstant(target.updatedAt(), java.time.ZoneId.systemDefault()))
                .build();
    }

    private Map<String, Object> mergeSensitiveConfig(Map<String, Object> existing, Map<String, Object> incoming) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existing != null && !existing.isEmpty()) {
            merged.putAll(existing);
        }
        if (incoming != null && !incoming.isEmpty()) {
            merged.putAll(incoming);
        }
        if (existing == null || existing.isEmpty() || incoming == null || incoming.isEmpty()) {
            return merged;
        }
        for (String key : List.of("password", "accessKey", "secretKey", "passphrase")) {
            Object incomingValue = incoming.get(key);
            if (incomingValue == null || String.valueOf(incomingValue).isBlank()) {
                Object existingValue = existing.get(key);
                if (existingValue != null) {
                    merged.put(key, existingValue);
                }
            }
        }
        return merged;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
