package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.MappingSample;

import java.util.List;

/**
 * 映射样例落地表的网关。
 */
public interface MappingSampleGateway {

    /**
     * 获取全部映射样例。
     */
    List<MappingSample> findAll();

    /**
     * 先清空再批量保存映射样例。
     */
    void replaceAll(List<MappingSample> mappingSamples);
}
