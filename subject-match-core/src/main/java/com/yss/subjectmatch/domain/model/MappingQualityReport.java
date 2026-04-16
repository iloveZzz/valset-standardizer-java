package com.yss.subjectmatch.domain.model;

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
    private Integer headerTotal;
    private Integer headerMapped;
    private Integer headerUnmapped;
    private List<String> headerUnmappedTop;

    private Integer subjectTotal;
    private Integer subjectMapped;
    private Integer subjectUnmapped;

    private Integer metricTotal;
    private Integer metricMapped;
    private Integer metricUnmapped;
}
