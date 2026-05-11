--liquibase formatted sql

--changeset codex:20260423-08-mysql-db-scheduler dbms:mysql
CREATE TABLE IF NOT EXISTS scheduled_tasks (
    task_name VARCHAR(255) NOT NULL,
    task_instance VARCHAR(255) NOT NULL,
    task_data LONGBLOB NULL,
    execution_time DATETIME(6) NOT NULL,
    picked BOOLEAN NOT NULL,
    picked_by VARCHAR(255) NULL,
    last_success DATETIME(6) NULL,
    last_failure DATETIME(6) NULL,
    consecutive_failures INT NULL,
    last_heartbeat DATETIME(6) NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (task_name, task_instance)
);

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'scheduled_tasks' AND index_name = 'execution_time_idx') = 0,
    'CREATE INDEX execution_time_idx ON scheduled_tasks (execution_time)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'scheduled_tasks' AND index_name = 'last_heartbeat_idx') = 0,
    'CREATE INDEX last_heartbeat_idx ON scheduled_tasks (last_heartbeat)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

--changeset codex:20260423-08-postgres-db-scheduler dbms:postgresql
CREATE TABLE IF NOT EXISTS scheduled_tasks (
    task_name TEXT NOT NULL,
    task_instance TEXT NOT NULL,
    task_data BYTEA NULL,
    execution_time TIMESTAMP WITH TIME ZONE NOT NULL,
    picked BOOLEAN NOT NULL,
    picked_by TEXT NULL,
    last_success TIMESTAMP WITH TIME ZONE NULL,
    last_failure TIMESTAMP WITH TIME ZONE NULL,
    consecutive_failures INT NULL,
    last_heartbeat TIMESTAMP WITH TIME ZONE NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX execution_time_idx ON scheduled_tasks (execution_time);
CREATE INDEX last_heartbeat_idx ON scheduled_tasks (last_heartbeat);
