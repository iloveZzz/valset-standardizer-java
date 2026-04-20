--liquibase formatted sql

--changeset codex:20260420-02-mysql-transfer-init dbms:mysql
INSERT INTO t_transfer_target (
    target_id, target_code, target_name, target_type, enabled, target_path_template, connection_config_json, target_meta_json
) VALUES
(920000000000000001, 'default-email-forward', '默认邮件转发目标', 'EMAIL', 1, NULL,
 '{"host":"smtp.example.com","port":25,"username":"transfer@example.com","password":"replace-me","protocol":"smtp","auth":true,"startTls":true,"ssl":false,"timeoutMillis":10000,"from":"transfer@example.com","to":"ops@example.com","cc":"","bcc":"","subjectTemplate":"文件转发：${fileName}","bodyTemplate":"文件已转发，请查收附件。"}',
 '{"usage":"email-forward","seed":true}'),
(920000000000000002, 'default-s3-archive', '默认S3归档目标', 'S3', 1, '/transfer/inbox/',
 '{"bucket":"transfer-bucket","region":"cn-north-1","endpointUrl":"https://s3.example.com","accessKey":"replace-me","secretKey":"replace-me","usePathStyle":true,"keyPrefix":"transfer/inbox/"}',
 '{"usage":"s3-archive","seed":true}'),
(920000000000000003, 'default-sftp-archive', '默认SFTP归档目标', 'SFTP', 1, '/transfer/inbox/',
 '{"host":"sftp.example.com","port":22,"username":"transfer","password":"replace-me","privateKeyPath":null,"passphrase":null,"remoteDir":"/transfer/inbox/","recursive":false,"includeHidden":false,"limit":0,"strictHostKeyChecking":false,"connectTimeoutMillis":10000,"channelTimeoutMillis":10000}',
 '{"usage":"sftp-archive","seed":true}');

--changeset codex:20260420-02-postgres-transfer-init dbms:postgresql
INSERT INTO t_transfer_target (
    target_id, target_code, target_name, target_type, enabled, target_path_template, connection_config_json, target_meta_json
) VALUES
(920000000000000001, 'default-email-forward', '默认邮件转发目标', 'EMAIL', TRUE, NULL,
 '{"host":"smtp.example.com","port":25,"username":"transfer@example.com","password":"replace-me","protocol":"smtp","auth":true,"startTls":true,"ssl":false,"timeoutMillis":10000,"from":"transfer@example.com","to":"ops@example.com","cc":"","bcc":"","subjectTemplate":"文件转发：${fileName}","bodyTemplate":"文件已转发，请查收附件。"}',
 '{"usage":"email-forward","seed":true}'),
(920000000000000002, 'default-s3-archive', '默认S3归档目标', 'S3', TRUE, '/transfer/inbox/',
 '{"bucket":"transfer-bucket","region":"cn-north-1","endpointUrl":"https://s3.example.com","accessKey":"replace-me","secretKey":"replace-me","usePathStyle":true,"keyPrefix":"transfer/inbox/"}',
 '{"usage":"s3-archive","seed":true}'),
(920000000000000003, 'default-sftp-archive', '默认SFTP归档目标', 'SFTP', TRUE, '/transfer/inbox/',
 '{"host":"sftp.example.com","port":22,"username":"transfer","password":"replace-me","privateKeyPath":null,"passphrase":null,"remoteDir":"/transfer/inbox/","recursive":false,"includeHidden":false,"limit":0,"strictHostKeyChecking":false,"connectTimeoutMillis":10000,"channelTimeoutMillis":10000}',
 '{"usage":"sftp-archive","seed":true}');
