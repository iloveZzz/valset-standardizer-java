package com.yss.subjectmatch.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 估值表 sheet 样式快照视图。
 */
@Data
@Builder
public class ValuationSheetStyleViewDTO {
    private Long id;
    private Long taskId;
    private Long fileId;
    private String sheetName;
    private String styleScope;
    private String sheetStyleJson;
    private Integer previewRowCount;
    private LocalDateTime createdAt;
}
