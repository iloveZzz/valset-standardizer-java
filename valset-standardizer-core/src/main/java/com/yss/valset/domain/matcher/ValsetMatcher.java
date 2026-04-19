package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.MatchContext;
import com.yss.valset.domain.model.ValsetMatchResult;
import com.yss.valset.domain.model.SubjectRecord;

import java.util.List;

/**
 * 主要配套发动机合同。
 */
public interface ValsetMatcher {
    /**
     * 一次性匹配所有科目。
     */
    List<ValsetMatchResult> matchSubjects(List<SubjectRecord> subjects, MatchContext context, int topK);

    /**
     * 将单个主题与完整主题集进行匹配。
     */
    ValsetMatchResult matchSubject(SubjectRecord subject, List<SubjectRecord> allSubjects, MatchContext context, int topK);
}
