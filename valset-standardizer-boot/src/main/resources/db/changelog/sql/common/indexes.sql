--liquibase formatted sql

--changeset codex:20260415-06-mysql-indexes dbms:mysql
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_file_info' AND index_name = 'uk_subject_match_file_fingerprint'
CREATE UNIQUE INDEX uk_subject_match_file_fingerprint ON t_subject_match_file_info(file_fingerprint);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_file_info' AND index_name = 'idx_subject_match_file_channel_status'
CREATE INDEX idx_subject_match_file_channel_status ON t_subject_match_file_info(source_channel, file_status);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_file_info' AND index_name = 'idx_subject_match_file_received_at'
CREATE INDEX idx_subject_match_file_received_at ON t_subject_match_file_info(received_at);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_file_info' AND index_name = 'idx_subject_match_file_last_task_id'
CREATE INDEX idx_subject_match_file_last_task_id ON t_subject_match_file_info(last_task_id);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_file_ingest_log' AND index_name = 'idx_subject_match_ingest_file_id'
CREATE INDEX idx_subject_match_ingest_file_id ON t_subject_match_file_ingest_log(file_id);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_file_ingest_log' AND index_name = 'idx_subject_match_ingest_channel_msg'
CREATE INDEX idx_subject_match_ingest_channel_msg ON t_subject_match_file_ingest_log(source_channel, channel_message_id);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_valuation_filedata' AND index_name = 'idx_ods_filedata_task_id'
CREATE INDEX idx_ods_filedata_task_id ON t_ods_valuation_filedata(task_id);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_valuation_filedata' AND index_name = 'idx_ods_filedata_file_id'
CREATE INDEX idx_ods_filedata_file_id ON t_ods_valuation_filedata(file_id);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_valuation_filedata' AND index_name = 'idx_ods_filedata_task_row'
CREATE INDEX idx_ods_filedata_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_valuation_sheet_style' AND index_name = 'uk_ods_sheet_style_file_sheet_scope'
CREATE UNIQUE INDEX uk_ods_sheet_style_file_sheet_scope ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_valuation_sheet_style' AND index_name = 'idx_ods_sheet_style_task_id'
CREATE INDEX idx_ods_sheet_style_task_id ON t_ods_valuation_sheet_style(task_id);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_parsed_workbook' AND index_name = 'idx_pwb_file_id'
CREATE INDEX idx_pwb_file_id ON t_subject_match_parsed_workbook(file_id);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_subject_match_result' AND index_name = 'idx_match_result_file_id'
CREATE INDEX idx_match_result_file_id ON t_subject_match_result(file_id);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_stg_external_valuation' AND index_name = 'idx_stg_val_file_id'
CREATE INDEX idx_stg_val_file_id ON t_stg_external_valuation(file_id);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_stg_external_valuation_basic_info' AND index_name = 'idx_stg_basic_info_vid_order'
CREATE INDEX idx_stg_basic_info_vid_order ON t_stg_external_valuation_basic_info(valuation_id, sort_order);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_stg_external_valuation_header' AND index_name = 'idx_stg_header_vid_col'
CREATE INDEX idx_stg_header_vid_col ON t_stg_external_valuation_header(valuation_id, column_index);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_stg_external_valuation_subject' AND index_name = 'idx_stg_subject_vid_row'
CREATE INDEX idx_stg_subject_vid_row ON t_stg_external_valuation_subject(valuation_id, row_data_number);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_stg_external_valuation_metric' AND index_name = 'idx_stg_metric_vid_row'
CREATE INDEX idx_stg_metric_vid_row ON t_stg_external_valuation_metric(valuation_id, row_data_number);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_dwd_external_valuation_subject' AND index_name = 'idx_dwd_subject_file_row'
CREATE INDEX idx_dwd_subject_file_row ON t_dwd_external_valuation_subject(file_id, row_data_number);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_dwd_external_valuation_subject' AND index_name = 'idx_dwd_subject_code'
CREATE INDEX idx_dwd_subject_code ON t_dwd_external_valuation_subject(standard_code);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_dwd_external_valuation_subject' AND index_name = 'idx_dwd_subject_status'
CREATE INDEX idx_dwd_subject_status ON t_dwd_external_valuation_subject(mapping_status);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_dwd_external_valuation_metric' AND index_name = 'idx_dwd_metric_file_row'
CREATE INDEX idx_dwd_metric_file_row ON t_dwd_external_valuation_metric(file_id, row_data_number);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_dwd_external_valuation_metric' AND index_name = 'idx_dwd_metric_code'
CREATE INDEX idx_dwd_metric_code ON t_dwd_external_valuation_metric(metric_code);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_dwd_external_valuation_metric' AND index_name = 'idx_dwd_metric_status'
CREATE INDEX idx_dwd_metric_status ON t_dwd_external_valuation_metric(mapping_status);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_standard_subject' AND index_name = 'idx_ods_standard_subject_code'
CREATE INDEX idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_standard_subject' AND index_name = 'idx_ods_standard_subject_root_level'
CREATE INDEX idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_mapping_hint' AND index_name = 'idx_ods_mapping_hint_key_code'
CREATE INDEX idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_mapping_sample' AND index_name = 'idx_ods_mapping_sample_org'
CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_mapping_sample' AND index_name = 'idx_ods_mapping_sample_ext_code'
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_ods_mapping_sample' AND index_name = 'idx_ods_mapping_sample_std_code'
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_tr_jjhzgzb' AND index_name = 'idx_tr_jjhzgzb_org'
CREATE INDEX idx_tr_jjhzgzb_org ON t_tr_jjhzgzb(org_cd);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_tr_jjhzgzb' AND index_name = 'idx_tr_jjhzgzb_subject'
CREATE INDEX idx_tr_jjhzgzb_subject ON t_tr_jjhzgzb(subject_cd);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_tr_jjhzgzb' AND index_name = 'idx_tr_jjhzgzb_biz_date'
CREATE INDEX idx_tr_jjhzgzb_biz_date ON t_tr_jjhzgzb(biz_date);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_tr_jjhzgzb' AND index_name = 'idx_tr_jjhzgzb_pd'
CREATE INDEX idx_tr_jjhzgzb_pd ON t_tr_jjhzgzb(pd_cd);
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_tr_index' AND index_name = 'idx_t_tr_index_date_org_pd'
CREATE INDEX idx_t_tr_index_date_org_pd ON t_tr_index(biz_date, org_cd, pd_cd);

--changeset codex:20260415-06-postgres-indexes dbms:postgresql
CREATE UNIQUE INDEX uk_subject_match_file_fingerprint ON t_subject_match_file_info(file_fingerprint);
CREATE INDEX idx_subject_match_file_channel_status ON t_subject_match_file_info(source_channel, file_status);
CREATE INDEX idx_subject_match_file_received_at ON t_subject_match_file_info(received_at);
CREATE INDEX idx_subject_match_file_last_task_id ON t_subject_match_file_info(last_task_id);
CREATE INDEX idx_subject_match_ingest_file_id ON t_subject_match_file_ingest_log(file_id);
CREATE INDEX idx_subject_match_ingest_channel_msg ON t_subject_match_file_ingest_log(source_channel, channel_message_id);

CREATE INDEX idx_ods_filedata_task_id ON t_ods_valuation_filedata(task_id);
CREATE INDEX idx_ods_filedata_file_id ON t_ods_valuation_filedata(file_id);
CREATE INDEX idx_ods_filedata_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
CREATE UNIQUE INDEX uk_ods_sheet_style_file_sheet_scope ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);
CREATE INDEX idx_ods_sheet_style_task_id ON t_ods_valuation_sheet_style(task_id);

CREATE INDEX idx_pwb_file_id ON t_subject_match_parsed_workbook(file_id);
CREATE INDEX idx_match_result_file_id ON t_subject_match_result(file_id);

CREATE INDEX idx_stg_val_file_id ON t_stg_external_valuation(file_id);
CREATE INDEX idx_stg_basic_info_vid_order ON t_stg_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX idx_stg_header_vid_col ON t_stg_external_valuation_header(valuation_id, column_index);
CREATE INDEX idx_stg_subject_vid_row ON t_stg_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX idx_stg_metric_vid_row ON t_stg_external_valuation_metric(valuation_id, row_data_number);

CREATE INDEX idx_dwd_subject_file_row ON t_dwd_external_valuation_subject(file_id, row_data_number);
CREATE INDEX idx_dwd_subject_code ON t_dwd_external_valuation_subject(standard_code);
CREATE INDEX idx_dwd_subject_status ON t_dwd_external_valuation_subject(mapping_status);
CREATE INDEX idx_dwd_metric_file_row ON t_dwd_external_valuation_metric(file_id, row_data_number);
CREATE INDEX idx_dwd_metric_code ON t_dwd_external_valuation_metric(metric_code);
CREATE INDEX idx_dwd_metric_status ON t_dwd_external_valuation_metric(mapping_status);

CREATE INDEX idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
CREATE INDEX idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
CREATE INDEX idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

CREATE INDEX idx_tr_jjhzgzb_org ON t_tr_jjhzgzb(org_cd);
CREATE INDEX idx_tr_jjhzgzb_subject ON t_tr_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_jjhzgzb_biz_date ON t_tr_jjhzgzb(biz_date);
CREATE INDEX idx_tr_jjhzgzb_pd ON t_tr_jjhzgzb(pd_cd);
CREATE INDEX idx_t_tr_index_date_org_pd ON t_tr_index(biz_date, org_cd, pd_cd);

--changeset codex:20260415-06-oracle-indexes dbms:oracle
CREATE UNIQUE INDEX uk_subject_match_file_fingerprint ON t_subject_match_file_info(file_fingerprint);
CREATE INDEX idx_subject_match_file_channel_status ON t_subject_match_file_info(source_channel, file_status);
CREATE INDEX idx_subject_match_file_received_at ON t_subject_match_file_info(received_at);
CREATE INDEX idx_subject_match_file_last_task_id ON t_subject_match_file_info(last_task_id);
CREATE INDEX idx_subject_match_ingest_file_id ON t_subject_match_file_ingest_log(file_id);
CREATE INDEX idx_subject_match_ingest_channel_msg ON t_subject_match_file_ingest_log(source_channel, channel_message_id);

CREATE INDEX idx_ods_filedata_task_id ON t_ods_valuation_filedata(task_id);
CREATE INDEX idx_ods_filedata_file_id ON t_ods_valuation_filedata(file_id);
CREATE INDEX idx_ods_filedata_task_row ON t_ods_valuation_filedata(task_id, row_data_number);
CREATE UNIQUE INDEX uk_ods_sheet_style_file_sheet_scope ON t_ods_valuation_sheet_style(file_id, sheet_name, style_scope);
CREATE INDEX idx_ods_sheet_style_task_id ON t_ods_valuation_sheet_style(task_id);

CREATE INDEX idx_pwb_file_id ON t_subject_match_parsed_workbook(file_id);
CREATE INDEX idx_match_result_file_id ON t_subject_match_result(file_id);

CREATE INDEX idx_stg_val_file_id ON t_stg_external_valuation(file_id);
CREATE INDEX idx_stg_basic_info_vid_order ON t_stg_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX idx_stg_header_vid_col ON t_stg_external_valuation_header(valuation_id, column_index);
CREATE INDEX idx_stg_subject_vid_row ON t_stg_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX idx_stg_metric_vid_row ON t_stg_external_valuation_metric(valuation_id, row_data_number);

CREATE INDEX idx_dwd_subject_file_row ON t_dwd_external_valuation_subject(file_id, row_data_number);
CREATE INDEX idx_dwd_subject_code ON t_dwd_external_valuation_subject(standard_code);
CREATE INDEX idx_dwd_subject_status ON t_dwd_external_valuation_subject(mapping_status);
CREATE INDEX idx_dwd_metric_file_row ON t_dwd_external_valuation_metric(file_id, row_data_number);
CREATE INDEX idx_dwd_metric_code ON t_dwd_external_valuation_metric(metric_code);
CREATE INDEX idx_dwd_metric_status ON t_dwd_external_valuation_metric(mapping_status);

CREATE INDEX idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
CREATE INDEX idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
CREATE INDEX idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

CREATE INDEX idx_tr_jjhzgzb_org ON t_tr_jjhzgzb(org_cd);
CREATE INDEX idx_tr_jjhzgzb_subject ON t_tr_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_jjhzgzb_biz_date ON t_tr_jjhzgzb(biz_date);
CREATE INDEX idx_tr_jjhzgzb_pd ON t_tr_jjhzgzb(pd_cd);
CREATE INDEX idx_t_tr_index_date_org_pd ON t_tr_index(biz_date, org_cd, pd_cd);
