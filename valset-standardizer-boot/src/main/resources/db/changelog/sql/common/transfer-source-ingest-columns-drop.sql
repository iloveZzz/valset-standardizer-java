--liquibase formatted sql

--changeset codex:20260424-02-mysql-transfer-source-ingest-columns-drop dbms:mysql
ALTER TABLE t_transfer_source
    DROP COLUMN ingest_lock_token,
    DROP COLUMN ingest_task_id;

--changeset codex:20260424-02-postgres-transfer-source-ingest-columns-drop dbms:postgresql
ALTER TABLE t_transfer_source
    DROP COLUMN ingest_lock_token,
    DROP COLUMN ingest_task_id;
