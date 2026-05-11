--liquibase formatted sql

--changeset codex:20260419-03-mysql-qlexpress-rule-engine-alter dbms:mysql
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_file_parse_profile' AND column_name = 'required_headers_json') = 0,
    'ALTER TABLE t_file_parse_profile ADD COLUMN required_headers_json TEXT COMMENT ''必选表头字段JSON''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_file_parse_profile' AND column_name = 'subject_code_pattern') = 0,
    'ALTER TABLE t_file_parse_profile ADD COLUMN subject_code_pattern VARCHAR(256) COMMENT ''科目代码正则''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
