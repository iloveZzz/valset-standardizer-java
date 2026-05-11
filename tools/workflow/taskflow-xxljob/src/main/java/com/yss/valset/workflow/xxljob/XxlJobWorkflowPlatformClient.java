package com.yss.valset.workflow.xxljob;

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
 * XXL-JOB 工作流客户端。
 */
@Component
public class XxlJobWorkflowPlatformClient extends AbstractWorkflowPlatformClient {

    @Override
    public EtlPlatformType platformType() {
        return EtlPlatformType.XXL_JOB;
    }

    @Override
    public void validate(WorkflowDefinitionDTO definition) {
        WorkflowEngineBindingDTO binding = definition == null ? null : definition.getEngineBinding();
        if (binding == null || binding.getPlatformType() != EtlPlatformType.XXL_JOB) {
            throw new IllegalArgumentException("XXL-JOB 绑定信息不合法");
        }
        if (!StringUtils.hasText(binding.getExternalJobGroup())) {
            throw new IllegalArgumentException("XXL-JOB 需要配置执行器分组");
        }
        if (!StringUtils.hasText(binding.getExternalJobHandler())) {
            throw new IllegalArgumentException("XXL-JOB 需要配置任务处理器");
        }
    }

    @Override
    protected Map<String, Object> platformSpecificPayload(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowPlatformCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobGroup", definition == null || definition.getEngineBinding() == null ? null : definition.getEngineBinding().getExternalJobGroup());
        payload.put("jobHandler", definition == null || definition.getEngineBinding() == null ? null : definition.getEngineBinding().getExternalJobHandler());
        payload.put("executorRouteStrategy", "FIRST");
        payload.put("jobDesc", definition == null ? null : definition.getWorkflowName());
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
    protected String rawPauseStatus() {
        return "STOPPED";
    }

    @Override
    protected String rawResumeStatus() {
        return "RUNNING";
    }

    @Override
    protected String rawRetryStatus() {
        return "RUNNING";
    }
}
