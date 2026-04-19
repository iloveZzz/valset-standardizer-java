--liquibase formatted sql

--changeset codex:20260419-02-mysql-qlexpress-rule-engine-init dbms:mysql
INSERT INTO t_file_parse_profile (
    id, profile_code, profile_name, version, file_scene, file_type_name, source_channel, status, priority,
    match_expr, header_expr, row_classify_expr, field_map_expr, transform_expr, trace_enabled, timeout_ms,
    checksum, creater, create_time, modifier, modify_time, published_time
) VALUES
(910000000000000001, 'default-valset-excel-v1', '默认估值模板-Excel', 'v1', 'VALSET', 'EXCEL', 'system', 'PUBLISHED', 1000,
 'containsAny(fileName, [''.xlsx'', ''.xls''])', 'isHeaderRow(row, requiredHeaders)', 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)',
 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))',
 'value', 1, 3000, 'seed-default-valset-excel-v1', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(910000000000000002, 'default-valset-csv-v1', '默认估值模板-CSV', 'v1', 'VALSET', 'CSV', 'system', 'PUBLISHED', 1000,
 'containsAny(fileName, [''.csv''])', 'isHeaderRow(row, requiredHeaders)', 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)',
 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))',
 'value', 1, 3000, 'seed-default-valset-csv-v1', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_file_parse_rule_step (
    id, profile_id, rule_type, step_name, priority, enabled, expr_text, expr_lang, input_schema_json,
    output_schema_json, error_policy, timeout_ms, creater, create_time, modifier, modify_time
) VALUES
(910000000000000101, 910000000000000001, 'HEADER_DETECT', '表头识别', 10, 1, 'isHeaderRow(row, requiredHeaders)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000102, 910000000000000001, 'ROW_CLASSIFY', '行分类', 20, 1, 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000103, 910000000000000001, 'FIELD_MAP', '字段映射', 30, 1, 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000104, 910000000000000001, 'VALUE_TRANSFORM', '值转换', 40, 1, 'value', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000201, 910000000000000002, 'HEADER_DETECT', '表头识别', 10, 1, 'isHeaderRow(row, requiredHeaders)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000202, 910000000000000002, 'ROW_CLASSIFY', '行分类', 20, 1, 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000203, 910000000000000002, 'FIELD_MAP', '字段映射', 30, 1, 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000204, 910000000000000002, 'VALUE_TRANSFORM', '值转换', 40, 1, 'value', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (1, NULL, NULL, 'system', NULL, 'ALL', '', 'column', 'subject_cd', '科目代码', 1, NULL, 1);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (2, NULL, NULL, NULL, NULL, 'ALL', '', 'basic', 'biz_date', '业务日期', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (3, NULL, NULL, NULL, NULL, 'ALL', 'VALUATION', 'column', 'n_hldamt', '持仓数量', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (5, NULL, NULL, NULL, NULL, 'ALL', 'VALUATION', 'column', 'n_price_cost', '单位成本', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (6, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_hldcst_locl', '本币持仓成本', 1, NULL, 1);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (7, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_cb_jz_bl', '本币成本占比', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (9, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_valprice', '证券估值行情', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (10, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_hldmkv_locl', '本币持仓市值', 1, NULL, 1);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (12, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_sz_jz_bl', '本币市值占比', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (13, NULL, NULL, NULL, NULL, 'ALL', '', 'basic', 'pd_name', '产品名称', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (14, NULL, NULL, NULL, NULL, 'ALL', '', 'basic', 'pd_code', '产品代码', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (15, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_hldvva_l', '本币证券估增', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (16, NULL, NULL, 'wangyancong2', NULL, 'ALL', 'VALUATION', 'column', 'c_subpend', '停牌信息', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (19, NULL, NULL, 'zhudaoming', NULL, 'ALL', '', 'column', 'c_rights', '权益信息', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (20, NULL, NULL, 'zhudaoming', NULL, 'ALL', 'VALUATION', 'column', 'c_cury_code', '估值币种', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (22, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_valrate', '货币估值汇率', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (23, NULL, NULL, 'zhudaoming', NULL, 'ALL', 'VALUATION', 'column', 'n_hldcst', '原币持仓成本', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (24, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_hldmkv', '原币持仓市值', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (26, NULL, NULL, NULL, NULL, 'ALL', '', 'column', 'n_hldvva', '原币证券估增', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (28, NULL, NULL, 'zhudaoming', NULL, 'ALL', 'VALUATION', 'column', 'c_mkt_code', '交易市场', 1, NULL, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (101, 'system', NULL, 'system', NULL, 'ALL', '', 'column', 'subject_nm', '科目名称', 1, NULL, 1);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (722, NULL, NULL, 'wr_user1', NULL, 'ALL', '', 'metric', 'total_assets', '资产合计', 1, 1, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (723, NULL, NULL, 'wr_user1', NULL, 'ALL', '', 'metric', 'total_liabi', '负债合计', 1, 1, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (724, NULL, NULL, 'wr_user1', NULL, 'ALL', '', 'metric', 'paid_capital', '实收资本', 1, 1, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (725, NULL, NULL, 'yuanlingling', NULL, 'ALL', '', 'metric', 'deviation', '偏离度', 1, 1, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (726, NULL, NULL, 'wr_user1', NULL, 'ALL', '', 'metric', 'asset_value', '资产净值', 1, 1, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (728, NULL, NULL, NULL, NULL, 'ALL', '', 'metric', 'yield', '本日收益', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (731, NULL, NULL, NULL, NULL, 'ALL', '', 'metric', 'avg_nav', '今日单位净值', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (732, NULL, NULL, NULL, NULL, 'ALL', '', 'metric', 'asset_value_y', '资产净值(原币)', 1, 1, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (733, NULL, NULL, NULL, NULL, 'ALL', '', 'metric', 'ten_sou_yield', '每日万份收益', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (734, NULL, NULL, 'liyuanyuan', NULL, 'ALL', '', 'metric', 'acc_net', '累计单位净值', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (739, NULL, NULL, NULL, NULL, 'ALL', '', 'metric', 'deviation_amt', '偏离金额', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (740, NULL, NULL, NULL, NULL, 'ALL', '', 'metric', 'yield2', '每日收益', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (743, NULL, NULL, 'liyuanyuan', NULL, 'ALL', '', 'metric', 'seven_annu_yield', '七日年化收益率', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (2501, 'lihongmin', NULL, 'lihongmin', NULL, 'ALL', 'VALUATION', 'column', 'project_cd', '项目代码', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (2502, 'lihongmin', NULL, 'lihongmin', NULL, 'ALL', 'VALUATION', 'column', 'project_nm', '项目名称', 1, 0, 0);
INSERT INTO `t_file_parse_rule` (`id`, `creater`, `create_time`, `modifier`, `modify_time`, `file_scene`, `file_type_name`, `region_name`, `column_map`, `column_map_name`, `status`, `multi_index`, `required`) VALUES (2901, 'lihongmin', NULL, 'lihongmin', NULL, 'ALL', 'VALUATION', 'column', 'x', '空列', 1, 0, 0);

INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (2, '', 'subject_nm', '科目名称：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3, '', 'n_hldamt', '数量', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (4, '', 'n_price_cost', '单位成本', NULL, 1, NULL, NULL, 'wr_user', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (5, '', 'n_hldcst_locl', '成本|本币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (6, '', 'n_cb_jz_bl', '成本占比', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (7, '', 'n_valprice', '行情', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (8, '', 'n_hldmkv_locl', '市值|本币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (9, '', 'n_sz_jz_bl', '市值占比', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (10, '', 'n_hldvva_l', '估值增值|本币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (11, '', 'c_subpend', '停牌信息', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (12, '', 'c_rights', '权益信息', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (13, '', 'c_cury_code', '币种', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (14, '', 'n_valrate', '汇率', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (15, '', 'n_hldcst', '成本|原币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (16, '', 'n_hldmkv', '市值|原币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (17, '', 'n_hldvva', '估值增值|原币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (18, '', 'n_hldcst_locl', '成本', NULL, 1, NULL, NULL, 'wr_user', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (19, '', 'n_hldmkv_locl', '市值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (20, '', 'n_hldvva_l', '估值增值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (21, '', 'n_cb_jz_bl', '成本占净值%', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (22, '', 'n_valprice', '市价', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (23, '', 'n_sz_jz_bl', '市值占净值%', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (24, '', 'n_hldcst', '成本原币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (25, '', 'n_hldcst_locl', '成本本币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (26, '', 'n_hldmkv', '市值原币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (27, '', 'n_hldmkv_locl', '市值本币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (28, '', 'n_hldvva', '估值增值原币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (29, '', 'n_hldvva_l', '估值增值本币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (30, '', 'n_hldamt', '证券数量', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (31, '', 'n_hldcst_locl', '证券成本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (32, '', 'n_cb_jz_bl', '成本净值比例', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (33, '', 'n_valprice', '行情价格', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (34, '', 'n_hldmkv_locl', '证券市值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (35, '', 'n_sz_jz_bl', '市值净值比例', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (36, '', 'n_hldamt', '数    量', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (37, '', 'n_hldcst_locl', '成    本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (38, '', 'n_valprice', '行情收市价', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (39, '', 'n_hldmkv_locl', '市    值', NULL, 1, NULL, NULL, 'yuanlingling', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (40, '', 'n_hldcst_locl', '成 本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (41, '', 'n_valprice', '估值价格', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (42, '', 'n_hldmkv_locl', '市 值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (43, '', 'n_hldcst_locl', '成本(本位币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (44, '', 'n_valprice', '行情收市价(本位币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (45, '', 'n_price_cost', '市值(本位币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (46, '', 'n_hldvva_l', '估值增值(本位币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (47, '', 'n_hldvva_l', '增值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (48, '', 'n_valprice', '行情行情', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (49, '', 'n_cb_jz_bl', '成本占净值(%)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (50, '', 'n_sz_jz_bl', '市值占净值(%)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (51, '', 'n_cb_jz_bl', '成本占净值比(%)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (52, '', 'n_sz_jz_bl', '市值占净值比(%)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (53, '', 'n_cb_jz_bl', '成本占净值%|成本占比|成本占比', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (54, '', 'n_sz_jz_bl', '市值占净值%|市值占比|市值占比', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (55, '', 'n_hldcst', '成本|原币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (56, '', 'n_hldcst_locl', '成本|本币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (57, '', 'n_hldmkv', '市值|原币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (58, '', 'n_hldmkv_locl', '市值|本币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (59, '', 'n_hldvva', '估值增值|原币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (60, '', 'n_hldvva_l', '估值增值|本币', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (61, '', 'n_hldcst_locl', '成    本|万千百十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (62, '', 'n_hldmkv_locl', '市值|万千百十亿千百十万千百十元角分', NULL, 1, NULL, NULL, 'yuanlingling', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (63, '', 'n_hldcst_locl', '成    本|千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (64, '', 'n_hldmkv_locl', '市    值|千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (65, '', 'n_hldvva_l', '估值增值|万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (66, '', 'n_hldvva_l', '估值增值|百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (67, '', 'n_hldcst_locl', '成本|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (68, '', 'n_hldmkv_locl', '市值|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (69, '', 'n_hldvva_l', '估值增值|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (70, '', 'n_hldvva_l', '增值|万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (74, '', 'n_hldcst_locl', '成本-本币|成本|本币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (75, '', 'n_hldmkv_locl', '市值-本币|市值|本币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (76, '', 'n_hldvva_l', '估值增值-本币|估值增值|本币|十亿千百十万千百十元角分', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (77, '', 'c_mkt_code', '交易场所', NULL, 1, NULL, NULL, 'malongtao', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (78, '', 'n_cb_jz_bl', '成本占比|%', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (79, '', 'n_sz_jz_bl', '市值占比|%', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (80, '', 'c_mkt_code', '交易市场', NULL, 1, NULL, NULL, 'wr_user', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (81, '', 'c_subpend', '停牌标志', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (82, '', 'c_rights', '权益信息3', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (83, '', 'pd_name', '产品简称：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (84, '', 'pd_code', '产品代码：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (85, '', 'subject_cd', '科目代码', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (86, '', 'pd_name', '产品简称', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (87, '', 'pd_name', '产品简称:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (88, '', 'pd_code', '产品代码', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (94, '', 'biz_date', '日期：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (95, '', 'biz_date', '业务日期：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (96, '', 'biz_date', '业务日期', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (97, '', 'biz_date', '业务日期:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (98, '', 'biz_date', '估值日期', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (99, '', 'biz_date', '估值日期:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (100, '', 'biz_date', '估值日期：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (101, '', 'subject_nm', '科目名称', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (201, '', 'subject_nm', '科目名称1：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (301, '', 'string', 'string', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (402, '', 'subject_cd', '测试目标列', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (600, '', 'asset_value', '资产净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (601, '', 'total_assets', '资产合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (602, '', 'paid_capital', '实收资本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (603, '', 'ten_sou_yield', '每日万份收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (604, '', 'yield2', '每日收益2', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (605, '', 'acc_net', '累计单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (606, '', 'avg_nav', '今日单位净值', NULL, 1, NULL, NULL, 'liyuanyuan', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (607, '', 'total_liabi', '负债合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (608, '', 'paid_capital', '实收资本(金额)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (609, '', 'paid_capital', '实收资本:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (610, '', 'paid_capital', '实收资本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (611, '', 'paid_capital', '811实收资本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (612, '', 'total_assets', '资产类合计:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (613, '', 'total_assets', '831资产合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (614, '', 'total_assets', '资产合计:', NULL, 1, NULL, NULL, 'wr_user', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (615, '', 'total_assets', '资产类合计：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (616, '', 'total_assets', '资产类合计', NULL, 1, NULL, NULL, 'wr_user', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (617, '', 'total_liabi', '负债类合计：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (618, '', 'total_liabi', '832负债合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (619, '', 'total_liabi', '负债合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (620, '', 'total_liabi', '负债类合计:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (621, '', 'total_liabi', '负债类合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (622, '', 'asset_value', '资产资产净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (623, '', 'asset_value', '今日资产净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (624, '', 'asset_value', '信托资产净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (625, '', 'asset_value', '基金资产净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (626, '', 'asset_value', '资产净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (627, '', 'asset_value', '841资产净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (628, '', 'asset_value', '基金资产净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (629, '', 'asset_value', '资产资产净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (630, '', 'asset_value', '委托资产净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (631, '', 'avg_nav', '资产单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (632, '', 'avg_nav', '基金单位净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (633, '', 'avg_nav', '资产单位净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (634, '', 'avg_nav', '今日单位净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (635, '', 'avg_nav', '今日单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (636, '', 'avg_nav', '901今日单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (637, '', 'avg_nav', '基金单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (638, '', 'acc_net', '累计单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (639, '', 'acc_net', '累计单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (640, '', 'acc_net', '累计单位净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (641, '', 'acc_net', '904累计单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (642, '', 'ten_sou_yield', '每日万份收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (643, '', 'ten_sou_yield', '每万份收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (644, '', 'seven_annu_yield', '七日年化收益率', NULL, 1, NULL, NULL, 'yuanlingling', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (645, '', 'yield', '本日收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (646, '', 'yield', '每日收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (647, '', 'avg_nav', '702', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (648, '', 'paid_capital', '601', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (650, '', 'total_assets', '604', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (652, '', 'asset_value', '基金资产净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (653, '', 'paid_capital', '实收信托', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (654, '', 'asset_value', '基金资产净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (655, '', 'total_assets', '资产类合计::', NULL, 1, NULL, NULL, 'wr_user', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (656, '', 'paid_capital', '实收资本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (657, '', 'ten_sou_yield', '每万份收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (658, '', 'yield', '本日收益', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (659, '', 'acc_net', '累计单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (660, '', 'total_liabi', '负债类合计', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (661, '', 'avg_nav', '基金单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (662, '', 'avg_nav', '单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (663, '', 'acc_net', '905', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (701, '', 'deviation_amt', '偏离金额', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (702, '', 'deviation_amt', '偏离金额:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (703, '', 'deviation', '偏离度', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (704, '', 'deviation', '偏离度:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (705, '', 'avg_nav', '－－今日单位净值其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (706, '', 'avg_nav', '－－今日单位净值其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (707, '', 'paid_capital', '－－实收资本其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (708, '', 'paid_capital', '－－实收资本其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (709, '', 'acc_net', '－－累计单位净值其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (710, '', 'acc_net', '－－累计单位净值其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (711, '', 'asset_value', '－－资产净值其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (713, '', 'asset_value', '－－资产净值其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (715, '', 'seven_annu_yield', '基金七日收益率:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (716, '', 'yield', '基金本日收益:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (717, '', 'ten_sou_yield', '每万份基金收益:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (718, '', 'acc_net', '－－累计单位净值其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (719, '', 'acc_net', '－－累计单位净值其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (720, '', 'asset_value', '－－资产净值其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (721, '', 'asset_value', '－－资产净值其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (724, '', 'avg_nav', '－－今日单位净值其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (725, '', 'avg_nav', '－－今日单位净值其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (726, '', 'paid_capital', '－－实收资本其中[601549][鼎坤优势甄选2260A级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (727, '', 'paid_capital', '－－实收资本其中[601550][鼎坤优势甄选2260B级]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (728, '', 'acc_net', '－－累计单位净值其中[HSP649][华夏阳光海外债1号美元-美元现汇]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (729, '', 'acc_net', '－－累计单位净值其中[SSP649][华夏阳光海外债1号]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (730, '', 'asset_value', '－－资产净值其中[HSP649][华夏阳光海外债1号美元-美元现汇]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (731, '', 'asset_value', '－－资产净值其中[SSP649][华夏阳光海外债1号]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (734, '', 'avg_nav', '－－今日单位净值其中[HSP649][华夏阳光海外债1号美元-美元现汇]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (735, '', 'avg_nav', '－－今日单位净值其中[SSP649][华夏阳光海外债1号]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (736, '', 'paid_capital', '－－实收资本其中[HSP649][华夏阳光海外债1号美元-美元现汇]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (737, '', 'paid_capital', '－－实收资本其中[SSP649][华夏阳光海外债1号]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (741, '', 'asset_value_y', '资产净值(原币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (742, '', 'asset_value_y', '－－资产净值其中[SSP649][华夏阳光海外债1号](原币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (743, '', 'asset_value_y', '－－资产净值其中[HSP649][华夏阳光海外债1号美元-美元现汇](原币)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (744, '', 'avg_nav', '理论信托参考收益单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (745, '', 'acc_net', '－－累计单位净值其中[012492A][012492A]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (746, '', 'acc_net', '－－累计单位净值其中[012492B][012492B]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (747, '', 'avg_nav', '－－今日单位净值其中[012492A][012492A]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (748, '', 'avg_nav', '－－今日单位净值其中[012492B][012492B]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (749, '', 'paid_capital', '－－实收资本其中[012492A][012492A]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (750, '', 'paid_capital', '－－实收资本其中[012492A][012492A]', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (751, '', 'avg_nav', '份额单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (752, '', 'avg_nav', '本日单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (753, '', 'avg_nav', '本日单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (754, '', 'asset_value', '产品资产净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (756, '', 'paid_capital', '实收资本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (757, '', 'acc_net', '累计单位净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (758, '', 'asset_value', '701基金资产净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (759, '', 'paid_capital', '601实收资本', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (760, '', 'total_assets', '604资产类合计：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (761, '', 'total_liabi', '605负债类合计：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (762, '', 'avg_nav', '902今日单位净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (766, '', 'acc_net', '905累计单位净值：', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (767, '', 'asset_value', '净值(市值)', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (773, '', 'acc_net', '份额累计净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (774, '', 'acc_net', '份额累计净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (775, '', 'paid_capital', '测试累计净值', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (776, '', 'total_assets', '测试001', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (778, '', 'acc_net', '905', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (779, '', 'acc_net', '期初单位净值:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (780, '', 'yield', '本日收益:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (781, '', 'ten_sou_yield', '每万份收益:', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (782, '', 'seven_annu_yield', '七日年化收益率(%):', NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (2700, '', 'asset_value', '净值(按市值):', NULL, 1, 'houyongheng', NULL, 'houyongheng', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (2900, '', 'deviation', '偏离度：', NULL, 1, 'zhudaoming', NULL, 'zhudaoming', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (2901, '', 'deviation_amt', '偏离金额：', NULL, 1, 'zhudaoming', NULL, 'zhudaoming', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3100, '', 'project_cd', '科目项目|项目代码', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3101, '', 'project_nm', '科目项目|项目名称', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3200, '', 'n_hldcst_locl', '成本(本位币)|亿千百十万千百十元角分', NULL, 1, 'zhudaoming', NULL, 'zhudaoming', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3201, '', 'n_hldmkv_locl', '市值(本位币)|亿千百十万千百十元角分', NULL, 1, 'zhudaoming', NULL, 'zhudaoming', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3300, '', 'subject_cd', '科目编码', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3400, '', 'n_hldcst_locl', '成本|万千百十亿千百十万千百十元角分', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3500, '', 'n_hldmkv_locl', '持仓成本', NULL, 1, 'yuanlingling', NULL, 'yuanlingling', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3600, '', 'x', 'wind代码', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3702, '', 'x', '市值占总资产%', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3703, '', 'x', '成本占净值%', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3704, '', 'x', '成本占总资产%', NULL, 1, 'lihongmin', NULL, 'lihongmin', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3800, '', 'c_cury_code', '111222', NULL, 1, 'wangyancong', NULL, 'wangyancong', NULL);
INSERT INTO `t_file_parse_source` (`id`, `file_type`, `column_map`, `column_name`, `file_ext_info`, `status`, `creater`, `create_time`, `modifier`, `modify_time`) VALUES (3801, '', 'avg_nav', '1111', NULL, 1, 'wangyancong', NULL, 'wangyancong', NULL);

INSERT INTO t_file_parse_publish_log (
    id, profile_id, version, publish_status, publish_time, publisher, publish_comment, validation_result_json, rollback_from_version
) VALUES
(940000000000000001, 910000000000000001, 'v1', 'PUBLISHED', CURRENT_TIMESTAMP, 'system', '系统初始化默认模板', '{"publishable":true,"issues":[]}', NULL),
(940000000000000002, 910000000000000002, 'v1', 'PUBLISHED', CURRENT_TIMESTAMP, 'system', '系统初始化默认模板', '{"publishable":true,"issues":[]}', NULL);

--changeset codex:20260419-02-postgres-qlexpress-rule-engine-init dbms:postgresql
INSERT INTO t_file_parse_profile (
    id, profile_code, profile_name, version, file_scene, file_type_name, source_channel, status, priority,
    match_expr, header_expr, row_classify_expr, field_map_expr, transform_expr, required_headers_json, subject_code_pattern, trace_enabled, timeout_ms,
    checksum, creater, create_time, modifier, modify_time, published_time
) VALUES
(910000000000000001, 'default-valset-excel-v1', '默认估值模板-Excel', 'v1', 'VALSET', 'EXCEL', 'system', 'PUBLISHED', 1000,
 'containsAny(fileName, [''.xlsx'', ''.xls''])', 'isHeaderRow(row, requiredHeaders)', 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)',
 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))',
 'value', '["科目代码","科目名称"]', '^\\d{4}[A-Za-z0-9]*$', TRUE, 3000, 'seed-default-valset-excel-v1', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(910000000000000002, 'default-valset-csv-v1', '默认估值模板-CSV', 'v1', 'VALSET', 'CSV', 'system', 'PUBLISHED', 1000,
 'containsAny(fileName, [''.csv''])', 'isHeaderRow(row, requiredHeaders)', 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)',
 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))',
 'value', '["科目代码","科目名称"]', '^\\d{4}[A-Za-z0-9]*$', TRUE, 3000, 'seed-default-valset-csv-v1', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_file_parse_rule_step (
    id, profile_id, rule_type, step_name, priority, enabled, expr_text, expr_lang, input_schema_json,
    output_schema_json, error_policy, timeout_ms, creater, create_time, modifier, modify_time
) VALUES
(910000000000000101, 910000000000000001, 'HEADER_DETECT', '表头识别', 10, TRUE, 'isHeaderRow(row, requiredHeaders)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000102, 910000000000000001, 'ROW_CLASSIFY', '行分类', 20, TRUE, 'classifyRow(row, footerKeywords)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000103, 910000000000000001, 'FIELD_MAP', '字段映射', 30, TRUE, 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000104, 910000000000000001, 'VALUE_TRANSFORM', '值转换', 40, TRUE, 'value', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000201, 910000000000000002, 'HEADER_DETECT', '表头识别', 10, TRUE, 'isHeaderRow(row, requiredHeaders)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000202, 910000000000000002, 'ROW_CLASSIFY', '行分类', 20, TRUE, 'classifyRow(row, footerKeywords)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000203, 910000000000000002, 'FIELD_MAP', '字段映射', 30, TRUE, 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(910000000000000204, 910000000000000002, 'VALUE_TRANSFORM', '值转换', 40, TRUE, 'value', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

INSERT INTO t_file_parse_rule (
    id, creater, create_time, modifier, modify_time, file_scene, file_type_name, region_name, column_map,
    column_map_name, status, multi_index, required
) VALUES
(920000000000000001, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '科目代码', '科目代码', TRUE, FALSE, TRUE),
(920000000000000002, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '科目名称', '科目名称', TRUE, FALSE, TRUE),
(920000000000000003, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '市值', '市值', TRUE, FALSE, FALSE),
(920000000000000004, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '成本', '成本', TRUE, FALSE, FALSE),
(920000000000000005, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '数量', '数量', TRUE, FALSE, FALSE);

INSERT INTO t_file_parse_source (
    id, file_type, column_map, column_name, file_ext_info, status, creater, create_time, modifier, modify_time
) VALUES
(930000000000000001, 'EXCEL', '科目代码', '科目代码', NULL, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(930000000000000002, 'EXCEL', '科目名称', '科目名称', NULL, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(930000000000000003, 'EXCEL', '市值', '市值', NULL, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(930000000000000004, 'EXCEL', '成本', '成本', NULL, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP),
(930000000000000005, 'EXCEL', '数量', '数量', NULL, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP);

INSERT INTO t_file_parse_publish_log (
    id, profile_id, version, publish_status, publish_time, publisher, publish_comment, validation_result_json, rollback_from_version
) VALUES
(940000000000000001, 910000000000000001, 'v1', 'PUBLISHED', CURRENT_TIMESTAMP, 'system', '系统初始化默认模板', '{"publishable":true,"issues":[]}', NULL),
(940000000000000002, 910000000000000002, 'v1', 'PUBLISHED', CURRENT_TIMESTAMP, 'system', '系统初始化默认模板', '{"publishable":true,"issues":[]}', NULL);

--changeset codex:20260419-02-oracle-qlexpress-rule-engine-init dbms:oracle
INSERT INTO t_file_parse_profile (
    id, profile_code, profile_name, version, file_scene, file_type_name, source_channel, status, priority,
    match_expr, header_expr, row_classify_expr, field_map_expr, transform_expr, required_headers_json, subject_code_pattern, trace_enabled, timeout_ms,
    checksum, creater, create_time, modifier, modify_time, published_time
) VALUES
(910000000000000001, 'default-valset-excel-v1', '默认估值模板-Excel', 'v1', 'VALSET', 'EXCEL', 'system', 'PUBLISHED', 1000,
 'containsAny(fileName, [''.xlsx'', ''.xls''])', 'isHeaderRow(row, requiredHeaders)', 'classifyRow(row, footerKeywords)',
 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))',
 'value', '["科目代码","科目名称"]', '^\\d{4}[A-Za-z0-9]*$', 1, 3000, 'seed-default-valset-excel-v1', 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO t_file_parse_profile (
    id, profile_code, profile_name, version, file_scene, file_type_name, source_channel, status, priority,
    match_expr, header_expr, row_classify_expr, field_map_expr, transform_expr, required_headers_json, subject_code_pattern, trace_enabled, timeout_ms,
    checksum, creater, create_time, modifier, modify_time, published_time
) VALUES
(910000000000000002, 'default-valset-csv-v1', '默认估值模板-CSV', 'v1', 'VALSET', 'CSV', 'system', 'PUBLISHED', 1000,
 'containsAny(fileName, [''.csv''])', 'isHeaderRow(row, requiredHeaders)', 'classifyRow(row, footerKeywords)',
 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))',
 'value', '["科目代码","科目名称"]', '^\\d{4}[A-Za-z0-9]*$', 1, 3000, 'seed-default-valset-csv-v1', 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO t_file_parse_rule_step (
    id, profile_id, rule_type, step_name, priority, enabled, expr_text, expr_lang, input_schema_json,
    output_schema_json, error_policy, timeout_ms, creater, create_time, modifier, modify_time
) VALUES
(910000000000000101, 910000000000000001, 'HEADER_DETECT', '表头识别', 10, 1, 'isHeaderRow(row, requiredHeaders)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000102, 910000000000000001, 'ROW_CLASSIFY', '行分类', 20, 1, 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000103, 910000000000000001, 'FIELD_MAP', '字段映射', 30, 1, 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000104, 910000000000000001, 'VALUE_TRANSFORM', '值转换', 40, 1, 'value', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000201, 910000000000000002, 'HEADER_DETECT', '表头识别', 10, 1, 'isHeaderRow(row, requiredHeaders)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000202, 910000000000000002, 'ROW_CLASSIFY', '行分类', 20, 1, 'classifyRowWithPattern(row, footerKeywords, subjectCodePattern)', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000203, 910000000000000002, 'FIELD_MAP', '字段映射', 30, 1, 'exactCandidate != null ? ''exact_header'' : (segmentCandidate != null ? ''header_segment'' : (aliasCandidate != null ? ''alias_contains'' : ''fallback''))', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(910000000000000204, 910000000000000002, 'VALUE_TRANSFORM', '值转换', 40, 1, 'value', 'qlexpress4',
 NULL, NULL, 'FAIL_FAST', 3000, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP);

INSERT INTO t_file_parse_rule (
    id, creater, create_time, modifier, modify_time, file_scene, file_type_name, region_name, column_map,
    column_map_name, status, multi_index, required
) VALUES
(920000000000000001, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '科目代码', '科目代码', 1, 0, 1),
(920000000000000002, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '科目名称', '科目名称', 1, 0, 1),
(920000000000000003, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '市值', '市值', 1, 0, 0),
(920000000000000004, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '成本', '成本', 1, 0, 0),
(920000000000000005, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP, 'VALSET', 'EXCEL', 'DEFAULT', '数量', '数量', 1, 0, 0);

INSERT INTO t_file_parse_source (
    id, file_type, column_map, column_name, file_ext_info, status, creater, create_time, modifier, modify_time
) VALUES
(930000000000000001, 'EXCEL', '科目代码', '科目代码', NULL, 1, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(930000000000000002, 'EXCEL', '科目名称', '科目名称', NULL, 1, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(930000000000000003, 'EXCEL', '市值', '市值', NULL, 1, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(930000000000000004, 'EXCEL', '成本', '成本', NULL, 1, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP),
(930000000000000005, 'EXCEL', '数量', '数量', NULL, 1, 'system', SYSTIMESTAMP, 'system', SYSTIMESTAMP);

INSERT INTO t_file_parse_publish_log (
    id, profile_id, version, publish_status, publish_time, publisher, publish_comment, validation_result_json, rollback_from_version
) VALUES
(940000000000000001, 910000000000000001, 'v1', 'PUBLISHED', SYSTIMESTAMP, 'system', '系统初始化默认模板', '{"publishable":true,"issues":[]}', NULL),
(940000000000000002, 910000000000000002, 'v1', 'PUBLISHED', SYSTIMESTAMP, 'system', '系统初始化默认模板', '{"publishable":true,"issues":[]}', NULL);
