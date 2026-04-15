package com.yss.subjectmatch.analysis.parser.db;

import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据库来源估值解析器占位实现。
 *
 * <p>当前核心流程已切换为基于 ODS 原始行表分析，DB 来源暂保留接口兼容。</p>
 */
@Slf4j
@Component
public class DbValuationDataParser implements ValuationDataParser {

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        log.warn("DB 来源估值解析暂未启用，sourceUri={}, sourceType={}", config.getSourceUri(), config.getSourceType());
        return ParsedValuationData.builder()
                .workbookPath("db://unsupported")
                .title("DB Source Unsupported")
                .basicInfo(Map.of())
                .headers(List.of())
                .headerDetails(List.of())
                .headerColumns(List.of())
                .subjects(List.of())
                .metrics(List.of())
                .build();
    }
}
