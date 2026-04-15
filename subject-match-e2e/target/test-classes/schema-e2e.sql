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

CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL,
    description VARCHAR(256),
    update_time VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS t_ods_valuation_filedata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    row_data_number INT NOT NULL,
    row_data_json TEXT NOT NULL
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
    subjects_json TEXT,
    metrics_json TEXT
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
    header_detail_json TEXT
);

CREATE TABLE IF NOT EXISTS t_dwd_external_valuation_subject (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    subject_code VARCHAR(128),
    subject_name VARCHAR(512),
    currency VARCHAR(32),
    market_value DECIMAL(24, 8),
    market_value_ratio DECIMAL(18, 8),
    cost DECIMAL(24, 8),
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

CREATE TABLE IF NOT EXISTS t_subject_match_file_info (
    file_id BIGINT PRIMARY KEY,
    file_name_original VARCHAR(255),
    file_name_normalized VARCHAR(255),
    file_extension VARCHAR(32),
    mime_type VARCHAR(128),
    file_size_bytes BIGINT,
    file_fingerprint VARCHAR(128),
    source_channel VARCHAR(64),
    source_uri VARCHAR(512),
    storage_type VARCHAR(32),
    storage_uri VARCHAR(512),
    file_format VARCHAR(32),
    file_status VARCHAR(32),
    created_by VARCHAR(128),
    received_at TIMESTAMP,
    stored_at TIMESTAMP,
    last_processed_at TIMESTAMP,
    last_task_id BIGINT,
    error_message TEXT,
    source_meta_json TEXT,
    storage_meta_json TEXT,
    remark TEXT
);

CREATE TABLE IF NOT EXISTS t_subject_match_file_ingest_log (
    ingest_id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    source_channel VARCHAR(64),
    source_uri VARCHAR(512),
    channel_message_id VARCHAR(256),
    ingest_status VARCHAR(32),
    ingest_time TIMESTAMP,
    ingest_meta_json TEXT,
    created_by VARCHAR(128),
    error_message TEXT
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
    external_code VARCHAR(255),
    external_name VARCHAR(512),
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    standard_system VARCHAR(128),
    system_name VARCHAR(256)
);
