--liquibase formatted sql

--changeset codex:20260425-01-mysql-transfer-tag dbms:mysql
CREATE TABLE t_transfer_tag (
    tag_id BIGINT PRIMARY KEY,
    tag_code VARCHAR(128) NOT NULL,
    tag_name VARCHAR(256) NOT NULL,
    tag_value VARCHAR(256) NOT NULL,
    enabled TINYINT(1) NOT NULL,
    priority INT NOT NULL,
    match_strategy VARCHAR(64),
    script_language VARCHAR(64),
    script_body TEXT,
    regex_target_field VARCHAR(128),
    regex_pattern TEXT,
    scope_type VARCHAR(64),
    scope_config_json TEXT,
    effective_from DATETIME,
    effective_to DATETIME,
    tag_meta_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_transfer_tag_code (tag_code)
);

CREATE TABLE t_transfer_object_tag (
    id BIGINT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_code VARCHAR(128) NOT NULL,
    tag_name VARCHAR(256) NOT NULL,
    tag_value VARCHAR(256) NOT NULL,
    match_strategy VARCHAR(64),
    match_reason VARCHAR(1024),
    matched_field VARCHAR(128),
    matched_value VARCHAR(512),
    match_snapshot_json TEXT,
    created_at DATETIME,
    KEY idx_transfer_object_tag_transfer_id (transfer_id)
);

--changeset codex:20260425-01-postgres-transfer-tag dbms:postgresql
CREATE TABLE t_transfer_tag (
    tag_id BIGINT PRIMARY KEY,
    tag_code VARCHAR(128) NOT NULL,
    tag_name VARCHAR(256) NOT NULL,
    tag_value VARCHAR(256) NOT NULL,
    enabled BOOLEAN NOT NULL,
    priority INTEGER NOT NULL,
    match_strategy VARCHAR(64),
    script_language VARCHAR(64),
    script_body TEXT,
    regex_target_field VARCHAR(128),
    regex_pattern TEXT,
    scope_type VARCHAR(64),
    scope_config_json TEXT,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    tag_meta_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_transfer_tag_code UNIQUE (tag_code)
);

CREATE TABLE t_transfer_object_tag (
    id BIGINT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_code VARCHAR(128) NOT NULL,
    tag_name VARCHAR(256) NOT NULL,
    tag_value VARCHAR(256) NOT NULL,
    match_strategy VARCHAR(64),
    match_reason VARCHAR(1024),
    matched_field VARCHAR(128),
    matched_value VARCHAR(512),
    match_snapshot_json TEXT,
    created_at TIMESTAMP
);

CREATE INDEX idx_transfer_object_tag_transfer_id ON t_transfer_object_tag (transfer_id);
