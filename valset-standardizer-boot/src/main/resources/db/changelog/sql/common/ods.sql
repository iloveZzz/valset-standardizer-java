--liquibase formatted sql

--changeset codex:20260415-02-mysql-ods dbms:mysql
CREATE TABLE t_ods_valuation_filedata (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    row_data_number INT NOT NULL,
    row_data_json TEXT NOT NULL
);

CREATE TABLE t_ods_valuation_sheet_style (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sheet_name VARCHAR(128) NOT NULL,
    style_scope VARCHAR(32) NOT NULL,
    sheet_style_json TEXT NOT NULL,
    preview_row_count INT,
    created_at DATETIME
);

CREATE TABLE t_ods_valuation_data (
    subject_code VARCHAR(128),
    subject_name VARCHAR(512)
);

--changeset codex:20260415-02-postgres-ods dbms:postgresql
CREATE TABLE t_ods_valuation_filedata (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    row_data_number INTEGER NOT NULL,
    row_data_json TEXT NOT NULL
);

CREATE TABLE t_ods_valuation_sheet_style (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sheet_name VARCHAR(128) NOT NULL,
    style_scope VARCHAR(32) NOT NULL,
    sheet_style_json TEXT NOT NULL,
    preview_row_count INTEGER,
    created_at TIMESTAMP
);

CREATE TABLE t_ods_valuation_data (
    subject_code VARCHAR(128),
    subject_name VARCHAR(512)
);

--changeset codex:20260415-02-oracle-ods dbms:oracle
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

CREATE TABLE t_ods_valuation_data (
    subject_code VARCHAR2(128 CHAR),
    subject_name VARCHAR2(512 CHAR)
);
