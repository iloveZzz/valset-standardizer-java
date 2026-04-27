--liquibase formatted sql

--changeset codex:20260423-05-mysql-transfer-init dbms:mysql
--validCheckSum 9:ede4f521aebdc45345d73a706f1034e5
INSERT INTO t_transfer_rule (
    rule_id, rule_code, rule_name, rule_version, enabled, priority, match_strategy, script_language,
    script_body, effective_from, effective_to, rule_meta_json
) VALUES (
    2047251487037984770,
    'all-file',
    '全部文件收取',
    '1.0.0',
    1,
    10,
    'ALL',
    'qlexpress4',
    '',
    NULL,
    NULL,
    '{"groupField":"group","targetType":"FILESYS","targetCode":"filesys-archive","targetPath":"/transfer/inbox","renamePattern":"${fileName}","maxRetryCount":3,"retryDelaySeconds":60,"groupStrategy":"NONE","groupTargetMapping":"{\n  \"finance@example.com\": \"filesys-finance\",\n  \"ops@example.com\": \"filesys-ops\"\n}"}'
);

INSERT INTO t_transfer_source (
    source_id, source_code, source_name, source_type, enabled,
    connection_config_json, source_meta_json, created_at, updated_at
) VALUES (
    2047251110490148865,
    'qq-mail',
    'qq邮箱',
    'EMAIL',
    1,
    '{"protocol":"imap","folder":"INBOX","ssl":true,"startTls":true,"limit":20,"host":"imap.qq.com","port":993,"username":"slime365@foxmail.com","password":"ENC:v1:oN4ZRax++VCgx06uv7ePCMkaJCtSVDGfNI4eVY4AeZqtzCq4vUZvFSgLp98="}',
    '{}',
    '2026-04-23 17:47:55',
    '2026-04-23 17:51:09'
);

INSERT INTO t_transfer_target (
    target_id, target_code, target_name, target_type, enabled, target_path_template,
    connection_config_json, target_meta_json, created_at, updated_at
) VALUES (
    2047251326219980801,
    'yss-filesys',
    '文件服务',
    'FILESYS',
    1,
    '/yss/wc',
    '{"chunkSize":8388608,"parentId":"283e6c2a0fcf4b1488db5856ad074806","storageSettingId":"9a4350d87d4b4085a5fa1a4e8038e09d"}',
    '{}',
    '2026-04-23 17:48:47',
    '2026-04-23 17:48:47'
);

--changeset codex:20260423-05-postgres-transfer-init dbms:postgresql
--validCheckSum 9:ede4f521aebdc45345d73a706f1034e5
INSERT INTO t_transfer_rule (
    rule_id, rule_code, rule_name, rule_version, enabled, priority, match_strategy, script_language,
    script_body, effective_from, effective_to, rule_meta_json
) VALUES (
    2047251487037984770,
    'all-file',
    '全部文件收取',
    '1.0.0',
    TRUE,
    10,
    'ALL',
    'qlexpress4',
    '',
    NULL,
    NULL,
    '{"groupField":"group","targetType":"FILESYS","targetCode":"filesys-archive","targetPath":"/transfer/inbox","renamePattern":"${fileName}","maxRetryCount":3,"retryDelaySeconds":60,"groupStrategy":"NONE","groupTargetMapping":"{\n  \"finance@example.com\": \"filesys-finance\",\n  \"ops@example.com\": \"filesys-ops\"\n}"}'
);

INSERT INTO t_transfer_source (
    source_id, source_code, source_name, source_type, enabled,
    connection_config_json, source_meta_json, created_at, updated_at
) VALUES (
    2047251110490148865,
    'qq-mail',
    'qq邮箱',
    'EMAIL',
    TRUE,
    '{"protocol":"imap","folder":"INBOX","ssl":true,"startTls":true,"limit":20,"host":"imap.qq.com","port":993,"username":"slime365@foxmail.com","password":"ENC:v1:oN4ZRax++VCgx06uv7ePCMkaJCtSVDGfNI4eVY4AeZqtzCq4vUZvFSgLp98="}',
    '{}',
    TIMESTAMP '2026-04-23 17:47:55',
    TIMESTAMP '2026-04-23 17:51:09'
);

INSERT INTO t_transfer_target (
    target_id, target_code, target_name, target_type, enabled, target_path_template,
    connection_config_json, target_meta_json, created_at, updated_at
) VALUES (
    2047251326219980801,
    'yss-filesys',
    '文件服务',
    'FILESYS',
    TRUE,
    '/yss/wc',
    '{"chunkSize":8388608,"parentId":"283e6c2a0fcf4b1488db5856ad074806","storageSettingId":"9a4350d87d4b4085a5fa1a4e8038e09d"}',
    '{}',
    TIMESTAMP '2026-04-23 17:48:47',
    TIMESTAMP '2026-04-23 17:48:47'
);

--changeset codex:20260423-06-mysql-transfer-init-fix dbms:mysql
UPDATE t_transfer_rule
SET rule_meta_json = '{"groupField":"group","targetType":"FILESYS","targetCode":"filesys-archive","targetPath":"/transfer/inbox","renamePattern":"${fileName}","maxRetryCount":3,"retryDelaySeconds":60,"groupStrategy":"NONE","groupTargetMapping":{"finance@example.com":"filesys-finance","ops@example.com":"filesys-ops"}}'
WHERE rule_id = 2047251487037984770;

--changeset codex:20260423-06-postgres-transfer-init-fix dbms:postgresql
UPDATE t_transfer_rule
SET rule_meta_json = '{"groupField":"group","targetType":"FILESYS","targetCode":"filesys-archive","targetPath":"/transfer/inbox","renamePattern":"${fileName}","maxRetryCount":3,"retryDelaySeconds":60,"groupStrategy":"NONE","groupTargetMapping":{"finance@example.com":"filesys-finance","ops@example.com":"filesys-ops"}}'
WHERE rule_id = 2047251487037984770;
