--liquibase formatted sql

--changeset codex:20260416-01-mysql-drop-parsed-workbook dbms:mysql
DROP TABLE t_subject_match_parsed_workbook;

--changeset codex:20260416-01-postgres-drop-parsed-workbook dbms:postgresql
DROP TABLE t_subject_match_parsed_workbook;

--changeset codex:20260416-01-oracle-drop-parsed-workbook dbms:oracle
DROP TABLE t_subject_match_parsed_workbook CASCADE CONSTRAINTS PURGE;
