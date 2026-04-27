package com.yss.valset.transfer.infrastructure.endpoint.s3;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.S3TargetConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 投递连接器测试。
 */
class S3TargetConnectorTest {

    private final S3TargetConnector connector = new S3TargetConnector();

    @Test
    void should_resolve_target_path_template_as_object_key_prefix() {
        TransferTarget target = new TransferTarget(
                301L,
                "s3-target",
                "S3 目标",
                TargetType.S3,
                true,
                "/archive/${yyyyMMdd}",
                Map.of("bucket", "test-bucket"),
                Map.of(),
                Instant.now(),
                Instant.now()
        );
        TransferRoute route = new TransferRoute(
                "route-301",
                "source-301",
                null,
                "source-code",
                "rule-301",
                TargetType.S3,
                "s3-target",
                null,
                null,
                null,
                true,
                TransferStatus.ROUTED,
                Map.of()
        );
        TransferObject transferObject = new TransferObject(
                "transfer-301",
                "source-301",
                "EMAIL",
                "source-code",
                "report.csv",
                "csv",
                "text/csv",
                128L,
                "fingerprint-301",
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
                "route-301",
                null,
                new ProbeResult(true, "CSV", Map.of()),
                Map.of()
        );
        TransferContext context = new TransferContext(transferObject, route, target, Map.of());
        S3TargetConfig config = new S3TargetConfig(
                "test-bucket",
                "cn-north-1",
                null,
                null,
                null,
                false,
                null
        );

        String objectKey = connector.buildObjectKey(config, context);

        assertThat(objectKey).isEqualTo("/archive/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/report.csv");
    }
}
