--liquibase formatted sql

--changeset codex:20260422-04-mysql-transfer-source dbms:mysql
--validCheckSum 9:322a1e7b48250fababbebf09e33d355a
CREATE TABLE t_transfer_source (
    source_id BIGINT PRIMARY KEY,
    source_code VARCHAR(128) NOT NULL,
    source_name VARCHAR(256) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    enabled TINYINT(1) NOT NULL,
    poll_cron VARCHAR(128),
    connection_config_json TEXT,
    source_meta_json TEXT,
    UNIQUE KEY uk_transfer_source_code (source_code)
);

--changeset codex:20260422-07-mysql-transfer-route-source dbms:mysql
ALTER TABLE t_transfer_route
    ADD COLUMN source_id BIGINT NULL,
    ADD COLUMN source_type VARCHAR(32) NULL,
    ADD COLUMN source_code VARCHAR(128) NULL;

CREATE INDEX idx_transfer_route_source_code ON t_transfer_route (source_code);

--changeset codex:20260426-02-mysql-transfer-route-poll-cron dbms:mysql
ALTER TABLE t_transfer_route
    ADD COLUMN poll_cron VARCHAR(128) NULL;

--changeset codex:20260426-02-postgres-transfer-route-poll-cron dbms:postgresql
ALTER TABLE t_transfer_route
    ADD COLUMN poll_cron VARCHAR(128) NULL;

--changeset codex:20260426-03-mysql-transfer-route-enabled dbms:mysql
ALTER TABLE t_transfer_route
    ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER target_code;

--changeset codex:20260426-03-postgres-transfer-route-enabled dbms:postgresql
ALTER TABLE t_transfer_route
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

--changeset codex:20260422-04-postgres-transfer-source dbms:postgresql
--validCheckSum 9:322a1e7b48250fababbebf09e33d355a
CREATE TABLE t_transfer_source (
    source_id BIGINT PRIMARY KEY,
    source_code VARCHAR(128) NOT NULL,
    source_name VARCHAR(256) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    poll_cron VARCHAR(128),
    connection_config_json TEXT,
    source_meta_json TEXT,
    CONSTRAINT uk_transfer_source_code UNIQUE (source_code)
);

--changeset codex:20260422-06-postgres-transfer-source-created-at dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'created_at'
ALTER TABLE t_transfer_source
    ADD COLUMN created_at TIMESTAMP NULL;

--changeset codex:20260422-06-postgres-transfer-source-updated-at dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'updated_at'
ALTER TABLE t_transfer_source
    ADD COLUMN updated_at TIMESTAMP NULL;

--changeset codex:20260422-06-postgres-transfer-target-created-at dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_target' AND column_name = 'created_at'
ALTER TABLE t_transfer_target
    ADD COLUMN created_at TIMESTAMP NULL;

--changeset codex:20260422-06-postgres-transfer-target-updated-at dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_target' AND column_name = 'updated_at'
ALTER TABLE t_transfer_target
    ADD COLUMN updated_at TIMESTAMP NULL;

--changeset codex:20260424-02-mysql-transfer-source-drop-checkpoint-config dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'checkpoint_config_json'
ALTER TABLE t_transfer_source
    DROP COLUMN checkpoint_config_json;

--changeset codex:20260424-02-postgres-transfer-source-drop-checkpoint-config dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'checkpoint_config_json'
ALTER TABLE t_transfer_source
    DROP COLUMN checkpoint_config_json;

--changeset codex:20260422-06-mysql-transfer-source-created-at dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'created_at'
ALTER TABLE t_transfer_source
    ADD COLUMN created_at DATETIME NULL;

--changeset codex:20260422-06-mysql-transfer-source-updated-at dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'updated_at'
ALTER TABLE t_transfer_source
    ADD COLUMN updated_at DATETIME NULL;

--changeset codex:20260422-06-mysql-transfer-target-created-at dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_target' AND column_name = 'created_at'
ALTER TABLE t_transfer_target
    ADD COLUMN created_at DATETIME NULL;

--changeset codex:20260422-06-mysql-transfer-target-updated-at dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_target' AND column_name = 'updated_at'
ALTER TABLE t_transfer_target
    ADD COLUMN updated_at DATETIME NULL;

--changeset codex:20260423-02-mysql-transfer-run-log dbms:mysql
CREATE TABLE t_transfer_run_log (
    run_log_id BIGINT PRIMARY KEY,
    source_id BIGINT,
    source_type VARCHAR(32),
    source_code VARCHAR(128),
    source_name VARCHAR(256),
    transfer_id BIGINT,
    route_id BIGINT,
    trigger_type VARCHAR(32),
    run_stage VARCHAR(32) NOT NULL,
    run_status VARCHAR(32) NOT NULL,
    log_message VARCHAR(1024),
    error_message VARCHAR(2048),
    created_at DATETIME,
    KEY idx_transfer_run_log_source_id (source_id),
    KEY idx_transfer_run_log_transfer_id (transfer_id),
    KEY idx_transfer_run_log_route_id (route_id)
);

--changeset codex:20260423-02-postgres-transfer-run-log dbms:postgresql
CREATE TABLE t_transfer_run_log (
    run_log_id BIGINT PRIMARY KEY,
    source_id BIGINT,
    source_type VARCHAR(32),
    source_code VARCHAR(128),
    source_name VARCHAR(256),
    transfer_id BIGINT,
    route_id BIGINT,
    trigger_type VARCHAR(32),
    run_stage VARCHAR(32) NOT NULL,
    run_status VARCHAR(32) NOT NULL,
    log_message VARCHAR(1024),
    error_message VARCHAR(2048),
    created_at TIMESTAMP
);

--changeset codex:20260423-03-mysql-transfer-object-mime-type dbms:mysql
ALTER TABLE t_transfer_object
    MODIFY COLUMN mime_type VARCHAR(512);

--changeset codex:20260423-03-postgres-transfer-object-mime-type dbms:postgresql
ALTER TABLE t_transfer_object
    ALTER COLUMN mime_type TYPE VARCHAR(512);

--changeset codex:20260428-01-mysql-transfer-object-business-fields dbms:mysql
ALTER TABLE t_transfer_object
    ADD COLUMN business_date DATE NULL,
    ADD COLUMN business_id VARCHAR(128) NULL,
    ADD COLUMN receive_date DATE NULL;

--changeset codex:20260428-01-postgres-transfer-object-business-fields dbms:postgresql
ALTER TABLE t_transfer_object
    ADD COLUMN business_date DATE;

ALTER TABLE t_transfer_object
    ADD COLUMN business_id VARCHAR(128);

ALTER TABLE t_transfer_object
    ADD COLUMN receive_date DATE;

--changeset codex:20260423-04-mysql-transfer-route-drop-transfer-id dbms:mysql
ALTER TABLE t_transfer_route
    DROP INDEX idx_transfer_route_transfer_id;

ALTER TABLE t_transfer_route
    DROP COLUMN transfer_id;

--changeset codex:20260423-04-postgres-transfer-route-drop-transfer-id dbms:postgresql
DROP INDEX IF EXISTS idx_transfer_route_transfer_id;

ALTER TABLE t_transfer_route
    DROP COLUMN transfer_id;

--changeset codex:20260423-07-mysql-transfer-checkpoint dbms:mysql
CREATE TABLE t_transfer_source_checkpoint (
    checkpoint_id BIGINT PRIMARY KEY,
    source_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    checkpoint_key VARCHAR(128) NOT NULL,
    checkpoint_value VARCHAR(1024),
    checkpoint_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_transfer_source_checkpoint_source_key (source_id, checkpoint_key),
    KEY idx_transfer_source_checkpoint_source_id (source_id),
    KEY idx_transfer_source_checkpoint_source_type (source_type)
);

CREATE TABLE t_transfer_source_checkpoint_item (
    checkpoint_item_id BIGINT PRIMARY KEY,
    source_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    item_key VARCHAR(512) NOT NULL,
    item_ref VARCHAR(1024),
    item_name VARCHAR(512),
    item_size BIGINT,
    item_mime_type VARCHAR(512),
    item_fingerprint VARCHAR(128),
    item_meta_json TEXT,
    trigger_type VARCHAR(32),
    processed_at DATETIME NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_transfer_source_checkpoint_item_source_key (source_id, item_key),
    KEY idx_transfer_source_checkpoint_item_source_id (source_id),
    KEY idx_transfer_source_checkpoint_item_source_type (source_type),
    KEY idx_transfer_source_checkpoint_item_processed_at (processed_at)
);

--changeset codex:20260423-07-postgres-transfer-checkpoint dbms:postgresql
CREATE TABLE t_transfer_source_checkpoint (
    checkpoint_id BIGINT PRIMARY KEY,
    source_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    checkpoint_key VARCHAR(128) NOT NULL,
    checkpoint_value VARCHAR(1024),
    checkpoint_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_transfer_source_checkpoint_source_key UNIQUE (source_id, checkpoint_key)
);

CREATE INDEX idx_transfer_source_checkpoint_source_id ON t_transfer_source_checkpoint (source_id);
CREATE INDEX idx_transfer_source_checkpoint_source_type ON t_transfer_source_checkpoint (source_type);

CREATE TABLE t_transfer_source_checkpoint_item (
    checkpoint_item_id BIGINT PRIMARY KEY,
    source_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    item_key VARCHAR(512) NOT NULL,
    item_ref VARCHAR(1024),
    item_name VARCHAR(512),
    item_size BIGINT,
    item_mime_type VARCHAR(512),
    item_fingerprint VARCHAR(128),
    item_meta_json TEXT,
    trigger_type VARCHAR(32),
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_transfer_source_checkpoint_item_source_key UNIQUE (source_id, item_key)
);

CREATE INDEX idx_transfer_source_checkpoint_item_source_id ON t_transfer_source_checkpoint_item (source_id);
CREATE INDEX idx_transfer_source_checkpoint_item_source_type ON t_transfer_source_checkpoint_item (source_type);
CREATE INDEX idx_transfer_source_checkpoint_item_processed_at ON t_transfer_source_checkpoint_item (processed_at);

--changeset codex:20260429-01-mysql-parse-queue dbms:mysql
CREATE TABLE t_parse_queue (
    queue_id BIGINT PRIMARY KEY,
    business_key VARCHAR(256) NOT NULL,
    transfer_id BIGINT NOT NULL,
    original_name VARCHAR(512),
    source_id BIGINT,
    source_type VARCHAR(32),
    source_code VARCHAR(128),
    route_id BIGINT,
    delivery_id BIGINT,
    tag_id BIGINT,
    tag_code VARCHAR(128),
    tag_name VARCHAR(256),
    file_status VARCHAR(32),
    delivery_status VARCHAR(32),
    parse_status VARCHAR(32) NOT NULL,
    trigger_mode VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    claimed_by VARCHAR(128),
    claimed_at DATETIME,
    parsed_at DATETIME,
    last_error_message VARCHAR(1024),
    object_snapshot_json TEXT,
    delivery_snapshot_json TEXT,
    parse_request_json TEXT,
    parse_result_json TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_parse_queue_business_key (business_key),
    KEY idx_parse_queue_transfer_id (transfer_id),
    KEY idx_parse_queue_source_code (source_code),
    KEY idx_parse_queue_route_id (route_id),
    KEY idx_parse_queue_tag_code (tag_code),
    KEY idx_parse_queue_parse_status (parse_status)
);

--changeset codex:20260429-01-postgres-parse-queue dbms:postgresql
CREATE TABLE t_parse_queue (
    queue_id BIGINT PRIMARY KEY,
    business_key VARCHAR(256) NOT NULL,
    transfer_id BIGINT NOT NULL,
    original_name VARCHAR(512),
    source_id BIGINT,
    source_type VARCHAR(32),
    source_code VARCHAR(128),
    route_id BIGINT,
    delivery_id BIGINT,
    tag_id BIGINT,
    tag_code VARCHAR(128),
    tag_name VARCHAR(256),
    file_status VARCHAR(32),
    delivery_status VARCHAR(32),
    parse_status VARCHAR(32) NOT NULL,
    trigger_mode VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    claimed_by VARCHAR(128),
    claimed_at TIMESTAMP,
    parsed_at TIMESTAMP,
    last_error_message VARCHAR(1024),
    object_snapshot_json TEXT,
    delivery_snapshot_json TEXT,
    parse_request_json TEXT,
    parse_result_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_parse_queue_business_key UNIQUE (business_key)
);

CREATE INDEX idx_parse_queue_transfer_id ON t_parse_queue (transfer_id);
CREATE INDEX idx_parse_queue_source_code ON t_parse_queue (source_code);
CREATE INDEX idx_parse_queue_route_id ON t_parse_queue (route_id);
CREATE INDEX idx_parse_queue_tag_code ON t_parse_queue (tag_code);
CREATE INDEX idx_parse_queue_parse_status ON t_parse_queue (parse_status);
