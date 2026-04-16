CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL,
    description VARCHAR(256),
    update_time VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS t_subject_match_file_info (
    file_id BIGINT PRIMARY KEY,
    file_name_original VARCHAR(512) NOT NULL,
    file_name_normalized VARCHAR(512),
    file_extension VARCHAR(32),
    mime_type VARCHAR(128),
    file_size_bytes BIGINT,
    file_fingerprint VARCHAR(128) NOT NULL,
    source_channel VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1024),
    storage_type VARCHAR(32) NOT NULL,
    storage_uri VARCHAR(1024),
    file_format VARCHAR(32),
    file_status VARCHAR(32) NOT NULL,
    created_by VARCHAR(128),
    received_at TIMESTAMP,
    stored_at TIMESTAMP,
    last_processed_at TIMESTAMP,
    last_task_id BIGINT,
    error_message VARCHAR(1024),
    source_meta_json TEXT,
    storage_meta_json TEXT,
    remark VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS t_subject_match_task (
    task_id BIGINT PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL,
    task_stage VARCHAR(32),
    task_status VARCHAR(32) NOT NULL,
    business_key VARCHAR(512),
    file_id BIGINT,
    input_payload TEXT,
    result_payload TEXT,
    task_start_time TIMESTAMP,
    parse_task_time_ms BIGINT,
    standardize_time_ms BIGINT,
    match_standard_subject_time_ms BIGINT
);

CREATE TABLE IF NOT EXISTS t_subject_match_schedule (
    schedule_id BIGINT PRIMARY KEY,
    schedule_name VARCHAR(256),
    task_type VARCHAR(64) NOT NULL,
    cron_expression VARCHAR(128) NOT NULL,
    enabled BOOLEAN,
    schedule_payload TEXT,
    last_trigger_time TIMESTAMP,
    next_trigger_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_subject_match_result (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    external_subject_code VARCHAR(128),
    external_subject_name VARCHAR(512),
    external_level INT,
    external_is_leaf BOOLEAN,
    anchor_subject_code VARCHAR(128),
    anchor_subject_name VARCHAR(512),
    anchor_level INT,
    anchor_path_text VARCHAR(1024),
    anchor_reason VARCHAR(1024),
    matched_standard_code VARCHAR(128),
    matched_standard_name VARCHAR(512),
    score DECIMAL(18, 8),
    score_name DECIMAL(18, 8),
    score_path DECIMAL(18, 8),
    score_keyword DECIMAL(18, 8),
    score_code DECIMAL(18, 8),
    score_history DECIMAL(18, 8),
    score_embedding DECIMAL(18, 8),
    confidence_level VARCHAR(32),
    needs_review BOOLEAN,
    match_reason VARCHAR(1024),
    candidate_count INT,
    top_candidates_json TEXT
);

CREATE TABLE IF NOT EXISTS t_subject_match_file_ingest_log (
    ingest_id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    source_channel VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1024),
    channel_message_id VARCHAR(256),
    ingest_status VARCHAR(32) NOT NULL,
    ingest_time TIMESTAMP NOT NULL,
    ingest_meta_json TEXT,
    created_by VARCHAR(128),
    error_message VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS t_ods_valuation_filedata (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    row_data_number INT NOT NULL,
    row_data_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS t_ods_valuation_sheet_style (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sheet_name VARCHAR(128) NOT NULL,
    style_scope VARCHAR(32) NOT NULL,
    sheet_style_json TEXT NOT NULL,
    preview_row_count INT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_ods_valuation_data (
    subject_code VARCHAR(128),
    subject_name VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS t_stg_external_valuation (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    workbook_path VARCHAR(512),
    sheet_name VARCHAR(128),
    header_row_number INT,
    data_start_row_number INT,
    title VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_stg_external_valuation_file_id ON t_stg_external_valuation(file_id);

CREATE TABLE IF NOT EXISTS t_stg_external_valuation_basic_info (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    info_key VARCHAR(128),
    info_value VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_stg_external_basic_info_vid ON t_stg_external_valuation_basic_info(valuation_id, sort_order);

CREATE TABLE IF NOT EXISTS t_stg_external_valuation_header (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    column_index INT NOT NULL,
    header_name VARCHAR(256),
    header_detail_json TEXT,
    header_column_meta_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_stg_external_header_vid ON t_stg_external_valuation_header(valuation_id, column_index);

CREATE TABLE IF NOT EXISTS t_stg_external_valuation_subject (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    subject_code VARCHAR(128),
    subject_name VARCHAR(512),
    level_no INT,
    parent_code VARCHAR(128),
    root_code VARCHAR(128),
    segment_count INT,
    path_codes_json TEXT,
    is_leaf BOOLEAN,
    raw_values_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_stg_external_subject_vid ON t_stg_external_valuation_subject(valuation_id, row_data_number);

CREATE TABLE IF NOT EXISTS t_stg_external_valuation_metric (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    metric_name VARCHAR(256),
    metric_type VARCHAR(64),
    metric_value VARCHAR(512),
    raw_values_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_stg_external_metric_vid ON t_stg_external_valuation_metric(valuation_id, row_data_number);

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    workbook_path VARCHAR(512),
    sheet_name VARCHAR(128),
    header_row_number INT,
    data_start_row_number INT,
    title VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation_basic_info (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    info_key VARCHAR(128),
    info_value VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation_header (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    column_index INT NOT NULL,
    header_name VARCHAR(256),
    header_detail_json TEXT,
    header_column_meta_json TEXT
);

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation_subject (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    subject_code VARCHAR(128),
    subject_name VARCHAR(512),
    level_no INT,
    parent_code VARCHAR(128),
    root_code VARCHAR(128),
    segment_count INT,
    path_codes_json TEXT,
    is_leaf BOOLEAN,
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    standard_values_json TEXT,
    mapping_rule_id BIGINT,
    mapping_source_id BIGINT,
    mapping_status VARCHAR(32),
    mapping_reason VARCHAR(512),
    mapping_confidence DECIMAL(10, 6),
    raw_values_json TEXT
);

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation_metric (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    metric_name VARCHAR(256),
    metric_type VARCHAR(64),
    metric_code VARCHAR(128),
    metric_standard_name VARCHAR(512),
    standard_value_text VARCHAR(512),
    standard_value_num DECIMAL(38, 12),
    standard_value_unit VARCHAR(64),
    standard_values_json TEXT,
    mapping_rule_id BIGINT,
    mapping_source_id BIGINT,
    mapping_status VARCHAR(32),
    mapping_reason VARCHAR(512),
    mapping_confidence DECIMAL(10, 6),
    raw_values_json TEXT
);

CREATE TABLE IF NOT EXISTS t_file_parse_rule (
    id BIGINT PRIMARY KEY,
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP,
    file_scene VARCHAR(64) NOT NULL,
    file_type_name VARCHAR(128) NOT NULL,
    region_name VARCHAR(64) NOT NULL,
    column_map VARCHAR(128) NOT NULL,
    column_map_name VARCHAR(256) NOT NULL,
    status BOOLEAN,
    multi_index BOOLEAN,
    required BOOLEAN
);

CREATE TABLE IF NOT EXISTS t_file_parse_source (
    id BIGINT PRIMARY KEY,
    file_type VARCHAR(128) NOT NULL,
    column_map VARCHAR(128) NOT NULL,
    column_name VARCHAR(256) NOT NULL,
    file_ext_info VARCHAR(512),
    status BOOLEAN,
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP
);

INSERT INTO t_file_parse_rule (id, creater, create_time, modifier, modify_time, file_scene, file_type_name, region_name, column_map, column_map_name, status, multi_index, required)
VALUES
    (1001, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'subject_cd', '科目代码', TRUE, FALSE, TRUE),
    (1002, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'subject_nm', '科目名称', TRUE, FALSE, TRUE),
    (1003, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'ccy_cd', '币种', TRUE, FALSE, FALSE),
    (1004, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_valrate', '汇率', TRUE, FALSE, FALSE),
    (1005, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_hldamt', '数量', TRUE, FALSE, FALSE),
    (1006, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_price_cost', '单位成本', TRUE, FALSE, FALSE),
    (1007, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_hldcst', '成本', TRUE, FALSE, FALSE),
    (1008, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_cb_jz_bl', '成本占比', TRUE, FALSE, FALSE),
    (1009, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_valprice', '行情', TRUE, FALSE, FALSE),
    (1010, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_hldmkv', '市值', TRUE, FALSE, FALSE),
    (1011, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_sz_jz_bl', '市值占比', TRUE, FALSE, FALSE),
    (1012, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'n_hldvva', '估值增值', TRUE, FALSE, FALSE),
    (1013, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'susp_info', '停牌信息', TRUE, FALSE, FALSE),
    (1014, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'valuat_equity', '权益信息', TRUE, FALSE, FALSE),
    (1015, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'biz_date', '日期', TRUE, FALSE, FALSE),
    (1016, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'source_tp', '来源类型', TRUE, FALSE, FALSE),
    (1017, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'jjhzgzb', '基金资产估值表', '通用', 'source_sign', '来源标记', TRUE, FALSE, FALSE);

INSERT INTO t_file_parse_source (id, file_type, column_map, column_name, file_ext_info, status, creater, create_time, modifier, modify_time)
VALUES
    (2001, 'COMMON', 'subject_cd', '科目代码', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2002, 'COMMON', 'subject_nm', '科目名称', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2003, 'COMMON', 'ccy_cd', '币种', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2004, 'COMMON', 'n_valrate', '汇率', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2005, 'COMMON', 'n_hldamt', '数量', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2006, 'COMMON', 'n_price_cost', '单位成本', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2007, 'COMMON', 'n_hldcst', '成本', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2008, 'COMMON', 'n_cb_jz_bl', '成本占比', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2009, 'COMMON', 'n_valprice', '行情', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2010, 'COMMON', 'n_hldmkv', '市值', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2011, 'COMMON', 'n_sz_jz_bl', '市值占比', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2012, 'COMMON', 'n_hldvva', '估值增值', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2013, 'COMMON', 'susp_info', '停牌信息', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2014, 'COMMON', 'valuat_equity', '权益信息', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2015, 'COMMON', 'biz_date', '日期', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2016, 'COMMON', 'source_tp', '来源类型', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP),
    (2017, 'COMMON', 'source_sign', '来源标记', NULL, TRUE, 'seed', CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS t_ods_standard_subject (
    id BIGINT PRIMARY KEY,
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    parent_code VARCHAR(128),
    parent_name VARCHAR(512),
    level INT,
    root_code VARCHAR(128),
    segment_count INT,
    path_codes_json TEXT,
    path_names_json TEXT,
    path_text VARCHAR(2048),
    normalized_name VARCHAR(512),
    normalized_path_text VARCHAR(2048),
    placeholder BOOLEAN
);

CREATE TABLE IF NOT EXISTS t_ods_mapping_hint (
    id BIGINT PRIMARY KEY,
    source VARCHAR(128),
    normalized_key VARCHAR(256),
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    support_count INT,
    confidence DECIMAL(18, 8)
);

CREATE TABLE IF NOT EXISTS t_ods_mapping_sample (
    id BIGINT PRIMARY KEY,
    org_name VARCHAR(256),
    org_id VARCHAR(128),
    external_code VARCHAR(500),
    external_name VARCHAR(512),
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    standard_system VARCHAR(128),
    system_name VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS t_tr_jjhzgzb (
    id BIGINT PRIMARY KEY,
    org_cd VARCHAR(30),
    pd_cd VARCHAR(30),
    biz_date VARCHAR(8),
    subject_cd VARCHAR(100),
    subject_nm VARCHAR(300),
    pa_subject_cd VARCHAR(100),
    pa_subject_nm VARCHAR(300),
    n_hldamt DECIMAL(26, 4),
    n_hldcst DECIMAL(26, 4),
    n_hldcst_locl DECIMAL(26, 4),
    n_hldmkv DECIMAL(26, 4),
    n_hldmkv_locl DECIMAL(26, 4),
    n_hldvva DECIMAL(26, 4),
    n_hldvva_l DECIMAL(26, 4),
    ccy_cd VARCHAR(3),
    n_valrate DECIMAL(26, 4),
    n_price_cost DECIMAL(26, 4),
    n_valprice DECIMAL(26, 4),
    n_cb_jz_bl DECIMAL(26, 4),
    n_sz_jz_bl DECIMAL(26, 4),
    n_zc_bl DECIMAL(26, 8),
    susp_info VARCHAR(300),
    valuat_equity VARCHAR(30),
    fin_attr_id_d VARCHAR(30),
    fin_mkt_cd VARCHAR(30),
    time_stamp TIMESTAMP,
    cons_float_tp_cd VARCHAR(30),
    source_tp VARCHAR(30),
    source_sign VARCHAR(300),
    sn SMALLINT,
    data_dt VARCHAR(8),
    is_audt BOOLEAN,
    audt_id VARCHAR(30),
    isin_cd VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS t_tr_index (
    id BIGINT PRIMARY KEY,
    org_cd VARCHAR(30),
    pd_cd VARCHAR(60),
    biz_date VARCHAR(8),
    indx_nm VARCHAR(300),
    indx_valu VARCHAR(300),
    source_tp VARCHAR(30),
    source_sign VARCHAR(300),
    time_stamp TIMESTAMP,
    is_audt BOOLEAN,
    audt_id VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS tr_spv_jjhzgzb (
    id BIGINT PRIMARY KEY,
    org_cd VARCHAR(30),
    pd_cd VARCHAR(30),
    biz_date VARCHAR(8),
    subject_cd VARCHAR(100),
    subject_nm VARCHAR(300),
    pa_subject_cd VARCHAR(100),
    pa_subject_nm VARCHAR(300),
    n_hldamt DECIMAL(26, 4),
    n_hldcst DECIMAL(26, 4),
    n_hldcst_locl DECIMAL(26, 4),
    n_hldmkv DECIMAL(26, 4),
    n_hldmkv_locl DECIMAL(26, 4),
    n_hldvva DECIMAL(26, 4),
    n_hldvva_l DECIMAL(26, 4),
    ccy_cd VARCHAR(3),
    n_valrate DECIMAL(26, 4),
    n_price_cost DECIMAL(26, 4),
    n_valprice DECIMAL(26, 4),
    n_cb_jz_bl DECIMAL(26, 4),
    n_sz_jz_bl DECIMAL(26, 4),
    n_zc_bl DECIMAL(26, 8),
    susp_info VARCHAR(300),
    valuat_equity VARCHAR(30),
    fin_attr_id_d VARCHAR(30),
    fin_mkt_cd VARCHAR(30),
    time_stamp TIMESTAMP,
    cons_float_tp_cd VARCHAR(30),
    source_tp VARCHAR(30),
    source_sign VARCHAR(300),
    sn SMALLINT,
    data_dt VARCHAR(8),
    isin_cd VARCHAR(30)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_subject_match_file_fingerprint ON t_subject_match_file_info(file_fingerprint);
CREATE INDEX IF NOT EXISTS idx_subject_match_file_channel_status ON t_subject_match_file_info(source_channel, file_status);
CREATE INDEX IF NOT EXISTS idx_subject_match_file_received_at ON t_subject_match_file_info(received_at);
CREATE INDEX IF NOT EXISTS idx_subject_match_file_last_task_id ON t_subject_match_file_info(last_task_id);
CREATE INDEX IF NOT EXISTS idx_subject_match_ingest_file_id ON t_subject_match_file_ingest_log(file_id);
CREATE INDEX IF NOT EXISTS idx_subject_match_ingest_channel_msg ON t_subject_match_file_ingest_log(source_channel, channel_message_id);

CREATE INDEX IF NOT EXISTS idx_ods_filedata_task_id ON t_ods_valuation_filedata(task_id);
CREATE INDEX IF NOT EXISTS idx_ods_filedata_file_id ON t_ods_valuation_filedata(file_id);
CREATE INDEX IF NOT EXISTS idx_ods_filedata_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ods_sheet_style_file_sheet_scope ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);
CREATE INDEX IF NOT EXISTS idx_ods_sheet_style_task_id ON t_ods_valuation_sheet_style(task_id);

CREATE INDEX IF NOT EXISTS idx_match_result_file_id ON t_subject_match_result(file_id);

CREATE INDEX IF NOT EXISTS idx_dwd_val_file_id ON t_dwd_external_valuation(file_id);
CREATE INDEX IF NOT EXISTS idx_dwd_basic_info_vid_order ON t_dwd_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_dwd_header_vid_col ON t_dwd_external_valuation_header(valuation_id, column_index);
CREATE INDEX IF NOT EXISTS idx_dwd_subject_vid_row ON t_dwd_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX IF NOT EXISTS idx_dwd_metric_vid_row ON t_dwd_external_valuation_metric(valuation_id, row_data_number);

CREATE INDEX IF NOT EXISTS idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
CREATE INDEX IF NOT EXISTS idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
CREATE INDEX IF NOT EXISTS idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
CREATE INDEX IF NOT EXISTS idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX IF NOT EXISTS idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX IF NOT EXISTS idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

CREATE INDEX IF NOT EXISTS idx_t_tr_jjhzgzb_org ON t_tr_jjhzgzb(org_cd);
CREATE INDEX IF NOT EXISTS idx_t_tr_jjhzgzb_subject ON t_tr_jjhzgzb(subject_cd);
CREATE INDEX IF NOT EXISTS idx_t_tr_jjhzgzb_biz_date ON t_tr_jjhzgzb(biz_date);
CREATE INDEX IF NOT EXISTS idx_t_tr_jjhzgzb_pd ON t_tr_jjhzgzb(pd_cd);

CREATE INDEX IF NOT EXISTS idx_t_tr_index_date_org_pd ON t_tr_index(biz_date, org_cd, pd_cd);

CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_org ON tr_spv_jjhzgzb(org_cd);
CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_subject ON tr_spv_jjhzgzb(subject_cd);
CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_biz_date ON tr_spv_jjhzgzb(biz_date);
CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_pd ON tr_spv_jjhzgzb(pd_cd);
