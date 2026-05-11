--liquibase formatted sql

--changeset codex:20260425-02-mysql-transfer-init-fix dbms:mysql
UPDATE t_transfer_rule
SET rule_meta_json = '{"targetType":"FILESYS","targetCode":"filesys-archive","targetPath":"/transfer/inbox","renamePattern":"${fileName}","maxRetryCount":3,"retryDelaySeconds":60}'
WHERE rule_id = 2047251487037984770;

--changeset codex:20260425-02-postgres-transfer-init-fix dbms:postgresql
UPDATE t_transfer_rule
SET rule_meta_json = '{"targetType":"FILESYS","targetCode":"filesys-archive","targetPath":"/transfer/inbox","renamePattern":"${fileName}","maxRetryCount":3,"retryDelaySeconds":60}'
WHERE rule_id = 2047251487037984770;
