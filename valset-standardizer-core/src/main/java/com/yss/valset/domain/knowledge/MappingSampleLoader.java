package com.yss.valset.domain.knowledge;

import com.yss.valset.domain.model.MappingSample;

import java.util.List;

/**
 * 用于映射评估样本的加载器。
 */
public interface MappingSampleLoader {
    /**
     * 加载映射样例数据。
     */
    List<MappingSample> load();
}
