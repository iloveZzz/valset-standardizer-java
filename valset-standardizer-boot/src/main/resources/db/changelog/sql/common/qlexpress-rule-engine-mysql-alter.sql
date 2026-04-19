--liquibase formatted sql

--changeset codex:20260419-03-mysql-qlexpress-rule-engine-alter dbms:mysql
ALTER TABLE t_file_parse_profile
    ADD COLUMN required_headers_json TEXT COMMENT '必选表头字段JSON';
ALTER TABLE t_file_parse_profile
    ADD COLUMN subject_code_pattern VARCHAR(256) COMMENT '科目代码正则';
