package com.yss.subjectmatch.knowledge;

import com.yss.subjectmatch.domain.knowledge.StandardSubjectLoader;
import com.yss.subjectmatch.domain.model.DataSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准科目加载门面。
 * <p>
 * 对应用层屏蔽不同数据源的标准科目加载实现，仅保留按数据源类型获取加载器的能力。
 * </p>
 */
@Slf4j
@Component
public class StandardSubjectLoaderRegistry {

    private final Map<DataSourceType, StandardSubjectLoader> loaderMap = new ConcurrentHashMap<>();

    public StandardSubjectLoaderRegistry(PoiStandardSubjectLoader poiLoader,
                                         CsvStandardSubjectLoader csvLoader,
                                         ApiStandardSubjectLoader apiLoader,
                                         DbStandardSubjectLoader dbLoader) {
        loaderMap.put(DataSourceType.EXCEL, poiLoader);
        loaderMap.put(DataSourceType.CSV, csvLoader);
        loaderMap.put(DataSourceType.API, apiLoader);
        loaderMap.put(DataSourceType.DB, dbLoader);
    }

    /**
     * 获取指定数据源类型对应的标准科目加载器。
     *
     * @param sourceType 数据源类型
     * @return 标准科目加载器
     */
    public StandardSubjectLoader getLoader(DataSourceType sourceType) {
        StandardSubjectLoader loader = loaderMap.get(sourceType);
        if (loader == null) {
            log.warn("未找到数据源类型 {} 对应的标准科目加载器", sourceType);
            throw new IllegalArgumentException("Unsupported data source type for standard subjects: " + sourceType);
        }
        log.debug("已选择 {} 对应的标准科目加载器 {}", sourceType, loader.getClass().getSimpleName());
        return loader;
    }
}
