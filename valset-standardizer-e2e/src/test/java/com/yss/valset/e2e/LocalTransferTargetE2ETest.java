package com.yss.valset.e2e;

import com.yss.valset.transfer.application.port.DeliverTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地目录投递端到端测试。
 */
@ActiveProfiles("e2e")
@SpringBootTest(
        classes = ValsetStandardizerBootApplicationTest.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.datasource.primary.url=jdbc:h2:mem:valset_standardizer_e2e_local_target;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.primary.username=sa",
                "spring.datasource.primary.password=",
                "spring.datasource.primary.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:valset_standardizer_e2e_local_target;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
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
public class LocalTransferTargetE2ETest {

    @Autowired
    private TransferTargetGateway transferTargetGateway;

    @Autowired
    private TransferObjectGateway transferObjectGateway;

    @Autowired
    private TransferRouteGateway transferRouteGateway;

    @Autowired
    private DeliverTransferUseCase deliverTransferUseCase;

    private Path sourceFile;
    private Path targetDir;

    @AfterEach
    void tearDown() throws Exception {
        if (sourceFile != null) {
            Files.deleteIfExists(sourceFile);
        }
        if (targetDir != null && Files.exists(targetDir)) {
            try (var stream = Files.walk(targetDir)) {
                stream.sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void should_deliver_attachment_to_local_directory_target() throws Exception {
        sourceFile = Files.createTempFile("valset-local-source-", ".txt");
        Files.writeString(sourceFile, "local delivery payload", StandardCharsets.UTF_8);
        targetDir = Files.createTempDirectory("valset-local-endpoint-");

        TransferTarget target = transferTargetGateway.save(new TransferTarget(
                null,
                "local-endpoint-e2e",
                "本地目录投递目标",
                TargetType.LOCAL_DIR,
                true,
                "archive/${yyyyMMdd}",
                Map.of(
                        "directory", targetDir.toString(),
                        "createParentDirectories", true
                ),
                Map.of("scenario", "local-endpoint-e2e"),
                Instant.now(),
                Instant.now()
        ));

        TransferObject transferObject = transferObjectGateway.save(new TransferObject(
                null,
                "9002",
                SourceType.LOCAL_DIR.name(),
                "local-source-e2e",
                sourceFile.getFileName().toString(),
                "txt",
                "text/plain",
                Files.size(sourceFile),
                "fingerprint-local-e2e",
                sourceFile.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sourceFile.toString(),
                TransferStatus.RECEIVED,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Map.of("sourceType", SourceType.LOCAL_DIR.name())
        ));

        TransferRoute route = transferRouteGateway.save(new TransferRoute(
                null,
                transferObject.sourceId(),
                SourceType.LOCAL_DIR,
                transferObject.sourceCode(),
                "10001",
                TargetType.LOCAL_DIR,
                target.targetCode(),
                "archive/${yyyyMMdd}",
                "copied-${fileName}",
                TransferStatus.ROUTED,
                new LinkedHashMap<>(Map.of(
                        "transferId", transferObject.transferId(),
                        "targetPath", "archive/${yyyyMMdd}",
                        "renamePattern", "copied-${fileName}",
                        "maxRetryCount", 1,
                        "retryDelaySeconds", 1
                ))
        ));

        deliverTransferUseCase.execute(route.routeId(), transferObject.transferId());

        Path expectedFile = targetDir
                .resolve("archive")
                .resolve(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE))
                .resolve("copied-" + sourceFile.getFileName())
                .normalize();
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(Files.readString(expectedFile, StandardCharsets.UTF_8)).isEqualTo("local delivery payload");
    }
}
