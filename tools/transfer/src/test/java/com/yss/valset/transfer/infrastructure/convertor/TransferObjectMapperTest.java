package com.yss.valset.transfer.infrastructure.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferObjectMapperTest {

    private final TransferObjectMapper mapper = Mappers.getMapper(TransferObjectMapper.class);
    private final TransferJsonMapper transferJsonMapper = new TransferJsonMapper(new ObjectMapper(), new TransferSecretCodec(new com.yss.valset.transfer.infrastructure.config.TransferCryptoProperties()));

    @Test
    void shouldPersistAndRestoreProbeResultSeparately() {
        Map<String, Object> probeAttributes = new LinkedHashMap<>();
        probeAttributes.put("fileName", "report.xlsx");
        probeAttributes.put("emptyText", "   ");
        probeAttributes.put("emptyMap", Map.of());
        probeAttributes.put("mimeType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        Map<String, Object> fileMeta = new LinkedHashMap<>();
        fileMeta.put("sourceCode", "qq-mail");
        fileMeta.put("triggerType", "MANUAL");
        fileMeta.put("blankValue", "");
        fileMeta.put("nestedEmpty", Map.of("child", "   "));

        TransferObject transferObject = new TransferObject(
                "1001",
                "2001",
                SourceType.EMAIL.name(),
                "qq-mail",
                "report.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                128L,
                "fingerprint-1",
                "mail-1",
                "mail-1",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/report.xlsx",
                TransferStatus.RECEIVED,
                Instant.parse("2026-04-24T00:00:00Z"),
                Instant.parse("2026-04-24T00:01:00Z"),
                "3001",
                null,
                new ProbeResult(true, null, probeAttributes),
                fileMeta
        ).withRealStoragePath("/data/archive/report.xlsx");

        TransferObjectPO po = mapper.toPO(transferObject, transferJsonMapper);
        assertThat(po.getProbeResultJson()).contains("\"detected\":true");
        assertThat(po.getProbeResultJson()).doesNotContain("detectedType");
        assertThat(po.getProbeResultJson()).doesNotContain("emptyText");
        assertThat(po.getProbeResultJson()).doesNotContain("emptyMap");
        assertThat(po.getFileMetaJson()).contains("triggerType");
        assertThat(po.getFileMetaJson()).doesNotContain("blankValue");
        assertThat(po.getFileMetaJson()).doesNotContain("nestedEmpty");
        assertThat(po.getRealStoragePath()).isEqualTo("/data/archive/report.xlsx");

        TransferObject restored = mapper.toDomain(po, transferJsonMapper);
        assertThat(restored.probeResult()).isNotNull();
        assertThat(restored.probeResult().detected()).isTrue();
        assertThat(restored.probeResult().detectedType()).isNull();
        assertThat(restored.probeResult().attributes()).containsEntry("fileName", "report.xlsx");
        assertThat(restored.fileMeta()).containsEntry("sourceCode", "qq-mail");
        assertThat(restored.realStoragePath()).isEqualTo("/data/archive/report.xlsx");
    }
}
