package com.yss.valset.transfer.application.impl.ingest;

import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.plugin.FileProbePluginRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultIngestTransferServiceTest {

    @Test
    void shouldUseStoredLocalTempPathForTaggingContext() {
        RecognitionContext analysisContext = new RecognitionContext(
                SourceType.EMAIL,
                "source-code",
                "mail-attachment.xls",
                "application/vnd.ms-excel",
                123L,
                "sender@example.com",
                "to@example.com",
                "cc@example.com",
                "bcc@example.com",
                "subject",
                "body",
                "mail-id",
                "imap",
                "INBOX",
                "/var/folders/8d/temp-mail-attachment.xls",
                Map.of("attachmentName", "mail-attachment.xls")
        );
        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "mail-attachment.xls",
                "xls",
                "application/vnd.ms-excel",
                123L,
                "fingerprint",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                "cc@example.com",
                "bcc@example.com",
                "subject",
                "body",
                "imap",
                "INBOX",
                "/Users/zhudaoming/.tmp/yss-transfer/outbox/2026-04-25/mail-attachment.xls",
                TransferStatus.RECEIVED,
                Instant.now(),
                Instant.now(),
                null,
                null,
                new ProbeResult(false, null, Map.<String, Object>of()),
                Map.of("attachmentName", "mail-attachment.xls")
        );

        RecognitionContext taggingContext = DefaultIngestTransferService.toTaggingContext(analysisContext, transferObject);

        assertThat(taggingContext.path()).isEqualTo(transferObject.localTempPath());
        assertThat(taggingContext.fileName()).isEqualTo(analysisContext.fileName());
        assertThat(taggingContext.sourceType()).isEqualTo(analysisContext.sourceType());
        assertThat(taggingContext.attributes()).isEqualTo(transferObject.fileMeta());
    }

    @Test
    void shouldNormalizeHttpCheckpointFingerprintToBoundedLength() throws Exception {
        DefaultIngestTransferService service = newService();
        TransferSource source = new TransferSource(
                "source-1",
                "source-code",
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
        RecognitionContext context = new RecognitionContext(
                SourceType.HTTP,
                "source-code",
                "manual-upload.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                123L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "/tmp/manual-upload.xlsx",
                Map.of(TransferConfigKeys.CHECKPOINT_FINGERPRINT, "manual-upload.xlsx|/very/long/path/that/should/not/be/stored/directly|1738500000000|123")
        );

        var method = DefaultIngestTransferService.class.getDeclaredMethod(
                "buildCheckpointItem",
                TransferSource.class,
                RecognitionContext.class,
                String.class,
                String.class
        );
        method.setAccessible(true);
        TransferSourceCheckpointItem result = (TransferSourceCheckpointItem) method.invoke(service, source, context, "MANUAL", "http-source-item");

        assertThat(result.itemFingerprint()).hasSize(16);
    }

    private DefaultIngestTransferService newService() {
        return new DefaultIngestTransferService(
                mock(TransferSourceGateway.class),
                mock(com.yss.valset.transfer.infrastructure.connector.SourceConnectorRegistry.class),
                mock(TransferObjectGateway.class),
                mock(TransferRunLogGateway.class),
                mock(TransferSourceCheckpointGateway.class),
                mock(ObjectProvider.class),
                mock(FileProbePluginRegistry.class),
                mock(TransferIngestProgressAppService.class),
                mock(TransferTaggingUseCase.class),
                "/tmp"
        );
    }

}
