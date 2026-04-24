--liquibase formatted sql

--changeset codex:20260423-07-mysql-transfer-drop-normalized-name dbms:mysql
ALTER TABLE t_transfer_object
    DROP COLUMN normalized_name;

--changeset codex:20260423-07-postgres-transfer-drop-normalized-name dbms:postgresql
ALTER TABLE t_transfer_object
    DROP COLUMN normalized_name;
