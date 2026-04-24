package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSourceConnectorTest {

    @Test
    void shouldPersistScanCursorForMailWithoutAttachments() throws Exception {
        RecordingTransferSourceCheckpointGateway checkpointGateway = new RecordingTransferSourceCheckpointGateway();
        EmailSourceConnector connector = new EmailSourceConnector(checkpointGateway, new NoopTransferSourceGateway());
        TransferSource source = new TransferSource(
                "source-1",
                "qq-mail",
                "QQ邮箱",
                SourceType.EMAIL,
                true,
                null,
                Map.of(
                        TransferConfigKeys.PROTOCOL, "imap",
                        TransferConfigKeys.HOST, "imap.example.com",
                        TransferConfigKeys.PORT, 993,
                        TransferConfigKeys.USERNAME, "user@example.com",
                        TransferConfigKeys.PASSWORD, "secret",
                        TransferConfigKeys.FOLDER, "INBOX"
                ),
                Map.of(),
                Map.of(),
                "IDLE",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        EmailSourceConfig config = EmailSourceConfig.from(source);

        Method method = EmailSourceConnector.class.getDeclaredMethod(
                "recordScanCursor",
                TransferSource.class,
                EmailSourceConfig.class,
                String.class,
                String.class,
                int.class,
                String.class
        );
        method.setAccessible(true);
        method.invoke(connector, source, config, "<mail-1@example.com>", "测试邮件", 1, "无附件");

        assertThat(checkpointGateway.getSavedCheckpoints()).hasSize(1);
        TransferSourceCheckpoint checkpoint = checkpointGateway.getSavedCheckpoints().get(0);
        assertThat(checkpoint.sourceId()).isEqualTo("source-1");
        assertThat(checkpoint.sourceType()).isEqualTo(SourceType.EMAIL.name());
        assertThat(checkpoint.checkpointKey()).isEqualTo(TransferConfigKeys.CHECKPOINT_SCAN_CURSOR);
        assertThat(checkpoint.checkpointValue()).isEqualTo("<mail-1@example.com>");
        assertThat(checkpoint.checkpointMeta())
                .containsEntry(TransferConfigKeys.SOURCE_CODE, "qq-mail")
                .containsEntry(TransferConfigKeys.MAIL_FOLDER, "INBOX")
                .containsEntry(TransferConfigKeys.MAIL_PROTOCOL, "imap")
                .containsEntry("scanReason", "无附件");
    }

    private static final class RecordingTransferSourceCheckpointGateway implements TransferSourceCheckpointGateway {
        private final List<TransferSourceCheckpoint> savedCheckpoints = new ArrayList<>();

        List<TransferSourceCheckpoint> getSavedCheckpoints() {
            return savedCheckpoints;
        }

        @Override
        public boolean existsProcessedItem(String sourceId, String itemKey) {
            return false;
        }

        @Override
        public Optional<com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem> findProcessedItem(String sourceId, String itemKey) {
            return Optional.empty();
        }

        @Override
        public com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem saveProcessedItem(com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteProcessedItemsBySourceId(String sourceId) {
        }

        @Override
        public void deleteCheckpointsBySourceId(String sourceId) {
        }

        @Override
        public Optional<TransferSourceCheckpoint> findCheckpoint(String sourceId, String checkpointKey) {
            return Optional.empty();
        }

        @Override
        public TransferSourceCheckpoint saveCheckpoint(TransferSourceCheckpoint checkpoint) {
            savedCheckpoints.add(checkpoint);
            return checkpoint;
        }

        @Override
        public List<TransferSourceCheckpoint> listCheckpointsBySourceId(String sourceId, Integer limit) {
            return List.of();
        }

        @Override
        public List<com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem> listProcessedItemsBySourceId(String sourceId, Integer limit) {
            return List.of();
        }
    }

    private static final class NoopTransferSourceGateway implements TransferSourceGateway {
        @Override
        public Optional<TransferSource> findById(String sourceId) {
            return Optional.empty();
        }

        @Override
        public Optional<TransferSource> findBySourceCode(String sourceCode) {
            return Optional.empty();
        }

        @Override
        public List<TransferSource> listEnabledSources() {
            return List.of();
        }

        @Override
        public List<TransferSource> listSources(String sourceType, String sourceCode, String sourceName, Boolean enabled, Integer limit) {
            return List.of();
        }

        @Override
        public TransferSource save(TransferSource transferSource) {
            return transferSource;
        }

        @Override
        public void deleteById(String sourceId) {
        }

        @Override
        public boolean tryAcquireIngestLock(String sourceId, String lockToken, Instant startedAt) {
            return false;
        }

        @Override
        public boolean requestIngestStop(String sourceId, Instant requestedAt) {
            return false;
        }

        @Override
        public boolean forceStopIngest(String sourceId, Instant finishedAt) {
            return false;
        }

        @Override
        public boolean releaseIngestLock(String sourceId, String lockToken, Instant finishedAt) {
            return false;
        }
    }
}
