--liquibase formatted sql

--changeset codex:20260420-03-mysql-transfer-email dbms:mysql
ALTER TABLE t_transfer_object
    ADD COLUMN mail_id VARCHAR(128) NULL,
    ADD COLUMN mail_from VARCHAR(512) NULL,
    ADD COLUMN mail_to TEXT NULL,
    ADD COLUMN mail_cc TEXT NULL,
    ADD COLUMN mail_bcc TEXT NULL,
    ADD COLUMN mail_subject VARCHAR(1024) NULL,
    ADD COLUMN mail_body LONGTEXT NULL,
    ADD COLUMN mail_protocol VARCHAR(32) NULL,
    ADD COLUMN mail_folder VARCHAR(256) NULL;

--changeset codex:20260420-03-postgres-transfer-email dbms:postgresql
ALTER TABLE t_transfer_object
    ADD COLUMN mail_id VARCHAR(128),
    ADD COLUMN mail_from VARCHAR(512),
    ADD COLUMN mail_to TEXT,
    ADD COLUMN mail_cc TEXT,
    ADD COLUMN mail_bcc TEXT,
    ADD COLUMN mail_subject VARCHAR(1024),
    ADD COLUMN mail_body TEXT,
    ADD COLUMN mail_protocol VARCHAR(32),
    ADD COLUMN mail_folder VARCHAR(256);
