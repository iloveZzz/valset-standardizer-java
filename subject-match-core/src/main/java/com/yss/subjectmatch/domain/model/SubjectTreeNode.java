package com.yss.subjectmatch.domain.model;

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
    private String subjectCode;
    private String subjectName;
    private Integer level;
    private String parentCode;
    private String rootCode;
    private Boolean leaf;
    private List<SubjectTreeNode> children;
}
