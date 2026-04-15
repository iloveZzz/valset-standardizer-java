package com.yss.subjectmatch.domain.matcher;

import com.yss.subjectmatch.domain.model.MatchCandidate;
import com.yss.subjectmatch.domain.model.StandardSubject;
import com.yss.subjectmatch.domain.model.SubjectRecord;

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
