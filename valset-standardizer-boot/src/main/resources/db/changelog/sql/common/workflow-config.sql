--liquibase formatted sql

--changeset codex:20260504-01-mysql-workflow-config dbms:mysql
CREATE TABLE IF NOT EXISTS t_workflow_definition (
    workflow_id VARCHAR(64) PRIMARY KEY,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_name VARCHAR(256) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    engine_type VARCHAR(64) NOT NULL,
    parse_fallback_stage VARCHAR(64),
    workflow_fallback_stage VARCHAR(64),
    version_no INT NOT NULL DEFAULT 1,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(1024),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_workflow_definition_code_version (workflow_code, version_no),
    KEY idx_workflow_definition_code_enabled (workflow_code, enabled),
    KEY idx_workflow_definition_business_type (business_type),
    KEY idx_workflow_definition_engine_type (engine_type)
);

CREATE TABLE IF NOT EXISTS t_workflow_stage (
    stage_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    step_code VARCHAR(64) NOT NULL,
    stage_name VARCHAR(128) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    stage_description VARCHAR(512),
    step_description VARCHAR(512),
    sort_order INT NOT NULL,
    retryable TINYINT(1) NOT NULL DEFAULT 1,
    skippable TINYINT(1) NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_workflow_stage_code (workflow_id, stage_code),
    KEY idx_workflow_stage_workflow_sort (workflow_id, sort_order)
);

CREATE TABLE IF NOT EXISTS t_workflow_stage_mapping (
    mapping_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_id VARCHAR(64),
    mapping_type VARCHAR(64) NOT NULL,
    mapping_value VARCHAR(128) NOT NULL,
    ignored TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME,
    KEY idx_workflow_stage_mapping_workflow (workflow_id),
    KEY idx_workflow_stage_mapping_stage (stage_id),
    KEY idx_workflow_stage_mapping_value (mapping_type, mapping_value)
);

CREATE TABLE IF NOT EXISTS t_workflow_status_mapping (
    mapping_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_status VARCHAR(128) NOT NULL,
    target_status VARCHAR(64) NOT NULL,
    status_label VARCHAR(128),
    created_at DATETIME,
    KEY idx_workflow_status_mapping_workflow (workflow_id),
    KEY idx_workflow_status_mapping_value (source_type, source_status)
);

CREATE TABLE IF NOT EXISTS t_workflow_executor_binding (
    binding_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_id VARCHAR(64),
    engine_type VARCHAR(64) NOT NULL,
    external_ref VARCHAR(256),
    config_json TEXT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_workflow_executor_binding_workflow (workflow_id),
    KEY idx_workflow_executor_binding_stage (stage_id),
    KEY idx_workflow_executor_binding_engine (engine_type)
);

INSERT INTO t_workflow_definition (workflow_id, workflow_code, workflow_name, business_type, engine_type, parse_fallback_stage, workflow_fallback_stage, version_no, enabled, status, description, created_at, updated_at)
SELECT 'wf_valuation_parse_v1', 'VALUATION_PARSE', '估值表解析工作流', 'VALUATION', 'INTERNAL', 'FILE_PARSE', 'DATA_PROCESSING', 1, 1, 'PUBLISHED', '从 yml 下沉的默认估值表解析阶段配置', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_definition WHERE workflow_code = 'VALUATION_PARSE');

INSERT INTO t_workflow_stage (stage_id, workflow_id, stage_code, step_code, stage_name, step_name, stage_description, step_description, sort_order, retryable, skippable, enabled, created_at, updated_at)
SELECT 'wfs_file_parse', 'wf_valuation_parse_v1', 'FILE_PARSE', 'FILE_PARSE', '文件解析', '文件解析', '文件识别、Sheet 解析、结构化解析', '文件识别、Sheet 解析、结构化解析', 1, 1, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_stage WHERE workflow_id = 'wf_valuation_parse_v1' AND stage_code = 'FILE_PARSE');
INSERT INTO t_workflow_stage (stage_id, workflow_id, stage_code, step_code, stage_name, step_name, stage_description, step_description, sort_order, retryable, skippable, enabled, created_at, updated_at)
SELECT 'wfs_structure_standardize', 'wf_valuation_parse_v1', 'STRUCTURE_STANDARDIZE', 'STRUCTURE_STANDARDIZE', '结构标准化', '结构标准化', '字段映射、数据清洗、STG 结构转换', '字段映射、数据清洗、STG 结构转换', 2, 1, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_stage WHERE workflow_id = 'wf_valuation_parse_v1' AND stage_code = 'STRUCTURE_STANDARDIZE');
INSERT INTO t_workflow_stage (stage_id, workflow_id, stage_code, step_code, stage_name, step_name, stage_description, step_description, sort_order, retryable, skippable, enabled, created_at, updated_at)
SELECT 'wfs_subject_recognize', 'wf_valuation_parse_v1', 'SUBJECT_RECOGNIZE', 'SUBJECT_RECOGNIZE', '科目识别', '科目识别', '科目匹配、属性识别、标签补全', '科目匹配、属性识别、标签补全', 3, 1, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_stage WHERE workflow_id = 'wf_valuation_parse_v1' AND stage_code = 'SUBJECT_RECOGNIZE');
INSERT INTO t_workflow_stage (stage_id, workflow_id, stage_code, step_code, stage_name, step_name, stage_description, step_description, sort_order, retryable, skippable, enabled, created_at, updated_at)
SELECT 'wfs_standard_landing', 'wf_valuation_parse_v1', 'STANDARD_LANDING', 'STANDARD_LANDING', '标准表落地', '标准表落地', 'STG/DWD/标准持仓/估值数据写入', 'STG/DWD/标准持仓/估值数据写入', 4, 1, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_stage WHERE workflow_id = 'wf_valuation_parse_v1' AND stage_code = 'STANDARD_LANDING');
INSERT INTO t_workflow_stage (stage_id, workflow_id, stage_code, step_code, stage_name, step_name, stage_description, step_description, sort_order, retryable, skippable, enabled, created_at, updated_at)
SELECT 'wfs_verify_archive', 'wf_valuation_parse_v1', 'VERIFY_ARCHIVE', 'VERIFY_ARCHIVE', '校验归档', '校验归档', '一致性校验、结果确认、归档完成', '一致性校验、结果确认、归档完成', 5, 1, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_stage WHERE workflow_id = 'wf_valuation_parse_v1' AND stage_code = 'VERIFY_ARCHIVE');

INSERT INTO t_workflow_stage_mapping (mapping_id, workflow_id, stage_id, mapping_type, mapping_value, ignored, created_at) VALUES
('wfsm_task_type_extract', 'wf_valuation_parse_v1', 'wfs_file_parse', 'TASK_TYPE', 'EXTRACT_DATA', 0, NOW()),
('wfsm_task_stage_extract', 'wf_valuation_parse_v1', 'wfs_file_parse', 'TASK_STAGE', 'EXTRACT', 0, NOW()),
('wfsm_task_stage_standardize', 'wf_valuation_parse_v1', 'wfs_structure_standardize', 'TASK_STAGE', 'STANDARDIZE', 0, NOW()),
('wfsm_parse_standardized', 'wf_valuation_parse_v1', 'wfs_structure_standardize', 'PARSE_LIFECYCLE', 'TASK_STANDARDIZED', 0, NOW()),
('wfsm_task_type_match', 'wf_valuation_parse_v1', 'wfs_subject_recognize', 'TASK_TYPE', 'MATCH_SUBJECT', 0, NOW()),
('wfsm_task_stage_match', 'wf_valuation_parse_v1', 'wfs_subject_recognize', 'TASK_STAGE', 'MATCH', 0, NOW()),
('wfsm_parse_persisted', 'wf_valuation_parse_v1', 'wfs_standard_landing', 'PARSE_LIFECYCLE', 'TASK_PERSISTED', 0, NOW()),
('wfsm_task_type_export', 'wf_valuation_parse_v1', 'wfs_verify_archive', 'TASK_TYPE', 'EXPORT_RESULT', 0, NOW()),
('wfsm_parse_succeeded', 'wf_valuation_parse_v1', 'wfs_verify_archive', 'PARSE_LIFECYCLE', 'TASK_SUCCEEDED', 0, NOW()),
('wfsm_parse_queue_completed', 'wf_valuation_parse_v1', 'wfs_verify_archive', 'PARSE_LIFECYCLE', 'QUEUE_COMPLETED', 0, NOW()),
('wfsm_ignore_cycle_started', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'CYCLE_STARTED', 1, NOW()),
('wfsm_ignore_cycle_finished', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'CYCLE_FINISHED', 1, NOW()),
('wfsm_ignore_batch_started', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'BATCH_STARTED', 1, NOW()),
('wfsm_ignore_batch_empty', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'BATCH_EMPTY', 1, NOW()),
('wfsm_ignore_batch_finished', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'BATCH_FINISHED', 1, NOW()),
('wfsm_ignore_parse_workbook', 'wf_valuation_parse_v1', NULL, 'IGNORE_WORKFLOW_TASK_TYPE', 'PARSE_WORKBOOK', 1, NOW())
ON DUPLICATE KEY UPDATE mapping_id = mapping_id;

INSERT INTO t_workflow_status_mapping (mapping_id, workflow_id, source_type, source_status, target_status, status_label, created_at) VALUES
('wfstm_task_success', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'SUCCESS', 'SUCCESS', '已完成', NOW()),
('wfstm_task_failed', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'FAILED', 'FAILED', '失败', NOW()),
('wfstm_task_canceled', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'CANCELED', 'STOPPED', '已停止', NOW()),
('wfstm_task_running', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'RUNNING', 'RUNNING', '处理中', NOW()),
('wfstm_task_retrying', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'RETRYING', 'RUNNING', '处理中', NOW()),
('wfstm_parse_started', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_EXECUTION_STARTED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_created', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_CREATED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_dispatched', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_DISPATCHED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_queue_subscribed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SUBSCRIBED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_raw_parsed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_RAW_PARSED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_standardized', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_STANDARDIZED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_persisted', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_PERSISTED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_succeeded', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_SUCCEEDED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_queue_completed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_COMPLETED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_reused', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_REUSED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_failed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_FAILED', 'FAILED', '失败', NOW()),
('wfstm_parse_queue_failed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_FAILED', 'FAILED', '失败', NOW()),
('wfstm_parse_repair_failed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_FILE_INFO_REPAIR_FAILED', 'FAILED', '失败', NOW()),
('wfstm_parse_queue_skipped', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SKIPPED', 'STOPPED', '已停止', NOW()),
('wfstm_parse_subscribe_conflict', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SUBSCRIBE_CONFLICT', 'STOPPED', '已停止', NOW()),
('wfstm_parse_subscribe_skipped', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SUBSCRIBE_SKIPPED', 'STOPPED', '已停止', NOW())
ON DUPLICATE KEY UPDATE mapping_id = mapping_id;

INSERT INTO t_workflow_executor_binding (binding_id, workflow_id, stage_id, engine_type, external_ref, config_json, enabled, created_at, updated_at)
SELECT 'wfeb_internal_default', 'wf_valuation_parse_v1', NULL, 'INTERNAL', 'DefaultTaskDispatcher', '{}', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_executor_binding WHERE binding_id = 'wfeb_internal_default');

--changeset codex:20260504-01-postgres-workflow-config dbms:postgresql
CREATE TABLE IF NOT EXISTS t_workflow_definition (
    workflow_id VARCHAR(64) PRIMARY KEY,
    workflow_code VARCHAR(128) NOT NULL,
    workflow_name VARCHAR(256) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    engine_type VARCHAR(64) NOT NULL,
    parse_fallback_stage VARCHAR(64),
    workflow_fallback_stage VARCHAR(64),
    version_no INT NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(1024),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_workflow_definition_code_version UNIQUE (workflow_code, version_no)
);

CREATE INDEX IF NOT EXISTS idx_workflow_definition_code_enabled ON t_workflow_definition (workflow_code, enabled);
CREATE INDEX IF NOT EXISTS idx_workflow_definition_business_type ON t_workflow_definition (business_type);
CREATE INDEX IF NOT EXISTS idx_workflow_definition_engine_type ON t_workflow_definition (engine_type);

CREATE TABLE IF NOT EXISTS t_workflow_stage (
    stage_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    step_code VARCHAR(64) NOT NULL,
    stage_name VARCHAR(128) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    stage_description VARCHAR(512),
    step_description VARCHAR(512),
    sort_order INT NOT NULL,
    retryable BOOLEAN NOT NULL DEFAULT TRUE,
    skippable BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_workflow_stage_code UNIQUE (workflow_id, stage_code)
);

CREATE INDEX IF NOT EXISTS idx_workflow_stage_workflow_sort ON t_workflow_stage (workflow_id, sort_order);

CREATE TABLE IF NOT EXISTS t_workflow_stage_mapping (
    mapping_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_id VARCHAR(64),
    mapping_type VARCHAR(64) NOT NULL,
    mapping_value VARCHAR(128) NOT NULL,
    ignored BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_stage_mapping_workflow ON t_workflow_stage_mapping (workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_stage_mapping_stage ON t_workflow_stage_mapping (stage_id);
CREATE INDEX IF NOT EXISTS idx_workflow_stage_mapping_value ON t_workflow_stage_mapping (mapping_type, mapping_value);

CREATE TABLE IF NOT EXISTS t_workflow_status_mapping (
    mapping_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_status VARCHAR(128) NOT NULL,
    target_status VARCHAR(64) NOT NULL,
    status_label VARCHAR(128),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_status_mapping_workflow ON t_workflow_status_mapping (workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_status_mapping_value ON t_workflow_status_mapping (source_type, source_status);

CREATE TABLE IF NOT EXISTS t_workflow_executor_binding (
    binding_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    stage_id VARCHAR(64),
    engine_type VARCHAR(64) NOT NULL,
    external_ref VARCHAR(256),
    config_json TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_executor_binding_workflow ON t_workflow_executor_binding (workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_executor_binding_stage ON t_workflow_executor_binding (stage_id);
CREATE INDEX IF NOT EXISTS idx_workflow_executor_binding_engine ON t_workflow_executor_binding (engine_type);

INSERT INTO t_workflow_definition (workflow_id, workflow_code, workflow_name, business_type, engine_type, parse_fallback_stage, workflow_fallback_stage, version_no, enabled, status, description, created_at, updated_at)
SELECT 'wf_valuation_parse_v1', 'VALUATION_PARSE', '估值表解析工作流', 'VALUATION', 'INTERNAL', 'FILE_PARSE', 'DATA_PROCESSING', 1, TRUE, 'PUBLISHED', '从 yml 下沉的默认估值表解析阶段配置', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_definition WHERE workflow_code = 'VALUATION_PARSE');

INSERT INTO t_workflow_stage (stage_id, workflow_id, stage_code, step_code, stage_name, step_name, stage_description, step_description, sort_order, retryable, skippable, enabled, created_at, updated_at) VALUES
('wfs_file_parse', 'wf_valuation_parse_v1', 'FILE_PARSE', 'FILE_PARSE', '文件解析', '文件解析', '文件识别、Sheet 解析、结构化解析', '文件识别、Sheet 解析、结构化解析', 1, TRUE, FALSE, TRUE, NOW(), NOW()),
('wfs_structure_standardize', 'wf_valuation_parse_v1', 'STRUCTURE_STANDARDIZE', 'STRUCTURE_STANDARDIZE', '结构标准化', '结构标准化', '字段映射、数据清洗、STG 结构转换', '字段映射、数据清洗、STG 结构转换', 2, TRUE, FALSE, TRUE, NOW(), NOW()),
('wfs_subject_recognize', 'wf_valuation_parse_v1', 'SUBJECT_RECOGNIZE', 'SUBJECT_RECOGNIZE', '科目识别', '科目识别', '科目匹配、属性识别、标签补全', '科目匹配、属性识别、标签补全', 3, TRUE, FALSE, TRUE, NOW(), NOW()),
('wfs_standard_landing', 'wf_valuation_parse_v1', 'STANDARD_LANDING', 'STANDARD_LANDING', '标准表落地', '标准表落地', 'STG/DWD/标准持仓/估值数据写入', 'STG/DWD/标准持仓/估值数据写入', 4, TRUE, FALSE, TRUE, NOW(), NOW()),
('wfs_verify_archive', 'wf_valuation_parse_v1', 'VERIFY_ARCHIVE', 'VERIFY_ARCHIVE', '校验归档', '校验归档', '一致性校验、结果确认、归档完成', '一致性校验、结果确认、归档完成', 5, TRUE, FALSE, TRUE, NOW(), NOW())
ON CONFLICT (stage_id) DO NOTHING;

INSERT INTO t_workflow_stage_mapping (mapping_id, workflow_id, stage_id, mapping_type, mapping_value, ignored, created_at) VALUES
('wfsm_task_type_extract', 'wf_valuation_parse_v1', 'wfs_file_parse', 'TASK_TYPE', 'EXTRACT_DATA', FALSE, NOW()),
('wfsm_task_stage_extract', 'wf_valuation_parse_v1', 'wfs_file_parse', 'TASK_STAGE', 'EXTRACT', FALSE, NOW()),
('wfsm_task_stage_standardize', 'wf_valuation_parse_v1', 'wfs_structure_standardize', 'TASK_STAGE', 'STANDARDIZE', FALSE, NOW()),
('wfsm_parse_standardized', 'wf_valuation_parse_v1', 'wfs_structure_standardize', 'PARSE_LIFECYCLE', 'TASK_STANDARDIZED', FALSE, NOW()),
('wfsm_task_type_match', 'wf_valuation_parse_v1', 'wfs_subject_recognize', 'TASK_TYPE', 'MATCH_SUBJECT', FALSE, NOW()),
('wfsm_task_stage_match', 'wf_valuation_parse_v1', 'wfs_subject_recognize', 'TASK_STAGE', 'MATCH', FALSE, NOW()),
('wfsm_parse_persisted', 'wf_valuation_parse_v1', 'wfs_standard_landing', 'PARSE_LIFECYCLE', 'TASK_PERSISTED', FALSE, NOW()),
('wfsm_task_type_export', 'wf_valuation_parse_v1', 'wfs_verify_archive', 'TASK_TYPE', 'EXPORT_RESULT', FALSE, NOW()),
('wfsm_parse_succeeded', 'wf_valuation_parse_v1', 'wfs_verify_archive', 'PARSE_LIFECYCLE', 'TASK_SUCCEEDED', FALSE, NOW()),
('wfsm_parse_queue_completed', 'wf_valuation_parse_v1', 'wfs_verify_archive', 'PARSE_LIFECYCLE', 'QUEUE_COMPLETED', FALSE, NOW()),
('wfsm_ignore_cycle_started', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'CYCLE_STARTED', TRUE, NOW()),
('wfsm_ignore_cycle_finished', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'CYCLE_FINISHED', TRUE, NOW()),
('wfsm_ignore_batch_started', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'BATCH_STARTED', TRUE, NOW()),
('wfsm_ignore_batch_empty', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'BATCH_EMPTY', TRUE, NOW()),
('wfsm_ignore_batch_finished', 'wf_valuation_parse_v1', NULL, 'IGNORE_PARSE_LIFECYCLE', 'BATCH_FINISHED', TRUE, NOW()),
('wfsm_ignore_parse_workbook', 'wf_valuation_parse_v1', NULL, 'IGNORE_WORKFLOW_TASK_TYPE', 'PARSE_WORKBOOK', TRUE, NOW())
ON CONFLICT (mapping_id) DO NOTHING;

INSERT INTO t_workflow_status_mapping (mapping_id, workflow_id, source_type, source_status, target_status, status_label, created_at) VALUES
('wfstm_task_success', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'SUCCESS', 'SUCCESS', '已完成', NOW()),
('wfstm_task_failed', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'FAILED', 'FAILED', '失败', NOW()),
('wfstm_task_canceled', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'CANCELED', 'STOPPED', '已停止', NOW()),
('wfstm_task_running', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'RUNNING', 'RUNNING', '处理中', NOW()),
('wfstm_task_retrying', 'wf_valuation_parse_v1', 'WORKFLOW_TASK', 'RETRYING', 'RUNNING', '处理中', NOW()),
('wfstm_parse_started', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_EXECUTION_STARTED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_created', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_CREATED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_dispatched', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_DISPATCHED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_queue_subscribed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SUBSCRIBED', 'RUNNING', '处理中', NOW()),
('wfstm_parse_raw_parsed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_RAW_PARSED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_standardized', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_STANDARDIZED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_persisted', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_PERSISTED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_succeeded', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_SUCCEEDED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_queue_completed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_COMPLETED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_reused', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_REUSED', 'SUCCESS', '已完成', NOW()),
('wfstm_parse_failed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'TASK_FAILED', 'FAILED', '失败', NOW()),
('wfstm_parse_queue_failed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_FAILED', 'FAILED', '失败', NOW()),
('wfstm_parse_repair_failed', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_FILE_INFO_REPAIR_FAILED', 'FAILED', '失败', NOW()),
('wfstm_parse_queue_skipped', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SKIPPED', 'STOPPED', '已停止', NOW()),
('wfstm_parse_subscribe_conflict', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SUBSCRIBE_CONFLICT', 'STOPPED', '已停止', NOW()),
('wfstm_parse_subscribe_skipped', 'wf_valuation_parse_v1', 'PARSE_LIFECYCLE', 'QUEUE_SUBSCRIBE_SKIPPED', 'STOPPED', '已停止', NOW())
ON CONFLICT (mapping_id) DO NOTHING;

INSERT INTO t_workflow_executor_binding (binding_id, workflow_id, stage_id, engine_type, external_ref, config_json, enabled, created_at, updated_at)
SELECT 'wfeb_internal_default', 'wf_valuation_parse_v1', NULL, 'INTERNAL', 'DefaultTaskDispatcher', '{}', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_workflow_executor_binding WHERE binding_id = 'wfeb_internal_default');
