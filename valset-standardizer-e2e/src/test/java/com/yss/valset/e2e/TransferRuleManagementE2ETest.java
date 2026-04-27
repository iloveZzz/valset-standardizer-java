package com.yss.valset.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由规则管理的端到端场景测试。
 */
@ActiveProfiles("e2e")
@SpringBootTest(
        classes = ValsetStandardizerBootApplicationTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.datasource.primary.url=jdbc:h2:mem:valset_standardizer_e2e_transfer_rule;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.primary.username=sa",
                "spring.datasource.primary.password=",
                "spring.datasource.primary.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:valset_standardizer_e2e_transfer_rule;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema-e2e.sql",
                "spring.liquibase.enabled=false",
                "subject.match.upload-dir=target/e2e/uploads",
                "subject.match.output-dir=target/e2e/output",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "yss.mybatis.mapper-scan=com.yss.valset.extract.repository.mapper,com.yss.valset.transfer.infrastructure.mapper",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
public class TransferRuleManagementE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransferRuleGateway transferRuleGateway;

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private APIRequestContext requestContext;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of(
                "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"
        )));
        requestContext = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL("http://127.0.0.1:" + port));
    }

    @AfterEach
    void tearDown() {
        if (requestContext != null) {
            requestContext.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    void should_manage_rule_crud_via_api() throws Exception {
        APIResponse templateNameResponse = requestContext.get("/api/transfer-rules/template-name");
        assertThat(templateNameResponse.status()).isEqualTo(200);
        assertThat(readJson(templateNameResponse).get("data").asText()).isEqualTo("transfer_rule");

        APIResponse createResponse = requestContext.post(
                "/api/transfer-rules",
                RequestOptions.create().setData(Map.of(
                        "ruleCode", "EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E",
                        "ruleName", "规则CRUD测试",
                        "ruleVersion", "1.0.0",
                        "enabled", true,
                        "priority", 12,
                        "matchStrategy", "SCRIPT_RULE",
                        "scriptLanguage", "qlexpress4",
                        "scriptBody", "sourceType != null && sourceType.name().equals(\"EMAIL\") && isExcel(fileName)",
                        "ruleMeta", Map.of(
                                "targetType", "FILESYS",
                                "targetCode", "finance-filesys-archive",
                                "targetPath", "/mail/${fileName}",
                                "renamePattern", "${mailSubject}_${fileName}",
                                "maxRetryCount", 3,
                                "retryDelaySeconds", 60
                        )
                ))
        );
        assertThat(createResponse.status()).isEqualTo(201);
        JsonNode createdRuleResponse = readJson(createResponse);
        JsonNode createdRule = createdRuleResponse.get("data").get("rule");
        assertThat(createdRule).isNotNull();
        String ruleId = createdRule.get("ruleId").asText();
        assertThat(ruleId).isNotNull();
        assertThat(createdRule.get("formTemplateName").asText()).isEqualTo("transfer_rule");
        assertNoGroupingKeys(createdRule.get("ruleMeta"));

        assertThat(transferRuleGateway.findById(ruleId)).isPresent();
        Integer ruleCount = jdbcTemplate.queryForObject(
                "select count(*) from t_transfer_rule where rule_code = ?",
                Integer.class,
                "EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E"
        );
        assertThat(ruleCount).isEqualTo(1);

        APIResponse getResponse = requestContext.get("/api/transfer-rules/" + ruleId);
        assertThat(getResponse.status()).isEqualTo(200);
        JsonNode detail = readJson(getResponse).get("data");
        assertThat(detail.get("ruleCode").asText()).isEqualTo("EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E");
        assertNoGroupingKeys(detail.get("ruleMeta"));

        APIResponse listResponse = requestContext.get(
                "/api/transfer-rules",
                RequestOptions.create()
                        .setQueryParam("ruleCode", "EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E")
                        .setQueryParam("enabled", "true")
                        .setQueryParam("limit", "10")
        );
        assertThat(listResponse.status()).isEqualTo(200);
        JsonNode listJson = readJson(listResponse).get("data");
        assertThat(listJson.isArray()).isTrue();
        assertThat(listJson).hasSize(1);
        assertThat(listJson.get(0).get("ruleId").asText()).isEqualTo(ruleId);

        APIResponse updateResponse = requestContext.put(
                "/api/transfer-rules/" + ruleId,
                RequestOptions.create().setData(Map.of(
                        "ruleCode", "EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E",
                        "ruleName", "按发件人分组路由测试-更新",
                        "ruleVersion", "1.0.1",
                        "enabled", false,
                        "priority", 18,
                        "matchStrategy", "SCRIPT_RULE",
                        "scriptLanguage", "qlexpress4",
                        "scriptBody", "sourceType != null && sourceType.name().equals(\"EMAIL\") && containsIgnoreCase(subject, \"日报\")",
                        "ruleMeta", Map.of(
                                "targetType", "FILESYS",
                                "targetCode", "ops-filesys-archive",
                                "targetPath", "/mail/${fileName}",
                                "renamePattern", "${mailSubject}_${fileName}",
                                "maxRetryCount", 5,
                                "retryDelaySeconds", 120
                        )
                ))
        );
        assertThat(updateResponse.status()).isEqualTo(200);
        JsonNode updatedRule = readJson(updateResponse).get("data").get("rule");
        assertThat(updatedRule.get("enabled").asBoolean()).isFalse();
        assertThat(updatedRule.get("priority").asInt()).isEqualTo(18);
        assertThat(updatedRule.get("scriptBody").asText())
                .isEqualTo("sourceType != null && sourceType.name().equals(\"EMAIL\") && containsIgnoreCase(subject, \"日报\")");
        assertNoGroupingKeys(updatedRule.get("ruleMeta"));
        String storedScriptBody = jdbcTemplate.queryForObject(
                "select script_body from t_transfer_rule where rule_id = ?",
                String.class,
                ruleId
        );
        assertThat(storedScriptBody)
                .isEqualTo("sourceType != null && sourceType.name().equals(\"EMAIL\") && containsIgnoreCase(subject, \"日报\")");

        APIResponse disabledListResponse = requestContext.get(
                "/api/transfer-rules",
                RequestOptions.create()
                        .setQueryParam("ruleCode", "EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E")
                        .setQueryParam("enabled", "false")
                        .setQueryParam("limit", "10")
        );
        assertThat(disabledListResponse.status()).isEqualTo(200);
        JsonNode disabledListJson = readJson(disabledListResponse).get("data");
        assertThat(disabledListJson).hasSize(1);
        assertThat(disabledListJson.get(0).get("enabled").asBoolean()).isFalse();

        APIResponse deleteResponse = requestContext.delete("/api/transfer-rules/" + ruleId);
        assertThat(deleteResponse.status()).isEqualTo(200);
        JsonNode deletedRule = readJson(deleteResponse).get("data").get("rule");
        assertThat(deletedRule.get("ruleId").asText()).isEqualTo(ruleId);

        APIResponse afterDeleteResponse = requestContext.get("/api/transfer-rules/" + ruleId);
        assertThat(afterDeleteResponse.status()).isEqualTo(400);
        assertThat(transferRuleGateway.findById(ruleId)).isEmpty();
        Integer afterDeleteCount = jdbcTemplate.queryForObject(
                "select count(*) from t_transfer_rule where rule_code = ?",
                Integer.class,
                "EMAIL_ATTACHMENT_GROUP_BY_SENDER_E2E"
        );
        assertThat(afterDeleteCount).isZero();
    }

    private JsonNode readJson(APIResponse response) throws Exception {
        return OBJECT_MAPPER.readTree(response.text());
    }

    private static void assertNoGroupingKeys(JsonNode ruleMeta) {
        assertThat(ruleMeta.has("groupStrategy")).isFalse();
        assertThat(ruleMeta.has("groupField")).isFalse();
        assertThat(ruleMeta.has("groupTargetMapping")).isFalse();
        assertThat(ruleMeta.has("groupExpression")).isFalse();
        assertThat(ruleMeta.has("regGroup")).isFalse();
    }
}
