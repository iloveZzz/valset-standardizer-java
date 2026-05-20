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
    /** 解析后的估值数据 */
    private ParsedValuationData parsedValuationData;
    /** 标准科目列表 */
    private List<StandardSubject> standardSubjects;
    /** 映射提示索引 */
    private MappingHintIndex mappingHintIndex;
    /** 匹配权重配置 */
    private MatchWeights weights;
    /** 运行时配置 */
    private MatchRuntimeConfig runtimeConfig;
}
