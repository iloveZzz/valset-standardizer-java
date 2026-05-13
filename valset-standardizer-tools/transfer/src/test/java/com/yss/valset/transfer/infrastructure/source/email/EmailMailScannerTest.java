package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.junit.jupiter.api.Test;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.UIDFolder;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailMailScannerTest {

    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway = mock(TransferSourceCheckpointGateway.class);
    private final TransferSourceGateway transferSourceGateway = mock(TransferSourceGateway.class);
    private final EmailAttachmentProcessor emailAttachmentProcessor = mock(EmailAttachmentProcessor.class);
    private final TransferIngestProgressAppService transferIngestProgressAppService = mock(TransferIngestProgressAppService.class);
    private final EmailMailScanner emailMailScanner = new EmailMailScanner(
            transferSourceCheckpointGateway,
            transferSourceGateway,
            emailAttachmentProcessor,
            transferIngestProgressAppService
    );

    @Test
    void shouldNotApplyMailTimeRangeWhenConfigIsAll() {
        EmailSourceConfig config = buildConfig(0);

        assertThat(emailMailScanner.resolveMailTimeLowerBound(config, Instant.parse("2026-04-25T00:00:00Z"))).isNull();
    }

    @Test
    void shouldIncludeMailWithinSevenDays() throws Exception {
        EmailSourceConfig config = buildConfig(7);
        Instant now = Instant.parse("2026-04-25T00:00:00Z");
        Message message = mock(Message.class);
        when(message.getReceivedDate()).thenReturn(java.util.Date.from(now.minusSeconds(6L * 24 * 3600)));

        Instant lowerBound = emailMailScanner.resolveMailTimeLowerBound(config, now);

        assertThat(emailMailScanner.isWithinMailTimeRange(message, lowerBound)).isTrue();
    }

    @Test
    void shouldExcludeMailOlderThanSevenDays() throws Exception {
        EmailSourceConfig config = buildConfig(7);
        Instant now = Instant.parse("2026-04-25T00:00:00Z");
        Message message = mock(Message.class);
        when(message.getReceivedDate()).thenReturn(java.util.Date.from(now.minusSeconds(8L * 24 * 3600)));

        Instant lowerBound = emailMailScanner.resolveMailTimeLowerBound(config, now);

        assertThat(emailMailScanner.isWithinMailTimeRange(message, lowerBound)).isFalse();
    }

    @Test
    void shouldFallbackToSentDateWhenReceivedDateMissing() throws Exception {
        EmailSourceConfig config = buildConfig(7);
        Instant now = Instant.parse("2026-04-25T00:00:00Z");
        Message message = mock(Message.class);
        when(message.getSentDate()).thenReturn(java.util.Date.from(now.minusSeconds(6L * 24 * 3600)));
        Instant lowerBound = emailMailScanner.resolveMailTimeLowerBound(config, now);

        assertThat(emailMailScanner.isWithinMailTimeRange(message, lowerBound)).isTrue();
    }

    @Test
    void shouldScanPop3InBatchesAndAdvanceCheckpoint() throws Exception {
        when(transferSourceCheckpointGateway.findCheckpoint(anyString(), anyString())).thenReturn(Optional.empty());
        when(transferSourceGateway.findById(anyString())).thenReturn(Optional.of(buildActiveSource()));
        when(emailAttachmentProcessor.extract(any(), any(), any(), anyString(), any(), any(), anyInt()))
                .thenReturn(new EmailAttachmentProcessor.AttachmentExtractionResult(List.of(), 0, 0));

        Folder folder = mock(Folder.class);
        Message first = buildMessage("mail-1", 1);
        Message second = buildMessage("mail-2", 2);
        Message third = buildMessage("mail-3", 3);
        when(folder.getMessages(1, 2)).thenReturn(new Message[]{first, second});
        when(folder.getMessages(3, 3)).thenReturn(new Message[]{third});

        EmailSourceConfig config = buildBatchConfig("pop3", 2);
        List<com.yss.valset.transfer.domain.model.RecognitionContext> contexts = emailMailScanner.scanMailFolder(
                buildActiveSource(),
                config,
                folder,
                null,
                3,
                null);

        assertThat(contexts).isEmpty();
        verify(folder, never()).getMessages();
        verify(folder).getMessages(1, 2);
        verify(folder).getMessages(3, 3);
        verify(transferSourceCheckpointGateway).saveCheckpoint(org.mockito.ArgumentMatchers.<com.yss.valset.transfer.domain.model.TransferSourceCheckpoint>argThat(checkpoint ->
                "mail-3".equals(checkpoint.checkpointValue())
                        && "3".equals(String.valueOf(checkpoint.checkpointMeta().get(TransferConfigKeys.MAIL_MESSAGE_NUMBER)))
                        && checkpoint.checkpointMeta().containsKey(TransferConfigKeys.MAIL_ID)));
    }

    @Test
    void shouldResumeFromImapUidCheckpoint() throws Exception {
        when(transferSourceCheckpointGateway.findCheckpoint(anyString(), anyString())).thenReturn(Optional.of(
                new com.yss.valset.transfer.domain.model.TransferSourceCheckpoint(
                        "checkpoint-1",
                        "source-1",
                        SourceType.EMAIL.name(),
                        TransferConfigKeys.CHECKPOINT_SCAN_CURSOR,
                        "mail-2",
                        Map.of(
                                TransferConfigKeys.MAIL_UID, 2L,
                                TransferConfigKeys.MAIL_MESSAGE_NUMBER, 2,
                                TransferConfigKeys.MAIL_ID, "mail-2"
                        ),
                        Instant.now(),
                        Instant.now()
                )));
        when(transferSourceGateway.findById(anyString())).thenReturn(Optional.of(buildActiveSource()));
        when(emailAttachmentProcessor.extract(any(), any(), any(), anyString(), any(), any(), anyInt()))
                .thenReturn(new EmailAttachmentProcessor.AttachmentExtractionResult(List.of(), 0, 0));

        UIDFolder uidFolder = mock(UIDFolder.class);
        Folder folder = mock(Folder.class);
        when(uidFolder.getUIDNext()).thenReturn(5L);
        Message third = buildMessage("mail-3", 3);
        Message fourth = buildMessage("mail-4", 4);
        when(uidFolder.getMessagesByUID(3L, 4L)).thenReturn(new Message[]{third, fourth});
        when(uidFolder.getUID(third)).thenReturn(3L);
        when(uidFolder.getUID(fourth)).thenReturn(4L);

        EmailSourceConfig config = buildBatchConfig("imap", 2);
        List<com.yss.valset.transfer.domain.model.RecognitionContext> contexts = emailMailScanner.scanMailFolder(
                buildActiveSource(),
                config,
                folder,
                uidFolder,
                4,
                null);

        assertThat(contexts).isEmpty();
        verify(uidFolder).getMessagesByUID(3L, 4L);
        verify(folder, never()).getMessages(anyInt(), anyInt());
        verify(transferSourceCheckpointGateway).saveCheckpoint(org.mockito.ArgumentMatchers.<com.yss.valset.transfer.domain.model.TransferSourceCheckpoint>argThat(checkpoint ->
                "mail-4".equals(checkpoint.checkpointValue())
                        && "4".equals(String.valueOf(checkpoint.checkpointMeta().get(TransferConfigKeys.MAIL_UID)))
                        && "4".equals(String.valueOf(checkpoint.checkpointMeta().get(TransferConfigKeys.MAIL_MESSAGE_NUMBER)))));
    }

    private EmailSourceConfig buildConfig(int mailTimeRangeDays) {
        Map<String, Object> connectionConfig = new LinkedHashMap<>();
        connectionConfig.put(TransferConfigKeys.PROTOCOL, "imap");
        connectionConfig.put(TransferConfigKeys.HOST, "mail.example.com");
        connectionConfig.put(TransferConfigKeys.USERNAME, "mail-user");
        connectionConfig.put(TransferConfigKeys.PASSWORD, "secret");
        connectionConfig.put(TransferConfigKeys.MAIL_TIME_RANGE_DAYS, mailTimeRangeDays);
        return EmailSourceConfig.from(new TransferSource(
                "source-1",
                "mail-source",
                "邮件来源",
                SourceType.EMAIL,
                true,
                null,
                connectionConfig,
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    private EmailSourceConfig buildBatchConfig(String protocol, int batchSize) {
        Map<String, Object> connectionConfig = new LinkedHashMap<>();
        connectionConfig.put(TransferConfigKeys.PROTOCOL, protocol);
        connectionConfig.put(TransferConfigKeys.HOST, "mail.example.com");
        connectionConfig.put(TransferConfigKeys.USERNAME, "mail-user");
        connectionConfig.put(TransferConfigKeys.PASSWORD, "secret");
        connectionConfig.put(TransferConfigKeys.MAIL_SCAN_BATCH_SIZE, batchSize);
        return EmailSourceConfig.from(new TransferSource(
                "source-1",
                "mail-source",
                "邮件来源",
                SourceType.EMAIL,
                true,
                null,
                connectionConfig,
                Map.of(),
                "RUNNING",
                "FETCH",
                null,
                null,
                null,
                null
        ));
    }

    private TransferSource buildActiveSource() {
        Map<String, Object> connectionConfig = new LinkedHashMap<>();
        connectionConfig.put(TransferConfigKeys.PROTOCOL, "imap");
        connectionConfig.put(TransferConfigKeys.HOST, "mail.example.com");
        connectionConfig.put(TransferConfigKeys.USERNAME, "mail-user");
        connectionConfig.put(TransferConfigKeys.PASSWORD, "secret");
        return new TransferSource(
                "source-1",
                "mail-source",
                "邮件来源",
                SourceType.EMAIL,
                true,
                null,
                connectionConfig,
                Map.of(),
                "RUNNING",
                "FETCH",
                null,
                null,
                null,
                null
        );
    }

    private Message buildMessage(String mailId, int messageNumber) throws Exception {
        Message message = mock(Message.class);
        when(message.getHeader("Message-ID")).thenReturn(new String[]{mailId});
        when(message.getMessageNumber()).thenReturn(messageNumber);
        return message;
    }
}
