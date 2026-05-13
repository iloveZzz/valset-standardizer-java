package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferSourceUpsertCommand;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.infrastructure.convertor.TransferSecretCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultTransferSourceManagementAppServiceTest {

    private record ServiceFixture(
            DefaultTransferSourceManagementAppService service,
            TransferSourceGateway transferSourceGateway
    ) {
    }

    private Path uploadRoot;

    @BeforeEach
    void setUp() throws IOException {
        uploadRoot = Files.createTempDirectory("transfer-http-upload-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (uploadRoot == null || !Files.exists(uploadRoot)) {
            return;
        }
        try (var stream = Files.walk(uploadRoot)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 测试清理失败不影响断言结果。
                }
            });
        }
    }

    @Test
    void upsertSourceShouldRejectEnabledToggleWhenEnabledRoutesExist() {
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferSourceCheckpointGateway transferSourceCheckpointGateway = mock(TransferSourceCheckpointGateway.class);
        TransferSecretCodec transferSecretCodec = mock(TransferSecretCodec.class);
        TransferSourceScheduleCoordinator transferSourceScheduleCoordinator = mock(TransferSourceScheduleCoordinator.class);
        com.yss.valset.transfer.application.port.TransferJobScheduler transferJobScheduler =
                mock(com.yss.valset.transfer.application.port.TransferJobScheduler.class);
        DefaultTransferSourceManagementAppService service = new DefaultTransferSourceManagementAppService(
                transferSourceGateway,
                transferRouteGateway,
                transferSourceCheckpointGateway,
                transferSecretCodec,
                transferSourceScheduleCoordinator,
                transferJobScheduler
        );

        TransferSource existing = new TransferSource(
                "1",
                "source-1",
                "来源1",
                SourceType.EMAIL,
                true,
                null,
                Map.of(),
                Map.of(),
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        when(transferSourceGateway.findById("1")).thenReturn(Optional.of(existing));
        when(transferRouteGateway.countEnabledBySourceId("1")).thenReturn(2L);

        TransferSourceUpsertCommand command = new TransferSourceUpsertCommand();
        command.setSourceId("1");
        command.setSourceCode("source-1");
        command.setSourceName("来源1");
        command.setSourceType(SourceType.EMAIL.name());
        command.setEnabled(false);
        command.setConnectionConfig(Map.of());
        command.setSourceMeta(Map.of());

        assertThatThrownBy(() -> service.upsertSource(command))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode().value())
                            .isEqualTo(HttpStatus.BAD_REQUEST.value());
                })
                .hasMessageContaining("无法启用或停用来源");

        verifyNoInteractions(transferSourceScheduleCoordinator, transferJobScheduler, transferSourceCheckpointGateway);
    }

    @Test
    void uploadFilesShouldStoreSingleHttpFile() throws Exception {
        ServiceFixture fixture = newService();
        DefaultTransferSourceManagementAppService service = fixture.service();
        TransferSourceGateway transferSourceGateway = fixture.transferSourceGateway();
        TransferSource source = httpSource(false, Map.of("allowMultipleFiles", true, "limit", 10));
        when(transferSourceGateway.findById("http-source-1")).thenReturn(Optional.of(source));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "单文件上传.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "hello".getBytes()
        );

        service.uploadFiles("http-source-1", List.of(file));

        Path sourceRoot = uploadRoot.resolve("http").resolve(source.sourceId());
        assertThat(Files.exists(sourceRoot)).isTrue();
        List<Path> storedFiles = collectRegularFiles(sourceRoot);
        assertThat(storedFiles).hasSize(1);
        assertThat(storedFiles.get(0).getFileName().toString()).isEqualTo("单文件上传.xlsx");
        assertThat(collectChildDirectories(sourceRoot)).isEmpty();
    }

    @Test
    void uploadFilesShouldStoreMultipleHttpFiles() throws Exception {
        ServiceFixture fixture = newService();
        DefaultTransferSourceManagementAppService service = fixture.service();
        TransferSourceGateway transferSourceGateway = fixture.transferSourceGateway();
        TransferSource source = httpSource(false, Map.of("allowMultipleFiles", true, "limit", 10));
        when(transferSourceGateway.findById("http-source-2")).thenReturn(Optional.of(source));

        MockMultipartFile first = new MockMultipartFile("files", "first.xlsx", "application/octet-stream", "one".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "second.xlsx", "application/octet-stream", "two".getBytes());

        service.uploadFiles("http-source-2", List.of(first, second));

        Path sourceRoot = uploadRoot.resolve("http").resolve(source.sourceId());
        List<Path> storedFiles = collectRegularFiles(sourceRoot);
        assertThat(storedFiles).hasSize(2);
        assertThat(storedFiles.stream().map(path -> path.getFileName().toString()).toList())
                .containsExactlyInAnyOrder("first.xlsx", "second.xlsx");
        assertThat(collectChildDirectories(sourceRoot)).isEmpty();
    }

    @Test
    void uploadFilesShouldRejectMultipleFilesWhenDisabled() throws Exception {
        ServiceFixture fixture = newService();
        DefaultTransferSourceManagementAppService service = fixture.service();
        TransferSourceGateway transferSourceGateway = fixture.transferSourceGateway();
        TransferSource source = httpSource(false, Map.of("allowMultipleFiles", false, "limit", 10));
        when(transferSourceGateway.findById("http-source-3")).thenReturn(Optional.of(source));

        MockMultipartFile first = new MockMultipartFile("files", "first.xlsx", "application/octet-stream", "one".getBytes());
        MockMultipartFile second = new MockMultipartFile("files", "second.xlsx", "application/octet-stream", "two".getBytes());

        assertThatThrownBy(() -> service.uploadFiles("http-source-3", List.of(first, second)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                })
                .hasMessageContaining("仅允许上传单个文件");
    }

    private ServiceFixture newService() throws Exception {
        TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
        TransferRouteGateway transferRouteGateway = mock(TransferRouteGateway.class);
        TransferSourceCheckpointGateway transferSourceCheckpointGateway = mock(TransferSourceCheckpointGateway.class);
        TransferSecretCodec transferSecretCodec = mock(TransferSecretCodec.class);
        TransferSourceScheduleCoordinator transferSourceScheduleCoordinator = mock(TransferSourceScheduleCoordinator.class);
        com.yss.valset.transfer.application.port.TransferJobScheduler transferJobScheduler =
                mock(com.yss.valset.transfer.application.port.TransferJobScheduler.class);
        DefaultTransferSourceManagementAppService service = new DefaultTransferSourceManagementAppService(
                transferSourceGateway,
                transferRouteGateway,
                transferSourceCheckpointGateway,
                transferSecretCodec,
                transferSourceScheduleCoordinator,
                transferJobScheduler
        );
        Field uploadRootField = DefaultTransferSourceManagementAppService.class.getDeclaredField("uploadRoot");
        uploadRootField.setAccessible(true);
        uploadRootField.set(service, uploadRoot.toString());
        return new ServiceFixture(service, transferSourceGateway);
    }

    private TransferSource httpSource(boolean enabled, Map<String, Object> connectionConfig) {
        return new TransferSource(
                UUID.randomUUID().toString(),
                "http-source-code",
                "HTTP 来源",
                SourceType.HTTP,
                enabled,
                null,
                connectionConfig,
                Map.of(),
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
    }

    private List<Path> collectRegularFiles(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
    }

    private List<Path> collectChildDirectories(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.list(root)) {
            return stream.filter(Files::isDirectory).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
    }
}
