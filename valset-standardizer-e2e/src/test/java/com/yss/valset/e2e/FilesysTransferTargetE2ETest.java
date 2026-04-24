package com.yss.valset.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.yss.filesys.feignsdk.client.YssFilesysTransferFeignClient;
import com.yss.filesys.feignsdk.dto.YssFilesysCheckUploadResultDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysFileRecordDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysTransferTaskDTO;
import com.yss.filesys.feignsdk.dto.YssFilesysUploadFlowResult;
import com.yss.filesys.feignsdk.properties.YssFilesysFeignSdkProperties;
import com.yss.filesys.feignsdk.service.YssFilesysTransferSdkService;
import com.yss.filesys.feignsdk.service.YssFilesysUploadFlowService;
import com.yss.valset.transfer.application.port.DeliverTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * filesys 目标创建与投递的端到端场景测试。
 */
@ActiveProfiles("e2e")
@Import(FilesysTransferTargetE2ETest.TestSupportConfiguration.class)
@SpringBootTest(
        classes = ValsetStandardizerBootApplicationTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.datasource.primary.url=jdbc:h2:mem:valset_standardizer_e2e_filesys;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.primary.username=sa",
                "spring.datasource.primary.password=",
                "spring.datasource.primary.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:valset_standardizer_e2e_filesys;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
                "yss.filesys.feignsdk.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
public class FilesysTransferTargetE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransferTargetGateway transferTargetGateway;

    @Autowired
    private TransferObjectGateway transferObjectGateway;

    @Autowired
    private TransferRouteGateway transferRouteGateway;

    @Autowired
    private DeliverTransferUseCase deliverTransferUseCase;

    @Autowired
    private RecordingFilesysUploadFlowService recordingFilesysUploadFlowService;

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private APIRequestContext requestContext;
    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of(
                "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"
        )));
        requestContext = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL("http://127.0.0.1:" + port));
        tempFile = Files.createTempFile("valset-filesys-e2e", ".txt");
        Files.writeString(tempFile, "filesys e2e payload", StandardCharsets.UTF_8);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (requestContext != null) {
            requestContext.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void should_create_filesys_target_and_deliver_attachment_to_filesys_storage() throws Exception {
        String targetCode = "filesys-e2e-target";
        Map<String, Object> connectionConfig = new LinkedHashMap<>();
        connectionConfig.put("parentId", "parent-e2e-001");
        connectionConfig.put("storageSettingId", "storage-e2e-001");
        connectionConfig.put("chunkSize", 4096);
        Map<String, Object> targetMeta = Map.of(
                "usage", "filesys-e2e",
                "scenario", "target-create-and-deliver"
        );

        APIResponse createResponse = requestContext.post(
                "/api/transfer-targets",
                RequestOptions.create().setData(Map.of(
                        "targetCode", targetCode,
                        "targetName", "filesys 目标测试",
                        "targetType", TargetType.FILESYS.name(),
                        "enabled", true,
                        "targetPathTemplate", "/archive/${yyyyMMdd}",
                        "connectionConfig", connectionConfig,
                        "targetMeta", targetMeta
                ))
        );
        assertThat(createResponse.status()).isEqualTo(201);
        JsonNode createJson = readJson(createResponse);
        JsonNode targetJson = createJson.get("target");
        assertThat(targetJson).isNotNull();
        Long targetId = targetJson.get("targetId").asLong();
        assertThat(targetId).isNotNull();
        assertThat(targetJson.get("targetType").asText()).isEqualTo(TargetType.FILESYS.name());
        assertThat(targetJson.get("connectionConfig").get("parentId").asText()).isEqualTo("parent-e2e-001");

        APIResponse listResponse = requestContext.get(
                "/api/transfer-targets",
                RequestOptions.create()
                        .setQueryParam("targetType", TargetType.FILESYS.name())
                        .setQueryParam("targetCode", targetCode)
                        .setQueryParam("enabled", "true")
                        .setQueryParam("limit", "10")
        );
        assertThat(listResponse.status()).isEqualTo(200);
        JsonNode listJson = readJson(listResponse);
        assertThat(listJson.isArray()).isTrue();
        assertThat(listJson).hasSize(1);
        assertThat(listJson.get(0).get("targetCode").asText()).isEqualTo(targetCode);
        assertThat(transferTargetGateway.findById(targetId)).isPresent();

        String fingerprint = "e2e-filesys-" + tempFile.getFileName();
        Map<String, Object> fileMeta = new LinkedHashMap<>();
        fileMeta.put("sourceType", SourceType.LOCAL_DIR.name());
        fileMeta.put("sourceCode", "e2e-local");
        fileMeta.put("scenario", "filesys-target");

        TransferObject transferObject = transferObjectGateway.save(new TransferObject(
                null,
                "9001",
                SourceType.LOCAL_DIR.name(),
                "e2e-local",
                tempFile.getFileName().toString(),
                "txt",
                "text/plain",
                Files.size(tempFile),
                fingerprint,
                "LOCAL_DIR:e2e-local:" + tempFile.getFileName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                tempFile.toString(),
                TransferStatus.RECEIVED,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                fileMeta
        ));
        assertThat(transferObject.transferId()).isNotNull();

        TransferRoute route = transferRouteGateway.save(new TransferRoute(
                null,
                transferObject.sourceId(),
                SourceType.valueOf(transferObject.sourceType()),
                transferObject.sourceCode(),
                "10001",
                TargetType.FILESYS,
                targetCode,
                "/archive/${yyyyMMdd}",
                null,
                TransferStatus.ROUTED,
                Map.of(
                        "transferId", transferObject.transferId(),
                        "maxRetryCount", 1,
                        "retryDelaySeconds", 1,
                        "scenario", "filesys-target"
                )
        ));
        assertThat(route.routeId()).isNotNull();

        deliverTransferUseCase.execute(route.routeId(), transferObject.transferId());

        RecordingFilesysUploadFlowService.Invocation invocation = recordingFilesysUploadFlowService.lastInvocation();
        assertThat(invocation).isNotNull();
        assertThat(invocation.fileName()).isEqualTo(tempFile.getFileName().toString());
        assertThat(invocation.mimeType()).isEqualTo("text/plain");
        assertThat(invocation.parentId()).isEqualTo("parent-e2e-001");
        assertThat(invocation.storageSettingId()).isEqualTo("storage-e2e-001");
        assertThat(invocation.chunkSize()).isEqualTo(4096L);
        assertThat(new String(invocation.content(), StandardCharsets.UTF_8)).isEqualTo("filesys e2e payload");

        Integer deliveryCount = jdbcTemplate.queryForObject(
                "select count(*) from t_transfer_delivery_record where route_id = ? and target_code = ? and execute_status = ?",
                Integer.class,
                route.routeId(),
                targetCode,
                "SUCCESS"
        );
        assertThat(deliveryCount).isEqualTo(1);

        String responseSnapshot = jdbcTemplate.queryForObject(
                "select response_snapshot_json from t_transfer_delivery_record where route_id = ? order by delivery_id desc limit 1",
                String.class,
                route.routeId()
        );
        assertThat(responseSnapshot).contains("yss-filesys 上传成功");
        assertThat(responseSnapshot).contains("fileId=e2e-file-001");
        assertThat(responseSnapshot).contains("storageSettingId=storage-e2e-001");
    }

    @Test
    void should_manage_filesys_target_crud_via_api() throws Exception {
        String targetCode = "filesys-crud-target";
        APIResponse createResponse = requestContext.post(
                "/api/transfer-targets",
                RequestOptions.create().setData(Map.of(
                        "targetCode", targetCode,
                        "targetName", "filesys CRUD 测试",
                        "targetType", TargetType.FILESYS.name(),
                        "enabled", true,
                        "targetPathTemplate", "/crud/${yyyyMMdd}",
                        "connectionConfig", Map.of(
                                "parentId", "crud-parent-001",
                                "storageSettingId", "crud-storage-001",
                                "chunkSize", 2048
                        ),
                        "targetMeta", Map.of(
                                "usage", "filesys-crud",
                                "scenario", "crud"
                        )
                ))
        );
        assertThat(createResponse.status()).isEqualTo(201);
        JsonNode created = readJson(createResponse).get("target");
        assertThat(created).isNotNull();
        Long targetId = created.get("targetId").asLong();
        assertThat(targetId).isNotNull();
        assertThat(created.get("targetType").asText()).isEqualTo(TargetType.FILESYS.name());

        APIResponse getResponse = requestContext.get("/api/transfer-targets/" + targetId);
        assertThat(getResponse.status()).isEqualTo(200);
        JsonNode detail = readJson(getResponse);
        assertThat(detail.get("targetCode").asText()).isEqualTo(targetCode);
        assertThat(detail.get("connectionConfig").get("storageSettingId").asText()).isEqualTo("crud-storage-001");

        APIResponse updateResponse = requestContext.put(
                "/api/transfer-targets/" + targetId,
                RequestOptions.create().setData(Map.of(
                        "targetCode", targetCode,
                        "targetName", "filesys CRUD 测试-更新",
                        "targetType", TargetType.FILESYS.name(),
                        "enabled", false,
                        "targetPathTemplate", "/crud-updated/${yyyyMMdd}",
                        "connectionConfig", Map.of(
                                "parentId", "crud-parent-002",
                                "storageSettingId", "crud-storage-002",
                                "chunkSize", 8192
                        ),
                        "targetMeta", Map.of(
                                "usage", "filesys-crud",
                                "scenario", "crud-updated"
                        )
                ))
        );
        assertThat(updateResponse.status()).isEqualTo(200);
        JsonNode updated = readJson(updateResponse).get("target");
        assertThat(updated.get("enabled").asBoolean()).isFalse();
        assertThat(updated.get("connectionConfig").get("storageSettingId").asText()).isEqualTo("crud-storage-002");

        APIResponse listResponse = requestContext.get(
                "/api/transfer-targets",
                RequestOptions.create()
                        .setQueryParam("targetType", TargetType.FILESYS.name())
                        .setQueryParam("targetCode", targetCode)
                        .setQueryParam("limit", "10")
        );
        assertThat(listResponse.status()).isEqualTo(200);
        JsonNode listJson = readJson(listResponse);
        assertThat(listJson).isNotEmpty();
        assertThat(listJson.get(0).get("targetId").asLong()).isEqualTo(targetId);

        APIResponse deleteResponse = requestContext.delete("/api/transfer-targets/" + targetId);
        assertThat(deleteResponse.status()).isEqualTo(200);
        JsonNode deleted = readJson(deleteResponse).get("target");
        assertThat(deleted.get("targetId").asLong()).isEqualTo(targetId);

        APIResponse afterDeleteResponse = requestContext.get("/api/transfer-targets/" + targetId);
        assertThat(afterDeleteResponse.status()).isEqualTo(500);
        assertThat(transferTargetGateway.findById(targetId)).isEmpty();
    }

    private JsonNode readJson(APIResponse response) throws Exception {
        return OBJECT_MAPPER.readTree(response.text());
    }

    @TestConfiguration
    static class TestSupportConfiguration {

        @Bean
        @Primary
        YssFilesysTransferFeignClient yssFilesysTransferFeignClient() {
            return RecordingFilesysUploadFlowService.fakeFeignClient();
        }

        @Bean
        @Primary
        YssFilesysFeignSdkProperties yssFilesysFeignSdkProperties() {
            YssFilesysFeignSdkProperties properties = new YssFilesysFeignSdkProperties();
            properties.setEnabled(false);
            properties.setDefaultChunkSize(4096L);
            return properties;
        }

        @Bean
        @Primary
        RecordingFilesysUploadFlowService recordingFilesysUploadFlowService(YssFilesysFeignSdkProperties properties) {
            return new RecordingFilesysUploadFlowService(properties);
        }
    }

    static class RecordingFilesysUploadFlowService extends YssFilesysUploadFlowService {

        private final AtomicReference<Invocation> lastInvocation = new AtomicReference<>();

        RecordingFilesysUploadFlowService(YssFilesysFeignSdkProperties properties) {
            super(new YssFilesysTransferSdkService(fakeFeignClient(), properties), properties);
        }

        @Override
        public YssFilesysUploadFlowResult upload(byte[] content,
                                                 String fileName,
                                                 String mimeType,
                                                 String parentId,
                                                 String storageSettingId,
                                                 long chunkSize) {
            byte[] safeContent = content == null ? new byte[0] : content.clone();
            lastInvocation.set(new Invocation(safeContent, fileName, mimeType, parentId, storageSettingId, chunkSize));
            return YssFilesysUploadFlowResult.builder()
                    .taskId("e2e-task-001")
                    .instantUpload(true)
                    .transferTask(YssFilesysTransferTaskDTO.builder()
                            .taskId("e2e-task-001")
                            .fileName(fileName)
                            .fileSize((long) safeContent.length)
                            .totalChunks(1)
                            .uploadedChunks(1)
                            .fileMd5("e2e-md5")
                            .status("DONE")
                            .taskType("UPLOAD")
                            .build())
                    .checkResult(YssFilesysCheckUploadResultDTO.builder()
                            .instantUpload(true)
                            .taskId("e2e-task-001")
                            .status("DONE")
                            .message("instant-upload")
                            .build())
                    .fileRecord(YssFilesysFileRecordDTO.builder()
                            .fileId("e2e-file-001")
                            .objectKey("archive/e2e/file.txt")
                            .originalName(fileName)
                            .displayName(fileName)
                            .suffix("txt")
                            .size((long) safeContent.length)
                            .mimeType(mimeType)
                            .isDir(false)
                            .parentId(parentId)
                            .storageSettingId(storageSettingId)
                            .build())
                    .build();
        }

        Invocation lastInvocation() {
            return lastInvocation.get();
        }

        static YssFilesysTransferFeignClient fakeFeignClient() {
            return (YssFilesysTransferFeignClient) Proxy.newProxyInstance(
                    YssFilesysTransferFeignClient.class.getClassLoader(),
                    new Class<?>[]{YssFilesysTransferFeignClient.class},
                    (proxy, method, args) -> {
                        Class<?> returnType = method.getReturnType();
                        if (returnType.equals(boolean.class)) {
                            return false;
                        }
                        if (returnType.equals(int.class) || returnType.equals(short.class) || returnType.equals(byte.class)) {
                            return 0;
                        }
                        if (returnType.equals(long.class)) {
                            return 0L;
                        }
                        if (returnType.equals(double.class)) {
                            return 0D;
                        }
                        if (returnType.equals(float.class)) {
                            return 0F;
                        }
                        return null;
                    }
            );
        }

        record Invocation(
                byte[] content,
                String fileName,
                String mimeType,
                String parentId,
                String storageSettingId,
                long chunkSize
        ) {
        }
    }
}
