package com.yss.valset.extract.standardization.mapping;

/**
 * 表头映射候选对象。
 */
public record HeaderMappingCandidate(
        Long ruleId,
        Long sourceId,
        String standardCode
) {
}
