package com.yss.valset.domain.exception;

import com.yss.valset.common.exception.BizException;

/**
 * Exception thrown when an unsupported data source type is requested.
 * This occurs when the RawDataExtractorRegistry receives a DataSourceType
 * that does not have a corresponding extractor implementation.
 */
public class UnsupportedDataSourceException extends BizException {

    public UnsupportedDataSourceException(Object dataSourceType) {
        super("UNSUPPORTED_DATA_SOURCE", "Unsupported data source type: " + dataSourceType);
    }
}
