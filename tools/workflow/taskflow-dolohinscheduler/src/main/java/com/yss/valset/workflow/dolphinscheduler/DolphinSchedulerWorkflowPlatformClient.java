package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.service.AbstractWorkflowPlatformClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DolphinScheduler 工作流客户端。
 */
@Component
public class DolphinSchedulerWorkflowPlatformClient extends AbstractWorkflowPlatformClient {

    @Override
    public EtlPlatformType platformType() {
        return EtlPlatformType.DOLPHIN_SCHEDULER;
    }

    @Override
    public void validate(WorkflowDefinitionDTO definition) {
        WorkflowEngineBindingDTO binding = definition == null ? null : definition.getEngineBinding();
        if (binding == null || binding.getPlatformType() != EtlPlatformType.DOLPHIN_SCHEDULER) {
            throw new IllegalArgumentException("DolphinScheduler 绑定信息不合法");
        }
        if (!StringUtils.hasText(binding.getExternalProjectCode())) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置项目编码");
        }
        if (!StringUtils.hasText(binding.getExternalWorkflowId())) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置工作流名称");
        }
    }

    @Override
    protected Map<String, Object> platformSpecificPayload(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowPlatformCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectCode", defaultProjectCode(definition));
        payload.put("workflowName", defaultExternalWorkflowId(definition));
        payload.put("namespace", definition == null || definition.getEngineBinding() == null ? null : definition.getEngineBinding().getExternalNamespace());
        payload.put("submitUser", definition == null ? null : definition.getWorkflowCode());
        payload.put("taskDefinitionCode", instance == null ? null : instance.getInstanceId());
        payload.put("stageCode", command == null ? null : command.getStageCode());
        payload.put("context", command == null ? Map.of() : command.getContext());
        payload.put("operationType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        payload.put("canonicalStatus", WorkflowStatus.fromRawStatus(instance == null ? null : instance.getRawStatus()).name());
        return payload;
    }

    @Override
    protected String rawTriggerStatus() {
        return "RUNNING";
    }

    @Override
    protected String rawStopStatus() {
        return "STOPPED";
    }

    @Override
    protected String rawRetryStatus() {
        return "RUNNING";
    }
}
