--liquibase formatted sql

-- Drop legacy schedule definition table.

--changeset codex:20260501-01-mysql-drop-subject-match-schedule dbms:mysql
DROP TABLE IF EXISTS t_subject_match_schedule;

--changeset codex:20260501-01-postgres-drop-subject-match-schedule dbms:postgresql
DROP TABLE IF EXISTS t_subject_match_schedule;

--changeset codex:20260501-01-oracle-drop-subject-match-schedule dbms:oracle
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE t_subject_match_schedule';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
