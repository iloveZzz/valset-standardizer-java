-- Test schema for H2 in-memory database
CREATE TABLE IF NOT EXISTS t_ods_valuation_filedata (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT NOT NULL,
    file_id       BIGINT NOT NULL,
    sheet_name    VARCHAR(128),
    row_data_number    INT    NOT NULL,
    row_data_json TEXT   NOT NULL,
    row_univer_json TEXT,
    header_meta_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_task_id ON t_ods_valuation_filedata(task_id);
CREATE INDEX IF NOT EXISTS idx_file_id ON t_ods_valuation_filedata(file_id);
CREATE INDEX IF NOT EXISTS idx_file_sheet_row ON t_ods_valuation_filedata(file_id, sheet_name, row_data_number);
CREATE INDEX IF NOT EXISTS idx_task_row ON t_ods_valuation_filedata(task_id, row_data_number);

CREATE TABLE IF NOT EXISTS t_ods_valuation_sheet_style (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sheet_name VARCHAR(128) NOT NULL,
    style_scope VARCHAR(32) NOT NULL,
    sheet_style_json TEXT NOT NULL,
    preview_row_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ods_sheet_style_file_sheet_scope ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);
CREATE INDEX IF NOT EXISTS idx_ods_sheet_style_task_id ON t_ods_valuation_sheet_style(task_id);
