package com.yss.valset.extract.standardization.mapping;

import lombok.Value;

/**
 * 表头映射候选对象。
 */
@Value
public class HeaderMappingCandidate {

    Long ruleId;
    Long sourceId;
    String standardCode;

    public Long ruleId() {
        return ruleId;
    }

    public Long sourceId() {
        return sourceId;
    }

    public String standardCode() {
        return standardCode;
    }
}
