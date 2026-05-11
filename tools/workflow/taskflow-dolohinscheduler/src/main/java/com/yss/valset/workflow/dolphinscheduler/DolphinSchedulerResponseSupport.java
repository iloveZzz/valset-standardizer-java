package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * DolphinScheduler 响应解析辅助器。
 */
@Component
public class DolphinSchedulerResponseSupport {

    @Getter
    @Setter
    private ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode toJsonNode(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return emptySuccessNode();
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("解析 DolphinScheduler 返回值失败：" + exception.getMessage(), exception);
        }
    }

    public JsonNode emptySuccessNode() {
        return objectMapper.createObjectNode()
                .put("code", 0)
                .put("msg", "success")
                .set("data", objectMapper.createObjectNode());
    }
}
