package com.yss.valset.task.infrastructure.adapter.workflow;

import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * LITEFLOW 工作流执行平台适配器。
 */
@Component
public class LiteFlowWorkflowEngineAdapter implements WorkflowEngineAdapter {

    @Override
    public WorkflowEngineType engineType() {
        return WorkflowEngineType.LITEFLOW;
    }

    @Override
    public void validate(WorkflowExecutorBindingDTO binding) {
        validateType(binding);
    }

    @Override
    public void trigger(String workflowCode, String stageCode, Map<String, Object> context) {
        throw new UnsupportedOperationException("LITEFLOW 平台暂未接入执行能力");
    }

    @Override
    public void stop(String instanceId) {
        throw new UnsupportedOperationException("LITEFLOW 平台暂未接入执行能力");
    }

    @Override
    public void retry(String instanceId, String stageCode) {
        throw new UnsupportedOperationException("LITEFLOW 平台暂未接入执行能力");
    }

    @Override
    public Optional<Map<String, Object>> query(String instanceId) {
        return Optional.empty();
    }

    private void validateType(WorkflowExecutorBindingDTO binding) {
        if (binding == null) {
            throw new IllegalArgumentException("平台绑定不能为空");
        }
        if (!StringUtils.hasText(binding.getEngineType())) {
            throw new IllegalArgumentException("平台绑定执行类型不能为空");
        }
        if (!WorkflowEngineType.LITEFLOW.name().equalsIgnoreCase(binding.getEngineType())) {
            throw new IllegalArgumentException("适配器与平台类型不匹配：" + binding.getEngineType());
        }
    }
}
