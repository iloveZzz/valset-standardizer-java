package com.yss.subjectmatch.domain.model;

/**
 * 支持的估值表数据源类型。
 */
public enum DataSourceType {
    /** Excel 文件数据源 */
    EXCEL,
    /** CSV 文件数据源 */
    CSV,
    /** 外部接口 API 数据源 */
    API,
    /** 本地数据库标准数据表 t_ods_valuation_data */
    DB
}
