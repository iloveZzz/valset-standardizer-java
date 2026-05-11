package com.yss.valset.domain.knowledge;

import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.StandardSubject;

import java.util.List;

/**
 * 标准主题数据源的加载程序。
 */
public interface StandardSubjectLoader {
    /**
     * 从数据源加载标准主题。
     */
    List<StandardSubject> load(DataSourceConfig config);
}
