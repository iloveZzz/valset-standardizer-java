package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    @JsonIgnore
    private String sheetStyleJson;
    private List<Map<String, Object>> titleRows;
    private List<Map<String, Object>> headerRows;
    private List<Map<String, Object>> mergeAreas;
    private Integer previewRowCount;
    private LocalDateTime createdAt;
}
