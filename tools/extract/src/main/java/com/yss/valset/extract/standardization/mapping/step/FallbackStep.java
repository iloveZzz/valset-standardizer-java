package com.yss.valset.extract.standardization.mapping.step;

import com.yss.valset.domain.model.MappingDecision;
import com.yss.valset.extract.standardization.mapping.HeaderMappingInput;
import com.yss.valset.extract.standardization.mapping.HeaderMappingLookup;
import com.yss.valset.extract.standardization.mapping.HeaderMappingStep;

/**
 * 兜底步骤（仅记录未匹配，不落主表字段）。
 */
public class FallbackStep implements HeaderMappingStep {

    @Override
    public MappingDecision map(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null) {
            return MappingDecision.builder()
                    .matched(Boolean.FALSE)
                    .strategy("fallback")
                    .confidence(0D)
                    .reason("No mapping input")
                    .build();
        }
        return MappingDecision.builder()
                .columnIndex(input.columnIndex())
                .headerText(input.headerText())
                .matched(Boolean.FALSE)
                .strategy("fallback")
                .confidence(0D)
                .reason("No standard mapping matched")
                .build();
    }
}
