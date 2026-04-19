package com.yss.valset.domain.gateway;

import com.yss.valset.domain.model.MappingHint;

import java.util.List;

/**
 * 历史映射经验落地表的网关。
 */
public interface MappingHintGateway {

    /**
     * 获取全部历史映射提示。
     */
    List<MappingHint> findAll();

    /**
     * 先清空再批量保存历史映射提示。
     */
    void replaceAll(List<MappingHint> mappingHints);
}
