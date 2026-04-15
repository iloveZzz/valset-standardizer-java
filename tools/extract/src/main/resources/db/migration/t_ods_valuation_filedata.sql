-- ODS Layer Table: Raw Valuation File Data
-- This table stores raw extracted row data from Excel/CSV valuation files
-- without any domain-level interpretation, supporting the Extract phase of the ETL pipeline.
--
-- Requirements: 3.1, 3.3
-- Feature: ODS Data Extraction

CREATE TABLE t_ods_valuation_filedata (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    task_id       BIGINT NOT NULL COMMENT 'Reference to the extraction task',
    file_id       BIGINT NOT NULL COMMENT 'Identifier for the source file',
    row_data_number    INT    NOT NULL COMMENT '1-based row number from source file',
    row_data_json TEXT   NOT NULL COMMENT 'JSON array of cell values as strings'
) COMMENT='ODS layer table for raw valuation file row data';

-- Index for querying rows by task
CREATE INDEX idx_task_id ON t_ods_valuation_filedata(task_id);

-- Index for querying rows by file
CREATE INDEX idx_file_id ON t_ods_valuation_filedata(file_id);

-- Composite index for efficient task+row queries
CREATE INDEX idx_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
