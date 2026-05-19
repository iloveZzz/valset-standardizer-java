-- Initial data for t_qlexpress_function
-- These rows catalog current Java built-in QLExpress functions and are disabled by default.

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000001, '行包含全部关键字', 'rowContainsAll', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function rowContainsAll(row, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'rowContainsAll');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000002, '行包含任一关键字', 'rowContainsAny', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function rowContainsAny(row, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'rowContainsAny');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000003, '文本包含任一关键字', 'containsAny', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function containsAny(source, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'containsAny');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000004, '文本包含全部关键字', 'containsAll', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function containsAll(source, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'containsAll');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000005, '文本非空判断', 'hasText', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function hasText(value) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse","transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'hasText');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000006, '行关键字命中数', 'rowHitCount', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function rowHitCount(row, keywords) { return 0; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'rowHitCount');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000007, '表头行判断', 'isHeaderRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isHeaderRow(row, requiredHeaders) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isHeaderRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000008, '数据起始行判断', 'isDataStartRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isDataStartRow(row) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isDataStartRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000009, '数据起始行判断(正则)', 'isDataStartRowWithPattern', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isDataStartRowWithPattern(row, pattern) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isDataStartRowWithPattern');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000010, '科目行判断', 'isSubjectRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isSubjectRow(row) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isSubjectRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000011, '科目行判断(正则)', 'isSubjectRowWithPattern', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isSubjectRowWithPattern(row, pattern) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isSubjectRowWithPattern');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000012, '指标候选行判断', 'isMetricCandidate', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isMetricCandidate(row) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isMetricCandidate');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000013, '指标候选行判断(正则)', 'isMetricCandidateWithPattern', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isMetricCandidateWithPattern(row, pattern) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isMetricCandidateWithPattern');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000014, '指标数据行判断', 'isMetricDataRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isMetricDataRow(row) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isMetricDataRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000015, '指标数据行判断(正则)', 'isMetricDataRowWithPattern', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isMetricDataRowWithPattern(row, pattern) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isMetricDataRowWithPattern');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000016, '指标行判断', 'isMetricRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isMetricRow(row) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isMetricRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000017, '指标行判断(正则)', 'isMetricRowWithPattern', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isMetricRowWithPattern(row, pattern) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isMetricRowWithPattern');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000018, '页脚行判断', 'isFooterRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isFooterRow(row, footerKeywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isFooterRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000019, '行分类', 'classifyRow', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function classifyRow(row, footerKeywords) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'classifyRow');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000020, '行分类(正则)', 'classifyRowWithPattern', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function classifyRowWithPattern(row, footerKeywords, pattern) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'classifyRowWithPattern');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000021, '首个有效文本包含任一关键字', 'firstMeaningfulTextContainsAny', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function firstMeaningfulTextContainsAny(row, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'firstMeaningfulTextContainsAny');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000022, '首个有效文本包含全部关键字', 'firstMeaningfulTextContainsAll', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function firstMeaningfulTextContainsAll(row, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'firstMeaningfulTextContainsAll');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000023, '非空单元格数量判断', 'hasAtLeastNonBlank', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function hasAtLeastNonBlank(row, minCount) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'hasAtLeastNonBlank');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000024, '指定列文本', 'textAt', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function textAt(row, index) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'textAt');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000025, '指定列值', 'valueAt', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function valueAt(row, index) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'valueAt');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000026, '行非空数量', 'rowNonBlankCount', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function rowNonBlankCount(row) { return 0; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'rowNonBlankCount');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000027, '首个有效文本', 'firstMeaningfulText', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function firstMeaningfulText(row) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'firstMeaningfulText');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000028, '新建Map', 'newMap', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function newMap() { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'newMap');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000029, '构造Map', 'mapOf', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function mapOf() { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'mapOf');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000030, 'Map写入', 'put', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function put(map, key, value) { return map; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.parse"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'put');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000031, '候选值存在判断', 'hasCandidate', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function hasCandidate(candidate) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.headerMapping"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'hasCandidate');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000032, '表头包含任一分段', 'headerContainsAnySegment', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function headerContainsAnySegment(headerText, segments) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.headerMapping"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'headerContainsAnySegment');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000033, '表头包含全部分段', 'headerContainsAllSegments', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function headerContainsAllSegments(headerText, segments) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["extract.headerMapping"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'headerContainsAllSegments');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000034, '忽略大小写包含', 'containsIgnoreCase', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function containsIgnoreCase(source, keyword) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'containsIgnoreCase');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000035, '正则匹配', 'matchesRegex', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function matchesRegex(source, regex) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'matchesRegex');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000036, '首个产品匹配规则', 'firstProductMatchRule', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function firstProductMatchRule(fileName, productMatchRules) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'firstProductMatchRule');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000037, '产品规则字段取值', 'productRuleValue', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function productRuleValue(rule, fieldName) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'productRuleValue');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000038, '产品匹配结果', 'productMatchResult', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function productMatchResult(rule, fileName) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'productMatchResult');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000039, '任一正则匹配', 'matchesAnyRegex', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function matchesAnyRegex(source, regexList) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'matchesAnyRegex');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000040, 'Excel文件判断', 'isExcel', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isExcel(fileName) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isExcel');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000041, 'Excel文件判断别名', 'isExcelFile', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isExcelFile(fileName) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isExcelFile');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000042, 'CSV文件判断', 'isCsv', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isCsv(fileName) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isCsv');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000043, 'CSV文件判断别名', 'isCsvFile', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isCsvFile(fileName) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isCsvFile');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000044, '读取Excel数据', 'readExcelData', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function readExcelData(source) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'readExcelData');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000045, '读取CSV数据', 'readCsvData', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function readCsvData(source) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'readCsvData');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000046, '读取Excel前N行', 'readExcelDataWithin', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function readExcelDataWithin(source, maxRows) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'readExcelDataWithin');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000047, '读取CSV前N行', 'readCsvDataWithin', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function readCsvDataWithin(source, maxRows) { return null; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'readCsvDataWithin');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000048, '前N行查找表头行', 'findHeaderRowIndexWithin', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function findHeaderRowIndexWithin(source, maxRows, keyword1, keyword2) { return -1; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'findHeaderRowIndexWithin');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000049, '前N行表头关键词判断', 'hasHeaderKeywordsWithinFirstRows', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function hasHeaderKeywordsWithinFirstRows(source, maxRows, keyword1, keyword2) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'hasHeaderKeywordsWithinFirstRows');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000050, '前N行表头关键词集合判断', 'hasHeaderKeywordsWithinFirstRowsByList', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'hasHeaderKeywordsWithinFirstRowsByList');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000051, '估值表判断', 'isValuationTable', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isValuationTable(source, maxRows) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isValuationTable');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000052, '关键词估值表判断', 'isValuationTableByKeywords', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isValuationTableByKeywords(source, maxRows, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isValuationTableByKeywords');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000053, '扩展配置估值表判断', 'isValuationTableByMeta', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function isValuationTableByMeta(source, meta) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'isValuationTableByMeta');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000054, '文本命中任一关键词', 'containsAnyText', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function containsAnyText(source, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'containsAnyText');

INSERT INTO t_qlexpress_function (function_id, function_cn_name, function_name, remark, script_body, enabled, ext_info_json, created_at, updated_at)
SELECT 920000000000000055, '文本命中全部关键词', 'containsAllText', 'Java内置函数清单记录，默认停用；实际逻辑由代码注册，不建议启用同名脚本函数。', 'function containsAllText(source, keywords) { return false; }', 0, '{"sourceType":"JAVA_BUILTIN","readOnly":true,"defaultEnabled":false,"enableAdvice":"Java Runner 已内置注册，同名启用会造成重复注册；此记录用于页面清单展示。","sourceModules":["transfer.rule"]}', '2026-05-19 00:00:00', '2026-05-19 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM t_qlexpress_function WHERE function_name = 'containsAllText');
