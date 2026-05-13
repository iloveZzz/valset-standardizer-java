package com.yss.valset.extract.standardization.mapping;

import java.util.List;
import lombok.Value;

/**
 * 表头映射输入。
 */
@Value
public class HeaderMappingInput {

    Integer columnIndex;
    String headerText;
    List<String> segments;

    public Integer columnIndex() {
        return columnIndex;
    }

    public String headerText() {
        return headerText;
    }

    public List<String> segments() {
        return segments;
    }
}
