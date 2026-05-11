CREATE TABLE t_file_parse_rule (
    id BIGINT PRIMARY KEY,
    creater VARCHAR(128),
    create_time DATETIME,
    modifier VARCHAR(128),
    modify_time DATETIME,
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
    file_type VARCHAR(128) NOT NULL,
    column_map VARCHAR(128) NOT NULL,
    column_name VARCHAR(256) NOT NULL,
    file_ext_info VARCHAR(512),
    status BOOLEAN,
    creater VARCHAR(128),
    create_time DATETIME,
    modifier VARCHAR(128),
    modify_time DATETIME
);
