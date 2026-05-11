package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运行时上下文传递到匹配器中。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchContext {
    private ParsedValuationData parsedValuationData;
    private List<StandardSubject> standardSubjects;
    private MappingHintIndex mappingHintIndex;
    private MatchWeights weights;
    private MatchRuntimeConfig runtimeConfig;
}
