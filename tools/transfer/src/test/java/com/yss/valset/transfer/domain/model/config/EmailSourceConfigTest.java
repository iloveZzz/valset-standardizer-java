package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSourceConfigTest {

    @Test
    void shouldSupportPop3WithDefaultPort() {
        EmailSourceConfig config = EmailSourceConfig.from(new TransferSource(
                "source-1",
                "mail-pop3",
                "邮件来源",
                null,
                true,
                null,
                buildConnectionConfig("pop3"),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(config.protocol()).isEqualTo("pop3");
        assertThat(config.port()).isEqualTo(110);
        assertThat(config.mailTimeRangeDays()).isZero();
        assertThat(config.ssl()).isFalse();
        assertThat(config.effectiveLimit()).isEqualTo(50);
    }

    @Test
    void shouldSupportPop3sWithCorrectDefaultPort() {
        EmailSourceConfig config = EmailSourceConfig.from(new TransferSource(
                "source-1",
                "mail-pop3s",
                "邮件来源",
                null,
                true,
                null,
                buildConnectionConfig("pop3s"),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(config.protocol()).isEqualTo("pop3s");
        assertThat(config.port()).isEqualTo(995);
        assertThat(config.mailTimeRangeDays()).isZero();
        assertThat(config.ssl()).isTrue();
    }

    private Map<String, Object> buildConnectionConfig(String protocol) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(TransferConfigKeys.PROTOCOL, protocol);
        config.put(TransferConfigKeys.HOST, "mail.example.com");
        config.put(TransferConfigKeys.USERNAME, "mail-user");
        config.put(TransferConfigKeys.PASSWORD, "secret");
        return config;
    }
}
