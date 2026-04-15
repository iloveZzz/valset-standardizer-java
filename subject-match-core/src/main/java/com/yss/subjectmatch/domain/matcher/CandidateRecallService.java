package com.yss.subjectmatch.domain.matcher;

import com.yss.subjectmatch.domain.model.StandardSubject;

import java.util.List;

/**
 * 匹配者的候选人召回
 */
public interface CandidateRecallService {
    /**
     * 回忆一下锚定选择的候选标准科目。
     */
    List<StandardSubject> recallCandidates(AnchorSelection anchorSelection, MatchEngineContext context);
}
