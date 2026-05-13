-- ODS Layer Table: Valuation Sheet Style Snapshot
-- This table stores sheet-level styling and header preview snapshots for Excel valuation files.
-- It is intentionally separated from row-level ODS raw data so that only header/title/merge
-- metadata needs to be retained for Univer rendering.
--
-- Requirements: style snapshot persistence
-- Feature: ODS Data Extraction

CREATE TABLE t_ods_valuation_sheet_style (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    task_id BIGINT NOT NULL COMMENT 'Reference to the extraction task',
    file_id BIGINT NOT NULL COMMENT 'Identifier for the source file',
    sheet_name VARCHAR(128) NOT NULL COMMENT 'Worksheet name',
    style_scope VARCHAR(32) NOT NULL COMMENT 'Style snapshot scope, for example HEADER_PREVIEW',
    sheet_style_json TEXT NOT NULL COMMENT 'Univer-compatible style snapshot for title/header/merge area',
    preview_row_count INT COMMENT 'Number of preview rows included in the style snapshot',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time'
) COMMENT='ODS layer table for valuation sheet style snapshots';

CREATE UNIQUE INDEX idx_ods_sheet_style_file_sheet_scope
    ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);

CREATE INDEX idx_ods_sheet_style_task_id
    ON t_ods_valuation_sheet_style(task_id);
