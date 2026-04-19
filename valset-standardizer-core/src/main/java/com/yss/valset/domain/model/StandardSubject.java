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
    private String standardCode;
    private String standardName;
    private String parentCode;
    private String parentName;
    private Integer level;
    private String rootCode;
    private Integer segmentCount;
    private List<String> pathCodes;
    private List<String> pathNames;
    private String pathText;
    private String normalizedName;
    private String normalizedPathText;
    private Boolean placeholder;
}
