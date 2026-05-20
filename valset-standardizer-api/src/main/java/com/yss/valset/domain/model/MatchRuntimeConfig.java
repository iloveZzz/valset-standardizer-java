package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 控制匹配器行为的运行时标志。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRuntimeConfig {
    /** Embedding策略 */
    private String embeddingStrategy;
    /** Embedding模型 */
    private String embeddingModel;
    /** Embedding查询指令 */
    private String embeddingQueryInstruction;
    /** Embedding Top K值 */
    private Integer embeddingTopK;
}
