--liquibase formatted sql

--changeset codex:20260423-08-mysql-transfer-source-ingest-lock dbms:mysql
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_status VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    ADD COLUMN ingest_started_at DATETIME NULL,
    ADD COLUMN ingest_finished_at DATETIME NULL,
    ADD COLUMN ingest_lock_token VARCHAR(128) NULL;

--changeset codex:20260423-08-postgres-transfer-source-ingest-lock dbms:postgresql
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_status VARCHAR(32) DEFAULT 'IDLE' NOT NULL,
    ADD COLUMN ingest_started_at TIMESTAMP,
    ADD COLUMN ingest_finished_at TIMESTAMP,
    ADD COLUMN ingest_lock_token VARCHAR(128);
