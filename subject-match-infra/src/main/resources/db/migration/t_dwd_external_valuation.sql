-- DWD Layer Tables: External Valuation Standard Data
-- 该组表用于保存由 ODS 原始行数据分析得到的外部估值标准结果，
-- 包含主表、基础信息、表头、估值明细和指标数据。

CREATE TABLE t_dwd_external_valuation (
    id                    BIGINT PRIMARY KEY COMMENT '主键',
    task_id               BIGINT      NOT NULL COMMENT '解析任务标识',
    file_id               BIGINT      NOT NULL COMMENT '文件标识',
    workbook_path         VARCHAR(512)         COMMENT '源文件路径',
    sheet_name            VARCHAR(128)         COMMENT '工作表名称',
    header_row_number     INT                  COMMENT '表头起始行号',
    data_start_row_number INT                  COMMENT '数据起始行号',
    title                 VARCHAR(512)         COMMENT '标题'
) COMMENT='DWD 外部估值主表';

CREATE INDEX idx_dwd_external_valuation_file_id ON t_dwd_external_valuation(file_id);

CREATE TABLE t_dwd_external_valuation_basic_info (
    id          BIGINT PRIMARY KEY COMMENT '主键',
    valuation_id BIGINT      NOT NULL COMMENT 'DWD 主表标识',
    sort_order  INT         NOT NULL COMMENT '顺序',
    info_key    VARCHAR(128)         COMMENT '基础信息键',
    info_value  VARCHAR(512)         COMMENT '基础信息值'
) COMMENT='DWD 外部估值基础信息表';

CREATE INDEX idx_dwd_external_basic_info_vid ON t_dwd_external_valuation_basic_info(valuation_id, sort_order);

CREATE TABLE t_dwd_external_valuation_header (
    id                BIGINT PRIMARY KEY COMMENT '主键',
    valuation_id      BIGINT NOT NULL COMMENT 'DWD 主表标识',
    column_index      INT    NOT NULL COMMENT '列序号，从0开始',
    header_name       VARCHAR(256) COMMENT '表头名称',
    header_detail_json TEXT        COMMENT '多层表头明细 JSON'
) COMMENT='DWD 外部估值表头表';

CREATE INDEX idx_dwd_external_header_vid ON t_dwd_external_valuation_header(valuation_id, column_index);

CREATE TABLE t_dwd_external_valuation_subject (
    id                 BIGINT PRIMARY KEY COMMENT '主键',
    valuation_id       BIGINT NOT NULL COMMENT 'DWD 主表标识',
    sheet_name         VARCHAR(128) COMMENT '工作表名称',
    row_data_number         INT          COMMENT '源行号',
    subject_code       VARCHAR(128) COMMENT '外部科目编码',
    subject_name       VARCHAR(512) COMMENT '外部科目名称',
    currency           VARCHAR(32)  COMMENT '币种',
    market_value       DECIMAL(24, 8) COMMENT '市值',
    market_value_ratio DECIMAL(18, 8) COMMENT '市值占比',
    cost               DECIMAL(24, 8) COMMENT '成本',
    level_no           INT            COMMENT '层级',
    parent_code        VARCHAR(128)   COMMENT '父级科目编码',
    root_code          VARCHAR(128)   COMMENT '根科目编码',
    segment_count      INT            COMMENT '路径分段数',
    path_codes_json    TEXT           COMMENT '路径编码 JSON',
    is_leaf            TINYINT(1)     COMMENT '是否叶子节点',
    raw_values_json    TEXT           COMMENT '原始行值 JSON'
) COMMENT='DWD 外部估值明细表';

CREATE INDEX idx_dwd_external_subject_vid ON t_dwd_external_valuation_subject(valuation_id, row_data_number);

CREATE TABLE t_dwd_external_valuation_metric (
    id             BIGINT PRIMARY KEY COMMENT '主键',
    valuation_id   BIGINT NOT NULL COMMENT 'DWD 主表标识',
    sheet_name     VARCHAR(128) COMMENT '工作表名称',
    row_data_number     INT          COMMENT '源行号',
    metric_name    VARCHAR(256) COMMENT '指标名称',
    metric_type    VARCHAR(64)  COMMENT '指标类型',
    metric_value   VARCHAR(512) COMMENT '指标值',
    raw_values_json TEXT        COMMENT '指标原始值 JSON'
) COMMENT='DWD 外部估值指标表';

CREATE INDEX idx_dwd_external_metric_vid ON t_dwd_external_valuation_metric(valuation_id, row_data_number);
