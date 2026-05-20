package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 主题层次结构的树节点表示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTreeNode {
    /** 主题代码 */
    private String subjectCode;
    /** 主题名称 */
    private String subjectName;
    /** 层级 */
    private Integer level;
    /** 父级代码 */
    private String parentCode;
    /** 根代码 */
    private String rootCode;
    /** 是否为叶子节点 */
    private Boolean leaf;
    /** 子节点列表 */
    private List<SubjectTreeNode> children;
}
