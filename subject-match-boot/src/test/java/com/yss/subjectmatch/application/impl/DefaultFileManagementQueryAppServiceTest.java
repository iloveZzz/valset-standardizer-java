package com.yss.subjectmatch.application.impl;

import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileIngestLogGateway;
import com.yss.subjectmatch.extract.repository.entity.ValuationSheetStylePO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationSheetStyleMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 文件管理查询服务测试。
 */
class DefaultFileManagementQueryAppServiceTest {

    @Test
    void querySheetStylesShouldMapStyleSnapshotDto() {
        SubjectMatchFileInfoGateway fileInfoGateway = mock(SubjectMatchFileInfoGateway.class);
        SubjectMatchFileIngestLogGateway ingestLogGateway = mock(SubjectMatchFileIngestLogGateway.class);
        ValuationSheetStyleMapper styleMapper = mock(ValuationSheetStyleMapper.class);
        DefaultFileManagementQueryAppService service =
                new DefaultFileManagementQueryAppService(fileInfoGateway, ingestLogGateway, styleMapper);

        ValuationSheetStylePO stylePO = new ValuationSheetStylePO();
        stylePO.setId(101L);
        stylePO.setTaskId(202L);
        stylePO.setFileId(303L);
        stylePO.setSheetName("Sheet1");
        stylePO.setStyleScope("HEADER_PREVIEW");
        stylePO.setSheetStyleJson("{\"title\":\"demo\"}");
        stylePO.setPreviewRowCount(10);
        stylePO.setCreatedAt(LocalDateTime.of(2026, 4, 15, 12, 30));
        when(styleMapper.findByFileId(303L)).thenReturn(List.of(stylePO));

        assertThat(service.querySheetStyles(303L))
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo(101L);
                    assertThat(dto.getTaskId()).isEqualTo(202L);
                    assertThat(dto.getFileId()).isEqualTo(303L);
                    assertThat(dto.getSheetName()).isEqualTo("Sheet1");
                    assertThat(dto.getStyleScope()).isEqualTo("HEADER_PREVIEW");
                    assertThat(dto.getSheetStyleJson()).contains("demo");
                    assertThat(dto.getPreviewRowCount()).isEqualTo(10);
                });
    }
}
