package com.yss.subjectmatch.domain.matcher;

import com.yss.subjectmatch.domain.model.MatchContext;
import com.yss.subjectmatch.domain.model.SubjectMatchResult;
import com.yss.subjectmatch.domain.model.SubjectRecord;

import java.util.List;

/**
 * 主要配套发动机合同。
 */
public interface SubjectMatcher {
    /**
     * 一次性匹配所有科目。
     */
    List<SubjectMatchResult> matchSubjects(List<SubjectRecord> subjects, MatchContext context, int topK);

    /**
     * 将单个主题与完整主题集进行匹配。
     */
    SubjectMatchResult matchSubject(SubjectRecord subject, List<SubjectRecord> allSubjects, MatchContext context, int topK);
}
