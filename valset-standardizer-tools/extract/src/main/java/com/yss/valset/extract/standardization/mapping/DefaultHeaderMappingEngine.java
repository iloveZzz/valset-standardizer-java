package com.yss.valset.extract.standardization.mapping;

import com.yss.valset.domain.model.MappingDecision;
import com.yss.valset.extract.standardization.mapping.step.AliasStep;
import com.yss.valset.extract.standardization.mapping.step.ExactHeaderStep;
import com.yss.valset.extract.standardization.mapping.step.FallbackStep;
import com.yss.valset.extract.standardization.mapping.step.HeaderSegmentStep;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认表头映射引擎（四步策略）。
 */
@Component
public class DefaultHeaderMappingEngine implements HeaderMappingEngine {

    private final List<HeaderMappingStep> steps = List.of(
            new ExactHeaderStep(),
            new HeaderSegmentStep(),
            new AliasStep(),
            new FallbackStep()
    );

    @Override
    public Map<Integer, MappingDecision> map(List<HeaderMappingInput> inputs, HeaderMappingLookup lookup, String strategyExpr) {
        Map<Integer, MappingDecision> decisions = new LinkedHashMap<>();
        if (inputs == null || inputs.isEmpty()) {
            return decisions;
        }
        for (HeaderMappingInput input : inputs) {
            MappingDecision selected = null;
            for (HeaderMappingStep step : steps) {
                MappingDecision decision = step.map(input, lookup);
                if (decision == null) {
                    continue;
                }
                selected = decision;
                if (Boolean.TRUE.equals(decision.getMatched())) {
                    break;
                }
            }
            if (selected != null && input != null && input.columnIndex() != null) {
                decisions.put(input.columnIndex(), selected);
            }
        }
        return decisions;
    }
}
