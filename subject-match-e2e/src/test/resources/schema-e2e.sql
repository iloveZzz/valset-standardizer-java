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

CREATE TABLE IF NOT EXISTS t_subject_match_parsed_workbook (
    id BIGINT PRIMARY KEY,
    task_id BIGINT,
    file_id BIGINT,
    workbook_path VARCHAR(512),
    sheet_name VARCHAR(128),
    header_row_number INT,
    data_start_row_number INT,
    title VARCHAR(512),
    basic_info_json TEXT,
    headers_json TEXT,
    header_details_json TEXT,
    header_columns_json TEXT,
    subjects_json TEXT,
    metrics_json TEXT
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

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation_metric (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    metric_name VARCHAR(256),
    metric_type VARCHAR(64),
    metric_value VARCHAR(512),
    raw_values_json TEXT
);

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

CREATE TABLE IF NOT EXISTS tr_fm_jjhzgzb (
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

CREATE INDEX IF NOT EXISTS idx_pwb_file_id ON t_subject_match_parsed_workbook(file_id);
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

CREATE INDEX IF NOT EXISTS idx_tr_fm_jjhzgzb_org ON tr_fm_jjhzgzb(org_cd);
CREATE INDEX IF NOT EXISTS idx_tr_fm_jjhzgzb_subject ON tr_fm_jjhzgzb(subject_cd);
CREATE INDEX IF NOT EXISTS idx_tr_fm_jjhzgzb_biz_date ON tr_fm_jjhzgzb(biz_date);
CREATE INDEX IF NOT EXISTS idx_tr_fm_jjhzgzb_pd ON tr_fm_jjhzgzb(pd_cd);

CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_org ON tr_spv_jjhzgzb(org_cd);
CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_subject ON tr_spv_jjhzgzb(subject_cd);
CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_biz_date ON tr_spv_jjhzgzb(biz_date);
CREATE INDEX IF NOT EXISTS idx_tr_spv_jjhzgzb_pd ON tr_spv_jjhzgzb(pd_cd);
