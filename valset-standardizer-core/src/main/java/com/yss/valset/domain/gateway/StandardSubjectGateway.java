package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.StandardSubject;

import java.util.List;

/**
 * 标准科目落地表的网关。
 */
public interface StandardSubjectGateway {

    /**
     * 获取全部标准科目。
     */
    List<StandardSubject> findAll();

    /**
     * 先清空再批量保存标准科目。
     */
    void replaceAll(List<StandardSubject> standardSubjects);
}
