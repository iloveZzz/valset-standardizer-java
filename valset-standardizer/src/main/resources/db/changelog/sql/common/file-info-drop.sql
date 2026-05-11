--liquibase formatted sql

--changeset codex:20260501-02-mysql-drop-valset-file-info dbms:mysql
DROP TABLE IF EXISTS t_valset_file_info;

--changeset codex:20260501-02-postgres-drop-valset-file-info dbms:postgresql
DROP TABLE IF EXISTS t_valset_file_info;

--changeset codex:20260501-02-oracle-drop-valset-file-info dbms:oracle
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE t_valset_file_info PURGE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/
