-- Generated from PO classes in subject-match-java.
-- This DDL matches the current workflow task model with task_stage and per-stage timing fields.

CREATE TABLE t_subject_match_file_info (
    file_id NUMBER(19) PRIMARY KEY,
    file_name_original VARCHAR2(512) NOT NULL,
    file_name_normalized VARCHAR2(512),
    file_extension VARCHAR2(32),
    mime_type VARCHAR2(128),
    file_size_bytes NUMBER(19),
    file_fingerprint VARCHAR2(128) NOT NULL,
    source_channel VARCHAR2(64) NOT NULL,
    source_uri VARCHAR2(1024),
    storage_type VARCHAR2(32) NOT NULL,
    storage_uri VARCHAR2(1024),
    file_format VARCHAR2(32),
    file_status VARCHAR2(32) NOT NULL,
    created_by VARCHAR2(128),
    received_at TIMESTAMP,
    stored_at TIMESTAMP,
    last_processed_at TIMESTAMP,
    last_task_id NUMBER(19),
    error_message VARCHAR2(1024),
    source_meta_json CLOB,
    storage_meta_json CLOB,
    remark VARCHAR2(1024)
);

CREATE TABLE t_subject_match_task (
    task_id NUMBER(19) PRIMARY KEY,
    task_type VARCHAR2(64 CHAR) NOT NULL,
    task_stage VARCHAR2(32 CHAR),
    task_status VARCHAR2(32 CHAR) NOT NULL,
    business_key VARCHAR2(512 CHAR),
    file_id NUMBER(19),
    input_payload CLOB,
    result_payload CLOB,
    task_start_time TIMESTAMP,
    parse_task_time_ms NUMBER(19),
    standardize_time_ms NUMBER(19),
    match_standard_subject_time_ms NUMBER(19)
);

CREATE TABLE t_subject_match_schedule (
    schedule_id NUMBER(19) PRIMARY KEY,
    schedule_name VARCHAR2(256 CHAR),
    task_type VARCHAR2(64 CHAR) NOT NULL,
    cron_expression VARCHAR2(128 CHAR) NOT NULL,
    enabled NUMBER(1),
    schedule_payload CLOB,
    last_trigger_time TIMESTAMP,
    next_trigger_time TIMESTAMP
);

CREATE TABLE t_subject_match_result (
    id NUMBER(19) PRIMARY KEY,
    task_id NUMBER(19) NOT NULL,
    file_id NUMBER(19) NOT NULL,
    external_subject_code VARCHAR2(128 CHAR),
    external_subject_name VARCHAR2(512 CHAR),
    external_level NUMBER(10),
    external_is_leaf NUMBER(1),
    anchor_subject_code VARCHAR2(128 CHAR),
    anchor_subject_name VARCHAR2(512 CHAR),
    anchor_level NUMBER(10),
    anchor_path_text VARCHAR2(1024 CHAR),
    anchor_reason VARCHAR2(1024 CHAR),
    matched_standard_code VARCHAR2(128 CHAR),
    matched_standard_name VARCHAR2(512 CHAR),
    score NUMBER(18, 8),
    score_name NUMBER(18, 8),
    score_path NUMBER(18, 8),
    score_keyword NUMBER(18, 8),
    score_code NUMBER(18, 8),
    score_history NUMBER(18, 8),
    score_embedding NUMBER(18, 8),
    confidence_level VARCHAR2(32 CHAR),
    needs_review NUMBER(1),
    match_reason VARCHAR2(1024 CHAR),
    candidate_count NUMBER(10),
    top_candidates_json CLOB
);

CREATE TABLE t_subject_match_file_ingest_log (
    ingest_id NUMBER(19) PRIMARY KEY,
    file_id NUMBER(19) NOT NULL,
    source_channel VARCHAR2(64) NOT NULL,
    source_uri VARCHAR2(1024),
    channel_message_id VARCHAR2(256),
    ingest_status VARCHAR2(32) NOT NULL,
    ingest_time TIMESTAMP NOT NULL,
    ingest_meta_json CLOB,
    created_by VARCHAR2(128),
    error_message VARCHAR2(1024)
);

CREATE TABLE t_ods_valuation_filedata (
    id NUMBER(19) PRIMARY KEY,
    task_id NUMBER(19) NOT NULL,
    file_id NUMBER(19) NOT NULL,
    row_data_number NUMBER(10) NOT NULL,
    row_data_json CLOB NOT NULL
);

CREATE TABLE t_ods_valuation_sheet_style (
    id NUMBER(19) PRIMARY KEY,
    task_id NUMBER(19) NOT NULL,
    file_id NUMBER(19) NOT NULL,
    sheet_name VARCHAR2(128 CHAR) NOT NULL,
    style_scope VARCHAR2(32 CHAR) NOT NULL,
    sheet_style_json CLOB NOT NULL,
    preview_row_count NUMBER(10),
    created_at TIMESTAMP
);

CREATE TABLE t_dwd_external_valuation (
    id NUMBER(19) PRIMARY KEY,
    task_id NUMBER(19) NOT NULL,
    file_id NUMBER(19) NOT NULL,
    workbook_path VARCHAR2(512 CHAR),
    sheet_name VARCHAR2(128 CHAR),
    header_row_number NUMBER(10),
    data_start_row_number NUMBER(10),
    title VARCHAR2(512 CHAR)
);

CREATE TABLE t_dwd_external_valuation_basic_info (
    id NUMBER(19) PRIMARY KEY,
    valuation_id NUMBER(19) NOT NULL,
    sort_order NUMBER(10) NOT NULL,
    info_key VARCHAR2(128 CHAR),
    info_value VARCHAR2(512 CHAR)
);

CREATE TABLE t_dwd_external_valuation_header (
    id NUMBER(19) PRIMARY KEY,
    valuation_id NUMBER(19) NOT NULL,
    column_index NUMBER(10) NOT NULL,
    header_name VARCHAR2(256 CHAR),
    header_detail_json CLOB,
    header_column_meta_json CLOB
);

CREATE TABLE t_dwd_external_valuation_subject (
    id NUMBER(19) PRIMARY KEY,
    valuation_id NUMBER(19) NOT NULL,
    sheet_name VARCHAR2(128 CHAR),
    row_data_number NUMBER(10),
    subject_code VARCHAR2(128 CHAR),
    subject_name VARCHAR2(512 CHAR),
    level_no NUMBER(10),
    parent_code VARCHAR2(128 CHAR),
    root_code VARCHAR2(128 CHAR),
    segment_count NUMBER(10),
    path_codes_json CLOB,
    is_leaf NUMBER(1),
    raw_values_json CLOB
);

CREATE TABLE t_dwd_external_valuation_metric (
    id NUMBER(19) PRIMARY KEY,
    valuation_id NUMBER(19) NOT NULL,
    sheet_name VARCHAR2(128 CHAR),
    row_data_number NUMBER(10),
    metric_name VARCHAR2(256 CHAR),
    metric_type VARCHAR2(64 CHAR),
    metric_value VARCHAR2(512 CHAR),
    raw_values_json CLOB
);

CREATE TABLE t_ods_valuation_data (
    subject_code VARCHAR2(128 CHAR),
    subject_name VARCHAR2(512 CHAR)
);

CREATE TABLE t_ods_standard_subject (
    id NUMBER(19) PRIMARY KEY,
    standard_code VARCHAR2(128 CHAR),
    standard_name VARCHAR2(512 CHAR),
    parent_code VARCHAR2(128 CHAR),
    parent_name VARCHAR2(512 CHAR),
    level NUMBER(10),
    root_code VARCHAR2(128 CHAR),
    segment_count NUMBER(10),
    path_codes_json CLOB,
    path_names_json CLOB,
    path_text VARCHAR2(2048 CHAR),
    normalized_name VARCHAR2(512 CHAR),
    normalized_path_text VARCHAR2(2048 CHAR),
    placeholder NUMBER(1)
);

CREATE TABLE t_ods_mapping_hint (
    id NUMBER(19) PRIMARY KEY,
    source VARCHAR2(128 CHAR),
    normalized_key VARCHAR2(256 CHAR),
    standard_code VARCHAR2(128 CHAR),
    standard_name VARCHAR2(512 CHAR),
    support_count NUMBER(10),
    confidence NUMBER(18, 8)
);

CREATE TABLE t_ods_mapping_sample (
    id NUMBER(19) PRIMARY KEY,
    org_name VARCHAR2(256 CHAR),
    org_id VARCHAR2(128 CHAR),
    external_code VARCHAR2(500 CHAR),
    external_name VARCHAR2(512 CHAR),
    standard_code VARCHAR2(128 CHAR),
    standard_name VARCHAR2(512 CHAR),
    standard_system VARCHAR2(128 CHAR),
    system_name VARCHAR2(256 CHAR)
);

CREATE INDEX idx_ods_filedata_task_id ON t_ods_valuation_filedata(task_id);
CREATE INDEX idx_ods_filedata_file_id ON t_ods_valuation_filedata(file_id);
CREATE INDEX idx_ods_filedata_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
CREATE UNIQUE INDEX uk_ods_sheet_style_file_sheet_scope
    ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);
CREATE INDEX idx_ods_sheet_style_task_id ON t_ods_valuation_sheet_style(task_id);

CREATE INDEX idx_match_result_file_id ON t_subject_match_result(file_id);

CREATE INDEX idx_dwd_val_file_id ON t_dwd_external_valuation(file_id);
CREATE INDEX idx_dwd_basic_info_vid_order ON t_dwd_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX idx_dwd_header_vid_col ON t_dwd_external_valuation_header(valuation_id, column_index);
CREATE INDEX idx_dwd_subject_vid_row ON t_dwd_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX idx_dwd_metric_vid_row ON t_dwd_external_valuation_metric(valuation_id, row_data_number);

CREATE UNIQUE INDEX uk_subject_match_file_fingerprint
    ON t_subject_match_file_info(file_fingerprint);
CREATE INDEX idx_subject_match_file_channel_status
    ON t_subject_match_file_info(source_channel, file_status);
CREATE INDEX idx_subject_match_file_received_at
    ON t_subject_match_file_info(received_at);
CREATE INDEX idx_subject_match_file_last_task_id
    ON t_subject_match_file_info(last_task_id);

CREATE INDEX idx_subject_match_ingest_file_id
    ON t_subject_match_file_ingest_log(file_id);
CREATE INDEX idx_subject_match_ingest_channel_msg
    ON t_subject_match_file_ingest_log(source_channel, channel_message_id);

CREATE TABLE t_tr_jjhzgzb (
    id NUMBER(19) PRIMARY KEY,
    org_cd VARCHAR2(30 CHAR),
    pd_cd VARCHAR2(30 CHAR),
    biz_date VARCHAR2(8 CHAR),
    subject_cd VARCHAR2(100 CHAR),
    subject_nm VARCHAR2(300 CHAR),
    pa_subject_cd VARCHAR2(100 CHAR),
    pa_subject_nm VARCHAR2(300 CHAR),
    n_hldamt NUMBER(26, 4),
    n_hldcst NUMBER(26, 4),
    n_hldcst_locl NUMBER(26, 4),
    n_hldmkv NUMBER(26, 4),
    n_hldmkv_locl NUMBER(26, 4),
    n_hldvva NUMBER(26, 4),
    n_hldvva_l NUMBER(26, 4),
    ccy_cd VARCHAR2(3 CHAR),
    n_valrate NUMBER(26, 4),
    n_price_cost NUMBER(26, 4),
    n_valprice NUMBER(26, 4),
    n_cb_jz_bl NUMBER(26, 4),
    n_sz_jz_bl NUMBER(26, 4),
    n_zc_bl NUMBER(26, 8),
    susp_info VARCHAR2(300 CHAR),
    valuat_equity VARCHAR2(30 CHAR),
    fin_attr_id_d VARCHAR2(30 CHAR),
    fin_mkt_cd VARCHAR2(30 CHAR),
    time_stamp TIMESTAMP,
    cons_float_tp_cd VARCHAR2(30 CHAR),
    source_tp VARCHAR2(30 CHAR),
    source_sign VARCHAR2(300 CHAR),
    sn NUMBER(5),
    data_dt VARCHAR2(8 CHAR),
    is_audt NUMBER(1),
    audt_id VARCHAR2(30 CHAR),
    isin_cd VARCHAR2(30 CHAR)
);
COMMENT ON TABLE t_tr_jjhzgzb IS '基金持仓估值表';
CREATE TABLE t_tr_index (
    id NUMBER(19) PRIMARY KEY,
    org_cd VARCHAR2(30 CHAR),
    pd_cd VARCHAR2(60 CHAR),
    biz_date VARCHAR2(8 CHAR),
    indx_nm VARCHAR2(300 CHAR),
    indx_valu VARCHAR2(300 CHAR),
    source_tp VARCHAR2(30 CHAR),
    source_sign VARCHAR2(300 CHAR),
    time_stamp TIMESTAMP,
    is_audt NUMBER(1),
    audt_id VARCHAR2(30 CHAR)
);

COMMENT ON TABLE t_tr_index IS '资产估值指标信息（原始数据）';
CREATE INDEX idx_t_tr_jjhzgzb_org ON t_tr_jjhzgzb(org_cd);
CREATE INDEX idx_t_tr_jjhzgzb_subject ON t_tr_jjhzgzb(subject_cd);
CREATE INDEX idx_t_tr_jjhzgzb_biz_date ON t_tr_jjhzgzb(biz_date);
CREATE INDEX idx_t_tr_jjhzgzb_pd ON t_tr_jjhzgzb(pd_cd);
CREATE INDEX idx_t_tr_index_date_org_pd ON t_tr_index(biz_date, org_cd, pd_cd);

CREATE TABLE tr_spv_jjhzgzb (
    id NUMBER(19) PRIMARY KEY,
    org_cd VARCHAR2(30 CHAR),
    pd_cd VARCHAR2(30 CHAR),
    biz_date VARCHAR2(8 CHAR),
    subject_cd VARCHAR2(100 CHAR),
    subject_nm VARCHAR2(300 CHAR),
    pa_subject_cd VARCHAR2(100 CHAR),
    pa_subject_nm VARCHAR2(300 CHAR),
    n_hldamt NUMBER(26, 4),
    n_hldcst NUMBER(26, 4),
    n_hldcst_locl NUMBER(26, 4),
    n_hldmkv NUMBER(26, 4),
    n_hldmkv_locl NUMBER(26, 4),
    n_hldvva NUMBER(26, 4),
    n_hldvva_l NUMBER(26, 4),
    ccy_cd VARCHAR2(3 CHAR),
    n_valrate NUMBER(26, 4),
    n_price_cost NUMBER(26, 4),
    n_valprice NUMBER(26, 4),
    n_cb_jz_bl NUMBER(26, 4),
    n_sz_jz_bl NUMBER(26, 4),
    n_zc_bl NUMBER(26, 8),
    susp_info VARCHAR2(300 CHAR),
    valuat_equity VARCHAR2(30 CHAR),
    fin_attr_id_d VARCHAR2(30 CHAR),
    fin_mkt_cd VARCHAR2(30 CHAR),
    time_stamp TIMESTAMP,
    cons_float_tp_cd VARCHAR2(30 CHAR),
    source_tp VARCHAR2(30 CHAR),
    source_sign VARCHAR2(300 CHAR),
    sn NUMBER(5),
    data_dt VARCHAR2(8 CHAR),
    isin_cd VARCHAR2(30 CHAR)
);


COMMENT ON TABLE tr_spv_jjhzgzb IS '资管持仓估值表';
CREATE INDEX idx_tr_spv_jjhzgzb_org ON tr_spv_jjhzgzb(org_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_subject ON tr_spv_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_biz_date ON tr_spv_jjhzgzb(biz_date);
CREATE INDEX idx_tr_spv_jjhzgzb_pd ON tr_spv_jjhzgzb(pd_cd);
