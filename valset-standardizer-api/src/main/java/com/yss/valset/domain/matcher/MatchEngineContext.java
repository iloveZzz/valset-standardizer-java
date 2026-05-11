package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.MappingHintIndex;
import com.yss.valset.domain.model.MatchRuntimeConfig;
import com.yss.valset.domain.model.MatchWeights;
import com.yss.valset.domain.model.StandardSubject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 候选人回忆和评分期间使用的内部匹配器上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEngineContext {
    private List<StandardSubject> standardSubjects;
    private MappingHintIndex mappingHintIndex;
    private MatchWeights weights;
    private MatchRuntimeConfig runtimeConfig;
    private EmbeddingProvider embeddingProvider;
}
