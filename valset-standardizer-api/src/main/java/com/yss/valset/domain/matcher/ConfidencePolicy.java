package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.ConfidenceLevel;
import com.yss.valset.domain.model.MatchCandidate;

/**
 * 信任分级合约。
 */
public interface ConfidencePolicy {
    /**
     * 使用最佳和次佳候选对结果进行分类。
     */
    ConfidenceLevel classify(MatchCandidate best, MatchCandidate secondBest);
}
