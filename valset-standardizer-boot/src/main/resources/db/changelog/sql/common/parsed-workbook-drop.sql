--liquibase formatted sql

--changeset codex:20260501-01-mysql-drop-parsed-workbook dbms:mysql
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_subject_match_parsed_workbook') > 0,
    'DROP TABLE t_subject_match_parsed_workbook',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

--changeset codex:20260501-01-postgres-drop-parsed-workbook dbms:postgresql
DROP TABLE IF EXISTS t_subject_match_parsed_workbook;

--changeset codex:20260501-01-oracle-drop-parsed-workbook dbms:oracle
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE t_subject_match_parsed_workbook PURGE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
