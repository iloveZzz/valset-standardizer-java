package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认 Spring Batch 阶段处理器。
 */
@Component
public class DefaultSpringBatchStageProcessor implements SpringBatchStageProcessor {

    @Override
    public SpringBatchStageExecutionResult process(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowStageDTO stage,
                                                   WorkflowPlatformCommand command) {
        LocalDateTime startTime = LocalDateTime.now();
        Map<String, Object> input = buildInput(definition, instance, stage, command);
        Map<String, Object> output = new LinkedHashMap<>(input);
        output.put("processedAt", startTime.toString());
        output.put("result", resolveResult(stage));
        output.put("platform", command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        output.put("operationType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        LocalDateTime endTime = LocalDateTime.now();
        return SpringBatchStageExecutionResult.builder()
                .status(resolveStatus(stage))
                .rawStatus(resolveStatus(stage).name())
                .message(resolveMessage(stage))
                .startTime(startTime)
                .endTime(endTime)
                .input(input)
                .output(output)
                .metadata(buildMetadata(definition, instance, stage, command))
                .build();
    }

    private Map<String, Object> buildInput(WorkflowDefinitionDTO definition,
                                           WorkflowInstanceDTO instance,
                                           WorkflowStageDTO stage,
                                           WorkflowPlatformCommand command) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("workflowCode", definition == null ? null : definition.getWorkflowCode());
        input.put("workflowVersionNo", definition == null ? null : definition.getWorkflowVersionNo());
        input.put("workflowName", definition == null ? null : definition.getWorkflowName());
        input.put("businessKey", instance == null ? null : instance.getBusinessKey());
        input.put("externalWorkflowId", command == null ? null : command.getExternalWorkflowId());
        input.put("stageCode", stage == null ? null : stage.getStageCode());
        input.put("stageName", stage == null ? null : stage.getStageName());
        input.put("stageOrder", stage == null ? null : stage.getStageOrder());
        input.put("context", command == null ? Map.of() : command.getContext());
        input.put("parameters", command == null ? Map.of() : command.getParameters());
        return input;
    }

    private Map<String, Object> buildMetadata(WorkflowDefinitionDTO definition,
                                              WorkflowInstanceDTO instance,
                                              WorkflowStageDTO stage,
                                              WorkflowPlatformCommand command) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("description", stage == null ? null : stage.getDescription());
        metadata.put("retryable", stage != null && stage.isRetryable());
        metadata.put("timeoutSeconds", stage == null ? null : stage.getTimeoutSeconds());
        metadata.put("instanceId", instance == null ? null : instance.getInstanceId());
        metadata.put("jobName", definition == null ? null : definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo());
        metadata.put("businessKey", instance == null ? null : instance.getBusinessKey());
        metadata.put("commandType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        return metadata;
    }

    private WorkflowStatus resolveStatus(WorkflowStageDTO stage) {
        if (stage == null || !stage.isRetryable()) {
            return WorkflowStatus.SUCCEEDED;
        }
        return WorkflowStatus.SUCCEEDED;
    }

    private String resolveResult(WorkflowStageDTO stage) {
        if (stage == null || !StringUtils.hasText(stage.getStageCode())) {
            return "DEFAULT";
        }
        return stage.getStageCode();
    }

    private String resolveMessage(WorkflowStageDTO stage) {
        if (stage == null || !StringUtils.hasText(stage.getDescription())) {
            return "阶段已完成";
        }
        return stage.getDescription();
    }
}
