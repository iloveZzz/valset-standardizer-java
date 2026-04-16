--liquibase formatted sql

--changeset codex:20260416-01-mysql-parse-dictionary dbms:mysql
CREATE TABLE IF NOT EXISTS t_file_parse_rule (
    id BIGINT PRIMARY KEY COMMENT '主键',
    creater VARCHAR(128) COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    modifier VARCHAR(128) COMMENT '修改人',
    modify_time DATETIME COMMENT '修改时间',
    file_scene VARCHAR(64) NOT NULL COMMENT '文件场景',
    file_type_name VARCHAR(128) NOT NULL COMMENT '文件类型名称',
    region_name VARCHAR(64) NOT NULL COMMENT '标准区域名称',
    column_map VARCHAR(128) NOT NULL COMMENT '标准列编码',
    column_map_name VARCHAR(256) NOT NULL COMMENT '标准列名称',
    status TINYINT(1) COMMENT '启用状态',
    multi_index TINYINT(1) COMMENT '是否多实例指标',
    required TINYINT(1) COMMENT '是否必需'
) COMMENT='文件解析标准规则表';


CREATE TABLE IF NOT EXISTS t_file_parse_source (
    id BIGINT PRIMARY KEY COMMENT '主键',
    rule_id BIGINT NOT NULL COMMENT '规则标识',
    file_type VARCHAR(128) NOT NULL COMMENT '文件类型',
    column_map VARCHAR(128) NOT NULL COMMENT '标准列编码',
    column_name VARCHAR(256) NOT NULL COMMENT '来源列名称',
    file_ext_info VARCHAR(512) COMMENT '扩展信息',
    status TINYINT(1) COMMENT '启用状态',
    creater VARCHAR(128) COMMENT '创建人',
    create_time DATETIME COMMENT '创建时间',
    modifier VARCHAR(128) COMMENT '修改人',
    modify_time DATETIME COMMENT '修改时间'
) COMMENT='文件解析来源映射表';

CREATE INDEX idx_file_parse_source_rule_type_name
    ON t_file_parse_source(rule_id, file_type, column_name);
CREATE INDEX idx_file_parse_source_rule_id ON t_file_parse_source(rule_id);
CREATE INDEX idx_file_parse_source_column_map ON t_file_parse_source(column_map);
CREATE INDEX idx_file_parse_source_column_name ON t_file_parse_source(column_name);

--changeset codex:20260416-01-postgres-parse-dictionary dbms:postgresql
CREATE TABLE t_file_parse_rule (
    id BIGINT PRIMARY KEY,
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP,
    file_scene VARCHAR(64) NOT NULL,
    file_type_name VARCHAR(128) NOT NULL,
    region_name VARCHAR(64) NOT NULL,
    column_map VARCHAR(128) NOT NULL,
    column_map_name VARCHAR(256) NOT NULL,
    status BOOLEAN,
    multi_index BOOLEAN,
    required BOOLEAN
);


CREATE TABLE t_file_parse_source (
    id BIGINT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    file_type VARCHAR(128) NOT NULL,
    column_map VARCHAR(128) NOT NULL,
    column_name VARCHAR(256) NOT NULL,
    file_ext_info VARCHAR(512),
    status BOOLEAN,
    creater VARCHAR(128),
    create_time TIMESTAMP,
    modifier VARCHAR(128),
    modify_time TIMESTAMP
);

--changeset codex:20260416-01-oracle-parse-dictionary dbms:oracle
CREATE TABLE t_file_parse_rule (
    id NUMBER(19) PRIMARY KEY,
    creater VARCHAR2(128 CHAR),
    create_time TIMESTAMP,
    modifier VARCHAR2(128 CHAR),
    modify_time TIMESTAMP,
    file_scene VARCHAR2(64 CHAR) NOT NULL,
    file_type_name VARCHAR2(128 CHAR) NOT NULL,
    region_name VARCHAR2(64 CHAR) NOT NULL,
    column_map VARCHAR2(128 CHAR) NOT NULL,
    column_map_name VARCHAR2(256 CHAR) NOT NULL,
    status NUMBER(1),
    multi_index NUMBER(1),
    required NUMBER(1)
);


CREATE TABLE t_file_parse_source (
    id NUMBER(19) PRIMARY KEY,
    file_type VARCHAR2(128 CHAR) NOT NULL,
    column_map VARCHAR2(128 CHAR) NOT NULL,
    column_name VARCHAR2(256 CHAR) NOT NULL,
    file_ext_info VARCHAR2(512 CHAR),
    status NUMBER(1),
    creater VARCHAR2(128 CHAR),
    create_time TIMESTAMP,
    modifier VARCHAR2(128 CHAR),
    modify_time TIMESTAMP
);
