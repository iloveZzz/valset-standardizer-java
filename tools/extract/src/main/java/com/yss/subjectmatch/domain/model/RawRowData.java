package com.yss.subjectmatch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 原始行数据值对象。
 * 表示从估值文件中提取的单行原始数据，用于在提取器和持久化层之间传输数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawRowData {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 行号（从1开始）
     */
    private int rowDataNumber;
    
    /**
     * 行数据JSON（JSON数组格式，保存所有列值）
     */
    private String rowDataJson;
}
