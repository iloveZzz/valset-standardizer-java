package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.MatchCandidate;
import com.yss.valset.domain.model.StandardSubject;
import com.yss.valset.domain.model.SubjectRecord;

import java.util.List;

/**
 * 对候选人标准科目进行评分。
 */
public interface CandidateScorer {
    /**
     * 对所有考生的一门外部科目进行评分。
     */
    List<MatchCandidate> score(
            SubjectRecord subject,
            AnchorSelection anchorSelection,
            List<StandardSubject> candidates,
            MatchEngineContext context
    );
}
