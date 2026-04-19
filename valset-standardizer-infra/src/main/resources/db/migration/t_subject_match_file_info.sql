CREATE TABLE t_subject_match_file_info (
    file_id BIGINT PRIMARY KEY,
    file_name_original VARCHAR(512) NOT NULL,
    file_name_normalized VARCHAR(512),
    file_extension VARCHAR(32),
    mime_type VARCHAR(128),
    file_size_bytes BIGINT,
    file_fingerprint VARCHAR(128) NOT NULL,
    source_channel VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1024),
    storage_type VARCHAR(32) NOT NULL,
    storage_uri VARCHAR(1024),
    file_format VARCHAR(32),
    file_status VARCHAR(32) NOT NULL,
    created_by VARCHAR(128),
    received_at DATETIME,
    stored_at DATETIME,
    last_processed_at DATETIME,
    last_task_id BIGINT,
    error_message VARCHAR(1024),
    source_meta_json TEXT,
    storage_meta_json TEXT,
    remark VARCHAR(1024)
);

CREATE UNIQUE INDEX uk_subject_match_file_fingerprint
    ON t_subject_match_file_info(file_fingerprint);
CREATE INDEX idx_subject_match_file_channel_status
    ON t_subject_match_file_info(source_channel, file_status);
CREATE INDEX idx_subject_match_file_received_at
    ON t_subject_match_file_info(received_at);
CREATE INDEX idx_subject_match_file_last_task_id
    ON t_subject_match_file_info(last_task_id);

CREATE TABLE t_subject_match_file_ingest_log (
    ingest_id BIGINT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    source_channel VARCHAR(64) NOT NULL,
    source_uri VARCHAR(1024),
    channel_message_id VARCHAR(256),
    ingest_status VARCHAR(32) NOT NULL,
    ingest_time DATETIME NOT NULL,
    ingest_meta_json TEXT,
    created_by VARCHAR(128),
    error_message VARCHAR(1024)
);

CREATE INDEX idx_subject_match_ingest_file_id
    ON t_subject_match_file_ingest_log(file_id);
CREATE INDEX idx_subject_match_ingest_channel_msg
    ON t_subject_match_file_ingest_log(source_channel, channel_message_id);
