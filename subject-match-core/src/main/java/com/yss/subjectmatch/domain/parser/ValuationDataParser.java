package com.yss.subjectmatch.domain.parser;

import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.DataSourceConfig;

import java.nio.file.Path;

/**
 * 估值数据分析器。
 * <p>
 * 当前分析阶段不再直接解析原始 Excel/CSV 文件，而是基于上一步抽取出的 ODS 原始行数据进行分析。
 * </p>
 */
public interface ValuationDataParser {
    /**
     * 将分析输入转换为内存中的结构化结果。
     *
     * @param config 数据源配置，其中 additionalParams 可携带原始数据文件标识等分析上下文
     * @return 结构化的估值分析结果
     */
    ParsedValuationData parse(DataSourceConfig config);
}
