package com.yss.subjectmatch.domain.matcher;

import com.yss.subjectmatch.domain.model.ConfidenceLevel;
import com.yss.subjectmatch.domain.model.MatchCandidate;

/**
 * 信任分级合约。
 */
public interface ConfidencePolicy {
    /**
     * 使用最佳和次佳候选对结果进行分类。
     */
    ConfidenceLevel classify(MatchCandidate best, MatchCandidate secondBest);
}
