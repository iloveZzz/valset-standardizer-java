--liquibase formatted sql

--changeset codex:20260415-03-mysql-dwd dbms:mysql
CREATE TABLE t_dwd_external_valuation (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    workbook_path VARCHAR(512),
    sheet_name VARCHAR(128),
    header_row_number INT,
    data_start_row_number INT,
    title VARCHAR(512)
);

CREATE TABLE t_dwd_external_valuation_basic_info (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    info_key VARCHAR(128),
    info_value VARCHAR(512)
);

CREATE TABLE t_dwd_external_valuation_header (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    column_index INT NOT NULL,
    header_name VARCHAR(256),
    header_detail_json TEXT,
    header_column_meta_json TEXT
);

CREATE TABLE t_dwd_external_valuation_subject (
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
    is_leaf TINYINT(1),
    raw_values_json TEXT
);

CREATE TABLE t_dwd_external_valuation_metric (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INT,
    metric_name VARCHAR(256),
    metric_type VARCHAR(64),
    metric_value VARCHAR(512),
    raw_values_json TEXT
);

--changeset codex:20260415-03-postgres-dwd dbms:postgresql
CREATE TABLE t_dwd_external_valuation (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    workbook_path VARCHAR(512),
    sheet_name VARCHAR(128),
    header_row_number INTEGER,
    data_start_row_number INTEGER,
    title VARCHAR(512)
);

CREATE TABLE t_dwd_external_valuation_basic_info (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sort_order INTEGER NOT NULL,
    info_key VARCHAR(128),
    info_value VARCHAR(512)
);

CREATE TABLE t_dwd_external_valuation_header (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    column_index INTEGER NOT NULL,
    header_name VARCHAR(256),
    header_detail_json TEXT,
    header_column_meta_json TEXT
);

CREATE TABLE t_dwd_external_valuation_subject (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INTEGER,
    subject_code VARCHAR(128),
    subject_name VARCHAR(512),
    level_no INTEGER,
    parent_code VARCHAR(128),
    root_code VARCHAR(128),
    segment_count INTEGER,
    path_codes_json TEXT,
    is_leaf BOOLEAN,
    raw_values_json TEXT
);

CREATE TABLE t_dwd_external_valuation_metric (
    id BIGINT PRIMARY KEY,
    valuation_id BIGINT NOT NULL,
    sheet_name VARCHAR(128),
    row_data_number INTEGER,
    metric_name VARCHAR(256),
    metric_type VARCHAR(64),
    metric_value VARCHAR(512),
    raw_values_json TEXT
);

--changeset codex:20260415-03-oracle-dwd dbms:oracle
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
