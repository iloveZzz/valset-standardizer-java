--liquibase formatted sql

--changeset codex:20260423-08-mysql-transfer-source-ingest-lock dbms:mysql
--validCheckSum 9:96ff2e7ba68ab479c133a82cd69846c3
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_status'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_status VARCHAR(32) NOT NULL DEFAULT 'IDLE';

--changeset codex:20260501-01-mysql-transfer-source-ingest-started-at dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_started_at'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_started_at DATETIME NULL;

--changeset codex:20260501-01-mysql-transfer-source-ingest-finished-at dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_finished_at'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_finished_at DATETIME NULL;

--changeset codex:20260501-01-mysql-transfer-source-ingest-lock-token dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_lock_token'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_lock_token VARCHAR(128) NULL;

--changeset codex:20260423-08-postgres-transfer-source-ingest-lock dbms:postgresql
--validCheckSum 9:96ff2e7ba68ab479c133a82cd69846c3
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'ingest_status'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_status VARCHAR(32) DEFAULT 'IDLE' NOT NULL;

--changeset codex:20260501-01-postgres-transfer-source-ingest-started-at dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'ingest_started_at'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_started_at TIMESTAMP;

--changeset codex:20260501-01-postgres-transfer-source-ingest-finished-at dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'ingest_finished_at'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_finished_at TIMESTAMP;

--changeset codex:20260501-01-postgres-transfer-source-ingest-lock-token dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'ingest_lock_token'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_lock_token VARCHAR(128);

--changeset codex:20260425-01-mysql-transfer-source-ingest-trigger-type dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 't_transfer_source' AND column_name = 'ingest_trigger_type'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_trigger_type VARCHAR(32) NULL;

--changeset codex:20260425-01-postgres-transfer-source-ingest-trigger-type dbms:postgresql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 't_transfer_source' AND column_name = 'ingest_trigger_type'
ALTER TABLE t_transfer_source
    ADD COLUMN ingest_trigger_type VARCHAR(32);
