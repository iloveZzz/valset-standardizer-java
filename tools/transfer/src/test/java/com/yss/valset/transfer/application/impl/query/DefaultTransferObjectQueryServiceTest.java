package com.yss.valset.transfer.application.impl.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferObjectDownloadViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.domain.gateway.TransferMailInfoGateway;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectMailFolderCount;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectSourceAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectStatusCount;
import com.yss.valset.transfer.domain.model.TransferObjectSizeAnalysis;
import com.yss.valset.transfer.domain.model.TransferMailInfo;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferObjectQueryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldForwardDeliveryStatusFilterToGateway() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);

        DefaultTransferObjectQueryService service = new DefaultTransferObjectQueryService(
                transferObjectGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferMailInfoGateway,
                new ObjectMapper()
        );

        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "mail.xls",
                "xls",
                "application/vnd.ms-excel",
                123L,
                "fingerprint",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/mail.xls",
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of()
        );
        when(transferObjectGateway.pageObjects(
                eq("source-1"),
                eq("EMAIL"),
                eq("source-code"),
                eq("RECEIVED"),
                eq("UNDELIVERED"),
                eq("mail-id"),
                eq("fingerprint"),
                eq("route-1"),
                eq("tag-1"),
                eq("TAG_CODE"),
                eq("tag-value"),
                eq(0),
                eq(10)
        )).thenReturn(new TransferObjectPage(List.of(transferObject), 1L, 0L, 10L));
        when(transferObjectTagGateway.listByTransferIds(List.of("transfer-1"))).thenReturn(List.of());
        when(transferDeliveryGateway.listRecordsByTransferIds(List.of("transfer-1"), "SUCCESS"))
                .thenReturn(List.of());

        PageResult<?> result = service.pageObjects(
                "source-1",
                "EMAIL",
                "source-code",
                "received",
                "未投递",
                "mail-id",
                "fingerprint",
                "route-1",
                "tag-1",
                "TAG_CODE",
                "tag-value",
                0,
                10
        );

        assertThat(result.getData()).hasSize(1);
        verify(transferObjectGateway).pageObjects(
                eq("source-1"),
                eq("EMAIL"),
                eq("source-code"),
                eq("RECEIVED"),
                eq("UNDELIVERED"),
                eq("mail-id"),
                eq("fingerprint"),
                eq("route-1"),
                eq("tag-1"),
                eq("TAG_CODE"),
                eq("tag-value"),
                eq(0),
                eq(10)
        );
    }

    @Test
    void shouldExposeUndeliveredCountInAnalysisView() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);

        DefaultTransferObjectQueryService service = new DefaultTransferObjectQueryService(
                transferObjectGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferMailInfoGateway,
                new ObjectMapper()
        );

        TransferObjectAnalysis analysis = new TransferObjectAnalysis(
                1L,
                1L,
                0L,
                List.of(new TransferObjectSourceAnalysis(
                        "EMAIL",
                        1L,
                        List.of(new TransferObjectStatusCount("RECEIVED", 1L)),
                        List.of(new TransferObjectMailFolderCount("INBOX", 1L)),
                        2L
                )),
                new TransferObjectSizeAnalysis(1L, 10L, List.of())
        );
        when(transferObjectGateway.analyzeObjects(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null)
        )).thenReturn(analysis);

        var result = service.analyzeObjects(null, null, null, null, null, null, null, null, null, null, null);

        assertThat(result.getSourceAnalyses()).hasSize(1);
        assertThat(result.getSourceAnalyses().get(0).getUndeliveredCount()).isEqualTo(2L);
    }

    @Test
    void shouldPrepareDownloadViewFromLocalTempPath() throws Exception {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);

        DefaultTransferObjectQueryService service = new DefaultTransferObjectQueryService(
                transferObjectGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferMailInfoGateway,
                new ObjectMapper()
        );

        Path filePath = tempDir.resolve("分拣对象导出.xlsx");
        Files.writeString(filePath, "download-content", StandardCharsets.UTF_8);

        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "分拣对象导出.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                16L,
                "fingerprint",
                "source-ref",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                filePath.toAbsolutePath().toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of()
        );
        when(transferObjectGateway.findById("transfer-1")).thenReturn(java.util.Optional.of(transferObject));

        TransferObjectDownloadViewDTO downloadView = service.downloadObject("transfer-1");

        assertThat(downloadView.getTransferId()).isEqualTo("transfer-1");
        assertThat(downloadView.getFilePath()).isEqualTo(filePath.toAbsolutePath().normalize());
        assertThat(downloadView.getFileName()).isEqualTo("分拣对象导出.xlsx");
        assertThat(downloadView.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(downloadView.getContentLength()).isEqualTo(Files.size(filePath));
    }

    @Test
    void shouldLoadMailInfoByTransferId() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);

        DefaultTransferObjectQueryService service = new DefaultTransferObjectQueryService(
                transferObjectGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferMailInfoGateway,
                new ObjectMapper()
        );

        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "mail.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                16L,
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
                "/tmp/mail.xlsx",
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of()
        );
        when(transferObjectGateway.findById("transfer-1")).thenReturn(java.util.Optional.of(transferObject));
        when(transferMailInfoGateway.findByTransferId("transfer-1")).thenReturn(java.util.Optional.of(
                new TransferMailInfo(
                        "transfer-1",
                        "mail-id",
                        "sender@example.com",
                        "to@example.com",
                        "cc@example.com",
                        "bcc@example.com",
                        "subject",
                        "body",
                        "imap",
                        "INBOX"
                )
        ));

        var result = service.getMailInfo("transfer-1");

        assertThat(result.getTransferId()).isEqualTo("transfer-1");
        assertThat(result.getMailId()).isEqualTo("mail-id");
        assertThat(result.getMailFrom()).isEqualTo("sender@example.com");
        assertThat(result.getMailTo()).isEqualTo("to@example.com");
        assertThat(result.getMailCc()).isEqualTo("cc@example.com");
        assertThat(result.getMailBcc()).isEqualTo("bcc@example.com");
        assertThat(result.getMailSubject()).isEqualTo("subject");
        assertThat(result.getMailBody()).isEqualTo("body");
        assertThat(result.getMailProtocol()).isEqualTo("imap");
        assertThat(result.getMailFolder()).isEqualTo("INBOX");
    }

    @Test
    void shouldGroupInboxRowsByMailIdAndKeepAttachments() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        TransferDeliveryGateway transferDeliveryGateway = mock(TransferDeliveryGateway.class);
        TransferMailInfoGateway transferMailInfoGateway = mock(TransferMailInfoGateway.class);

        DefaultTransferObjectQueryService service = new DefaultTransferObjectQueryService(
                transferObjectGateway,
                transferObjectTagGateway,
                transferDeliveryGateway,
                transferMailInfoGateway,
                new ObjectMapper()
        );

        TransferObject firstAttachment = createEmailObject(
                "transfer-1",
                "mail-1",
                "attachment-a.xlsx",
                "2026-04-27T09:00:00Z"
        );
        TransferObject secondAttachment = createEmailObject(
                "transfer-2",
                "mail-1",
                "attachment-b.xlsx",
                "2026-04-27T10:00:00Z"
        );
        TransferObject otherMail = createEmailObject(
                "transfer-3",
                "mail-2",
                "standalone.docx",
                "2026-04-27T11:00:00Z"
        );

        when(transferObjectGateway.listEmailInboxObjects("source-code", null))
                .thenReturn(List.of(firstAttachment, secondAttachment, otherMail));
        when(transferMailInfoGateway.listByTransferIds(List.of("transfer-2", "transfer-3"))).thenReturn(List.of(
                new TransferMailInfo(
                        "transfer-2",
                        "mail-1",
                        "sender@example.com",
                        "to@example.com",
                        null,
                        null,
                        "subject-1",
                        "body-1",
                        "imap",
                        "INBOX"
                ),
                new TransferMailInfo(
                        "transfer-3",
                        "mail-2",
                        "sender@example.com",
                        "to@example.com",
                        null,
                        null,
                        "subject-2",
                        "body-2",
                        "imap",
                        "INBOX"
                )
        ));
        when(transferDeliveryGateway.listRecordsByTransferIds(List.of("transfer-1", "transfer-2", "transfer-3"), "SUCCESS"))
                .thenReturn(List.of(
                        new TransferDeliveryRecord(null, null, "transfer-1", null, null, "SUCCESS", null, null, null, null, null),
                        new TransferDeliveryRecord(null, null, "transfer-2", null, null, "SUCCESS", null, null, null, null, null)
                ));
        when(transferObjectTagGateway.listByTransferIds(List.of("transfer-1", "transfer-2", "transfer-3")))
                .thenReturn(List.of());

        PageResult<TransferObjectViewDTO> page = service.pageMailInbox("source-code", null, null, 0, 10);

        assertThat(page.getData()).hasSize(2);
        TransferObjectViewDTO first = page.getData().get(0);
        assertThat(first.getPrimaryTransferId()).isEqualTo("transfer-3");
        assertThat(first.getAttachmentCount()).isEqualTo(1);
        TransferObjectViewDTO second = page.getData().get(1);
        assertThat(second.getPrimaryTransferId()).isEqualTo("transfer-2");
        assertThat(second.getAttachmentCount()).isEqualTo(2);
        assertThat(second.getTransferIds()).containsExactly("transfer-2", "transfer-1");
        assertThat(second.getAttachments()).hasSize(2);
    }

    private TransferObject createEmailObject(String transferId, String mailId, String originalName, String receivedAt) {
        return new TransferObject(
                transferId,
                "source-1",
                "EMAIL",
                "source-code",
                originalName,
                "xlsx",
                "application/vnd.ms-excel",
                123L,
                "fingerprint-" + transferId,
                "source-ref-" + transferId,
                mailId,
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject-" + mailId,
                "body-" + mailId,
                "imap",
                "INBOX",
                "/tmp/" + originalName,
                TransferStatus.RECEIVED,
                java.time.Instant.parse(receivedAt),
                java.time.Instant.parse(receivedAt),
                null,
                null,
                null,
                Map.of()
        );
    }
}
