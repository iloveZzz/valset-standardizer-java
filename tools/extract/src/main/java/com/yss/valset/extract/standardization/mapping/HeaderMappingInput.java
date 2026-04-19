package com.yss.valset.extract.standardization.mapping;

import java.util.List;

/**
 * 表头映射输入。
 */
public record HeaderMappingInput(
        Integer columnIndex,
        String headerText,
        List<String> segments
) {
}
