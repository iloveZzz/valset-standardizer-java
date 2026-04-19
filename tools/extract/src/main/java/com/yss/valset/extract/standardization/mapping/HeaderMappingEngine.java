package com.yss.valset.extract.standardization.mapping;

import com.yss.valset.domain.model.MappingDecision;

import java.util.List;
import java.util.Map;

/**
 * 表头映射引擎。
 */
public interface HeaderMappingEngine {

    default Map<Integer, MappingDecision> map(List<HeaderMappingInput> inputs, HeaderMappingLookup lookup) {
        return map(inputs, lookup, null);
    }

    Map<Integer, MappingDecision> map(List<HeaderMappingInput> inputs, HeaderMappingLookup lookup, String strategyExpr);
}
