package com.yss.valset.domain.exporter;

import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.ValsetMatchResult;

import java.util.List;
import java.util.Map;

/**
 * 导出已解析、匹配和评估的工件。
 */
public interface ResultExporter {
    /**
     * 导出解析工件。
     */
    void exportParsedValuationData(Long taskId, ParsedValuationData parsedValuationData);

    /**
     * 导出匹配工件。
     */
    void exportMatchResults(Long taskId, ParsedValuationData parsedValuationData, List<ValsetMatchResult> results);

    /**
     * 导出映射评估工件。
     */
    void exportMappingEvaluation(Long taskId, Map<String, Object> evaluationResult);
}
