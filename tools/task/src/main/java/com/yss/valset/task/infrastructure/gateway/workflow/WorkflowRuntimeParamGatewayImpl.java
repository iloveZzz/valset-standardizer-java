package com.yss.valset.task.infrastructure.gateway.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;
import com.yss.valset.task.application.port.workflow.WorkflowRuntimeParamGateway;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowRuntimeParamPO;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowRuntimeParamRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 工作流运行参数网关实现。
 */
@Component
public class WorkflowRuntimeParamGatewayImpl implements WorkflowRuntimeParamGateway {

    private static final String DEFAULT_NAMESPACE = "subject.match.workflow";

    private final WorkflowRuntimeParamRepository repository;

    public WorkflowRuntimeParamGatewayImpl(WorkflowRuntimeParamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WorkflowRuntimeParamDTO> findByNamespace(String paramNamespace) {
        if (!StringUtils.hasText(paramNamespace)) {
            return Optional.empty();
        }
        WorkflowRuntimeParamPO po = repository.selectOne(
                Wrappers.lambdaQuery(WorkflowRuntimeParamPO.class)
                        .eq(WorkflowRuntimeParamPO::getParamNamespace, paramNamespace.trim())
                        .last("limit 1")
        );
        return Optional.ofNullable(toDTO(po));
    }

    @Override
    public WorkflowRuntimeParamDTO save(WorkflowRuntimeParamSaveCommand command) {
        WorkflowRuntimeParamPO po = repository.selectOne(
                Wrappers.lambdaQuery(WorkflowRuntimeParamPO.class)
                        .eq(WorkflowRuntimeParamPO::getParamNamespace, namespace(command))
                        .last("limit 1")
        );
        if (po == null) {
            po = new WorkflowRuntimeParamPO();
            po.setRuntimeParamId(null);
            po.setCreatedAt(LocalDateTime.now());
        }
        po.setParamNamespace(namespace(command));
        po.setSkipExcelStyleParsing(command.getSkipExcelStyleParsing());
        po.setEnableMatchProcess(command.getEnableMatchProcess());
        po.setPersistStandardizedDwdDetails(command.getPersistStandardizedDwdDetails());
        po.setDescription(command.getDescription());
        po.setUpdatedAt(LocalDateTime.now());
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(po.getUpdatedAt());
        }
        if (StringUtils.hasText(po.getRuntimeParamId())) {
            repository.updateById(po);
        } else {
            repository.insert(po);
        }
        return toDTO(po);
    }

    private String namespace(WorkflowRuntimeParamSaveCommand command) {
        String namespace = command == null ? null : command.getParamNamespace();
        return StringUtils.hasText(namespace) ? namespace.trim() : DEFAULT_NAMESPACE;
    }

    private WorkflowRuntimeParamDTO toDTO(WorkflowRuntimeParamPO po) {
        if (po == null) {
            return null;
        }
        WorkflowRuntimeParamDTO dto = new WorkflowRuntimeParamDTO();
        BeanUtils.copyProperties(po, dto);
        return dto;
    }
}
