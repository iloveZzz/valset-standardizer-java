package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    /**
     * 主键。
     */
    private String id;
    /**
     * 任务主键。
     */
    private String taskId;
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 工作表名称。
     */
    private String sheetName;
    /**
     * 样式作用域。
     */
    private String styleScope;
    @JsonIgnore
    private String sheetStyleJson;
    /**
     * 标题行信息。
     */
    private List<Map<String, Object>> titleRows;
    /**
     * 表头行信息。
     */
    private List<Map<String, Object>> headerRows;
    /**
     * 合并单元格区域。
     */
    private List<Map<String, Object>> mergeAreas;
    /**
     * 预览行数。
     */
    private Integer previewRowCount;
    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
