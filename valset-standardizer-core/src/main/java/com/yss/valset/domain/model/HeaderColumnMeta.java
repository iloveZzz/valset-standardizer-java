package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 估值表单列头的结构化元数据。
 *
 * <p>用于完整表达合并表头的列级信息，便于前端渲染和下游落库。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderColumnMeta {
    /**
     * 列序号，从 0 开始。
     */
    private Integer columnIndex;

    /**
     * 扁平化后的表头名称，通常由多级路径通过 "|" 拼接而成。
     */
    private String headerName;

    /**
     * 表头层级路径段列表。
     */
    private List<String> pathSegments;

    /**
     * 完整表头路径。
     */
    private String headerPath;

    /**
     * 是否为空列。
     */
    private Boolean blankColumn;
}
