--liquibase formatted sql

--changeset codex:20260424-02-mysql-transfer-source-ingest-columns-drop dbms:mysql
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_lock_token') > 0,
    'ALTER TABLE t_transfer_source DROP COLUMN ingest_lock_token',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_task_id') > 0,
    'ALTER TABLE t_transfer_source DROP COLUMN ingest_task_id',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

--changeset codex:20260424-02-postgres-transfer-source-ingest-columns-drop dbms:postgresql
ALTER TABLE t_transfer_source
    DROP COLUMN IF EXISTS ingest_lock_token,
    DROP COLUMN IF EXISTS ingest_task_id;
