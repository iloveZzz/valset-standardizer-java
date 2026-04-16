package com.yss.subjectmatch.extract.standardization.mapping.step;

import com.yss.subjectmatch.domain.model.MappingDecision;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingCandidate;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingInput;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingLookup;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingStep;

/**
 * 精确表头匹配步骤。
 */
public class ExactHeaderStep implements HeaderMappingStep {

    @Override
    public MappingDecision map(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null || input.headerText() == null || input.headerText().isBlank()) {
            return null;
        }
        HeaderMappingCandidate candidate = lookup.findExact(input.headerText().trim());
        if (candidate == null) {
            return null;
        }
        return MappingDecision.builder()
                .columnIndex(input.columnIndex())
                .headerText(input.headerText())
                .standardCode(candidate.standardCode())
                .matchedRuleId(candidate.ruleId())
                .matchedSourceId(candidate.sourceId())
                .strategy("exact_header")
                .confidence(0.98D)
                .reason("Exact header match")
                .matchedText(input.headerText().trim())
                .matched(Boolean.TRUE)
                .build();
    }
}
