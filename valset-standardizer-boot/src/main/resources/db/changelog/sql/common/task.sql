--liquibase formatted sql

--changeset codex:20260430-01-mysql-outsourced-data-task dbms:mysql
--validCheckSum: 9:3c95ef4be4525304e10a9e95ad4baf62
CREATE TABLE t_outsourced_data_task_batch (
    batch_id VARCHAR(128) PRIMARY KEY,
    batch_name VARCHAR(256) NOT NULL,
    business_date DATE NOT NULL,
    valuation_date DATE,
    product_code VARCHAR(128),
    product_name VARCHAR(256),
    manager_name VARCHAR(256),
    file_id VARCHAR(128),
    filesys_file_id VARCHAR(128),
    file_fingerprint VARCHAR(128),
    original_file_name VARCHAR(512),
    source_type VARCHAR(64),
    current_stage VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    started_at DATETIME,
    ended_at DATETIME,
    duration_ms BIGINT,
    last_error_code VARCHAR(128),
    last_error_message VARCHAR(1024),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_outsourced_task_batch_biz_file (business_date, product_code, file_fingerprint),
    KEY idx_outsourced_task_batch_business_date (business_date),
    KEY idx_outsourced_task_batch_product_code (product_code),
    KEY idx_outsourced_task_batch_current_stage (current_stage),
    KEY idx_outsourced_task_batch_status (status)
);

CREATE TABLE t_outsourced_data_task_step (
    step_id VARCHAR(160) PRIMARY KEY,
    batch_id VARCHAR(128) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    task_id VARCHAR(128),
    task_type VARCHAR(64),
    run_no INT NOT NULL DEFAULT 1,
    current_flag TINYINT(1) NOT NULL DEFAULT 1,
    trigger_mode VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    started_at DATETIME,
    ended_at DATETIME,
    duration_ms BIGINT,
    input_summary VARCHAR(1024),
    output_summary VARCHAR(1024),
    error_code VARCHAR(128),
    error_message VARCHAR(1024),
    log_ref VARCHAR(256),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_outsourced_task_step_run (batch_id, stage, run_no),
    KEY idx_outsourced_task_step_batch_id (batch_id),
    KEY idx_outsourced_task_step_stage (stage),
    KEY idx_outsourced_task_step_status (status),
    KEY idx_outsourced_task_step_current (batch_id, stage, current_flag)
);

CREATE TABLE t_outsourced_data_task_log (
    log_id VARCHAR(160) PRIMARY KEY,
    batch_id VARCHAR(128) NOT NULL,
    step_id VARCHAR(160),
    stage VARCHAR(64),
    log_level VARCHAR(32),
    message VARCHAR(2048),
    occurred_at DATETIME,
    created_at DATETIME,
    KEY idx_outsourced_task_log_batch_id (batch_id),
    KEY idx_outsourced_task_log_step_id (step_id),
    KEY idx_outsourced_task_log_stage (stage),
    KEY idx_outsourced_task_log_occurred_at (occurred_at)
);

--changeset codex:20260430-01-postgres-outsourced-data-task dbms:postgresql
CREATE TABLE t_outsourced_data_task_batch (
    batch_id VARCHAR(128) PRIMARY KEY,
    batch_name VARCHAR(256) NOT NULL,
    business_date DATE NOT NULL,
    valuation_date DATE,
    product_code VARCHAR(128),
    product_name VARCHAR(256),
    manager_name VARCHAR(256),
    file_id VARCHAR(128),
    filesys_file_id VARCHAR(128),
    file_fingerprint VARCHAR(128),
    original_file_name VARCHAR(512),
    source_type VARCHAR(64),
    current_stage VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    duration_ms BIGINT,
    last_error_code VARCHAR(128),
    last_error_message VARCHAR(1024),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_outsourced_task_batch_biz_file UNIQUE (business_date, product_code, file_fingerprint)
);

CREATE INDEX idx_outsourced_task_batch_business_date ON t_outsourced_data_task_batch (business_date);
CREATE INDEX idx_outsourced_task_batch_product_code ON t_outsourced_data_task_batch (product_code);
CREATE INDEX idx_outsourced_task_batch_current_stage ON t_outsourced_data_task_batch (current_stage);
CREATE INDEX idx_outsourced_task_batch_status ON t_outsourced_data_task_batch (status);

CREATE TABLE t_outsourced_data_task_step (
    step_id VARCHAR(160) PRIMARY KEY,
    batch_id VARCHAR(128) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    task_id VARCHAR(128),
    task_type VARCHAR(64),
    run_no INT NOT NULL DEFAULT 1,
    current_flag BOOLEAN NOT NULL DEFAULT TRUE,
    trigger_mode VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    duration_ms BIGINT,
    input_summary VARCHAR(1024),
    output_summary VARCHAR(1024),
    error_code VARCHAR(128),
    error_message VARCHAR(1024),
    log_ref VARCHAR(256),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE t_outsourced_data_task_step ADD CONSTRAINT uk_outsourced_task_step_run UNIQUE (batch_id, stage, run_no);
CREATE INDEX idx_outsourced_task_step_batch_id ON t_outsourced_data_task_step (batch_id);
CREATE INDEX idx_outsourced_task_step_stage ON t_outsourced_data_task_step (stage);
CREATE INDEX idx_outsourced_task_step_status ON t_outsourced_data_task_step (status);
CREATE INDEX idx_outsourced_task_step_current ON t_outsourced_data_task_step (batch_id, stage, current_flag);

CREATE TABLE t_outsourced_data_task_log (
    log_id VARCHAR(160) PRIMARY KEY,
    batch_id VARCHAR(128) NOT NULL,
    step_id VARCHAR(160),
    stage VARCHAR(64),
    log_level VARCHAR(32),
    message VARCHAR(2048),
    occurred_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX idx_outsourced_task_log_batch_id ON t_outsourced_data_task_log (batch_id);
CREATE INDEX idx_outsourced_task_log_step_id ON t_outsourced_data_task_log (step_id);
CREATE INDEX idx_outsourced_task_log_stage ON t_outsourced_data_task_log (stage);
CREATE INDEX idx_outsourced_task_log_occurred_at ON t_outsourced_data_task_log (occurred_at);

--changeset codex:20260501-01-mysql-outsourced-task-step-current-flag dbms:mysql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_outsourced_data_task_step'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_outsourced_data_task_step' AND column_name = 'current_flag'
ALTER TABLE t_outsourced_data_task_step
    ADD COLUMN current_flag TINYINT(1) NOT NULL DEFAULT 1 AFTER run_no;

--changeset codex:20260501-02-mysql-outsourced-task-step-current-index dbms:mysql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_outsourced_data_task_step' AND column_name = 'current_flag'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_outsourced_data_task_step' AND index_name = 'idx_outsourced_task_step_current'
CREATE INDEX idx_outsourced_task_step_current ON t_outsourced_data_task_step (batch_id, stage, current_flag);

--changeset codex:20260501-01-postgres-outsourced-task-step-current-flag dbms:postgresql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 't_outsourced_data_task_step'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_outsourced_data_task_step' AND column_name = 'current_flag'
ALTER TABLE t_outsourced_data_task_step
    ADD COLUMN current_flag BOOLEAN NOT NULL DEFAULT TRUE;

--changeset codex:20260501-02-postgres-outsourced-task-step-current-index dbms:postgresql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_outsourced_data_task_step' AND column_name = 'current_flag'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND tablename = 't_outsourced_data_task_step' AND indexname = 'idx_outsourced_task_step_current'
CREATE INDEX idx_outsourced_task_step_current ON t_outsourced_data_task_step (batch_id, stage, current_flag);
