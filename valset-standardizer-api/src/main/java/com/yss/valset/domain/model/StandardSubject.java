package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从参考工作簿加载的标准主题条目。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardSubject {
    /** 标准代码 */
    private String standardCode;
    /** 标准名称 */
    private String standardName;
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
    /** 路径代码列表 */
    private List<String> pathCodes;
    /** 路径名称列表 */
    private List<String> pathNames;
    /** 路径文本 */
    private String pathText;
    /** 规范化名称 */
    private String normalizedName;
    /** 规范化路径文本 */
    private String normalizedPathText;
    /** 是否占位符 */
    private Boolean placeholder;
}
