package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.SubjectRecord;

import java.util.List;

/**
 * 为外部主题选择锚定主题。
 */
public interface AnchorSelector {
    /**
     * 选择候选外部科目的锚定科目。
     */
    AnchorSelection select(SubjectRecord subject, List<SubjectRecord> allSubjects);
}
