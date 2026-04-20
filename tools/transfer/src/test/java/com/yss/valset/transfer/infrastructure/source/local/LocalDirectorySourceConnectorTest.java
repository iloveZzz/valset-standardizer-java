package com.yss.valset.transfer.infrastructure.source.local;

import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDirectorySourceConnectorTest {

    @Test
    void fetchesFilesFromLocalDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("transfer-local-dir-test");
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "hello transfer");

        LocalDirectorySourceConnector connector = new LocalDirectorySourceConnector();
        TransferSource source = new TransferSource(
                1L,
                tempDir.toString(),
                "本地目录",
                SourceType.LOCAL_DIR,
                true,
                null,
                Map.of("directory", tempDir.toString(), "recursive", false),
                Map.of(),
                Map.of()
        );

        List<?> contexts = connector.fetch(source);
        assertThat(contexts).hasSize(1);
        assertThat(contexts.get(0).toString()).contains("sample.txt");
    }
}
