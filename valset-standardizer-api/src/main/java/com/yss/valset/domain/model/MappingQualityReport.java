package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 标准化映射质量报告。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingQualityReport {
    /** 表头总数 */
    private Integer headerTotal;
    /** 已映射表头数 */
    private Integer headerMapped;
    /** 未映射表头数 */
    private Integer headerUnmapped;
    /** 未映射表头示例列表 */
    private List<String> headerUnmappedTop;

    /** 主题总数 */
    private Integer subjectTotal;
    /** 已映射主题数 */
    private Integer subjectMapped;
    /** 未映射主题数 */
    private Integer subjectUnmapped;

    /** 指标总数 */
    private Integer metricTotal;
    /** 已映射指标数 */
    private Integer metricMapped;
    /** 未映射指标数 */
    private Integer metricUnmapped;
}
