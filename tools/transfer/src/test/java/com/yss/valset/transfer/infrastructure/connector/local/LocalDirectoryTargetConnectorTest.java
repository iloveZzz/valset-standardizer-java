package com.yss.valset.transfer.infrastructure.connector.local;

import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地目录投递连接器测试。
 */
class LocalDirectoryTargetConnectorTest {

    private final LocalDirectoryTargetConnector connector = new LocalDirectoryTargetConnector();

    @Test
    void should_copy_source_file_to_local_target_directory() throws Exception {
        Path sourceFile = Files.createTempFile("valset-local-target-source-", ".txt");
        Path targetDir = Files.createTempDirectory("valset-local-target-output-");
        Files.writeString(sourceFile, "local target payload", StandardCharsets.UTF_8);

        Map<String, Object> connectionConfig = new LinkedHashMap<>();
        connectionConfig.put("directory", targetDir.toString());
        connectionConfig.put("createParentDirectories", true);
        TransferTarget target = new TransferTarget(
                101L,
                "local-target",
                "本地目录目标",
                TargetType.LOCAL_DIR,
                true,
                "archive/${yyyyMMdd}",
                connectionConfig,
                Map.of("scenario", "local-target-test"),
                Instant.now(),
                Instant.now()
        );
        Map<String, Object> routeMeta = new LinkedHashMap<>();
        routeMeta.put("targetPath", "route-out/${yyyyMMdd}");
        routeMeta.put("renamePattern", "copied-${fileName}");
        TransferRoute route = new TransferRoute(
                "route-001",
                "source-001",
                null,
                "source-code",
                "rule-001",
                TargetType.LOCAL_DIR,
                "local-target",
                "route-out/${yyyyMMdd}",
                "copied-${fileName}",
                TransferStatus.ROUTED,
                routeMeta
        );
        TransferObject transferObject = new TransferObject(
                "transfer-001",
                "source-001",
                "LOCAL_DIR",
                "source-code",
                sourceFile.getFileName().toString(),
                "txt",
                "text/plain",
                Files.size(sourceFile),
                "fingerprint-001",
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
                "route-001",
                null,
                new ProbeResult(true, "LOCAL_FILE", Map.of("sourceType", "LOCAL_DIR")),
                Map.of()
        );
        TransferContext context = new TransferContext(transferObject, route, target, routeMeta);

        TransferResult result = connector.send(context);

        assertThat(result.success()).isTrue();
        Path expectedFile = targetDir
                .resolve("route-out")
                .resolve(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
                .resolve("copied-" + sourceFile.getFileName())
                .normalize();
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(Files.readString(expectedFile, StandardCharsets.UTF_8)).isEqualTo("local target payload");
    }

    @Test
    void should_expand_home_directory_for_local_target_path() throws Exception {
        Path sourceFile = Files.createTempFile("valset-local-target-source-", ".txt");
        Path fakeHome = Files.createTempDirectory("valset-local-target-home-");
        Path expectedRoot = fakeHome.resolve(".tmp").resolve("yss-transfer").resolve("outbox");
        String previousHome = System.getProperty("user.home");
        Files.writeString(sourceFile, "local target payload", StandardCharsets.UTF_8);

        try {
            System.setProperty("user.home", fakeHome.toString());

            Map<String, Object> connectionConfig = new LinkedHashMap<>();
            connectionConfig.put("directory", "~/.tmp/yss-transfer/outbox/");
            connectionConfig.put("createParentDirectories", true);
            TransferTarget target = new TransferTarget(
                    102L,
                    "local-target-home",
                    "本地目录目标",
                    TargetType.LOCAL_DIR,
                    true,
                    "~/.tmp/yss-transfer/outbox/",
                    connectionConfig,
                    Map.of("scenario", "local-target-home-test"),
                    Instant.now(),
                    Instant.now()
            );
            TransferRoute route = new TransferRoute(
                    "route-002",
                    "source-002",
                    null,
                    "source-code",
                    "rule-002",
                    TargetType.LOCAL_DIR,
                    "local-target-home",
                    null,
                    null,
                    TransferStatus.ROUTED,
                    Map.of()
            );
            TransferObject transferObject = new TransferObject(
                    "transfer-002",
                    "source-002",
                    "LOCAL_DIR",
                    "source-code",
                    sourceFile.getFileName().toString(),
                    "txt",
                    "text/plain",
                    Files.size(sourceFile),
                    "fingerprint-002",
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
                    "route-002",
                    null,
                    new ProbeResult(true, "LOCAL_FILE", Map.of("sourceType", "LOCAL_DIR")),
                    Map.of()
            );
            TransferContext context = new TransferContext(transferObject, route, target, Map.of());

            TransferResult result = connector.send(context);

            assertThat(result.success()).isTrue();
            Path expectedFile = expectedRoot.resolve(sourceFile.getFileName()).normalize();
            assertThat(Files.exists(expectedFile)).isTrue();
            assertThat(Files.readString(expectedFile, StandardCharsets.UTF_8)).isEqualTo("local target payload");
        } finally {
            if (previousHome != null) {
                System.setProperty("user.home", previousHome);
            }
            if (Files.exists(fakeHome)) {
                try (var stream = Files.walk(fakeHome)) {
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
    }
}
