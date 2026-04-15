package com.yss.subjectmatch.domain.model;

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
    private String embeddingStrategy;
    private String embeddingModel;
    private String embeddingQueryInstruction;
    private Integer embeddingTopK;
}
