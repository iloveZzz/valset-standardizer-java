--liquibase formatted sql

--changeset codex:20260415-01-mysql-core dbms:mysql
--validCheckSum: 9:bca0e4e496a402a86d2b1311f226fe69
CREATE TABLE leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL,
    description VARCHAR(256),
    update_time VARCHAR(64)
);

CREATE TABLE t_subject_match_task (
    task_id BIGINT PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL,
    task_stage VARCHAR(32),
    task_status VARCHAR(32) NOT NULL,
    business_key VARCHAR(512),
    file_id BIGINT,
    input_payload TEXT,
    result_payload TEXT,
    task_start_time DATETIME,
    parse_task_time_ms BIGINT,
    standardize_time_ms BIGINT,
    match_standard_subject_time_ms BIGINT
);

CREATE TABLE t_subject_match_result (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    external_subject_code VARCHAR(128),
    external_subject_name VARCHAR(512),
    external_level INT,
    external_is_leaf TINYINT(1),
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
    needs_review TINYINT(1),
    match_reason VARCHAR(1024),
    candidate_count INT,
    top_candidates_json TEXT
);

CREATE TABLE t_valset_file_ingest_log (
    ingest_id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    source_channel VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1024),
    channel_message_id VARCHAR(256),
    ingest_status VARCHAR(32) NOT NULL,
    ingest_time DATETIME NOT NULL,
    ingest_meta_json TEXT,
    created_by VARCHAR(128),
    error_message VARCHAR(1024)
);

--changeset codex:20260415-01-postgres-core dbms:postgresql
--validCheckSum: 9:bca0e4e496a402a86d2b1311f226fe69
CREATE TABLE leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INTEGER NOT NULL,
    description VARCHAR(256),
    update_time VARCHAR(64)
);

CREATE TABLE t_subject_match_task (
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

CREATE TABLE t_subject_match_result (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    external_subject_code VARCHAR(128),
    external_subject_name VARCHAR(512),
    external_level INTEGER,
    external_is_leaf BOOLEAN,
    anchor_subject_code VARCHAR(128),
    anchor_subject_name VARCHAR(512),
    anchor_level INTEGER,
    anchor_path_text VARCHAR(1024),
    anchor_reason VARCHAR(1024),
    matched_standard_code VARCHAR(128),
    matched_standard_name VARCHAR(512),
    score NUMERIC(18, 8),
    score_name NUMERIC(18, 8),
    score_path NUMERIC(18, 8),
    score_keyword NUMERIC(18, 8),
    score_code NUMERIC(18, 8),
    score_history NUMERIC(18, 8),
    score_embedding NUMERIC(18, 8),
    confidence_level VARCHAR(32),
    needs_review BOOLEAN,
    match_reason VARCHAR(1024),
    candidate_count INTEGER,
    top_candidates_json TEXT
);

CREATE TABLE t_valset_file_ingest_log (
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

--changeset codex:20260415-01-oracle-core dbms:oracle
--validCheckSum: 9:bca0e4e496a402a86d2b1311f226fe69
CREATE TABLE leaf_alloc (
    biz_tag VARCHAR2(128 CHAR) PRIMARY KEY,
    max_id NUMBER(19) NOT NULL,
    step NUMBER(10) NOT NULL,
    description VARCHAR2(256 CHAR),
    update_time VARCHAR2(64 CHAR)
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

CREATE TABLE t_valset_file_ingest_log (
    ingest_id NUMBER(19) PRIMARY KEY,
    file_id NUMBER(19) NOT NULL,
    source_channel VARCHAR2(64 CHAR) NOT NULL,
    source_uri VARCHAR2(1024 CHAR),
    channel_message_id VARCHAR2(256 CHAR),
    ingest_status VARCHAR2(32 CHAR) NOT NULL,
    ingest_time TIMESTAMP NOT NULL,
    ingest_meta_json CLOB,
    created_by VARCHAR2(128 CHAR),
    error_message VARCHAR2(1024 CHAR)
);
