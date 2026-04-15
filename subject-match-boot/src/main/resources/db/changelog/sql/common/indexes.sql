--liquibase formatted sql

--changeset codex:20260415-06-mysql-indexes dbms:mysql
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

CREATE INDEX idx_dwd_val_file_id ON t_dwd_external_valuation(file_id);
CREATE INDEX idx_dwd_basic_info_vid_order ON t_dwd_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX idx_dwd_header_vid_col ON t_dwd_external_valuation_header(valuation_id, column_index);
CREATE INDEX idx_dwd_subject_vid_row ON t_dwd_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX idx_dwd_metric_vid_row ON t_dwd_external_valuation_metric(valuation_id, row_data_number);

CREATE INDEX idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
CREATE INDEX idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
CREATE INDEX idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

CREATE INDEX idx_tr_fm_jjhzgzb_org ON tr_fm_jjhzgzb(org_cd);
CREATE INDEX idx_tr_fm_jjhzgzb_subject ON tr_fm_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_fm_jjhzgzb_biz_date ON tr_fm_jjhzgzb(biz_date);
CREATE INDEX idx_tr_fm_jjhzgzb_pd ON tr_fm_jjhzgzb(pd_cd);

CREATE INDEX idx_tr_spv_jjhzgzb_org ON tr_spv_jjhzgzb(org_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_subject ON tr_spv_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_biz_date ON tr_spv_jjhzgzb(biz_date);
CREATE INDEX idx_tr_spv_jjhzgzb_pd ON tr_spv_jjhzgzb(pd_cd);

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

CREATE INDEX idx_dwd_val_file_id ON t_dwd_external_valuation(file_id);
CREATE INDEX idx_dwd_basic_info_vid_order ON t_dwd_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX idx_dwd_header_vid_col ON t_dwd_external_valuation_header(valuation_id, column_index);
CREATE INDEX idx_dwd_subject_vid_row ON t_dwd_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX idx_dwd_metric_vid_row ON t_dwd_external_valuation_metric(valuation_id, row_data_number);

CREATE INDEX idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
CREATE INDEX idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
CREATE INDEX idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

CREATE INDEX idx_tr_fm_jjhzgzb_org ON tr_fm_jjhzgzb(org_cd);
CREATE INDEX idx_tr_fm_jjhzgzb_subject ON tr_fm_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_fm_jjhzgzb_biz_date ON tr_fm_jjhzgzb(biz_date);
CREATE INDEX idx_tr_fm_jjhzgzb_pd ON tr_fm_jjhzgzb(pd_cd);

CREATE INDEX idx_tr_spv_jjhzgzb_org ON tr_spv_jjhzgzb(org_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_subject ON tr_spv_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_biz_date ON tr_spv_jjhzgzb(biz_date);
CREATE INDEX idx_tr_spv_jjhzgzb_pd ON tr_spv_jjhzgzb(pd_cd);

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

CREATE INDEX idx_dwd_val_file_id ON t_dwd_external_valuation(file_id);
CREATE INDEX idx_dwd_basic_info_vid_order ON t_dwd_external_valuation_basic_info(valuation_id, sort_order);
CREATE INDEX idx_dwd_header_vid_col ON t_dwd_external_valuation_header(valuation_id, column_index);
CREATE INDEX idx_dwd_subject_vid_row ON t_dwd_external_valuation_subject(valuation_id, row_data_number);
CREATE INDEX idx_dwd_metric_vid_row ON t_dwd_external_valuation_metric(valuation_id, row_data_number);

CREATE INDEX idx_ods_standard_subject_code ON t_ods_standard_subject(standard_code);
CREATE INDEX idx_ods_standard_subject_root_level ON t_ods_standard_subject(root_code, level);
CREATE INDEX idx_ods_mapping_hint_key_code ON t_ods_mapping_hint(normalized_key, standard_code);
CREATE INDEX idx_ods_mapping_sample_org ON t_ods_mapping_sample(org_name);
CREATE INDEX idx_ods_mapping_sample_ext_code ON t_ods_mapping_sample(external_code);
CREATE INDEX idx_ods_mapping_sample_std_code ON t_ods_mapping_sample(standard_code);

CREATE INDEX idx_tr_fm_jjhzgzb_org ON tr_fm_jjhzgzb(org_cd);
CREATE INDEX idx_tr_fm_jjhzgzb_subject ON tr_fm_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_fm_jjhzgzb_biz_date ON tr_fm_jjhzgzb(biz_date);
CREATE INDEX idx_tr_fm_jjhzgzb_pd ON tr_fm_jjhzgzb(pd_cd);

CREATE INDEX idx_tr_spv_jjhzgzb_org ON tr_spv_jjhzgzb(org_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_subject ON tr_spv_jjhzgzb(subject_cd);
CREATE INDEX idx_tr_spv_jjhzgzb_biz_date ON tr_spv_jjhzgzb(biz_date);
CREATE INDEX idx_tr_spv_jjhzgzb_pd ON tr_spv_jjhzgzb(pd_cd);
