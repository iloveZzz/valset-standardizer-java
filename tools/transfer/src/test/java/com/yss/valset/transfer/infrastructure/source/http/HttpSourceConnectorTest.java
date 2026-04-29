package com.yss.valset.transfer.infrastructure.source.http;

import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class HttpSourceConnectorTest {

    private Path tempHome;
    private String originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        originalUserHome = System.getProperty("user.home");
        tempHome = Files.createTempDirectory("http-source-connector-test");
        System.setProperty("user.home", tempHome.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        if (tempHome == null || !Files.exists(tempHome)) {
            return;
        }
        try (var stream = Files.walk(tempHome)) {
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
    void fetchShouldIgnoreScanCursorAndReturnAllTopLevelHttpFiles() throws Exception {
        TransferSourceCheckpointGateway checkpointGateway = mock(TransferSourceCheckpointGateway.class);
        TransferSourceGateway sourceGateway = mock(TransferSourceGateway.class);
        HttpSourceConnector connector = new HttpSourceConnector(checkpointGateway, sourceGateway);

        TransferSource source = new TransferSource(
                "http-source-1",
                "http-source-code",
                "HTTP 来源",
                SourceType.HTTP,
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
        when(sourceGateway.findById("http-source-1")).thenReturn(Optional.of(source));
        when(checkpointGateway.existsProcessedItem("http-source-1", anyString())).thenReturn(false);

        Path directory = tempHome.resolve(".tmp/valset-standardizer/uploads/http/http-source-1");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("a.xlsx"), "a");
        Files.writeString(directory.resolve("b.xlsx"), "b");
        Files.writeString(directory.resolve("c.xlsx"), "c");

        List<?> contexts = connector.fetch(source);

        assertThat(contexts).hasSize(3);
        verify(checkpointGateway).existsProcessedItem("http-source-1", buildCheckpointKey(directory.resolve("a.xlsx")));
        verify(checkpointGateway).existsProcessedItem("http-source-1", buildCheckpointKey(directory.resolve("b.xlsx")));
        verify(checkpointGateway).existsProcessedItem("http-source-1", buildCheckpointKey(directory.resolve("c.xlsx")));
        verify(checkpointGateway, never()).findCheckpoint("http-source-1", "scanCursor");
        verify(sourceGateway, times(3)).findById("http-source-1");
    }

    private String buildCheckpointKey(Path file) throws IOException {
        return file.toAbsolutePath() + "|" + Files.getLastModifiedTime(file).toMillis() + "|" + Files.size(file);
    }
}
