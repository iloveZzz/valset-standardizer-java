package com.yss.valset.file.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Excel sheet 样式视图。
 */
@Data
@Builder
public class ValuationSheetStyleViewDTO implements java.io.Serializable{
    private String id;
    private String taskId;
    private String fileId;
    private String sheetName;
    private String styleScope;
    private String sheetStyleJson;
    private List<Map<String, Object>> titleRows;
    private List<Map<String, Object>> headerRows;
    private List<Map<String, Object>> mergeAreas;
    private Integer previewRowCount;
    private LocalDateTime createdAt;
}
