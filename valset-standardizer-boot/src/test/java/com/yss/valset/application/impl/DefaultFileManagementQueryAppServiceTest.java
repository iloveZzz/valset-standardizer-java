package com.yss.valset.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.gateway.ValsetFileIngestLogGateway;
import com.yss.valset.extract.repository.entity.ValuationSheetStylePO;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
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
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        ValsetFileIngestLogGateway ingestLogGateway = mock(ValsetFileIngestLogGateway.class);
        ValuationSheetStyleMapper styleMapper = mock(ValuationSheetStyleMapper.class);
        DefaultFileManagementQueryAppService service =
                new DefaultFileManagementQueryAppService(fileInfoGateway, ingestLogGateway, styleMapper, new ObjectMapper());

        ValuationSheetStylePO stylePO = new ValuationSheetStylePO();
        stylePO.setId(101L);
        stylePO.setTaskId(202L);
        stylePO.setFileId(303L);
        stylePO.setSheetName("Sheet1");
        stylePO.setStyleScope("HEADER_PREVIEW");
        stylePO.setSheetStyleJson("""
                {
                  "sheetName": "Sheet1",
                  "previewRowCount": 2,
                  "mergeData": [
                    {"firstRow":0,"lastRow":1,"firstColumn":0,"lastColumn":2}
                  ],
                  "headerRowNumbers": [0,1],
                  "cellData": {
                    "0": {
                      "0": {"v":"估值表标题"}
                    },
                    "1": {
                      "0": {"v":"科目代码"},
                      "1": {"v":"科目名称"}
                    }
                  }
                }
                """);
        stylePO.setPreviewRowCount(10);
        stylePO.setCreatedAt(LocalDateTime.of(2026, 4, 15, 12, 30));
        when(styleMapper.findByFileId(303L)).thenReturn(List.of(stylePO));

        assertThat(service.querySheetStyles(303L))
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo("101");
                    assertThat(dto.getTaskId()).isEqualTo("202");
                    assertThat(dto.getFileId()).isEqualTo("303");
                    assertThat(dto.getSheetName()).isEqualTo("Sheet1");
                    assertThat(dto.getStyleScope()).isEqualTo("HEADER_PREVIEW");
                    assertThat(dto.getSheetStyleJson()).contains("估值表标题");
                    assertThat(dto.getTitleRows()).hasSize(1);
                    assertThat(dto.getHeaderRows()).hasSize(1);
                    assertThat(dto.getMergeAreas()).hasSize(1);
                    assertThat(dto.getPreviewRowCount()).isEqualTo(10);
                });
    }

    @Test
    void queryFileInfoByPathShouldResolveNormalizedPath() {
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        ValsetFileIngestLogGateway ingestLogGateway = mock(ValsetFileIngestLogGateway.class);
        ValuationSheetStyleMapper styleMapper = mock(ValuationSheetStyleMapper.class);
        DefaultFileManagementQueryAppService service =
                new DefaultFileManagementQueryAppService(fileInfoGateway, ingestLogGateway, styleMapper, new ObjectMapper());

        String relativePath = "./input/valset-demo.xlsx";
        String absolutePath = Path.of(relativePath).toAbsolutePath().normalize().toString();
        com.yss.valset.domain.model.ValsetFileInfo fileInfo = com.yss.valset.domain.model.ValsetFileInfo.builder()
                .fileId(808L)
                .fileNameOriginal("valset-demo.xlsx")
                .localTempPath(absolutePath)
                .build();

        when(fileInfoGateway.findByPath(relativePath)).thenReturn(null);
        when(fileInfoGateway.findByPath(absolutePath)).thenReturn(fileInfo);

        assertThat(service.queryFileInfoByPath(relativePath))
                .satisfies(dto -> {
                    assertThat(dto.getFileId()).isEqualTo("808");
                    assertThat(dto.getFileNameOriginal()).isEqualTo("valset-demo.xlsx");
                    assertThat(dto.getLocalTempPath()).isEqualTo(absolutePath);
                });
    }
}
