--liquibase formatted sql

--changeset codex:20260426-01-mysql-transfer-source-poll-cron-cleanup dbms:mysql
UPDATE t_transfer_source
SET poll_cron = NULL
WHERE poll_cron IS NOT NULL
  AND poll_cron <> '';

--changeset codex:20260426-01-postgres-transfer-source-poll-cron-cleanup dbms:postgresql
UPDATE t_transfer_source
SET poll_cron = NULL
WHERE poll_cron IS NOT NULL
  AND poll_cron <> '';
