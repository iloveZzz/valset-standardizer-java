--liquibase formatted sql

--changeset codex:20260423-07-mysql-transfer-drop-normalized-name dbms:mysql
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_object' AND column_name = 'normalized_name') > 0,
    'ALTER TABLE t_transfer_object DROP COLUMN normalized_name',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

--changeset codex:20260423-07-postgres-transfer-drop-normalized-name dbms:postgresql
ALTER TABLE t_transfer_object
    DROP COLUMN IF EXISTS normalized_name;
