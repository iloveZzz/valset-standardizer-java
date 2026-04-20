--liquibase formatted sql

--changeset codex:20260420-01-mysql-transfer dbms:mysql
CREATE TABLE t_transfer_object (
    transfer_id BIGINT PRIMARY KEY,
    source_id BIGINT,
    source_type VARCHAR(32),
    source_code VARCHAR(128),
    original_name VARCHAR(512) NOT NULL,
    normalized_name VARCHAR(512),
    extension VARCHAR(32),
    mime_type VARCHAR(128),
    size_bytes BIGINT,
    fingerprint VARCHAR(128) NOT NULL,
    source_ref VARCHAR(1024),
    local_temp_path VARCHAR(1024),
    status VARCHAR(32) NOT NULL,
    received_at DATETIME,
    stored_at DATETIME,
    route_id BIGINT,
    error_message VARCHAR(1024),
    file_meta_json TEXT,
    UNIQUE KEY uk_transfer_object_fingerprint (fingerprint)
);

CREATE TABLE t_transfer_rule (
    rule_id BIGINT PRIMARY KEY,
    rule_code VARCHAR(128) NOT NULL,
    rule_name VARCHAR(256) NOT NULL,
    rule_version VARCHAR(64),
    enabled TINYINT(1) NOT NULL,
    priority INT NOT NULL,
    match_strategy VARCHAR(64),
    script_language VARCHAR(64),
    script_body TEXT,
    effective_from DATETIME,
    effective_to DATETIME,
    rule_meta_json TEXT,
    UNIQUE KEY uk_transfer_rule_code (rule_code)
);

CREATE TABLE t_transfer_route (
    route_id BIGINT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_code VARCHAR(128) NOT NULL,
    target_path VARCHAR(1024),
    rename_pattern VARCHAR(512),
    route_status VARCHAR(32) NOT NULL,
    route_meta_json TEXT,
    KEY idx_transfer_route_transfer_id (transfer_id)
);

CREATE TABLE t_transfer_delivery_record (
    delivery_id BIGINT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    transfer_id BIGINT,
    target_type VARCHAR(32) NOT NULL,
    target_code VARCHAR(128) NOT NULL,
    execute_status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    request_snapshot_json TEXT,
    response_snapshot_json TEXT,
    error_message VARCHAR(1024),
    delivered_at DATETIME,
    KEY idx_transfer_delivery_route_id (route_id)
);

CREATE TABLE t_transfer_target (
    target_id BIGINT PRIMARY KEY,
    target_code VARCHAR(128) NOT NULL,
    target_name VARCHAR(256) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    enabled TINYINT(1) NOT NULL,
    target_path_template VARCHAR(1024),
    connection_config_json TEXT,
    target_meta_json TEXT,
    UNIQUE KEY uk_transfer_target_code (target_code)
);

--changeset codex:20260420-01-postgres-transfer dbms:postgresql
CREATE TABLE t_transfer_object (
    transfer_id BIGINT PRIMARY KEY,
    source_id BIGINT,
    source_type VARCHAR(32),
    source_code VARCHAR(128),
    original_name VARCHAR(512) NOT NULL,
    normalized_name VARCHAR(512),
    extension VARCHAR(32),
    mime_type VARCHAR(128),
    size_bytes BIGINT,
    fingerprint VARCHAR(128) NOT NULL,
    source_ref VARCHAR(1024),
    local_temp_path VARCHAR(1024),
    status VARCHAR(32) NOT NULL,
    received_at TIMESTAMP,
    stored_at TIMESTAMP,
    route_id BIGINT,
    error_message VARCHAR(1024),
    file_meta_json TEXT,
    CONSTRAINT uk_transfer_object_fingerprint UNIQUE (fingerprint)
);

CREATE TABLE t_transfer_rule (
    rule_id BIGINT PRIMARY KEY,
    rule_code VARCHAR(128) NOT NULL,
    rule_name VARCHAR(256) NOT NULL,
    rule_version VARCHAR(64),
    enabled BOOLEAN NOT NULL,
    priority INTEGER NOT NULL,
    match_strategy VARCHAR(64),
    script_language VARCHAR(64),
    script_body TEXT,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    rule_meta_json TEXT,
    CONSTRAINT uk_transfer_rule_code UNIQUE (rule_code)
);

CREATE TABLE t_transfer_route (
    route_id BIGINT PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_code VARCHAR(128) NOT NULL,
    target_path VARCHAR(1024),
    rename_pattern VARCHAR(512),
    route_status VARCHAR(32) NOT NULL,
    route_meta_json TEXT
);

CREATE INDEX idx_transfer_route_transfer_id ON t_transfer_route (transfer_id);

CREATE TABLE t_transfer_delivery_record (
    delivery_id BIGINT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    transfer_id BIGINT,
    target_type VARCHAR(32) NOT NULL,
    target_code VARCHAR(128) NOT NULL,
    execute_status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    request_snapshot_json TEXT,
    response_snapshot_json TEXT,
    error_message VARCHAR(1024),
    delivered_at TIMESTAMP
);

CREATE INDEX idx_transfer_delivery_route_id ON t_transfer_delivery_record (route_id);

CREATE TABLE t_transfer_target (
    target_id BIGINT PRIMARY KEY,
    target_code VARCHAR(128) NOT NULL,
    target_name VARCHAR(256) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    target_path_template VARCHAR(1024),
    connection_config_json TEXT,
    target_meta_json TEXT,
    CONSTRAINT uk_transfer_target_code UNIQUE (target_code)
);
