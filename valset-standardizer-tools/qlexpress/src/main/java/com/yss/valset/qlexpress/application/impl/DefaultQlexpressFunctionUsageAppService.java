package com.yss.valset.qlexpress.application.impl;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionFlowUsageDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionUsageDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionUsageReferenceDTO;
import com.yss.valset.qlexpress.application.service.QlexpressFunctionUsageAppService;
import com.yss.valset.qlexpress.infrastructure.entity.QlexpressFunctionPO;
import com.yss.valset.qlexpress.infrastructure.mapper.QlexpressFunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 默认 QLExpress 函数使用关系查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultQlexpressFunctionUsageAppService implements QlexpressFunctionUsageAppService {

    public static final String STATUS_DIRECT_REFERENCED = "DIRECT_REFERENCED";
    public static final String STATUS_DEPENDENCY_ONLY = "DEPENDENCY_ONLY";
    public static final String STATUS_SCOPE_AVAILABLE = "SCOPE_AVAILABLE";

    private static final String SCOPE_COMMON = "common";
    private static final String SCOPE_EXTRACT_PARSE = "extract.parse";
    private static final String SCOPE_EXTRACT_HEADER_MAPPING = "extract.headerMapping";
    private static final String SCOPE_TRANSFER_RULE = "transfer.rule";

    private static final String FLOW_TRANSFER = "TRANSFER_RECOGNITION";
    private static final String FLOW_FILE_PARSE = "FILE_PARSE";
    private static final String FLOW_STRUCTURE_STANDARDIZE = "STRUCTURE_STANDARDIZE";
    private static final String FLOW_STANDARD_LANDING = "STANDARD_LANDING";

    private static final List<FlowDefinition> FLOW_DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
            new FlowDefinition(FLOW_TRANSFER, "文件接入与识别", SCOPE_TRANSFER_RULE),
            new FlowDefinition(FLOW_FILE_PARSE, "原始文件抽取与解析", SCOPE_EXTRACT_PARSE),
            new FlowDefinition(FLOW_STRUCTURE_STANDARDIZE, "字段映射与结构标准化", SCOPE_EXTRACT_HEADER_MAPPING),
            new FlowDefinition(FLOW_STANDARD_LANDING, "估值贴源数据落地", null)
    ));

    private static final List<ExpressionSource> FIXED_SOURCES = Collections.unmodifiableList(Arrays.asList(
            new ExpressionSource(FLOW_FILE_PARSE, "原始文件抽取与解析", SCOPE_EXTRACT_PARSE,
                    "isHeaderRow(row, requiredHeaders)", "PARSE_EXPRESSION", "解析固定表达式",
                    "HEADER_ROW_EXPR", "表头识别表达式", Boolean.TRUE),
            new ExpressionSource(FLOW_FILE_PARSE, "原始文件抽取与解析", SCOPE_EXTRACT_PARSE,
                    "isDataStartRow(row)", "PARSE_EXPRESSION", "解析固定表达式",
                    "DATA_START_EXPR", "数据起始行表达式", Boolean.TRUE),
            new ExpressionSource(FLOW_FILE_PARSE, "原始文件抽取与解析", SCOPE_EXTRACT_PARSE,
                    "classifyRowWithPattern(row, footerKeywords, subjectCodePattern)", "PARSE_EXPRESSION", "解析固定表达式",
                    "ROW_CLASSIFY_EXPR", "行分类表达式", Boolean.TRUE),
            new ExpressionSource(FLOW_STRUCTURE_STANDARDIZE, "字段映射与结构标准化", SCOPE_EXTRACT_HEADER_MAPPING,
                    "exactCandidate != null ? 'exact_header' : (segmentCandidate != null ? 'header_segment' : (aliasCandidate != null ? 'alias_contains' : 'fallback'))",
                    "HEADER_MAPPING_EXPRESSION", "表头映射表达式", "STRATEGY_EXPR", "表头策略选择表达式", Boolean.TRUE),
            new ExpressionSource(FLOW_STRUCTURE_STANDARDIZE, "字段映射与结构标准化", SCOPE_EXTRACT_HEADER_MAPPING,
                    "exactCandidate != null ? 0.98 : (segmentCandidate != null ? 0.92 : (aliasCandidate != null ? 0.80 : 0.0))",
                    "HEADER_MAPPING_EXPRESSION", "表头映射表达式", "CONFIDENCE_EXPR", "表头匹配置信度表达式", Boolean.TRUE),
            new ExpressionSource(FLOW_STRUCTURE_STANDARDIZE, "字段映射与结构标准化", SCOPE_EXTRACT_HEADER_MAPPING,
                    "exactCandidate != null ? '精确表头匹配' : (segmentCandidate != null ? '按表头分段匹配' : (aliasCandidate != null ? '按别名白名单匹配' : '未命中标准表头'))",
                    "HEADER_MAPPING_EXPRESSION", "表头映射表达式", "REASON_EXPR", "表头匹配原因表达式", Boolean.TRUE)
    ));

    private final QlexpressFunctionRepository qlexpressFunctionRepository;
    private final QlexpressFunctionUsageQueryRepository usageQueryRepository;
    private final ObjectMapper objectMapper;

    @Override
    public QlexpressFunctionUsageDTO getUsage(String functionId) {
        if (!hasText(functionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数ID不能为空");
        }
        QlexpressFunctionPO function = qlexpressFunctionRepository.selectById(functionId);
        if (function == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到函数，functionId=" + functionId);
        }
        return buildUsage(function, true);
    }

    @Override
    public QlexpressFunctionUsageDTO summarizeUsage(QlexpressFunctionPO function) {
        if (function == null) {
            return null;
        }
        UsageScanContext context = loadContext();
        return buildUsage(function, false, context);
    }

    @Override
    public Map<String, QlexpressFunctionUsageDTO> summarizeUsages(List<QlexpressFunctionPO> functions) {
        if (functions == null || functions.isEmpty()) {
            return Collections.emptyMap();
        }
        UsageScanContext context = loadContext();
        Map<String, QlexpressFunctionUsageDTO> result = new LinkedHashMap<>();
        for (QlexpressFunctionPO function : functions) {
            if (function == null || !hasText(function.getFunctionId())) {
                continue;
            }
            result.put(function.getFunctionId(), buildUsage(function, false, context));
        }
        return result;
    }

    private QlexpressFunctionUsageDTO buildUsage(QlexpressFunctionPO function, boolean includeDetails) {
        return buildUsage(function, includeDetails, loadContext());
    }

    private QlexpressFunctionUsageDTO buildUsage(QlexpressFunctionPO function, boolean includeDetails, UsageScanContext context) {
        List<String> sourceModules = sourceModules(function.getExtInfoJson());
        List<QlexpressFunctionFlowUsageDTO> flowUsages = flowUsages(sourceModules);
        List<QlexpressFunctionUsageReferenceDTO> directReferences = directReferences(function.getFunctionName(), context);
        List<QlexpressFunctionUsageReferenceDTO> dependencyReferences = dependencyReferences(function.getFunctionName(), includeDetails, context);
        String usageStatus = usageStatus(sourceModules, directReferences, dependencyReferences);
        return QlexpressFunctionUsageDTO.builder()
                .functionId(function.getFunctionId())
                .functionName(function.getFunctionName())
                .sourceModules(sourceModules)
                .flowUsages(flowUsages)
                .directReferences(includeDetails ? directReferences : Collections.emptyList())
                .dependencyReferences(includeDetails ? dependencyReferences : Collections.emptyList())
                .usageStatus(usageStatus)
                .usageStatusName(usageStatusName(usageStatus))
                .build();
    }

    private UsageScanContext loadContext() {
        return new UsageScanContext(expressionSources(), allFunctions());
    }

    private List<QlexpressFunctionFlowUsageDTO> flowUsages(List<String> sourceModules) {
        return FLOW_DEFINITIONS.stream()
                .map(flow -> QlexpressFunctionFlowUsageDTO.builder()
                        .flowCode(flow.flowCode)
                        .flowName(flow.flowName)
                        .runnerScope(flow.runnerScope)
                        .matched(matchesScope(sourceModules, flow.runnerScope))
                        .build())
                .collect(Collectors.toList());
    }

    private List<QlexpressFunctionUsageReferenceDTO> directReferences(String functionName, UsageScanContext context) {
        if (!hasText(functionName)) {
            return Collections.emptyList();
        }
        List<QlexpressFunctionUsageReferenceDTO> references = new ArrayList<>();
        for (ExpressionSource source : context.expressionSources) {
            Set<String> functions = outFunctions(source.expression, functionName);
            if (functions.contains(functionName)) {
                references.add(toReference(source, functions));
            }
        }
        return references;
    }

    private List<QlexpressFunctionUsageReferenceDTO> dependencyReferences(String functionName, boolean includeDetails, UsageScanContext context) {
        if (!hasText(functionName)) {
            return Collections.emptyList();
        }
        List<QlexpressFunctionUsageReferenceDTO> references = new ArrayList<>();
        for (QlexpressFunctionPO function : context.functions) {
            if (Objects.equals(functionName, function.getFunctionName()) || !hasText(function.getScriptBody())) {
                continue;
            }
            Set<String> functions = outFunctions(function.getScriptBody(), functionName);
            if (functions.contains(functionName)) {
                List<String> modules = sourceModules(function.getExtInfoJson());
                ExpressionSource source = new ExpressionSource(
                        firstFlowCode(modules),
                        firstFlowName(modules),
                        firstRunnerScope(modules),
                        includeDetails ? function.getScriptBody() : "",
                        "FUNCTION_SCRIPT",
                        "函数脚本",
                        function.getFunctionId(),
                        nullToText(function.getFunctionCnName(), function.getFunctionName()),
                        Boolean.TRUE.equals(function.getEnabled())
                );
                references.add(toReference(source, functions));
            }
        }
        return references;
    }

    private List<ExpressionSource> expressionSources() {
        List<ExpressionSource> sources = new ArrayList<>(FIXED_SOURCES);
        sources.addAll(usageQueryRepository.listTransferRuleSources());
        sources.addAll(usageQueryRepository.listTransferTagSources());
        return sources;
    }

    private List<QlexpressFunctionPO> allFunctions() {
        List<QlexpressFunctionPO> records = qlexpressFunctionRepository.selectList(
                Wrappers.lambdaQuery(QlexpressFunctionPO.class)
                        .orderByAsc(QlexpressFunctionPO::getFunctionId)
        );
        return records == null ? Collections.emptyList() : records;
    }

    @RequiredArgsConstructor
    private static class UsageScanContext {
        private final List<ExpressionSource> expressionSources;
        private final List<QlexpressFunctionPO> functions;
    }

    private QlexpressFunctionUsageReferenceDTO toReference(ExpressionSource source, Set<String> functions) {
        return QlexpressFunctionUsageReferenceDTO.builder()
                .flowCode(source.flowCode)
                .flowName(source.flowName)
                .runnerScope(source.runnerScope)
                .expression(source.expression)
                .sourceType(source.sourceType)
                .sourceTypeName(source.sourceTypeName)
                .sourceId(source.sourceId)
                .sourceName(source.sourceName)
                .enabled(source.enabled)
                .referencedFunctions(new ArrayList<>(functions))
                .build();
    }

    private Set<String> outFunctions(String expression, String fallbackFunctionName) {
        if (!hasText(expression)) {
            return Collections.emptySet();
        }
        try {
            Set<String> functions = new Express4Runner(InitOptions.DEFAULT_OPTIONS).getOutFunctions(expression);
            return functions == null ? Collections.emptySet() : new LinkedHashSet<>(functions);
        } catch (Exception ignored) {
            if (containsFunctionCall(expression, fallbackFunctionName)) {
                return new LinkedHashSet<>(Collections.singletonList(fallbackFunctionName));
            }
            return Collections.emptySet();
        }
    }

    private boolean containsFunctionCall(String expression, String functionName) {
        if (!hasText(expression) || !hasText(functionName)) {
            return false;
        }
        return Pattern.compile("(^|[^A-Za-z0-9_])" + Pattern.quote(functionName) + "\\s*\\(").matcher(expression).find();
    }

    private String usageStatus(List<String> sourceModules,
                               List<QlexpressFunctionUsageReferenceDTO> directReferences,
                               List<QlexpressFunctionUsageReferenceDTO> dependencyReferences) {
        if (directReferences != null && !directReferences.isEmpty()) {
            return STATUS_DIRECT_REFERENCED;
        }
        if (dependencyReferences != null && !dependencyReferences.isEmpty()) {
            return STATUS_DEPENDENCY_ONLY;
        }
        return STATUS_SCOPE_AVAILABLE;
    }

    public static String usageStatusName(String usageStatus) {
        if (STATUS_DIRECT_REFERENCED.equals(usageStatus)) {
            return "已引用";
        }
        if (STATUS_DEPENDENCY_ONLY.equals(usageStatus)) {
            return "仅函数库依赖调用";
        }
        return "作用域可用但未直接引用";
    }

    private List<String> sourceModules(String extInfoJson) {
        try {
            Map<String, Object> extInfo = objectMapper.readValue(extInfoJson, new TypeReference<Map<String, Object>>() {});
            return sourceModules(extInfo.get("sourceModules"));
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private List<String> sourceModules(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .filter(item -> item != null && hasText(String.valueOf(item)))
                    .map(item -> String.valueOf(item).trim())
                    .distinct()
                    .collect(Collectors.toList());
        }
        String text = String.valueOf(value).trim();
        return hasText(text) ? Collections.singletonList(text) : Collections.emptyList();
    }

    private boolean matchesScope(List<String> sourceModules, String runnerScope) {
        if (!hasText(runnerScope)) {
            return false;
        }
        if (sourceModules == null || sourceModules.isEmpty()) {
            return false;
        }
        for (String module : sourceModules) {
            if (runnerScope.equalsIgnoreCase(module) || SCOPE_COMMON.equalsIgnoreCase(module)) {
                return true;
            }
        }
        return false;
    }

    private String firstRunnerScope(List<String> sourceModules) {
        if (sourceModules == null || sourceModules.isEmpty()) {
            return null;
        }
        String firstNonCommon = sourceModules.stream()
                .filter(this::hasText)
                .filter(module -> !SCOPE_COMMON.equalsIgnoreCase(module))
                .findFirst()
                .orElse(sourceModules.get(0));
        return firstNonCommon;
    }

    private String firstFlowCode(List<String> sourceModules) {
        FlowDefinition flow = firstFlow(sourceModules);
        return flow == null ? null : flow.flowCode;
    }

    private String firstFlowName(List<String> sourceModules) {
        FlowDefinition flow = firstFlow(sourceModules);
        return flow == null ? "公共函数" : flow.flowName;
    }

    private FlowDefinition firstFlow(List<String> sourceModules) {
        if (sourceModules == null || sourceModules.isEmpty()) {
            return null;
        }
        for (String module : sourceModules) {
            for (FlowDefinition flow : FLOW_DEFINITIONS) {
                if (flow.runnerScope != null && flow.runnerScope.equalsIgnoreCase(module)) {
                    return flow;
                }
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String nullToText(String first, String fallback) {
        return hasText(first) ? first : fallback;
    }

    @RequiredArgsConstructor
    private static class FlowDefinition {
        private final String flowCode;
        private final String flowName;
        private final String runnerScope;
    }

    public static class ExpressionSource {
        private final String flowCode;
        private final String flowName;
        private final String runnerScope;
        private final String expression;
        private final String sourceType;
        private final String sourceTypeName;
        private final String sourceId;
        private final String sourceName;
        private final Boolean enabled;

        public ExpressionSource(String flowCode,
                                String flowName,
                                String runnerScope,
                                String expression,
                                String sourceType,
                                String sourceTypeName,
                                String sourceId,
                                String sourceName,
                                Boolean enabled) {
            this.flowCode = flowCode;
            this.flowName = flowName;
            this.runnerScope = runnerScope;
            this.expression = expression;
            this.sourceType = sourceType;
            this.sourceTypeName = sourceTypeName;
            this.sourceId = sourceId;
            this.sourceName = sourceName;
            this.enabled = enabled;
        }
    }

    @Service
    @RequiredArgsConstructor
    public static class QlexpressFunctionUsageQueryRepository {

        private final JdbcTemplate jdbcTemplate;

        public List<ExpressionSource> listTransferRuleSources() {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select rule_id, rule_code, rule_name, enabled, script_body "
                            + "from t_transfer_rule where script_language = ? and script_body is not null",
                    "qlexpress4"
            );
            return rows == null ? Collections.emptyList() : rows.stream()
                    .filter(row -> hasTextStatic(stringValue(row, "script_body")))
                    .map(row -> new ExpressionSource(
                            FLOW_TRANSFER,
                            "文件接入与识别",
                            SCOPE_TRANSFER_RULE,
                            stringValue(row, "script_body"),
                            "TRANSFER_RULE",
                            "分拣规则",
                            stringValue(row, "rule_id"),
                            nullToTextStatic(stringValue(row, "rule_name"), stringValue(row, "rule_code")),
                            booleanValue(row, "enabled")
                    ))
                    .collect(Collectors.toList());
        }

        public List<ExpressionSource> listTransferTagSources() {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select tag_id, tag_code, tag_name, enabled, script_body "
                            + "from t_transfer_tag where script_language = ? and script_body is not null",
                    "qlexpress4"
            );
            return rows == null ? Collections.emptyList() : rows.stream()
                    .filter(row -> hasTextStatic(stringValue(row, "script_body")))
                    .map(row -> new ExpressionSource(
                            FLOW_TRANSFER,
                            "文件接入与识别",
                            SCOPE_TRANSFER_RULE,
                            stringValue(row, "script_body"),
                            "TRANSFER_TAG",
                            "识别标签",
                            stringValue(row, "tag_id"),
                            nullToTextStatic(stringValue(row, "tag_name"), stringValue(row, "tag_code")),
                            booleanValue(row, "enabled")
                    ))
                    .collect(Collectors.toList());
        }

        private static String stringValue(Map<String, Object> row, String key) {
            Object value = findValue(row, key);
            return value == null ? null : String.valueOf(value);
        }

        private static Boolean booleanValue(Map<String, Object> row, String key) {
            Object value = findValue(row, key);
            if (value == null) {
                return Boolean.FALSE;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
            return "1".equals(text) || "true".equals(text) || "y".equals(text);
        }

        private static Object findValue(Map<String, Object> row, String key) {
            if (row == null || key == null) {
                return null;
            }
            if (row.containsKey(key)) {
                return row.get(key);
            }
            if (row.containsKey(key.toUpperCase(Locale.ROOT))) {
                return row.get(key.toUpperCase(Locale.ROOT));
            }
            String camelKey = toCamelKey(key);
            return row.get(camelKey);
        }

        private static String toCamelKey(String key) {
            StringBuilder builder = new StringBuilder();
            boolean upperNext = false;
            for (char ch : key.toCharArray()) {
                if (ch == '_') {
                    upperNext = true;
                } else if (upperNext) {
                    builder.append(Character.toUpperCase(ch));
                    upperNext = false;
                } else {
                    builder.append(Character.toLowerCase(ch));
                }
            }
            return builder.toString();
        }

        private static String nullToTextStatic(String first, String fallback) {
            return first != null && !first.trim().isEmpty() ? first : fallback;
        }

        private static boolean hasTextStatic(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
