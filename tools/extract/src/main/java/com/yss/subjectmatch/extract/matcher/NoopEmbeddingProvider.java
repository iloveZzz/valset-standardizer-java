package com.yss.subjectmatch.extract.matcher;

import com.yss.subjectmatch.domain.matcher.EmbeddingProvider;
import com.yss.subjectmatch.domain.model.MatchRuntimeConfig;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class NoopEmbeddingProvider implements EmbeddingProvider {
    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public float[] encodeQuery(String text, MatchRuntimeConfig runtimeConfig) {
        return new float[0];
    }

    @Override
    public List<float[]> encodeDocuments(List<String> texts, MatchRuntimeConfig runtimeConfig) {
        return Collections.emptyList();
    }

    @Override
    public double cosineSimilarity(float[] left, float[] right) {
        return 0D;
    }
}
