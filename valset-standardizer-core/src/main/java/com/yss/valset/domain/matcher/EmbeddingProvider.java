package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.MatchRuntimeConfig;

import java.util.List;

/**
 * 嵌入提供商合同。
 * <p>
 * 当前的 Java 迁移保留了此接口，但运行时没有嵌入支持的评分。
 */
public interface EmbeddingProvider {
    /**
     * 提供者是否启用。
     */
    boolean enabled();

    /**
     * 将查询字符串编码为嵌入向量。
     */
    float[] encodeQuery(String text, MatchRuntimeConfig runtimeConfig);

    /**
     * 将一批文档字符串编码为嵌入向量。
     */
    List<float[]> encodeDocuments(List<String> texts, MatchRuntimeConfig runtimeConfig);

    /**
     * 计算两个向量之间的余弦相似度。
     */
    double cosineSimilarity(float[] left, float[] right);
}
