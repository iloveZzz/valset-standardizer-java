package com.yss.valset.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.yss.valset.ValsetStandardizerBootApplication;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TargetType;
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
 * 投递目标敏感配置保护端到端测试。
 */
@ActiveProfiles("e2e")
@SpringBootTest(
        classes = ValsetStandardizerBootApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.datasource.primary.url=jdbc:h2:mem:valset_standardizer_e2e_secret_target;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.primary.username=sa",
                "spring.datasource.primary.password=",
                "spring.datasource.primary.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:valset_standardizer_e2e_secret_target;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
public class TransferTargetSecretProtectionE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransferTargetGateway transferTargetGateway;

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
    void should_encrypt_password_in_database_and_hide_it_from_api() throws Exception {
        String targetCode = "email-secret-protection-e2e";
        Map<String, Object> connectionConfig = new java.util.LinkedHashMap<>();
        connectionConfig.put("host", "smtp.example.com");
        connectionConfig.put("port", 25);
        connectionConfig.put("username", "transfer@example.com");
        connectionConfig.put("password", "super-secret-123");
        connectionConfig.put("protocol", "smtp");
        connectionConfig.put("auth", true);
        connectionConfig.put("startTls", true);
        connectionConfig.put("ssl", false);
        connectionConfig.put("timeoutMillis", 10000);
        connectionConfig.put("from", "transfer@example.com");
        connectionConfig.put("to", "ops@example.com");
        APIResponse createResponse = requestContext.post(
                "/api/transfer-targets",
                RequestOptions.create().setData(Map.of(
                        "targetCode", targetCode,
                        "targetName", "邮件目标敏感配置测试",
                        "targetType", TargetType.EMAIL.name(),
                        "enabled", true,
                        "targetPathTemplate", "/mail/inbox",
                        "connectionConfig", connectionConfig,
                        "targetMeta", Map.of(
                                "usage", "secret-protection"
                        )
                ))
        );
        assertThat(createResponse.status()).isEqualTo(201);
        JsonNode created = readJson(createResponse).get("endpoint");
        assertThat(created.get("targetCode").asText()).isEqualTo(targetCode);
        assertThat(created.get("connectionConfig").get("password").asText()).isBlank();

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from t_transfer_target where target_code = ? and connection_config_json like ? and connection_config_json not like ?",
                Integer.class,
                targetCode,
                "%ENC:v1:%",
                "%super-secret-123%"
        );
        assertThat(count).isEqualTo(1);

        assertThat(transferTargetGateway.findById(created.get("targetId").asLong()))
                .isPresent()
                .get()
                .satisfies(target -> assertThat(target.connectionConfig().get("password")).isEqualTo("super-secret-123"));

        Map<String, Object> updateConnectionConfig = new java.util.LinkedHashMap<>();
        updateConnectionConfig.put("host", "smtp.example.com");
        updateConnectionConfig.put("port", 25);
        updateConnectionConfig.put("username", "transfer@example.com");
        updateConnectionConfig.put("password", "");
        updateConnectionConfig.put("protocol", "smtp");
        updateConnectionConfig.put("auth", true);
        updateConnectionConfig.put("startTls", true);
        updateConnectionConfig.put("ssl", false);
        updateConnectionConfig.put("timeoutMillis", 10000);
        updateConnectionConfig.put("from", "transfer@example.com");
        updateConnectionConfig.put("to", "ops@example.com");
        APIResponse updateWithSecretResponse = requestContext.put(
                "/api/transfer-targets/" + created.get("targetId").asLong(),
                RequestOptions.create().setData(new java.util.LinkedHashMap<>(Map.of(
                        "targetCode", targetCode,
                        "targetName", "邮件目标敏感配置测试-更新",
                        "targetType", TargetType.EMAIL.name(),
                        "enabled", true,
                        "targetPathTemplate", "/mail/inbox",
                        "connectionConfig", updateConnectionConfig,
                        "targetMeta", Map.of(
                                "usage", "secret-protection-updated"
                        )
                )))
        );
        assertThat(updateWithSecretResponse.status()).isEqualTo(200);
        JsonNode updated = readJson(updateWithSecretResponse).get("endpoint");
        assertThat(updated.get("connectionConfig").get("password").asText()).isBlank();
        assertThat(transferTargetGateway.findById(created.get("targetId").asLong()))
                .isPresent()
                .get()
                .satisfies(target -> assertThat(target.connectionConfig().get("password")).isEqualTo("super-secret-123"));
    }

    private JsonNode readJson(APIResponse response) throws Exception {
        return OBJECT_MAPPER.readTree(response.text());
    }
}
