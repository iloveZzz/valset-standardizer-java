CREATE TABLE t_ods_mapping_sample (
    id BIGINT PRIMARY KEY,
    org_name VARCHAR(256),
    org_id VARCHAR(128),
    external_code VARCHAR(500),
    external_name VARCHAR(512),
    standard_code VARCHAR(128),
    standard_name VARCHAR(512),
    standard_system VARCHAR(128),
    system_name VARCHAR(256)
);

CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);
