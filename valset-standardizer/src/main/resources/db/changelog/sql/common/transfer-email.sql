--liquibase formatted sql

--changeset codex:20260427-01-mysql-transfer-email-info dbms:mysql
CREATE TABLE t_transfer_mail_info (
    transfer_id BIGINT PRIMARY KEY,
    mail_id VARCHAR(128) NULL,
    mail_from VARCHAR(512) NULL,
    mail_to TEXT NULL,
    mail_cc TEXT NULL,
    mail_bcc TEXT NULL,
    mail_subject VARCHAR(1024) NULL,
    mail_body LONGTEXT NULL,
    mail_protocol VARCHAR(32) NULL,
    mail_folder VARCHAR(256) NULL
);

--changeset codex:20260427-01-postgres-transfer-email-info dbms:postgresql
CREATE TABLE t_transfer_mail_info (
    transfer_id BIGINT PRIMARY KEY,
    mail_id VARCHAR(128),
    mail_from VARCHAR(512),
    mail_to TEXT,
    mail_cc TEXT,
    mail_bcc TEXT,
    mail_subject VARCHAR(1024),
    mail_body TEXT,
    mail_protocol VARCHAR(32),
    mail_folder VARCHAR(256)
);
