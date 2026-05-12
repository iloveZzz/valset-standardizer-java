package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.junit.jupiter.api.Test;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.UIDFolder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailAttachmentProcessorTest {

    private final EmailAttachmentProcessor emailAttachmentProcessor = new EmailAttachmentProcessor(
            mock(com.yss.valset.transfer.domain.rule.ScriptRuleEngineAdapter.class),
            mock(TransferIngestProgressAppService.class)
    );

    @Test
    void shouldResolveMessageByUidMetadataFirst() throws Exception {
        Folder folder = mock(Folder.class);
        UIDFolder uidFolder = mock(UIDFolder.class);
        Message message = mock(Message.class);
        when(uidFolder.getMessageByUID(88L)).thenReturn(message);

        Message resolved = emailAttachmentProcessor.resolveMessage(folder, uidFolder, "mail-88", Map.of(
                TransferConfigKeys.MAIL_UID, 88L,
                TransferConfigKeys.MAIL_MESSAGE_NUMBER, 3
        ), buildConfig());

        assertThat(resolved).isSameAs(message);
        verify(folder, never()).getMessage(3);
    }

    @Test
    void shouldResolveMessageByMessageNumberWhenUidMissing() throws Exception {
        Folder folder = mock(Folder.class);
        Message message = mock(Message.class);
        when(folder.getMessage(3)).thenReturn(message);
        when(message.getHeader("Message-ID")).thenReturn(new String[]{"mail-3"});

        Message resolved = emailAttachmentProcessor.resolveMessage(folder, null, "mail-3", Map.of(
                TransferConfigKeys.MAIL_MESSAGE_NUMBER, 3
        ), buildConfig());

        assertThat(resolved).isSameAs(message);
    }

    @Test
    void shouldFallbackToFolderScanWhenMetadataMisses() throws Exception {
        Folder folder = mock(Folder.class);
        Message message = mock(Message.class);
        when(message.getHeader("Message-ID")).thenReturn(new String[]{"mail-9"});
        when(folder.getMessages()).thenReturn(new Message[]{message});

        Message resolved = emailAttachmentProcessor.resolveMessage(folder, null, "mail-9", null, buildConfig());

        assertThat(resolved).isSameAs(message);
    }

    private EmailSourceConfig buildConfig() {
        Map<String, Object> connectionConfig = new LinkedHashMap<>();
        connectionConfig.put(TransferConfigKeys.PROTOCOL, "imap");
        connectionConfig.put(TransferConfigKeys.HOST, "mail.example.com");
        connectionConfig.put(TransferConfigKeys.USERNAME, "mail-user");
        connectionConfig.put(TransferConfigKeys.PASSWORD, "secret");
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
}
