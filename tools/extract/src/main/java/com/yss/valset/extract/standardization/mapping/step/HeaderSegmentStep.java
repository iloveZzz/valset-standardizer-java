package com.yss.valset.extract.standardization.mapping.step;

import com.yss.valset.domain.model.MappingDecision;
import com.yss.valset.extract.standardization.mapping.HeaderMappingCandidate;
import com.yss.valset.extract.standardization.mapping.HeaderMappingInput;
import com.yss.valset.extract.standardization.mapping.HeaderMappingLookup;
import com.yss.valset.extract.standardization.mapping.HeaderMappingStep;

import java.util.ArrayList;
import java.util.List;

/**
 * 多级表头分段匹配步骤。
 */
public class HeaderSegmentStep implements HeaderMappingStep {

    @Override
    public MappingDecision map(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        if (input.headerText() != null && !input.headerText().isBlank()) {
            for (String segment : input.headerText().split("\\|")) {
                if (segment != null && !segment.isBlank()) {
                    candidates.add(segment.trim());
                }
            }
        }
        if (input.segments() != null) {
            for (String segment : input.segments()) {
                if (segment != null && !segment.isBlank()) {
                    candidates.add(segment.trim());
                }
            }
        }
        for (String segment : candidates) {
            HeaderMappingCandidate candidate = lookup.findExact(segment);
            if (candidate == null) {
                continue;
            }
            return MappingDecision.builder()
                    .columnIndex(input.columnIndex())
                    .headerText(input.headerText())
                    .standardCode(candidate.standardCode())
                    .matchedRuleId(candidate.ruleId())
                    .matchedSourceId(candidate.sourceId())
                    .strategy("header_segment")
                    .confidence(0.92D)
                    .reason("Matched by header segment")
                    .matchedText(segment)
                    .matched(Boolean.TRUE)
                    .build();
        }
        return null;
    }
}
