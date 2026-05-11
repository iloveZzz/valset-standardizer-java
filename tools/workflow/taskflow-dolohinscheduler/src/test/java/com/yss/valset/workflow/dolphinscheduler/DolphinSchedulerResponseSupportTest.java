package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DolphinSchedulerResponseSupportTest {

    @Test
    void shouldParseJsonResponseBody() {
        DolphinSchedulerResponseSupport support = new DolphinSchedulerResponseSupport();
        support.setObjectMapper(new ObjectMapper());

        JsonNode response = support.toJsonNode("""
                {"code":0,"msg":"success","data":{"totalCount":7,"workflowInstanceStatusCounts":[{"state":"SUCCESS","count":4}]}}
                """);

        assertThat(response.path("data").path("totalCount").asInt()).isEqualTo(7);
        assertThat(response.path("data").path("workflowInstanceStatusCounts").isArray()).isTrue();
    }

    @Test
    void shouldReturnEmptySuccessNodeForBlankResponse() {
        DolphinSchedulerResponseSupport support = new DolphinSchedulerResponseSupport();
        support.setObjectMapper(new ObjectMapper());

        JsonNode response = support.toJsonNode("   ");

        assertThat(response.path("code").asInt()).isZero();
        assertThat(response.path("msg").asText()).isEqualTo("success");
        assertThat(response.path("data").isObject()).isTrue();
    }
}
