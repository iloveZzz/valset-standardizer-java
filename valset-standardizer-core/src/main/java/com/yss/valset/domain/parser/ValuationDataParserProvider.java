package com.yss.valset.domain.parser;

import com.yss.valset.domain.model.DataSourceType;

/**
 * 估值数据分析器门面。
 * <p>
 * 应用层只依赖该接口选择合适的分析器，不直接依赖具体路由实现。
 * </p>
 */
public interface ValuationDataParserProvider {

    /**
     * 根据数据源类型获取对应的估值数据分析器。
     *
     * @param sourceType 数据源类型
     * @return 对应的估值数据分析器
     */
    ValuationDataParser getParser(DataSourceType sourceType);
}
