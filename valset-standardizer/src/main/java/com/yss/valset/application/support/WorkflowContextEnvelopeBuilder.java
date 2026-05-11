package com.yss.valset.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.dto.workflow.WorkflowContextKeys;
import com.yss.valset.application.dto.workflow.WorkflowContextPayloadSupport;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流上下文信封构建器。
 *
 * <p>
 * 用于把公共上下文和业务上下文打包成统一信封，便于存库、透传和后续再拆包。
 * </p>
 */
@Component
public class WorkflowContextEnvelopeBuilder {

    private final ObjectMapper objectMapper;

    public WorkflowContextEnvelopeBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建结构化信封对象。
     */
    public Map<String, Object> buildEnvelope(Map<String, Object> commonContext, Map<String, Object> businessContext) {
        return WorkflowContextPayloadSupport.buildEnvelope(commonContext, businessContext);
    }

    /**
     * 将信封序列化为 JSON，失败时退回为字符串形式，避免上下文丢失。
     */
    public String buildEnvelopeJson(Map<String, Object> commonContext, Map<String, Object> businessContext) {
        Map<String, Object> envelope = buildEnvelope(commonContext, businessContext);
        if (envelope.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            return envelope.toString();
        }
    }
}
