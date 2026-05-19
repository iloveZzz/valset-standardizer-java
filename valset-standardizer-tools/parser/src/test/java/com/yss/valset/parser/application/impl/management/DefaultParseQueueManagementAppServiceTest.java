package com.yss.valset.parser.application.impl.management;

import com.yss.valset.transfer.domain.model.TransferObjectTag;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultParseQueueManagementAppServiceTest {

    @Test
    void isValuationTagShouldIgnoreLegacyFileInfoGatewayGeneratedTag() throws Exception {
        DefaultParseQueueManagementAppService service = new DefaultParseQueueManagementAppService(
                null,
                null,
                null,
                null,
                null
        );
        Method method = DefaultParseQueueManagementAppService.class.getDeclaredMethod("isValuationTag", TransferObjectTag.class);
        method.setAccessible(true);
        TransferObjectTag legacyTag = new TransferObjectTag(
                null,
                "2001",
                "valuation-tag-id",
                "VALUATION_TABLE",
                "估值表",
                "VALUATION_TABLE",
                "SCRIPT_RULE",
                "文件主数据自动标记为估值表",
                "fileMeta",
                "VALUATION_TABLE",
                Collections.singletonMap("source", "file-info-gateway"),
                Instant.now()
        );
        TransferObjectTag recognizedTag = new TransferObjectTag(
                null,
                "2002",
                "valuation-tag-id",
                "VALUATION_TABLE",
                "估值表",
                "VALUATION_TABLE",
                "SCRIPT_RULE",
                "同时命中科目代码和科目名称",
                "probeResult",
                "VALUATION_TABLE",
                Collections.singletonMap("source", "tagging-service"),
                Instant.now()
        );

        assertThat((Boolean) method.invoke(service, legacyTag)).isFalse();
        assertThat((Boolean) method.invoke(service, recognizedTag)).isTrue();
    }
}
