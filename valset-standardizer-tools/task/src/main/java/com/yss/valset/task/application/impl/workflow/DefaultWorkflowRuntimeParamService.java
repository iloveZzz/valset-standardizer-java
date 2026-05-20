package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;
import com.yss.valset.application.service.workflow.WorkflowRuntimeParamService;
import com.yss.valset.common.support.WorkflowRuntimeParamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认工作流运行参数服务。
 */
@Slf4j
@Service
public class DefaultWorkflowRuntimeParamService implements WorkflowRuntimeParamService {

    private static final String DEFAULT_NAMESPACE = "subject.match.workflow";

    private final WorkflowRuntimeParamProperties defaultProperties;
    private final AtomicReference<WorkflowRuntimeParamDTO> cache = new AtomicReference<>();

    public DefaultWorkflowRuntimeParamService(WorkflowRuntimeParamProperties defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    @Override
    public WorkflowRuntimeParamDTO getRuntimeParam() {
        WorkflowRuntimeParamDTO cached = cache.get();
        if (cached != null) {
            return copy(cached);
        }
        synchronized (cache) {
            cached = cache.get();
            if (cached == null) {
                WorkflowRuntimeParamDTO loaded = defaultRuntimeParam();
                cache.set(copy(loaded));
                cached = loaded;
            }
        }
        return copy(cached);
    }

    @Override
    public WorkflowRuntimeParamDTO saveRuntimeParam(WorkflowRuntimeParamSaveCommand command) {
        WorkflowRuntimeParamSaveCommand normalized = normalize(command);
        WorkflowRuntimeParamDTO saved = toRuntimeParam(normalized);
        cache.set(copy(saved));
        return copy(saved);
    }

    @Override
    public void refresh() {
        cache.set(null);
    }

    @Override
    public boolean enableMatchProcess() {
        return Boolean.TRUE.equals(getRuntimeParam().getEnableMatchProcess());
    }

    @Override
    public boolean skipExcelStyleParsing() {
        return Boolean.TRUE.equals(getRuntimeParam().getSkipExcelStyleParsing());
    }

    private WorkflowRuntimeParamSaveCommand normalize(WorkflowRuntimeParamSaveCommand command) {
        WorkflowRuntimeParamSaveCommand normalized = command == null ? new WorkflowRuntimeParamSaveCommand() : command;
        if (!StringUtils.hasText(normalized.getParamNamespace())) {
            normalized.setParamNamespace(DEFAULT_NAMESPACE);
        }
        if (normalized.getSkipExcelStyleParsing() == null) {
            normalized.setSkipExcelStyleParsing(defaultProperties.isSkipExcelStyleParsing());
        }
        if (normalized.getEnableMatchProcess() == null) {
            normalized.setEnableMatchProcess(defaultProperties.isEnableMatchProcess());
        }
        return normalized;
    }

    private WorkflowRuntimeParamDTO defaultRuntimeParam() {
        WorkflowRuntimeParamDTO dto = new WorkflowRuntimeParamDTO();
        dto.setParamNamespace(DEFAULT_NAMESPACE);
        dto.setSkipExcelStyleParsing(defaultProperties.isSkipExcelStyleParsing());
        dto.setEnableMatchProcess(defaultProperties.isEnableMatchProcess());
        return dto;
    }

    private WorkflowRuntimeParamDTO toRuntimeParam(WorkflowRuntimeParamSaveCommand command) {
        WorkflowRuntimeParamDTO dto = defaultRuntimeParam();
        if (command == null) {
            return dto;
        }
        dto.setParamNamespace(command.getParamNamespace());
        dto.setSkipExcelStyleParsing(command.getSkipExcelStyleParsing());
        dto.setEnableMatchProcess(command.getEnableMatchProcess());
        dto.setDescription(command.getDescription());
        return dto;
    }

    private WorkflowRuntimeParamDTO copy(WorkflowRuntimeParamDTO source) {
        if (source == null) {
            return null;
        }
        WorkflowRuntimeParamDTO copy = new WorkflowRuntimeParamDTO();
        copy.setRuntimeParamId(source.getRuntimeParamId());
        copy.setParamNamespace(source.getParamNamespace());
        copy.setSkipExcelStyleParsing(source.getSkipExcelStyleParsing());
        copy.setEnableMatchProcess(source.getEnableMatchProcess());
        copy.setDescription(source.getDescription());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }
}
