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
    /** 主题代码 */
    private String subjectCode;
    /** 主题名称 */
    private String subjectName;
    /** 父级代码 */
    private String parentCode;
    /** 父级名称 */
    private String parentName;
    /** 层级 */
    private Integer level;
    /** 根代码 */
    private String rootCode;
    /** 段数 */
    private Integer segmentCount;
    /** 是否为叶子节点 */
    private Boolean leaf;
    /** 路径代码列表 */
    private List<String> pathCodes;
}
