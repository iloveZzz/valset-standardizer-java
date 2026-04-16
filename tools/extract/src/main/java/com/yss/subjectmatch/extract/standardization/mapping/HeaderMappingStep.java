package com.yss.subjectmatch.extract.standardization.mapping;

import com.yss.subjectmatch.domain.model.MappingDecision;

/**
 * 表头映射步骤。
 */
public interface HeaderMappingStep {

    MappingDecision map(HeaderMappingInput input, HeaderMappingLookup lookup);
}
