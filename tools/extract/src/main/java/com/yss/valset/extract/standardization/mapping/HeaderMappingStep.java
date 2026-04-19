package com.yss.valset.extract.standardization.mapping;

import com.yss.valset.domain.model.MappingDecision;

/**
 * 表头映射步骤。
 */
public interface HeaderMappingStep {

    MappingDecision map(HeaderMappingInput input, HeaderMappingLookup lookup);
}
