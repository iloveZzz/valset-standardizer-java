package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 扁平化的主题关系视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectRelation {
    private String subjectCode;
    private String subjectName;
    private String parentCode;
    private String parentName;
    private Integer level;
    private String rootCode;
    private Integer segmentCount;
    private Boolean leaf;
    private List<String> pathCodes;
}
