--liquibase formatted sql

--changeset codex:20260419-01-mysql-qlexpress-rule-engine dbms:mysql
CREATE TABLE IF NOT EXISTS t_file_parse_profile (
    id BIGINT PRIMARY KEY COMMENT '主键',
    profile_code VARCHAR(128) NOT NULL COMMENT '模板编码',
    profile_name VARCHAR(256) NOT NULL COMMENT '模板名称',
    version VARCHAR(64) NOT NULL COMMENT '版本号',
    file_scene VARCHAR(64) NOT NULL COMMENT '文件场景',
    file_type_name VARCHAR(128) NOT NULL COMMENT '文件类型名称',
    source_channel VARCHAR(64) COMMENT '来源渠道',
    status VARCHAR(32) COMMENT '模板状态',
    priority INT COMMENT '优先级',
    match_expr TEXT COMMENT '模板匹配表达式',
    header_expr TEXT COMMENT '表头识别表达式',
    row_classify_expr TEXT COMMENT '行分类表达式',
    field_map_expr TEXT COMMENT '字段映射表达式',
    transform_expr TEXT COMMENT '值转换表达式',
    trace_enabled TINYINT(1) COMMENT '是否开启追踪',
    timeout_ms BIGINT COMMENT '超时时间毫秒数',
    checksum VARCHAR(128) COMMENT '校验值',
    creater VARCHAR(128) COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    modifier VARCHAR(128) COMMENT '修改人',
    modify_time DATETIME COMMENT '修改时间',
    published_time DATETIME COMMENT '发布时间'
) COMMENT='文件解析模板主表';

ALTER TABLE t_file_parse_profile
    ADD UNIQUE KEY uk_file_parse_profile_code_version (profile_code, version);
ALTER TABLE t_file_parse_profile
    ADD KEY idx_file_parse_profile_status_modify (status, modify_time);

CREATE TABLE IF NOT EXISTS t_file_parse_rule_step (
    id BIGINT PRIMARY KEY COMMENT '主键',
    profile_id BIGINT NOT NULL COMMENT '模板主表ID',
    rule_type VARCHAR(64) NOT NULL COMMENT '规则类型',
    step_name VARCHAR(128) NOT NULL COMMENT '步骤名称',
    priority INT COMMENT '优先级',
    enabled TINYINT(1) COMMENT '是否启用',
    expr_text TEXT COMMENT '表达式文本',
    expr_lang VARCHAR(32) COMMENT '表达式语言',
    input_schema_json TEXT COMMENT '输入结构',
    output_schema_json TEXT COMMENT '输出结构',
    error_policy VARCHAR(64) COMMENT '异常策略',
    timeout_ms BIGINT COMMENT '超时时间毫秒数',
    creater VARCHAR(128) COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    modifier VARCHAR(128) COMMENT '修改人',
    modify_time DATETIME COMMENT '修改时间'
) COMMENT='文件解析规则步骤表';

ALTER TABLE t_file_parse_rule_step
    ADD UNIQUE KEY uk_file_parse_rule_step_profile_name (profile_id, step_name);
ALTER TABLE t_file_parse_rule_step
    ADD KEY idx_file_parse_rule_step_profile_priority (profile_id, priority, id);

CREATE TABLE IF NOT EXISTS t_file_parse_case (
    id BIGINT PRIMARY KEY COMMENT '主键',
    profile_id BIGINT NOT NULL COMMENT '模板主表ID',
    sample_file_id BIGINT COMMENT '样例文件ID',
    sample_file_name VARCHAR(256) COMMENT '样例文件名',
    expected_sheet_name VARCHAR(128) COMMENT '期望sheet名',
    expected_header_row INT COMMENT '期望表头行',
    expected_data_start_row INT COMMENT '期望数据起始行',
    expected_subject_count INT COMMENT '期望科目数',
    expected_metric_count INT COMMENT '期望指标数',
    expected_output_hash VARCHAR(128) COMMENT '期望输出哈希',
    status VARCHAR(32) COMMENT '样例状态',
    creater VARCHAR(128) COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    modifier VARCHAR(128) COMMENT '修改人',
    modify_time DATETIME COMMENT '修改时间'
) COMMENT='文件解析规则回归样例表';

ALTER TABLE t_file_parse_case
    ADD KEY idx_file_parse_case_profile_sample (profile_id, sample_file_id);

CREATE TABLE IF NOT EXISTS t_file_parse_publish_log (
    id BIGINT PRIMARY KEY COMMENT '主键',
    profile_id BIGINT NOT NULL COMMENT '模板主表ID',
    version VARCHAR(64) NOT NULL COMMENT '版本号',
    publish_status VARCHAR(32) COMMENT '发布状态',
    publish_time DATETIME COMMENT '发布时间',
    publisher VARCHAR(128) COMMENT '发布人',
    publish_comment VARCHAR(512) COMMENT '发布说明',
    validation_result_json TEXT COMMENT '校验结果',
    rollback_from_version VARCHAR(64) COMMENT '回滚来源版本'
) COMMENT='文件解析规则发布日志表';

ALTER TABLE t_file_parse_publish_log
    ADD KEY idx_file_parse_publish_log_profile_time (profile_id, publish_time, id);

CREATE TABLE IF NOT EXISTS t_file_parse_rule_trace (
    id BIGINT PRIMARY KEY COMMENT '主键',
    trace_scope VARCHAR(64) COMMENT '追踪范围',
    trace_type VARCHAR(64) COMMENT '追踪类型',
    profile_id BIGINT COMMENT '模板主表ID',
    profile_code VARCHAR(128) COMMENT '模板编码',
    version VARCHAR(64) COMMENT '版本号',
    file_id BIGINT COMMENT '文件主键',
    task_id BIGINT COMMENT '任务主键',
    step_name VARCHAR(128) COMMENT '步骤名称',
    expression TEXT COMMENT '表达式文本',
    input_json TEXT COMMENT '输入内容',
    output_json TEXT COMMENT '输出内容',
    success TINYINT(1) COMMENT '是否成功',
    cost_ms BIGINT COMMENT '耗时毫秒',
    error_message VARCHAR(1024) COMMENT '错误信息',
    trace_time DATETIME COMMENT '追踪时间'
) COMMENT='文件解析规则追踪表';

ALTER TABLE t_file_parse_rule_trace
    ADD KEY idx_file_parse_rule_trace_profile_time (profile_id, trace_time, id);
ALTER TABLE t_file_parse_rule_trace
    ADD KEY idx_file_parse_rule_trace_file_task_type (file_id, task_id, trace_type, trace_time);

--changeset codex:20260419-01-postgres-qlexpress-rule-engine dbms:postgresql
CREATE TABLE t_file_parse_profile (
    id BIGINT PRIMARY KEY,
    profile_code VARCHAR(128) NOT NULL,
    profile_name VARCHAR(256) NOT NULL,
    version VARCHAR(64) NOT NULL,
    file_scene VARCHAR(64) NOT NULL,
    file_type_name VARCHAR(128) NOT NULL,
    source_channel VARCHAR(64),
    status VARCHAR(32),
    priority INT,
    match_expr TEXT,
    header_expr TEXT,
    row_classify_expr TEXT,
    field_map_expr TEXT,
    transform_expr TEXT,
    required_headers_json TEXT,
    subject_code_pattern TEXT,
    trace_enabled BOOLEAN,
    timeout_ms BIGINT,
    checksum VARCHAR(128),
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP,
    published_time TIMESTAMP
);

CREATE TABLE t_file_parse_rule_step (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    rule_type VARCHAR(64) NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    priority INT,
    enabled BOOLEAN,
    expr_text TEXT,
    expr_lang VARCHAR(32),
    input_schema_json TEXT,
    output_schema_json TEXT,
    error_policy VARCHAR(64),
    timeout_ms BIGINT,
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP
);

CREATE TABLE t_file_parse_case (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    sample_file_id BIGINT,
    sample_file_name VARCHAR(256),
    expected_sheet_name VARCHAR(128),
    expected_header_row INT,
    expected_data_start_row INT,
    expected_subject_count INT,
    expected_metric_count INT,
    expected_output_hash VARCHAR(128),
    status VARCHAR(32),
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP
);

CREATE TABLE t_file_parse_publish_log (
    id BIGINT PRIMARY KEY,
    profile_id BIGINT NOT NULL,
    version VARCHAR(64) NOT NULL,
    publish_status VARCHAR(32),
    publish_time TIMESTAMP,
    publisher VARCHAR(128),
    publish_comment VARCHAR(512),
    validation_result_json TEXT,
    rollback_from_version VARCHAR(64)
);

CREATE TABLE t_file_parse_rule_trace (
    id BIGINT PRIMARY KEY,
    trace_scope VARCHAR(64),
    trace_type VARCHAR(64),
    profile_id BIGINT,
    profile_code VARCHAR(128),
    version VARCHAR(64),
    file_id BIGINT,
    task_id BIGINT,
    step_name VARCHAR(128),
    expression TEXT,
    input_json TEXT,
    output_json TEXT,
    success BOOLEAN,
    cost_ms BIGINT,
    error_message VARCHAR(1024),
    trace_time TIMESTAMP
);

--changeset codex:20260419-01-oracle-qlexpress-rule-engine dbms:oracle
CREATE TABLE t_file_parse_profile (
    id NUMBER(19) PRIMARY KEY,
    profile_code VARCHAR2(128 CHAR) NOT NULL,
    profile_name VARCHAR2(256 CHAR) NOT NULL,
    version VARCHAR2(64 CHAR) NOT NULL,
    file_scene VARCHAR2(64 CHAR) NOT NULL,
    file_type_name VARCHAR2(128 CHAR) NOT NULL,
    source_channel VARCHAR2(64 CHAR),
    status VARCHAR2(32 CHAR),
    priority NUMBER(10),
    match_expr CLOB,
    header_expr CLOB,
    row_classify_expr CLOB,
    field_map_expr CLOB,
    transform_expr CLOB,
    required_headers_json CLOB,
    subject_code_pattern VARCHAR2(256 CHAR),
    trace_enabled NUMBER(1),
    timeout_ms NUMBER(19),
    checksum VARCHAR2(128 CHAR),
    creater VARCHAR2(128 CHAR),
    create_time TIMESTAMP,
    modifier VARCHAR2(128 CHAR),
    modify_time TIMESTAMP,
    published_time TIMESTAMP
);

CREATE TABLE t_file_parse_rule_step (
    id NUMBER(19) PRIMARY KEY,
    profile_id NUMBER(19) NOT NULL,
    rule_type VARCHAR2(64 CHAR) NOT NULL,
    step_name VARCHAR2(128 CHAR) NOT NULL,
    priority NUMBER(10),
    enabled NUMBER(1),
    expr_text CLOB,
    expr_lang VARCHAR2(32 CHAR),
    input_schema_json CLOB,
    output_schema_json CLOB,
    error_policy VARCHAR2(64 CHAR),
    timeout_ms NUMBER(19),
    creater VARCHAR2(128 CHAR),
    create_time TIMESTAMP,
    modifier VARCHAR2(128 CHAR),
    modify_time TIMESTAMP
);

CREATE TABLE t_file_parse_case (
    id NUMBER(19) PRIMARY KEY,
    profile_id NUMBER(19) NOT NULL,
    sample_file_id NUMBER(19),
    sample_file_name VARCHAR2(256 CHAR),
    expected_sheet_name VARCHAR2(128 CHAR),
    expected_header_row NUMBER(10),
    expected_data_start_row NUMBER(10),
    expected_subject_count NUMBER(10),
    expected_metric_count NUMBER(10),
    expected_output_hash VARCHAR2(128 CHAR),
    status VARCHAR2(32 CHAR),
    creater VARCHAR2(128 CHAR),
    create_time TIMESTAMP,
    modifier VARCHAR2(128 CHAR),
    modify_time TIMESTAMP
);

CREATE TABLE t_file_parse_publish_log (
    id NUMBER(19) PRIMARY KEY,
    profile_id NUMBER(19) NOT NULL,
    version VARCHAR2(64 CHAR) NOT NULL,
    publish_status VARCHAR2(32 CHAR),
    publish_time TIMESTAMP,
    publisher VARCHAR2(128 CHAR),
    publish_comment VARCHAR2(512 CHAR),
    validation_result_json CLOB,
    rollback_from_version VARCHAR2(64 CHAR)
);

CREATE TABLE t_file_parse_rule_trace (
    id NUMBER(19) PRIMARY KEY,
    trace_scope VARCHAR2(64 CHAR),
    trace_type VARCHAR2(64 CHAR),
    profile_id NUMBER(19),
    profile_code VARCHAR2(128 CHAR),
    version VARCHAR2(64 CHAR),
    file_id NUMBER(19),
    task_id NUMBER(19),
    step_name VARCHAR2(128 CHAR),
    expression CLOB,
    input_json CLOB,
    output_json CLOB,
    success NUMBER(1),
    cost_ms NUMBER(19),
    error_message VARCHAR2(1024 CHAR),
    trace_time TIMESTAMP
);
