--liquibase formatted sql

--changeset codex:20260424-01-mysql-transfer-source-ingest-task-id dbms:mysql
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_task_id VARCHAR(128) NULL;

--changeset codex:20260424-01-postgres-transfer-source-ingest-task-id dbms:postgresql
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_task_id VARCHAR(128);
