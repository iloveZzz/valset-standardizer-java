--liquibase formatted sql

--changeset codex:20260430-01-mysql-workflow-task-rename dbms:mysql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_subject_match_task'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 't_valset_workflow_task'
RENAME TABLE t_subject_match_task TO t_valset_workflow_task;

--changeset codex:20260430-01-postgres-workflow-task-rename dbms:postgresql
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 't_subject_match_task'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 't_valset_workflow_task'
ALTER TABLE t_subject_match_task RENAME TO t_valset_workflow_task;

--changeset codex:20260430-01-oracle-workflow-task-rename dbms:oracle
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM user_tables WHERE table_name = 'T_SUBJECT_MATCH_TASK'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM user_tables WHERE table_name = 'T_VALSET_WORKFLOW_TASK'
ALTER TABLE t_subject_match_task RENAME TO t_valset_workflow_task;
