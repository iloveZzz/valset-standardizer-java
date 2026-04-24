package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.junit.jupiter.api.Test;

import jakarta.mail.Message;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailMailScannerTest {

    private final EmailMailScanner emailMailScanner = new EmailMailScanner(
            mock(TransferSourceCheckpointGateway.class),
            mock(TransferSourceGateway.class),
            mock(EmailAttachmentProcessor.class)
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
}
