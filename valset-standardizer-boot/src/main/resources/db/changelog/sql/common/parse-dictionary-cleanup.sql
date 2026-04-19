--liquibase formatted sql

--changeset codex:20260416-02-mysql-parse-dictionary-cleanup dbms:mysql
ALTER TABLE t_file_parse_source DROP INDEX idx_file_parse_source_rule_type_name;
ALTER TABLE t_file_parse_source DROP INDEX idx_file_parse_source_rule_id;
ALTER TABLE t_file_parse_source DROP COLUMN rule_id;

--changeset codex:20260416-02-postgres-parse-dictionary-cleanup dbms:postgresql
DROP INDEX IF EXISTS idx_file_parse_source_rule_type_name;
DROP INDEX IF EXISTS idx_file_parse_source_rule_id;
ALTER TABLE t_file_parse_source DROP COLUMN IF EXISTS rule_id;

--changeset codex:20260416-02-oracle-parse-dictionary-cleanup dbms:oracle
BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_file_parse_source_rule_type_name';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 AND SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_file_parse_source_rule_id';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 AND SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

ALTER TABLE t_file_parse_source DROP COLUMN rule_id;
