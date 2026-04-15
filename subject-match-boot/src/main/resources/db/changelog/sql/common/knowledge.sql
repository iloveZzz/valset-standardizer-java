--liquibase formatted sql

--changeset codex:20260415-04-mysql-knowledge dbms:mysql
CREATE TABLE t_ods_standard_subject (
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
    placeholder TINYINT(1)
);

CREATE TABLE t_ods_mapping_hint (
    id BIGINT PRIMARY KEY,
    source VARCHAR(128),
    normalized_key VARCHAR(256),
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    support_count INT,
    confidence DECIMAL(18, 8)
);

CREATE TABLE t_ods_mapping_sample (
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

--changeset codex:20260415-04-postgres-knowledge dbms:postgresql
CREATE TABLE t_ods_standard_subject (
    id BIGINT PRIMARY KEY,
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    parent_code VARCHAR(128),
    parent_name VARCHAR(512),
    level INTEGER,
    root_code VARCHAR(128),
    segment_count INTEGER,
    path_codes_json TEXT,
    path_names_json TEXT,
    path_text VARCHAR(2048),
    normalized_name VARCHAR(512),
    normalized_path_text VARCHAR(2048),
    placeholder BOOLEAN
);

CREATE TABLE t_ods_mapping_hint (
    id BIGINT PRIMARY KEY,
    source VARCHAR(128),
    normalized_key VARCHAR(256),
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    support_count INTEGER,
    confidence NUMERIC(18, 8)
);

CREATE TABLE t_ods_mapping_sample (
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

--changeset codex:20260415-04-oracle-knowledge dbms:oracle
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
