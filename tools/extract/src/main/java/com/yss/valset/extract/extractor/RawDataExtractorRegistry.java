package com.yss.valset.extract.extractor;

import com.yss.valset.domain.exception.UnsupportedDataSourceException;
import com.yss.valset.domain.extractor.RawDataExtractor;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.extract.extractor.CsvRawDataExtractor;
import com.yss.valset.extract.extractor.PoiRawDataExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 根据数据源类型派发原始数据提取器。
 */
@Slf4j
@Component
public class RawDataExtractorRegistry {

    private final Map<DataSourceType, RawDataExtractor> extractorMap;

    public RawDataExtractorRegistry(PoiRawDataExtractor poiRawDataExtractor,
                                    CsvRawDataExtractor csvRawDataExtractor) {
        this.extractorMap = Map.of(
                DataSourceType.EXCEL, poiRawDataExtractor,
                DataSourceType.CSV, csvRawDataExtractor
        );
    }

    /**
     * 获取指定数据源类型的提取器。
     */
    public RawDataExtractor getExtractor(DataSourceType sourceType) {
        RawDataExtractor extractor = extractorMap.get(sourceType);
        if (extractor == null) {
            log.warn("未找到数据源类型 {} 对应的原始数据提取器", sourceType);
            throw new UnsupportedDataSourceException(sourceType);
        }
        log.debug("已选择 {} 对应的原始数据提取器 {}", sourceType, extractor.getClass().getSimpleName());
        return extractor;
    }
}
