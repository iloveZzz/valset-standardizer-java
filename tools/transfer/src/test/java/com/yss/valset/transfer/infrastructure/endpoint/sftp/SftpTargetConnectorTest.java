package com.yss.valset.transfer.infrastructure.endpoint.sftp;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.SftpTargetConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SFTP 投递连接器测试。
 */
class SftpTargetConnectorTest {

    private final SftpTargetConnector connector = new SftpTargetConnector();

    @Test
    void should_resolve_target_path_template_as_remote_directory() {
        TransferTarget target = new TransferTarget(
                201L,
                "sftp-target",
                "SFTP 目标",
                TargetType.SFTP,
                true,
                "/upload/${yyyyMMdd}",
                Map.of(),
                Map.of(),
                Instant.now(),
                Instant.now()
        );
        TransferRoute route = new TransferRoute(
                "route-201",
                "source-201",
                null,
                "source-code",
                "rule-201",
                TargetType.SFTP,
                "sftp-target",
                null,
                null,
                null,
                true,
                TransferStatus.ROUTED,
                Map.of()
        );
        TransferObject transferObject = new TransferObject(
                "transfer-201",
                "source-201",
                "SFTP",
                "source-code",
                "report.csv",
                "csv",
                "text/csv",
                128L,
                "fingerprint-201",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "/tmp/report.csv",
                TransferStatus.RECEIVED,
                Instant.now(),
                Instant.now(),
                "route-201",
                null,
                new ProbeResult(true, "CSV", Map.of()),
                Map.of()
        );
        TransferContext context = new TransferContext(transferObject, route, target, Map.of());
        SftpTargetConfig config = new SftpTargetConfig(
                "sftp.example.com",
                22,
                "user",
                "password",
                null,
                null,
                null,
                false,
                false,
                0,
                false,
                10_000,
                10_000
        );

        String remoteFilePath = connector.buildRemoteFilePath(config, context, transferObject);

        assertThat(remoteFilePath).isEqualTo("/upload/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/report.csv");
    }
}
