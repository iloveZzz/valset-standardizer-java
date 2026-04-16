package com.yss.subjectmatch.extract.standardization.mapping.step;

import com.yss.subjectmatch.domain.model.MappingDecision;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingCandidate;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingInput;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingLookup;
import com.yss.subjectmatch.extract.standardization.mapping.HeaderMappingStep;

/**
 * 别名匹配步骤（白名单别名，包含匹配）。
 */
public class AliasStep implements HeaderMappingStep {

    @Override
    public MappingDecision map(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null || input.headerText() == null || input.headerText().isBlank()) {
            return null;
        }
        HeaderMappingCandidate candidate = lookup.findAliasContains(input.headerText().trim());
        if (candidate == null) {
            return null;
        }
        return MappingDecision.builder()
                .columnIndex(input.columnIndex())
                .headerText(input.headerText())
                .standardCode(candidate.standardCode())
                .matchedRuleId(candidate.ruleId())
                .matchedSourceId(candidate.sourceId())
                .strategy("alias_contains")
                .confidence(0.80D)
                .reason("Matched by alias whitelist")
                .matchedText(input.headerText().trim())
                .matched(Boolean.TRUE)
                .build();
    }
}
