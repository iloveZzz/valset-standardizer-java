package com.yss.valset.transfer.application.impl.ingest;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
