--liquibase formatted sql

--changeset codex:20260506-01-mysql-workflow-platform dbms:mysql
CREATE TABLE IF NOT EXISTS t_etl_workflow_definition (
    workflow_id VARCHAR(64) PRIMARY KEY,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_name VARCHAR(256) NOT NULL,
    workflow_version_no INT NOT NULL,
    platform_type VARCHAR(64) NOT NULL,
    description VARCHAR(1024),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_etl_workflow_definition_code_version (workflow_code, workflow_version_no),
    KEY idx_etl_workflow_definition_code (workflow_code),
    KEY idx_etl_workflow_definition_platform (platform_type)
);

CREATE TABLE IF NOT EXISTS t_etl_workflow_stage (
    stage_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    stage_name VARCHAR(128) NOT NULL,
    stage_order INT NOT NULL,
    description VARCHAR(512),
    retryable TINYINT(1) NOT NULL DEFAULT 1,
    timeout_seconds INT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_etl_workflow_stage_code (workflow_id, stage_code),
    KEY idx_etl_workflow_stage_workflow_order (workflow_id, stage_order)
);

CREATE TABLE IF NOT EXISTS t_etl_workflow_engine_binding (
    binding_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    platform_type VARCHAR(64) NOT NULL,
    external_workflow_id VARCHAR(256),
    external_project_code VARCHAR(128),
    external_namespace VARCHAR(128),
    external_job_group VARCHAR(128),
    external_job_handler VARCHAR(128),
    config_json TEXT,
    attributes_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_etl_workflow_binding_workflow (workflow_id),
    KEY idx_etl_workflow_binding_platform (platform_type)
);

CREATE TABLE IF NOT EXISTS t_etl_workflow_instance (
    instance_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_version_no INT NOT NULL,
    platform_type VARCHAR(64) NOT NULL,
    business_key VARCHAR(256),
    current_stage_code VARCHAR(64),
    external_instance_id VARCHAR(256),
    external_workflow_id VARCHAR(256),
    status VARCHAR(32),
    raw_status VARCHAR(64),
    trigger_time DATETIME,
    start_time DATETIME,
    end_time DATETIME,
    message VARCHAR(1024),
    context_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_etl_workflow_instance_code_version (workflow_code, workflow_version_no),
    KEY idx_etl_workflow_instance_status (status),
    KEY idx_etl_workflow_instance_external (external_instance_id)
);

CREATE TABLE IF NOT EXISTS t_etl_workflow_stage_log (
    log_id VARCHAR(64) PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_version_no INT NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    stage_name VARCHAR(128),
    stage_order INT,
    status VARCHAR(32),
    raw_status VARCHAR(64),
    message VARCHAR(1024),
    start_time DATETIME,
    end_time DATETIME,
    payload_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_etl_workflow_stage_log_instance (instance_id),
    KEY idx_etl_workflow_stage_log_stage (stage_code),
    KEY idx_etl_workflow_stage_log_status (status)
);

--changeset codex:20260506-01-postgres-workflow-platform dbms:postgresql
CREATE TABLE IF NOT EXISTS t_etl_workflow_definition (
    workflow_id VARCHAR(64) PRIMARY KEY,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_name VARCHAR(256) NOT NULL,
    workflow_version_no INT NOT NULL,
    platform_type VARCHAR(64) NOT NULL,
    description VARCHAR(1024),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_etl_workflow_definition_code_version UNIQUE (workflow_code, workflow_version_no)
);

CREATE INDEX IF NOT EXISTS idx_etl_workflow_definition_code ON t_etl_workflow_definition (workflow_code);
CREATE INDEX IF NOT EXISTS idx_etl_workflow_definition_platform ON t_etl_workflow_definition (platform_type);

CREATE TABLE IF NOT EXISTS t_etl_workflow_stage (
    stage_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    stage_name VARCHAR(128) NOT NULL,
    stage_order INT NOT NULL,
    description VARCHAR(512),
    retryable BOOLEAN NOT NULL DEFAULT TRUE,
    timeout_seconds INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_etl_workflow_stage_code UNIQUE (workflow_id, stage_code)
);

CREATE INDEX IF NOT EXISTS idx_etl_workflow_stage_workflow_order ON t_etl_workflow_stage (workflow_id, stage_order);

CREATE TABLE IF NOT EXISTS t_etl_workflow_engine_binding (
    binding_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    platform_type VARCHAR(64) NOT NULL,
    external_workflow_id VARCHAR(256),
    external_project_code VARCHAR(128),
    external_namespace VARCHAR(128),
    external_job_group VARCHAR(128),
    external_job_handler VARCHAR(128),
    config_json TEXT,
    attributes_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_etl_workflow_binding_workflow UNIQUE (workflow_id)
);

CREATE INDEX IF NOT EXISTS idx_etl_workflow_binding_platform ON t_etl_workflow_engine_binding (platform_type);

CREATE TABLE IF NOT EXISTS t_etl_workflow_instance (
    instance_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_version_no INT NOT NULL,
    platform_type VARCHAR(64) NOT NULL,
    business_key VARCHAR(256),
    current_stage_code VARCHAR(64),
    external_instance_id VARCHAR(256),
    external_workflow_id VARCHAR(256),
    status VARCHAR(32),
    raw_status VARCHAR(64),
    trigger_time TIMESTAMP,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    message VARCHAR(1024),
    context_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_etl_workflow_instance_code_version ON t_etl_workflow_instance (workflow_code, workflow_version_no);
CREATE INDEX IF NOT EXISTS idx_etl_workflow_instance_status ON t_etl_workflow_instance (status);
CREATE INDEX IF NOT EXISTS idx_etl_workflow_instance_external ON t_etl_workflow_instance (external_instance_id);

CREATE TABLE IF NOT EXISTS t_etl_workflow_stage_log (
    log_id VARCHAR(64) PRIMARY KEY,
    instance_id VARCHAR(64) NOT NULL,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_version_no INT NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    stage_name VARCHAR(128),
    stage_order INT,
    status VARCHAR(32),
    raw_status VARCHAR(64),
    message VARCHAR(1024),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    payload_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_etl_workflow_stage_log_instance ON t_etl_workflow_stage_log (instance_id);
CREATE INDEX IF NOT EXISTS idx_etl_workflow_stage_log_stage ON t_etl_workflow_stage_log (stage_code);
CREATE INDEX IF NOT EXISTS idx_etl_workflow_stage_log_status ON t_etl_workflow_stage_log (status);
