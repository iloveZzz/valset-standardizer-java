-- Test schema for H2 in-memory database
CREATE TABLE IF NOT EXISTS t_ods_valuation_filedata (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT NOT NULL,
    file_id       BIGINT NOT NULL,
    row_data_number    INT    NOT NULL,
    row_data_json TEXT   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_id ON t_ods_valuation_filedata(task_id);
CREATE INDEX IF NOT EXISTS idx_file_id ON t_ods_valuation_filedata(file_id);
CREATE INDEX IF NOT EXISTS idx_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
