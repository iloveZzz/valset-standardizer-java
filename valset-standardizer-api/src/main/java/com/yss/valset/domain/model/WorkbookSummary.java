package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 已解析工作簿的摘要统计信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbookSummary {
    /** 原始文件名 */
    private String fileNameOriginal;
    /** 标题 */
    private String title;
    /** sheet名称 */
    private String sheetName;
    /** 表头行号 */
    private Integer headerRowNumber;
    /** 数据开始行号 */
    private Integer dataStartRowNumber;
    /** 基础信息映射 */
    private Map<String, String> basicInfo;
    /** 主题总数 */
    private Integer subjectCount;
    /** 叶子主题数 */
    private Integer leafSubjectCount;
    /** 非叶子主题数 */
    private Integer nonLeafSubjectCount;
    /** 指标数 */
    private Integer metricCount;
    /** 指标行数 */
    private Integer metricRowCount;
    /** 指标数据数 */
    private Integer metricDataCount;
    /** 根主题数 */
    private Integer rootSubjectCount;
    /** 最大层级 */
    private Integer maxLevel;
    /** 重复主题代码列表 */
    private List<String> duplicateSubjectCodes;
    /** 层级分布统计 */
    private Map<Integer, Integer> levelDistribution;
}
