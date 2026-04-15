package com.yss.subjectmatch.analysis.parser.facade;

import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import com.yss.subjectmatch.domain.parser.ValuationDataParserProvider;
import com.yss.subjectmatch.analysis.parser.api.ApiValuationDataParser;
import com.yss.subjectmatch.analysis.parser.db.DbValuationDataParser;
import com.yss.subjectmatch.analysis.parser.file.OdsValuationDataParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 估值分析门面。
 * <p>
 * 对应用层屏蔽具体分析器路由细节，仅暴露按数据源类型获取分析器的能力。
 * </p>
 */
@Slf4j
@Component
public class ValuationAnalysisFacade implements ValuationDataParserProvider {

    private final Map<DataSourceType, ValuationDataParser> parserMap = new ConcurrentHashMap<>();

    public ValuationAnalysisFacade(OdsValuationDataParser odsValuationDataParser,
                                   ApiValuationDataParser apiValuationDataParser,
                                   DbValuationDataParser dbValuationDataParser) {
        parserMap.put(DataSourceType.EXCEL, odsValuationDataParser);
        parserMap.put(DataSourceType.CSV, odsValuationDataParser);
        parserMap.put(DataSourceType.API, apiValuationDataParser);
        parserMap.put(DataSourceType.DB, dbValuationDataParser);
    }

    /**
     * 获取指定数据源类型对应的估值分析器。
     *
     * @param sourceType 数据源类型
     * @return 对应的估值分析器
     */
    @Override
    public ValuationDataParser getParser(DataSourceType sourceType) {
        ValuationDataParser parser = parserMap.get(sourceType);
        if (parser == null) {
            log.warn("未找到数据源类型 {} 对应的估值分析器", sourceType);
            throw new IllegalArgumentException("Unsupported data source type: " + sourceType);
        }
        log.debug("已选择 {} 对应的估值分析器 {}", sourceType, parser.getClass().getSimpleName());
        return parser;
    }
}
