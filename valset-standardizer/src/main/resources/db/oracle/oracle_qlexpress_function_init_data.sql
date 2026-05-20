-- ----------------------------
-- Initial data for t_qlexpress_function
-- ----------------------------
-- 说明：
-- 1. 本脚本整理当前工程公开 QLExpress 函数，T_QLEXPRESS_FUNCTION 是公开函数注册源。
-- 2. extract.parse 解析函数由 QLExpress4 纯脚本实现，不再委托 Java 解析门面。
-- 3. sourceModules 用于运行时按 Runner 场景加载函数：common / extract.parse / extract.headerMapping / transfer.rule。

DECLARE
  v_now DATE := TO_DATE('2026-05-19 00:00:00', 'SYYYY-MM-DD HH24:MI:SS');

  FUNCTION function_remark(
    p_function_name IN VARCHAR2
  ) RETURN VARCHAR2 IS
  BEGIN
    CASE p_function_name
      WHEN 'rowContainsAll' THEN RETURN '使用场景：估值表解析时判断一行是否同时包含必需表头或业务关键字。上下文变量要求：row 为当前行单元格 List，keywords 为待匹配关键字 List。';
      WHEN 'rowContainsAny' THEN RETURN '使用场景：估值表解析时判断一行是否命中任一候选关键字。上下文变量要求：row 为当前行单元格 List，keywords 为待匹配关键字 List。';
      WHEN 'containsAny' THEN RETURN '使用场景：解析脚本内判断单个文本是否命中任一关键字。上下文变量要求：source 为文本或可转文本值，keywords 为关键字 List。';
      WHEN 'containsAll' THEN RETURN '使用场景：解析脚本内判断单个文本是否同时命中全部关键字。上下文变量要求：source 为文本或可转文本值，keywords 为关键字 List。';
      WHEN 'hasText' THEN RETURN '使用场景：通用脚本判断上下文值是否存在有效文本，常用于规则前置校验。上下文变量要求：value 可为任意值，函数内部按字符串判空。';
      WHEN 'rowHitCount' THEN RETURN '使用场景：估值表解析时统计当前行命中的关键字数量，支撑表头识别。上下文变量要求：row 为当前行单元格 List，keywords 为关键字 List。';
      WHEN 'isHeaderRow' THEN RETURN '使用场景：估值表解析时识别表头行。上下文变量要求：row 为当前行单元格 List，requiredHeaders 为必须出现的表头关键字 List。';
      WHEN 'isDataStartRow' THEN RETURN '使用场景：估值表解析时识别数据区开始行，兼容科目行和指标候选行。上下文变量要求：row 为当前行单元格 List。';
      WHEN 'isDataStartRowWithPattern' THEN RETURN '使用场景：估值表解析时按科目代码模式识别数据区开始行。上下文变量要求：row 为当前行单元格 List，pattern 为科目代码模式参数，当前脚本按兼容参数保留。';
      WHEN 'isSubjectRow' THEN RETURN '使用场景：估值表解析时识别科目行。上下文变量要求：row 为当前行单元格 List，默认不传科目代码模式。';
      WHEN 'isSubjectRowWithPattern' THEN RETURN '使用场景：估值表解析时按可选模式识别科目代码及其后续科目名称。上下文变量要求：row 为当前行单元格 List，pattern 为科目代码模式参数。';
      WHEN 'isMetricCandidate' THEN RETURN '使用场景：估值表解析时识别指标候选行。上下文变量要求：row 为当前行单元格 List。';
      WHEN 'isMetricCandidateWithPattern' THEN RETURN '使用场景：估值表解析时按可选科目代码模式识别指标候选行。上下文变量要求：row 为当前行单元格 List，pattern 为科目代码模式参数。';
      WHEN 'isMetricDataRow' THEN RETURN '使用场景：估值表解析时识别两列型指标数据行。上下文变量要求：row 为当前行单元格 List。';
      WHEN 'isMetricDataRowWithPattern' THEN RETURN '使用场景：估值表解析时按可选科目代码模式识别两列型指标数据行。上下文变量要求：row 为当前行单元格 List，pattern 为科目代码模式参数。';
      WHEN 'isMetricRow' THEN RETURN '使用场景：估值表解析时识别多列型指标行。上下文变量要求：row 为当前行单元格 List。';
      WHEN 'isMetricRowWithPattern' THEN RETURN '使用场景：估值表解析时按可选科目代码模式识别多列型指标行。上下文变量要求：row 为当前行单元格 List，pattern 为科目代码模式参数。';
      WHEN 'isFooterRow' THEN RETURN '使用场景：估值表解析时识别制表、复核、经办等无效行。上下文变量要求：row 为当前行单元格 List，footerKeywords 为可选页脚关键字 List。';
      WHEN 'classifyRow' THEN RETURN '使用场景：估值表解析时将当前行归类为科目、指标、页脚或忽略。上下文变量要求：row 为当前行单元格 List，footerKeywords 为可选页脚关键字 List。';
      WHEN 'classifyRowWithPattern' THEN RETURN '使用场景：估值表解析时按科目代码模式完成行分类。上下文变量要求：row 为当前行单元格 List，footerKeywords 为页脚关键字 List，pattern 为科目代码模式参数。';
      WHEN 'firstMeaningfulTextContainsAny' THEN RETURN '使用场景：解析脚本内判断一行首个有效单元格是否命中任一关键字。上下文变量要求：row 为当前行单元格 List，keywords 为关键字 List。';
      WHEN 'firstMeaningfulTextContainsAll' THEN RETURN '使用场景：解析脚本内判断一行首个有效单元格是否命中全部关键字。上下文变量要求：row 为当前行单元格 List，keywords 为关键字 List。';
      WHEN 'hasAtLeastNonBlank' THEN RETURN '使用场景：解析脚本内判断当前行有效单元格数量是否达到阈值。上下文变量要求：row 为当前行单元格 List，minCount 为最小非空数量。';
      WHEN 'textAt' THEN RETURN '使用场景：解析脚本内安全读取指定列文本。上下文变量要求：row 为当前行单元格 List，index 为从 0 开始的列序号。';
      WHEN 'valueAt' THEN RETURN '使用场景：解析脚本内安全读取指定列原始值。上下文变量要求：row 为当前行单元格 List，index 为从 0 开始的列序号。';
      WHEN 'rowNonBlankCount' THEN RETURN '使用场景：解析脚本内统计当前行有效单元格数量。上下文变量要求：row 为当前行单元格 List，空值和 - 不计入有效值。';
      WHEN 'firstMeaningfulText' THEN RETURN '使用场景：解析脚本内提取当前行首个有效文本，用于页脚和关键字判断。上下文变量要求：row 为当前行单元格 List。';
      WHEN 'containsChineseText' THEN RETURN '使用场景：解析脚本内判断单元格文本是否包含中文。上下文变量要求：value 为待判断值。';
      WHEN 'isNumericText' THEN RETURN '使用场景：解析脚本内判断单元格文本是否为数字，支持负数、千分位、小数和百分号。上下文变量要求：value 为待判断值。';
      WHEN 'isRightNextNumeric' THEN RETURN '使用场景：估值表解析时判断指定列紧邻右侧单元格是否为数字。上下文变量要求：row 为当前行单元格 List，startIndex 为锚点列序号。';
      WHEN 'rightNumericCount' THEN RETURN '使用场景：估值表解析时统计指定列右侧数字单元格数量。上下文变量要求：row 为当前行单元格 List，startIndex 为锚点列序号。';
      WHEN 'newMap' THEN RETURN '使用场景：解析脚本内兼容旧规则构造结果 Map。上下文变量要求：按 k1/v1 到 k10/v10 传入键值对；新规则优先使用 QLExpress Map 字面量。';
      WHEN 'mapOf' THEN RETURN '使用场景：解析脚本内按键值对构造结果 Map。上下文变量要求：按 k1/v1 到 k10/v10 传入键值对，key 为空的项会被跳过。';
      WHEN 'put' THEN RETURN '使用场景：解析脚本内向结果 Map 写入字段。上下文变量要求：map 为可为空的 Map，key 为字段名，value 为字段值。';
      WHEN 'matchesKeyword' THEN RETURN '使用场景：解析脚本内执行宽松关键字匹配，支持相等和互相包含。上下文变量要求：source 为待匹配文本或单元格值，keyword 为关键字。';
      WHEN 'isSubjectCodeText' THEN RETURN '使用场景：估值表解析时判断文本是否像科目代码。上下文变量要求：value 为待判断文本，pattern 为可选科目代码正则；未传 pattern 时使用本函数脚本内配置的默认规则。';
      WHEN 'hasSubjectNameAfterCode' THEN RETURN '使用场景：估值表解析时判断科目代码后是否存在科目名称。上下文变量要求：row 为当前行单元格 List，codeIndex 为科目代码所在列序号。';
      WHEN 'isSubjectDetailRowByColumn' THEN RETURN '使用场景：估值表解析时按已定位的科目代码列识别科目明细行。上下文变量要求：row 为当前行单元格 List，subjectCodeColumnIndex 为科目代码列序号，pattern 为可选科目代码正则。';
      WHEN 'isMetricDataRowByColumn' THEN RETURN '使用场景：估值表解析时按已定位的科目代码列识别指标数据行。上下文变量要求：科目代码列文本包含中文，且紧邻右侧单元格为数字。';
      WHEN 'isMetricRowByColumn' THEN RETURN '使用场景：估值表解析时按已定位的科目代码列识别指标行。上下文变量要求：科目代码列文本包含中文，紧邻右侧不是数字，但右侧任一单元格包含数字。';
      WHEN 'isMetricDetailRowByColumn' THEN RETURN '使用场景：估值表解析时按已定位的科目代码列识别指标明细行。上下文变量要求：科目代码列文本包含中文，且右侧至少存在一个数字单元格。';
      WHEN 'isValuationDataRowByColumn' THEN RETURN '使用场景：估值表解析时按已定位的科目代码列识别估值数据区明细行。上下文变量要求：row 为当前行单元格 List，subjectCodeColumnIndex 为科目代码列序号，pattern 为可选科目代码正则。';
      WHEN 'hasCandidate' THEN RETURN '使用场景：表头映射时判断候选映射值是否存在。上下文变量要求：candidate 为候选值；运行范围 extract.headerMapping 需注入 qlHeaderFns。';
      WHEN 'headerContainsAnySegment' THEN RETURN '使用场景：表头映射时判断表头文本是否包含任一分段。上下文变量要求：headerText 为表头文本，segments 为分段 List；需注入 qlHeaderFns。';
      WHEN 'headerContainsAllSegments' THEN RETURN '使用场景：表头映射时判断表头文本是否包含全部分段。上下文变量要求：headerText 为表头文本，segments 为分段 List；需注入 qlHeaderFns。';
      WHEN 'transferText' THEN RETURN '使用场景：文件分拣、标签识别脚本内统一把值转为文本。上下文变量要求：value 可为任意值，空值返回空字符串。';
      WHEN 'textMatches' THEN RETURN '使用场景：文件分拣、标签识别脚本内执行文本、关键字或 LIKE 兼容匹配。上下文变量要求：source 为待匹配文本，keyword 为关键字或 LIKE 模式。';
      WHEN 'containsIgnoreCase' THEN RETURN '使用场景：文件分拣规则兼容旧的忽略大小写包含函数名。上下文变量要求：source 为待匹配文本，keyword 为关键字；当前复用 textMatches。';
      WHEN 'regexMatches' THEN RETURN '使用场景：通用脚本正则匹配函数。上下文变量要求：source 为待匹配文本，regex 为正则表达式。';
      WHEN 'matchesRegex' THEN RETURN '使用场景：文件分拣规则兼容旧正则匹配函数名。上下文变量要求：source 为待匹配文本，regex 为规则文本或 LIKE 模式；当前复用 textMatches。';
      WHEN 'matchesAnyRegex' THEN RETURN '使用场景：文件分拣规则判断任一规则文本是否命中文件信息。上下文变量要求：source 为待匹配文本，regexList 为规则文本 List。';
      WHEN 'containsAnyText' THEN RETURN '使用场景：文件分拣、标签识别脚本内判断文本是否命中任一关键字。上下文变量要求：source 为待匹配文本，keywords 为关键字 List。';
      WHEN 'containsAllText' THEN RETURN '使用场景：文件分拣、标签识别脚本内判断文本是否命中全部关键字。上下文变量要求：source 为待匹配文本，keywords 为关键字 List。';
      WHEN 'isExcel' THEN RETURN '使用场景：文件分拣、标签识别脚本内判断文件名是否为 Excel。上下文变量要求：fileName 为文件名或路径文本。';
      WHEN 'isExcelFile' THEN RETURN '使用场景：文件分拣、标签识别脚本内兼容 Excel 文件判断别名。上下文变量要求：fileName 为文件名或路径文本。';
      WHEN 'isCsv' THEN RETURN '使用场景：文件分拣、标签识别脚本内判断文件名是否为 CSV。上下文变量要求：fileName 为文件名或路径文本。';
      WHEN 'isCsvFile' THEN RETURN '使用场景：文件分拣、标签识别脚本内兼容 CSV 文件判断别名。上下文变量要求：fileName 为文件名或路径文本。';
      WHEN 'readExcelData' THEN RETURN '使用场景：标签识别脚本兼容旧的 Excel 数据读取函数名。上下文变量要求：运行上下文需由 Java 预置 previewRows；source 参数仅作兼容占位。';
      WHEN 'readCsvData' THEN RETURN '使用场景：标签识别脚本兼容旧的 CSV 数据读取函数名。上下文变量要求：运行上下文需由 Java 预置 previewRows；source 参数仅作兼容占位。';
      WHEN 'readExcelDataWithin' THEN RETURN '使用场景：标签识别脚本读取 Excel 前 N 行预览数据。上下文变量要求：运行上下文需由 Java 预置 previewRows；source 和 maxRows 仅表达调用意图。';
      WHEN 'readCsvDataWithin' THEN RETURN '使用场景：标签识别脚本读取 CSV 前 N 行预览数据。上下文变量要求：运行上下文需由 Java 预置 previewRows；source 和 maxRows 仅表达调用意图。';
      WHEN 'rowText' THEN RETURN '使用场景：标签识别脚本内将预览行拼接成可匹配文本。上下文变量要求：row 为 previewRows 中的一行 List。';
      WHEN 'rowContainsAllText' THEN RETURN '使用场景：标签识别脚本内判断预览行是否包含全部表头关键字。上下文变量要求：row 为预览行 List，keywords 为关键字 List。';
      WHEN 'findHeaderRowIndexInRows' THEN RETURN '使用场景：标签识别脚本内从预览数据中定位表头行。上下文变量要求：rows 为预览行 List，keywords 为必须命中的表头关键字 List。';
      WHEN 'findHeaderRowIndexWithin' THEN RETURN '使用场景：标签识别脚本内按两个关键字定位前 N 行表头。上下文变量要求：运行上下文需预置 previewRows；source、maxRows 为兼容参数，keyword1/keyword2 为表头关键字。';
      WHEN 'hasHeaderKeywordsWithinFirstRows' THEN RETURN '使用场景：标签识别脚本内判断前 N 行是否存在指定两个表头关键字。上下文变量要求：运行上下文需预置 previewRows，keyword1/keyword2 为表头关键字。';
      WHEN 'hasHeaderKeywordsWithinFirstRowsByList' THEN RETURN '使用场景：标签识别脚本内按关键字集合判断预览数据是否包含目标表头。上下文变量要求：运行上下文需预置 previewRows，keywords 为表头关键字 List。';
      WHEN 'isValuationTable' THEN RETURN '使用场景：标签识别脚本内按默认“科目代码、科目名称”判断估值表。上下文变量要求：运行上下文需预置 previewRows，source 为文件标识，maxRows 为扫描行数意图。';
      WHEN 'isValuationTableByKeywords' THEN RETURN '使用场景：标签识别脚本内按自定义表头关键字判断估值表。上下文变量要求：运行上下文需预置 previewRows，keywords 为表头关键字 List。';
      WHEN 'headerKeywordsFromMeta' THEN RETURN '使用场景：标签识别脚本内从标签扩展配置提取表头关键字。上下文变量要求：meta 为 Map，可包含 headerKeywords；缺省返回科目代码和科目名称。';
      WHEN 'isValuationTableByMeta' THEN RETURN '使用场景：标签识别脚本内按标签扩展配置判断估值表。上下文变量要求：运行上下文需预置 previewRows，meta 为 Map，可包含 scanLimit、headerKeywords。';
      WHEN 'productRuleValue' THEN RETURN '使用场景：产品匹配标签脚本内读取产品规则字段。上下文变量要求：rule 为产品规则 Map，fieldName 为字段名。';
      WHEN 'productRuleMatches' THEN RETURN '使用场景：产品匹配标签脚本内判断文件名是否命中单条产品规则。上下文变量要求：fileName 为文件名，rule 为产品规则 Map，可包含 matchKeywords、matchRules。';
      WHEN 'firstProductMatchRule' THEN RETURN '使用场景：产品匹配标签脚本内查找首条命中的产品规则。上下文变量要求：fileName 为文件名，productMatchRules 为已启用产品规则 Map 列表。';
      WHEN 'productMatchResult' THEN RETURN '使用场景：产品匹配标签脚本内把命中的产品规则转换为动态标签结果。上下文变量要求：rule 为命中规则 Map，fileName 为文件名；返回 Map 供标签持久化。';
      ELSE RETURN '使用场景：系统内置公开函数，函数名和启停由数据库配置。上下文变量要求：请按函数参数传入对应脚本上下文值。';
    END CASE;
  END;

  PROCEDURE seed_function(
    p_function_id IN NUMBER,
    p_function_cn_name IN VARCHAR2,
    p_function_name IN VARCHAR2,
    p_script_body IN CLOB,
    p_source_modules IN CLOB
  ) IS
    v_remark VARCHAR2(1024);
  BEGIN
    v_remark := function_remark(p_function_name);

    MERGE INTO "T_QLEXPRESS_FUNCTION" target
    USING (SELECT p_function_name AS function_name FROM DUAL) source
    ON (target."FUNCTION_NAME" = source.function_name)
    WHEN MATCHED THEN UPDATE SET
      target."FUNCTION_CN_NAME" = p_function_cn_name,
      target."REMARK" = v_remark,
      target."SCRIPT_BODY" = p_script_body,
      target."ENABLED" = 1,
      target."EXT_INFO_JSON" = TO_CLOB('{"sourceType":"SYSTEM_SEED","readOnly":true,"sourceModules":') || p_source_modules || TO_CLOB('}'),
      target."UPDATED_AT" = v_now
    WHEN NOT MATCHED THEN INSERT (
      "FUNCTION_ID", "FUNCTION_CN_NAME", "FUNCTION_NAME", "REMARK", "SCRIPT_BODY", "ENABLED", "EXT_INFO_JSON", "CREATED_AT", "UPDATED_AT"
    ) VALUES (
      p_function_id, p_function_cn_name, p_function_name, v_remark, p_script_body, 1,
      TO_CLOB('{"sourceType":"SYSTEM_SEED","readOnly":true,"sourceModules":') || p_source_modules || TO_CLOB('}'),
      v_now, v_now
    );
  END;
BEGIN
  seed_function(
    920000000000000001,
    '行包含全部关键字',
    'rowContainsAll',
    TO_CLOB(q'[
function rowContainsAll(row, keywords) {
  if (keywords == null || keywords.length == 0) {
    return false;
  };
  return rowHitCount(row, keywords) >= keywords.length;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000002,
    '行包含任一关键字',
    'rowContainsAny',
    TO_CLOB(q'[
function rowContainsAny(row, keywords) {
  if (row == null || keywords == null) {
    return false;
  };
  for (keyword : keywords) {
    if (keyword == null) {
      continue;
    };
    kw = "" + keyword;
    if (kw == "") {
      continue;
    };
    for (cell : row) {
      if (matchesKeyword(cell, kw)) {
        return true;
      };
    };
  };
  return false;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000003,
    '文本包含任一关键字',
    'containsAny',
    TO_CLOB(q'[
function containsAny(source, keywords) {
  if (source == null || keywords == null) {
    return false;
  };
  text = "" + source;
  if (text == "") {
    return false;
  };
  for (keyword : keywords) {
    if (matchesKeyword(text, keyword)) {
      return true;
    };
  };
  return false;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000004,
    '文本包含全部关键字',
    'containsAll',
    TO_CLOB(q'[
function containsAll(source, keywords) {
  if (source == null || keywords == null || keywords.length == 0) {
    return false;
  };
  text = "" + source;
  if (text == "") {
    return false;
  };
  for (keyword : keywords) {
    if (!matchesKeyword(text, keyword)) {
      return false;
    };
  };
  return true;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000005,
    '文本非空判断',
    'hasText',
    TO_CLOB(q'[
function hasText(value) {
  return value != null && ("" + value) != "";
}
]'),
    TO_CLOB('["common"]')
  );
  seed_function(
    920000000000000066,
    '正则匹配',
    'regexMatches',
    TO_CLOB(q'[
function regexMatches(source, regex) {
  return qlCommonFns["matchesRegex"](source, regex);
}
]'),
    TO_CLOB('["common"]')
  );
  seed_function(
    920000000000000006,
    '行关键字命中数',
    'rowHitCount',
    TO_CLOB(q'[
function rowHitCount(row, keywords) {
  if (row == null || keywords == null) {
    return 0;
  };
  hitCount = 0;
  for (keyword : keywords) {
    if (keyword == null) {
      continue;
    };
    kw = "" + keyword;
    if (kw == "") {
      continue;
    };
    matched = false;
    for (cell : row) {
      if (matchesKeyword(cell, kw)) {
        matched = true;
        break;
      };
    };
    if (matched) {
      hitCount = hitCount + 1;
    };
  };
  return hitCount;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000007,
    '表头行判断',
    'isHeaderRow',
    TO_CLOB(q'[
function isHeaderRow(row, requiredHeaders) {
  return rowContainsAll(row, requiredHeaders);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000008,
    '数据起始行判断',
    'isDataStartRow',
    TO_CLOB(q'[
function isDataStartRow(row) {
  return isDataStartRowWithPattern(row, null);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000009,
    '数据起始行判断(正则)',
    'isDataStartRowWithPattern',
    TO_CLOB(q'[
function isDataStartRowWithPattern(row, pattern) {
  return isSubjectRowWithPattern(row, pattern) || isMetricCandidateWithPattern(row, pattern);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000010,
    '科目行判断',
    'isSubjectRow',
    TO_CLOB(q'[
function isSubjectRow(row) {
  return isSubjectRowWithPattern(row, null);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000011,
    '科目行判断(正则)',
    'isSubjectRowWithPattern',
    TO_CLOB(q'[
function isSubjectRowWithPattern(row, pattern) {
  if (row == null) {
    return false;
  };
  for (i = 0; i < row.length; i++) {
    if (isSubjectCodeText(row[i], pattern)) {
      return hasSubjectNameAfterCode(row, i);
    };
  };
  return false;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000012,
    '指标候选行判断',
    'isMetricCandidate',
    TO_CLOB(q'[
function isMetricCandidate(row) {
  return isMetricCandidateWithPattern(row, null);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000013,
    '指标候选行判断(正则)',
    'isMetricCandidateWithPattern',
    TO_CLOB(q'[
function isMetricCandidateWithPattern(row, pattern) {
  return isMetricDataRowWithPattern(row, pattern) || isMetricRowWithPattern(row, pattern);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000014,
    '指标数据行判断',
    'isMetricDataRow',
    TO_CLOB(q'[
function isMetricDataRow(row) {
  return isMetricDataRowWithPattern(row, null);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000015,
    '指标数据行判断(正则)',
    'isMetricDataRowWithPattern',
    TO_CLOB(q'[
function isMetricDataRowWithPattern(row, pattern) {
  if (row == null || row.length < 2) {
    return false;
  };
  firstCell = textAt(row, 0);
  if (firstCell == "" || isSubjectCodeText(firstCell, pattern)) {
    return false;
  };
  if (textAt(row, 1) == "") {
    return false;
  };
  for (i = 2; i < row.length; i++) {
    if (textAt(row, i) != "") {
      return false;
    };
  };
  return true;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000016,
    '指标行判断',
    'isMetricRow',
    TO_CLOB(q'[
function isMetricRow(row) {
  return isMetricRowWithPattern(row, null);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000017,
    '指标行判断(正则)',
    'isMetricRowWithPattern',
    TO_CLOB(q'[
function isMetricRowWithPattern(row, pattern) {
  if (row == null || row.length < 2) {
    return false;
  };
  firstCell = textAt(row, 0);
  if (firstCell == "" || isSubjectCodeText(firstCell, pattern)) {
    return false;
  };
  filledCount = 0;
  for (i = 1; i < row.length; i++) {
    if (textAt(row, i) != "") {
      filledCount = filledCount + 1;
    };
  };
  return filledCount >= 2;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000018,
    '页脚行判断',
    'isFooterRow',
    TO_CLOB(q'[
function isFooterRow(row, footerKeywords) {
  keywords = footerKeywords;
  if (keywords == null || keywords.length == 0) {
    keywords = ["制表", "复核", "经办"];
  };
  return rowContainsAny(row, keywords);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000019,
    '行分类',
    'classifyRow',
    TO_CLOB(q'[
function classifyRow(row, footerKeywords) {
  return classifyRowWithPattern(row, footerKeywords, null);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000020,
    '行分类(正则)',
    'classifyRowWithPattern',
    TO_CLOB(q'[
function classifyRowWithPattern(row, footerKeywords, pattern) {
  if (isSubjectRowWithPattern(row, pattern)) {
    return "SUBJECT";
  };
  if (isMetricDataRowWithPattern(row, pattern)) {
    return "METRIC_DATA";
  };
  if (isMetricRowWithPattern(row, pattern)) {
    return "METRIC_ROW";
  };
  if (isFooterRow(row, footerKeywords)) {
    return "FOOTER";
  };
  return "IGNORE";
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000021,
    '首个有效文本包含任一关键字',
    'firstMeaningfulTextContainsAny',
    TO_CLOB(q'[
function firstMeaningfulTextContainsAny(row, keywords) {
  return containsAny(firstMeaningfulText(row), keywords);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000022,
    '首个有效文本包含全部关键字',
    'firstMeaningfulTextContainsAll',
    TO_CLOB(q'[
function firstMeaningfulTextContainsAll(row, keywords) {
  return containsAll(firstMeaningfulText(row), keywords);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000023,
    '非空单元格数量判断',
    'hasAtLeastNonBlank',
    TO_CLOB(q'[
function hasAtLeastNonBlank(row, minCount) {
  return minCount != null && minCount > 0 && rowNonBlankCount(row) >= minCount;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000024,
    '指定列文本',
    'textAt',
    TO_CLOB(q'[
function textAt(row, index) {
  if (row == null || index == null || index < 0 || index >= row.length || row[index] == null) {
    return "";
  };
  return "" + row[index];
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000025,
    '指定列值',
    'valueAt',
    TO_CLOB(q'[
function valueAt(row, index) {
  if (row == null || index == null || index < 0 || index >= row.length) {
    return null;
  };
  return row[index];
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000026,
    '行非空数量',
    'rowNonBlankCount',
    TO_CLOB(q'[
function rowNonBlankCount(row) {
  if (row == null) {
    return 0;
  };
  count = 0;
  for (cell : row) {
    text = cell == null ? "" : "" + cell;
    if (text != "" && text != "-") {
      count = count + 1;
    };
  };
  return count;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000027,
    '首个有效文本',
    'firstMeaningfulText',
    TO_CLOB(q'[
function firstMeaningfulText(row) {
  if (row == null) {
    return "";
  };
  for (cell : row) {
    text = cell == null ? "" : "" + cell;
    if (text != "" && text != "-") {
      return text;
    };
  };
  return "";
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000070,
    '中文文本判断',
    'containsChineseText',
    TO_CLOB(q'[
function containsChineseText(value) {
  if (value == null) {
    return false;
  };
  text = "" + value;
  if (text == "" || text == "-" || text == "0") {
    return false;
  };
  chineseTextPattern = ".*[一-龥].*";
  return regexMatches(text, chineseTextPattern);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000071,
    '数字文本判断',
    'isNumericText',
    TO_CLOB(q'[
function isNumericText(value) {
  if (value == null) {
    return false;
  };
  text = "" + value;
  if (text == "" || text == "-") {
    return false;
  };
  numberWithThousandsPattern = "^-?[0-9]+(,[0-9]{3})*([.][0-9]+)?%?\$";
  numberPattern = "^-?[0-9]+([.][0-9]+)?%?\$";
  return regexMatches(text, numberWithThousandsPattern) || regexMatches(text, numberPattern);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000072,
    '右侧数字数量',
    'rightNumericCount',
    TO_CLOB(q'[
function rightNumericCount(row, startIndex) {
  if (row == null || startIndex == null || startIndex < 0 || startIndex >= row.length) {
    return 0;
  };
  count = 0;
  for (i = startIndex + 1; i < row.length; i++) {
    if (isNumericText(textAt(row, i))) {
      count = count + 1;
    };
  };
  return count;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000075,
    '紧邻右侧数字判断',
    'isRightNextNumeric',
    TO_CLOB(q'[
function isRightNextNumeric(row, startIndex) {
  if (row == null || startIndex == null || startIndex < 0 || startIndex + 1 >= row.length) {
    return false;
  };
  return isNumericText(textAt(row, startIndex + 1));
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000028,
    '新建Map',
    'newMap',
    TO_CLOB(q'[
function newMap(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10) {
  return mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000029,
    '构造Map',
    'mapOf',
    TO_CLOB(q'[
function mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10) {
  m = {:};
  if (k1 != null) {
    m[k1] = v1;
  };
  if (k2 != null) {
    m[k2] = v2;
  };
  if (k3 != null) {
    m[k3] = v3;
  };
  if (k4 != null) {
    m[k4] = v4;
  };
  if (k5 != null) {
    m[k5] = v5;
  };
  if (k6 != null) {
    m[k6] = v6;
  };
  if (k7 != null) {
    m[k7] = v7;
  };
  if (k8 != null) {
    m[k8] = v8;
  };
  if (k9 != null) {
    m[k9] = v9;
  };
  if (k10 != null) {
    m[k10] = v10;
  };
  return m;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000030,
    'Map写入',
    'put',
    TO_CLOB(q'[
function put(map, key, value) {
  result = map == null ? {:} : map;
  if (key != null) {
    result[key] = value;
  };
  return result;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000056,
    '关键字匹配',
    'matchesKeyword',
    TO_CLOB(q'[
function matchesKeyword(source, keyword) {
  if (source == null || keyword == null) {
    return false;
  };
  text = "" + source;
  kw = "" + keyword;
  if (text == "" || kw == "") {
    return false;
  };
  return text == kw || kw in text || text in kw;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000057,
    '科目代码文本判断',
    'isSubjectCodeText',
    TO_CLOB(q'[
function isSubjectCodeText(value, pattern) {
  if (value == null) {
    return false;
  };
  text = "" + value;
  if (text == "" || text == "-") {
    return false;
  };
  chineseTextPattern = ".*[一-龥].*";
  if (regexMatches(text, chineseTextPattern)) {
    return false;
  };
  if ("科" in text || "目" in text || "名" in text || "称" in text || "：" in text || ":" in text || "," in text || "，" in text) {
    return false;
  };
  defaultSubjectCodePattern = "^([0-9]{4}[A-Za-z0-9._ -]*|[A-Za-z][A-Za-z0-9._ -]*[0-9][A-Za-z0-9._ -]*)\$";
  subjectCodePattern = pattern == null || pattern == "" ? defaultSubjectCodePattern : pattern;
  return regexMatches(text, subjectCodePattern);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000058,
    '科目名称后继判断',
    'hasSubjectNameAfterCode',
    TO_CLOB(q'[
function hasSubjectNameAfterCode(row, codeIndex) {
  if (row == null || codeIndex == null || codeIndex < 0) {
    return false;
  };
  for (i = codeIndex + 1; i < row.length; i++) {
    text = textAt(row, i);
    if (text != "" && text != "-") {
      return true;
    };
  };
  return false;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000067,
    '科目明细行判断',
    'isSubjectDetailRowByColumn',
    TO_CLOB(q'[
function isSubjectDetailRowByColumn(row, subjectCodeColumnIndex, pattern) {
  if (row == null || subjectCodeColumnIndex == null || subjectCodeColumnIndex < 0 || subjectCodeColumnIndex >= row.length) {
    return false;
  };
  return isSubjectCodeText(textAt(row, subjectCodeColumnIndex), pattern) && hasSubjectNameAfterCode(row, subjectCodeColumnIndex);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000068,
    '指标明细行判断',
    'isMetricDetailRowByColumn',
    TO_CLOB(q'[
function isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) {
  if (row == null || subjectCodeColumnIndex == null || subjectCodeColumnIndex < 0 || subjectCodeColumnIndex >= row.length || isFooterRow(row, null)) {
    return false;
  };
  metricName = textAt(row, subjectCodeColumnIndex);
  if (!containsChineseText(metricName)) {
    return false;
  };
  return rightNumericCount(row, subjectCodeColumnIndex) >= 1;
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000073,
    '指标数据行判断(按科目代码列)',
    'isMetricDataRowByColumn',
    TO_CLOB(q'[
function isMetricDataRowByColumn(row, subjectCodeColumnIndex, pattern) {
  return isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) && isRightNextNumeric(row, subjectCodeColumnIndex);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000074,
    '指标行判断(按科目代码列)',
    'isMetricRowByColumn',
    TO_CLOB(q'[
function isMetricRowByColumn(row, subjectCodeColumnIndex, pattern) {
  return isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern) && !isRightNextNumeric(row, subjectCodeColumnIndex);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000069,
    '估值数据明细行判断',
    'isValuationDataRowByColumn',
    TO_CLOB(q'[
function isValuationDataRowByColumn(row, subjectCodeColumnIndex, pattern) {
  return isSubjectDetailRowByColumn(row, subjectCodeColumnIndex, pattern) || isMetricDetailRowByColumn(row, subjectCodeColumnIndex, pattern);
}
]'),
    TO_CLOB('["extract.parse"]')
  );
  seed_function(
    920000000000000031,
    '候选值存在判断',
    'hasCandidate',
    TO_CLOB(q'[
function hasCandidate(candidate) {
  return qlHeaderFns.hasCandidate(candidate);
}
]'),
    TO_CLOB('["extract.headerMapping"]')
  );
  seed_function(
    920000000000000032,
    '表头包含任一分段',
    'headerContainsAnySegment',
    TO_CLOB(q'[
function headerContainsAnySegment(headerText, segments) {
  return qlHeaderFns.headerContainsAnySegment(headerText, segments);
}
]'),
    TO_CLOB('["extract.headerMapping"]')
  );
  seed_function(
    920000000000000033,
    '表头包含全部分段',
    'headerContainsAllSegments',
    TO_CLOB(q'[
function headerContainsAllSegments(headerText, segments) {
  return qlHeaderFns.headerContainsAllSegments(headerText, segments);
}
]'),
    TO_CLOB('["extract.headerMapping"]')
  );
  seed_function(
    920000000000000059,
    '传输文本转换',
    'transferText',
    TO_CLOB(q'[
function transferText(value) {
  return value == null ? "" : "" + value;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000060,
    '传输文本匹配',
    'textMatches',
    TO_CLOB(q'[
function textMatches(source, keyword) {
  text = transferText(source);
  kw = transferText(keyword);
  if (text == "" || kw == "") {
    return false;
  };
  return text == kw || kw in text || text in kw || text like kw;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000034,
    '忽略大小写包含',
    'containsIgnoreCase',
    TO_CLOB(q'[
function containsIgnoreCase(source, keyword) {
  return textMatches(source, keyword);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000035,
    '正则匹配兼容',
    'matchesRegex',
    TO_CLOB(q'[
function matchesRegex(source, regex) {
  return textMatches(source, regex);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000039,
    '任一匹配兼容',
    'matchesAnyRegex',
    TO_CLOB(q'[
function matchesAnyRegex(source, regexList) {
  return containsAnyText(source, regexList);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000054,
    '文本命中任一关键词',
    'containsAnyText',
    TO_CLOB(q'[
function containsAnyText(source, keywords) {
  if (source == null || keywords == null) {
    return false;
  };
  for (keyword : keywords) {
    if (textMatches(source, keyword)) {
      return true;
    };
  };
  return false;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000055,
    '文本命中全部关键词',
    'containsAllText',
    TO_CLOB(q'[
function containsAllText(source, keywords) {
  if (source == null || keywords == null || keywords.length == 0) {
    return false;
  };
  for (keyword : keywords) {
    if (!textMatches(source, keyword)) {
      return false;
    };
  };
  return true;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000040,
    'Excel文件判断',
    'isExcel',
    TO_CLOB(q'[
function isExcel(fileName) {
  text = transferText(fileName);
  return text like "%.xlsx" || text like "%.xls" || text like "%.XLSX" || text like "%.XLS";
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000041,
    'Excel文件判断别名',
    'isExcelFile',
    TO_CLOB(q'[
function isExcelFile(fileName) {
  return isExcel(fileName);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000042,
    'CSV文件判断',
    'isCsv',
    TO_CLOB(q'[
function isCsv(fileName) {
  text = transferText(fileName);
  return text like "%.csv" || text like "%.CSV";
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000043,
    'CSV文件判断别名',
    'isCsvFile',
    TO_CLOB(q'[
function isCsvFile(fileName) {
  return isCsv(fileName);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000044,
    '读取Excel数据兼容',
    'readExcelData',
    TO_CLOB(q'[
function readExcelData(source) {
  return source == null ? [] : source;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000045,
    '读取CSV数据兼容',
    'readCsvData',
    TO_CLOB(q'[
function readCsvData(source) {
  return source == null ? [] : source;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000046,
    '读取Excel前N行兼容',
    'readExcelDataWithin',
    TO_CLOB(q'[
function readExcelDataWithin(source, maxRows) {
  return source == null ? [] : source;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000047,
    '读取CSV前N行兼容',
    'readCsvDataWithin',
    TO_CLOB(q'[
function readCsvDataWithin(source, maxRows) {
  return source == null ? [] : source;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000061,
    '行文本拼接',
    'rowText',
    TO_CLOB(q'[
function rowText(row) {
  if (row == null) {
    return "";
  };
  text = "";
  for (cell : row) {
    c = transferText(cell);
    if (c != "") {
      text = text + " " + c;
    };
  };
  return text;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000062,
    '行命中全部文本',
    'rowContainsAllText',
    TO_CLOB(q'[
function rowContainsAllText(row, keywords) {
  return containsAllText(rowText(row), keywords);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000063,
    '预览行查找表头行',
    'findHeaderRowIndexInRows',
    TO_CLOB(q'[
function findHeaderRowIndexInRows(rows, keywords) {
  if (rows == null || keywords == null || keywords.length == 0) {
    return -1;
  };
  for (i = 0; i < rows.length; i++) {
    if (rowContainsAllText(rows[i], keywords)) {
      return i;
    };
  };
  return -1;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000048,
    '前N行查找表头行',
    'findHeaderRowIndexWithin',
    TO_CLOB(q'[
function findHeaderRowIndexWithin(source, maxRows, keyword1, keyword2) {
  return findHeaderRowIndexInRows(source, [keyword1, keyword2]);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000049,
    '前N行表头关键词判断',
    'hasHeaderKeywordsWithinFirstRows',
    TO_CLOB(q'[
function hasHeaderKeywordsWithinFirstRows(source, maxRows, keyword1, keyword2) {
  return findHeaderRowIndexWithin(source, maxRows, keyword1, keyword2) >= 0;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000050,
    '前N行表头关键词集合判断',
    'hasHeaderKeywordsWithinFirstRowsByList',
    TO_CLOB(q'[
function hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords) {
  return findHeaderRowIndexInRows(source, keywords) >= 0;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000051,
    '估值表判断',
    'isValuationTable',
    TO_CLOB(q'[
function isValuationTable(source, maxRows) {
  return hasHeaderKeywordsWithinFirstRows(source, maxRows, "科目代码", "科目名称");
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000052,
    '关键词估值表判断',
    'isValuationTableByKeywords',
    TO_CLOB(q'[
function isValuationTableByKeywords(source, maxRows, keywords) {
  return hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords);
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000064,
    '标签元数据表头关键词',
    'headerKeywordsFromMeta',
    TO_CLOB(q'[
function headerKeywordsFromMeta(meta) {
  if (meta == null) {
    return ["科目代码", "科目名称"];
  };
  keywords = meta["headerKeywords"];
  if (keywords == null || keywords.length == 0) {
    return ["科目代码", "科目名称"];
  };
  return keywords;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000053,
    '扩展配置估值表判断',
    'isValuationTableByMeta',
    TO_CLOB(q'[
function isValuationTableByMeta(source, meta) {
  return isValuationTableByKeywords(source, meta == null ? null : meta["scanLimit"], headerKeywordsFromMeta(meta));
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000037,
    '产品规则字段取值',
    'productRuleValue',
    TO_CLOB(q'[
function productRuleValue(rule, fieldName) {
  if (rule == null || fieldName == null) {
    return null;
  };
  return rule[fieldName];
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000065,
    '产品规则命中判断',
    'productRuleMatches',
    TO_CLOB(q'[
function productRuleMatches(fileName, rule) {
  keywords = productRuleValue(rule, "matchKeywords");
  if (keywords != null && keywords.length > 0) {
    return containsAllText(fileName, keywords);
  };
  return textMatches(fileName, productRuleValue(rule, "matchRules"));
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000036,
    '首个产品匹配规则',
    'firstProductMatchRule',
    TO_CLOB(q'[
function firstProductMatchRule(fileName, productMatchRules) {
  if (fileName == null || productMatchRules == null) {
    return null;
  };
  for (rule : productMatchRules) {
    if (productRuleMatches(fileName, rule)) {
      return rule;
    };
  };
  return null;
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
  seed_function(
    920000000000000038,
    '产品匹配结果',
    'productMatchResult',
    TO_CLOB(q'[
function productMatchResult(rule, fileName) {
  if (rule == null) {
    return null;
  };
  snapshot = {"ruleId": productRuleValue(rule, "id"), "pdCd": productRuleValue(rule, "pdCd"), "pdNm": productRuleValue(rule, "pdNm"), "orgCd": productRuleValue(rule, "orgCd"), "orgNm": productRuleValue(rule, "orgNm"), "pdType": productRuleValue(rule, "pdType"), "fileType": productRuleValue(rule, "fileType"), "fileTypeName": productRuleValue(rule, "fileTypeName"), "matchRules": productRuleValue(rule, "matchRules"), "jobName": productRuleValue(rule, "jobName"), "jobScene": productRuleValue(rule, "jobScene")};
  return {"matched": true, "message": "产品匹配规则命中", "tagValue": snapshot["ruleId"], "matchedField": "fileName", "matchedValue": fileName, "snapshot": snapshot};
}
]'),
    TO_CLOB('["transfer.rule"]')
  );
END;
/

COMMIT;
