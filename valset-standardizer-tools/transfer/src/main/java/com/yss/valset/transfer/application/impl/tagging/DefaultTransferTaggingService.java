package com.yss.valset.transfer.application.impl.tagging;

import com.yss.valset.transfer.application.command.TransferTagTestCommand;
import com.yss.valset.transfer.application.dto.TransferTagTestResultDTO;
import com.yss.valset.transfer.application.service.TransferObjectBusinessFieldProjectionUseCase;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.domain.gateway.ProductMatchRuleGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.ProductMatchRule;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.domain.rule.RuleEngine;
import com.yss.valset.transfer.domain.rule.TransferRuleFunctions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 默认文件对象标签服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTransferTaggingService implements TransferTaggingUseCase {

    private final TransferObjectGateway transferObjectGateway;
    private final TransferTagGateway transferTagGateway;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final TransferObjectBusinessFieldProjectionUseCase transferObjectBusinessFieldProjectionUseCase;
    private final ProductMatchRuleGateway productMatchRuleGateway;
    private final RuleEngine ruleEngine;
    private final TransferRuleFunctions transferRuleFunctions = new TransferRuleFunctions();

    @Override
    public List<TransferObjectTag> tag(TransferObject transferObject, RecognitionContext recognitionContext, ProbeResult probeResult) {
        if (transferObject == null || transferObject.transferId() == null) {
            return java.util.Arrays.asList();
        }
        RecognitionContext effectiveRecognitionContext = resolveTaggingContext(recognitionContext, transferObject);
        String tagPath = resolveTagPath(effectiveRecognitionContext, transferObject);
        log.info("开始文件对象打标，transferId={}，sourceId={}，sourceCode={}，originalName={}，taggingPath={}",
                transferObject.transferId(),
                transferObject.sourceId(),
                transferObject.sourceCode(),
                transferObject.originalName(),
                tagPath);
        List<TransferTagDefinition> tagDefinitions = transferTagGateway.listEnabledTags();
        List<TransferObjectTag> tags = new ArrayList<>();
        if (!tagDefinitions.isEmpty()) {
            List<ProductMatchRule> productMatchRules = loadProductMatchRules();
            for (TransferTagDefinition tagDefinition : tagDefinitions) {
                TagEvaluation evaluation = evaluate(tagDefinition, effectiveRecognitionContext, probeResult, transferObject, productMatchRules);
                if (!evaluation.matched()) {
                    continue;
                }
                tags.add(new TransferObjectTag(
                        null,
                        transferObject.transferId(),
                        tagDefinition.tagId(),
                        tagDefinition.tagCode(),
                        tagDefinition.tagName(),
                        firstNonBlank(evaluation.tagValue(), tagDefinition.tagValue()),
                        tagDefinition.matchStrategy(),
                        evaluation.message(),
                        evaluation.matchedField(),
                        evaluation.matchedValue(),
                        evaluation.snapshot(),
                        Instant.now()
                ));
            }
            if (!tags.isEmpty()) {
                transferObjectTagGateway.saveAll(tags);
            }
        }
        try {
            transferObjectBusinessFieldProjectionUseCase.project(transferObject, tags);
        } catch (Exception exception) {
            log.warn("文件对象业务字段回填失败，继续执行后续流程，transferId={}，sourceId={}，sourceCode={}",
                    transferObject.transferId(),
                    transferObject.sourceId(),
                    transferObject.sourceCode(),
                    exception);
        }
        return tags;
    }

    @Override
    public List<TransferObjectTag> retag(String transferId, boolean overwrite) {
        if (transferId == null || transferId.trim().isEmpty()) {
            return java.util.Arrays.asList();
        }
        TransferObject transferObject = transferObjectGateway.findById(transferId).orElse(null);
        if (transferObject == null) {
            return java.util.Arrays.asList();
        }
        List<TransferTagDefinition> tagDefinitions = transferTagGateway.listEnabledTags();
        if (tagDefinitions.isEmpty()) {
            return java.util.Arrays.asList();
        }
        if (overwrite) {
            transferObjectTagGateway.deleteByTransferId(transferId);
        }
        return tag(transferObject, toRecognitionContext(transferObject), transferObject.probeResult());
    }

    @Override
    public TransferTagTestResultDTO test(String tagId, TransferTagTestCommand command) {
        TransferTagDefinition definition = transferTagGateway.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签，tagId=" + tagId));
        RecognitionContext recognitionContext = new RecognitionContext(
                parseSourceType(command == null ? null : command.getSourceType()),
                normalizeText(command == null ? null : command.getSourceCode()),
                normalizeText(command == null ? null : command.getFileName()),
                normalizeText(command == null ? null : command.getMimeType()),
                command == null ? null : command.getFileSize(),
                normalizeText(command == null ? null : command.getSender()),
                null,
                null,
                null,
                normalizeText(command == null ? null : command.getSubject()),
                normalizeText(command == null ? null : command.getBody()),
                null,
                null,
                normalizeText(command == null ? null : command.getMailFolder()),
                normalizeText(command == null ? null : command.getPath()),
                command == null || command.getAttributes() == null ? java.util.Collections.emptyMap() : command.getAttributes()
        );
        TagEvaluation evaluation = evaluate(definition, recognitionContext, null, null, loadProductMatchRules());
        return TransferTagTestResultDTO.builder()
                .tagId(tagId)
                .matched(evaluation.matched())
                .matchStrategy(definition.matchStrategy())
                .matchReason(evaluation.message())
                .matchedByScript(evaluation.matchedByScript())
                .matchedByRegex(evaluation.matchedByRegex())
                .matchedField(evaluation.matchedField())
                .matchedValue(evaluation.matchedValue())
                .contextSnapshot(evaluation.snapshot())
                .build();
    }

    private TagEvaluation evaluate(TransferTagDefinition definition,
                                   RecognitionContext recognitionContext,
                                   ProbeResult probeResult,
                                   TransferObject transferObject,
                                   List<ProductMatchRule> productMatchRules) {
        if (definition == null) {
            return TagEvaluation.miss("标签为空");
        }
        if (!definition.enabled()) {
            return TagEvaluation.miss("标签未启用");
        }
        String strategy = normalizeText(definition.matchStrategy()).toUpperCase(Locale.ROOT);
        boolean scriptMatched = false;
        boolean regexMatched = false;
        String matchedField = null;
        String matchedValue = null;
        String tagValue = null;
        String message = "标签未命中";
        Map<String, Object> scriptResult = java.util.Collections.emptyMap();
        if (strategy.contains("SCRIPT")) {
            RuleEvaluationResult result = ruleEngine.evaluate(buildRuleDefinition(definition), new RuleContext(recognitionContext, probeResult, buildVariables(recognitionContext, transferObject, productMatchRules, definition.tagMeta())));
            scriptMatched = result != null && result.matched();
            message = result == null ? "脚本未返回结果" : result.message();
            scriptResult = result == null || result.result() == null ? java.util.Collections.emptyMap() : result.result();
            if (scriptMatched) {
                tagValue = resultText(scriptResult, "tagValue");
                matchedField = resultText(scriptResult, "matchedField");
                matchedValue = resultText(scriptResult, "matchedValue");
            }
        }
        if (strategy.contains("REGEX")) {
            RegexMatchResult regexResult = evaluateRegex(definition, recognitionContext, transferObject);
            regexMatched = regexResult.matched();
            matchedField = regexResult.matchedField();
            matchedValue = regexResult.matchedValue();
            if (regexResult.message() != null && !regexResult.message().trim().isEmpty()) {
                message = regexResult.message();
            }
        }
        boolean matched;
        if ("".equals(strategy)) {
            matched = scriptMatched || regexMatched;
        } else if ("SCRIPT_RULE".equals(strategy)) {
            matched = scriptMatched;
        } else if ("REGEX_RULE".equals(strategy)) {
            matched = regexMatched;
        } else if ("SCRIPT_AND_REGEX".equals(strategy)) {
            matched = scriptMatched && regexMatched;
        } else if ("SCRIPT_OR_REGEX".equals(strategy)) {
            matched = scriptMatched || regexMatched;
        } else {
            matched = scriptMatched || regexMatched;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sourceType", recognitionContext == null ? null : recognitionContext.sourceType());
        snapshot.put("sourceCode", recognitionContext == null ? null : recognitionContext.sourceCode());
        snapshot.put("fileName", recognitionContext == null ? null : recognitionContext.fileName());
        snapshot.put("sender", recognitionContext == null ? null : recognitionContext.sender());
        snapshot.put("subject", recognitionContext == null ? null : recognitionContext.subject());
        snapshot.put("path", resolveTagPath(recognitionContext, transferObject));
        snapshot.put("probeDetected", probeResult != null && probeResult.detected());
        snapshot.put("probeDetectedType", probeResult == null ? null : probeResult.detectedType());
        snapshot.put("probeAttributesCount", probeResult == null || probeResult.attributes() == null ? 0 : probeResult.attributes().size());
        mergeScriptResult(snapshot, scriptResult);
        return new TagEvaluation(matched, scriptMatched, regexMatched, message, matchedField, matchedValue, snapshot, tagValue);
    }

    private RuleDefinition buildRuleDefinition(TransferTagDefinition definition) {
        return new RuleDefinition(
                definition.tagId(),
                definition.tagCode(),
                definition.tagName(),
                "1.0.0",
                definition.enabled(),
                definition.priority(),
                definition.matchStrategy(),
                definition.scriptLanguage(),
                normalizeScriptBody(definition.scriptBody()),
                null,
                null,
                definition.tagMeta()
        );
    }

    private String normalizeScriptBody(String scriptBody) {
        if (scriptBody == null || scriptBody.trim().isEmpty()) {
            return scriptBody;
        }
        String normalized = scriptBody.trim();
        if ((normalized.contains("isValuationTableByMeta(source, tagMeta)")
                || normalized.contains("isValuationTableByMeta(previewRows, tagMeta)"))
                && (normalized.contains("var source =")
                || normalized.contains("String(filePath)")
                || normalized.contains("String(source)")
                || normalized.contains("filePath.trim()")
                || normalized.contains("source.trim()"))) {
            return "source = hasText(filePath) ? filePath : path;\n"
                    + "if (!hasText(source)) {\n"
                    + "    return false;\n"
                    + "}\n"
                    + "if (!(isExcelFile(source) || isCsvFile(source))) {\n"
                    + "    return false;\n"
                    + "}\n"
                    + "return isValuationTableByMeta(previewRows, tagMeta);";
        }
        return scriptBody;
    }

    private Map<String, Object> buildVariables(RecognitionContext recognitionContext,
                                               TransferObject transferObject,
                                               List<ProductMatchRule> productMatchRules,
                                               Map<String, Object> tagMeta) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (transferObject != null) {
            variables.putIfAbsent("transferId", transferObject.transferId());
            variables.putIfAbsent("sourceId", transferObject.sourceId());
            variables.putIfAbsent("sourceType", transferObject.sourceType());
            variables.putIfAbsent("sourceCode", transferObject.sourceCode());
            variables.putIfAbsent("fileName", transferObject.originalName());
            variables.putIfAbsent("fileSize", transferObject.sizeBytes());
            variables.putIfAbsent("sender", transferObject.mailFrom());
            variables.putIfAbsent("subject", transferObject.mailSubject());
            variables.putIfAbsent("path", resolveTagPath(recognitionContext, transferObject));
            variables.putIfAbsent("mailFolder", transferObject.mailFolder());
            variables.putIfAbsent("mimeType", transferObject.mimeType());
            variables.putIfAbsent("attributes", transferObject.fileMeta());
            variables.putIfAbsent("tags", java.util.Arrays.asList());
        }
        variables.putIfAbsent("productMatchRules", normalizeProductMatchRules(productMatchRules));
        variables.putIfAbsent("tagMeta", tagMeta == null ? java.util.Collections.emptyMap() : tagMeta);
        variables.putIfAbsent("previewRows", resolvePreviewRows(variables));
        return variables;
    }

    private List<Map<String, Object>> normalizeProductMatchRules(List<ProductMatchRule> productMatchRules) {
        if (productMatchRules == null || productMatchRules.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Map<String, Object>> values = new ArrayList<>(productMatchRules.size());
        for (ProductMatchRule rule : productMatchRules) {
            if (rule == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rule.getId());
            item.put("fileTypeName", rule.getFileTypeName());
            item.put("pdCd", rule.getPdCd());
            item.put("pdNm", rule.getPdNm());
            item.put("orgCd", rule.getOrgCd());
            item.put("orgNm", rule.getOrgNm());
            item.put("pdType", rule.getPdType());
            item.put("fileType", rule.getFileType());
            item.put("matchRules", rule.getMatchRules());
            item.put("matchKeywords", parseMatchKeywords(rule.getMatchRules()));
            item.put("jobName", rule.getJobName());
            item.put("jobScene", rule.getJobScene());
            values.add(item);
        }
        return values;
    }

    private List<String> parseMatchKeywords(String matchRules) {
        String text = normalizeText(matchRules);
        if (text.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String normalized = text
                .replace("(.*)", "|")
                .replace(".*", "|")
                .replace(".xlsx?", "|")
                .replace("\\.xlsx?", "|")
                .replace(".xls?", "|")
                .replace("\\.xls?", "|")
                .replace(".xlsx", "|")
                .replace("\\.xlsx", "|")
                .replace(".xls", "|")
                .replace("\\.xls", "|")
                .replace("^", "|")
                .replace("$", "|")
                .replace("(", "|")
                .replace(")", "|");
        List<String> keywords = new ArrayList<>();
        for (String item : normalized.split("[|,;\\n]")) {
            String keyword = normalizeText(item);
            if (keyword.isEmpty() || "?".equals(keyword) || "*".equals(keyword) || ".".equals(keyword)) {
                continue;
            }
            keywords.add(keyword);
        }
        if (keywords.isEmpty()) {
            keywords.add(text);
        }
        return keywords;
    }

    private List<List<String>> resolvePreviewRows(Map<String, Object> variables) {
        Object existingPreviewRows = variables.get("previewRows");
        if (existingPreviewRows instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<List<String>> rows = (List<List<String>>) existingPreviewRows;
            return rows;
        }
        String source = firstNonBlank(asString(variables.get("filePath")), asString(variables.get("path")));
        if (source.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String fileName = firstNonBlank(asString(variables.get("fileName")), source);
        if (!transferRuleFunctions.isExcelFile(fileName) && !transferRuleFunctions.isCsvFile(fileName)) {
            return java.util.Collections.emptyList();
        }
        try {
            return transferRuleFunctions.isCsvFile(fileName)
                    ? transferRuleFunctions.readCsvDataWithin(source, previewScanLimit(variables.get("tagMeta")))
                    : transferRuleFunctions.readExcelDataWithin(source, previewScanLimit(variables.get("tagMeta")));
        } catch (RuntimeException exception) {
            log.warn("文件预览行读取失败，继续按空预览执行脚本，source={}", source, exception);
            return java.util.Collections.emptyList();
        }
    }

    private int previewScanLimit(Object tagMeta) {
        if (tagMeta instanceof Map<?, ?>) {
            Object scanLimit = ((Map<?, ?>) tagMeta).get("scanLimit");
            if (scanLimit instanceof Number) {
                return ((Number) scanLimit).intValue();
            }
            if (scanLimit != null) {
                try {
                    return Integer.parseInt(String.valueOf(scanLimit));
                } catch (NumberFormatException ignored) {
                    return 100;
                }
            }
        }
        return 100;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<ProductMatchRule> loadProductMatchRules() {
        try {
            List<ProductMatchRule> rules = productMatchRuleGateway.listEnabledRules();
            return rules == null ? java.util.Collections.emptyList() : rules;
        } catch (RuntimeException exception) {
            log.warn("产品识别规则加载失败，继续执行其他标签规则", exception);
            return java.util.Collections.emptyList();
        }
    }

    private String resultText(Map<String, Object> result, String key) {
        if (result == null || result.isEmpty() || key == null) {
            return null;
        }
        Object value = result.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private void mergeScriptResult(Map<String, Object> snapshot, Map<String, Object> scriptResult) {
        if (snapshot == null || scriptResult == null || scriptResult.isEmpty()) {
            return;
        }
        Object scriptSnapshot = scriptResult.get("snapshot");
        if (scriptSnapshot instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) scriptSnapshot).entrySet()) {
                if (entry != null && entry.getKey() != null) {
                    snapshot.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        for (Map.Entry<String, Object> entry : scriptResult.entrySet()) {
            if (entry == null || entry.getKey() == null || isReservedScriptResultKey(entry.getKey())) {
                continue;
            }
            snapshot.put(entry.getKey(), entry.getValue());
        }
    }

    private boolean isReservedScriptResultKey(String key) {
        return "matched".equals(key)
                || "message".equals(key)
                || "tagValue".equals(key)
                || "matchedField".equals(key)
                || "matchedValue".equals(key)
                || "snapshot".equals(key)
                || "routes".equals(key);
    }

    private RecognitionContext toRecognitionContext(TransferObject transferObject) {
        if (transferObject == null) {
            return null;
        }
        Map<String, Object> fileMeta = transferObject.fileMeta() == null ? java.util.Collections.emptyMap() : transferObject.fileMeta();
        return new RecognitionContext(
                parseSourceType(transferObject.sourceType()),
                transferObject.sourceCode(),
                transferObject.originalName(),
                transferObject.mimeType(),
                transferObject.sizeBytes(),
                transferObject.mailFrom(),
                transferObject.mailTo(),
                transferObject.mailCc(),
                transferObject.mailBcc(),
                transferObject.mailSubject(),
                transferObject.mailBody(),
                transferObject.mailId(),
                transferObject.mailProtocol(),
                transferObject.mailFolder(),
                transferObject.localTempPath(),
                fileMeta
        );
    }

    private String resolveTagPath(RecognitionContext recognitionContext, TransferObject transferObject) {
        if (transferObject != null && transferObject.localTempPath() != null && !transferObject.localTempPath().trim().isEmpty()) {
            return transferObject.localTempPath();
        }
        if (recognitionContext != null && recognitionContext.path() != null && !recognitionContext.path().trim().isEmpty()) {
            return recognitionContext.path();
        }
        return null;
    }

    private RecognitionContext resolveTaggingContext(RecognitionContext recognitionContext, TransferObject transferObject) {
        if (transferObject == null) {
            return recognitionContext;
        }
        return new RecognitionContext(
                recognitionContext == null ? null : recognitionContext.sourceType(),
                recognitionContext == null ? null : recognitionContext.sourceCode(),
                recognitionContext == null ? transferObject.originalName() : recognitionContext.fileName(),
                recognitionContext == null ? transferObject.mimeType() : recognitionContext.mimeType(),
                recognitionContext == null ? transferObject.sizeBytes() : recognitionContext.fileSize(),
                recognitionContext == null ? transferObject.mailFrom() : recognitionContext.sender(),
                recognitionContext == null ? transferObject.mailTo() : recognitionContext.recipientsTo(),
                recognitionContext == null ? transferObject.mailCc() : recognitionContext.recipientsCc(),
                recognitionContext == null ? transferObject.mailBcc() : recognitionContext.recipientsBcc(),
                recognitionContext == null ? transferObject.mailSubject() : recognitionContext.subject(),
                recognitionContext == null ? transferObject.mailBody() : recognitionContext.body(),
                recognitionContext == null ? transferObject.mailId() : recognitionContext.mailId(),
                recognitionContext == null ? transferObject.mailProtocol() : recognitionContext.mailProtocol(),
                recognitionContext == null ? transferObject.mailFolder() : recognitionContext.mailFolder(),
                resolveTagPath(recognitionContext, transferObject),
                transferObject.fileMeta()
        );
    }

    private RegexMatchResult evaluateRegex(TransferTagDefinition definition,
                                           RecognitionContext recognitionContext,
                                           TransferObject transferObject) {
        String pattern = normalizeText(definition.regexPattern());
        if (pattern.trim().isEmpty()) {
            return RegexMatchResult.miss("正则配置为空");
        }
        String candidate = firstNonBlank(
                recognitionContext == null ? null : recognitionContext.fileName(),
                transferObject == null ? null : transferObject.originalName(),
                recognitionContext == null ? null : recognitionContext.subject(),
                transferObject == null ? null : transferObject.mailSubject(),
                recognitionContext == null ? null : recognitionContext.sourceCode(),
                transferObject == null ? null : transferObject.sourceCode()
        );
        if (candidate.trim().isEmpty()) {
            return RegexMatchResult.miss("正则候选值为空");
        }
        boolean matched = Pattern.compile(pattern).matcher(candidate).find();
        return matched
                ? RegexMatchResult.hit("fileName", candidate, "正则命中")
                : RegexMatchResult.miss("正则未命中");
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private SourceType parseSourceType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return SourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            return null;
        }
    }

    private static final class TagEvaluation {
        private final boolean matched;
        private final boolean matchedByScript;
        private final boolean matchedByRegex;
        private final String message;
        private final String matchedField;
        private final String matchedValue;
        private final Map<String, Object> snapshot;
        private final String tagValue;

        private TagEvaluation(boolean matched,
                              boolean matchedByScript,
                              boolean matchedByRegex,
                              String message,
                              String matchedField,
                              String matchedValue,
                              Map<String, Object> snapshot,
                              String tagValue) {
            this.matched = matched;
            this.matchedByScript = matchedByScript;
            this.matchedByRegex = matchedByRegex;
            this.message = message;
            this.matchedField = matchedField;
            this.matchedValue = matchedValue;
            this.snapshot = snapshot;
            this.tagValue = tagValue;
        }

        static TagEvaluation miss(String message) {
            return new TagEvaluation(false, false, false, message, null, null, java.util.Collections.emptyMap(), null);
        }

        boolean matched() {
            return matched;
        }

        boolean matchedByScript() {
            return matchedByScript;
        }

        boolean matchedByRegex() {
            return matchedByRegex;
        }

        String message() {
            return message;
        }

        String matchedField() {
            return matchedField;
        }

        String matchedValue() {
            return matchedValue;
        }

        Map<String, Object> snapshot() {
            return snapshot;
        }

        String tagValue() {
            return tagValue;
        }
    }

    private static final class RegexMatchResult {
        private final boolean matched;
        private final String matchedField;
        private final String matchedValue;
        private final String message;

        private RegexMatchResult(boolean matched, String matchedField, String matchedValue, String message) {
            this.matched = matched;
            this.matchedField = matchedField;
            this.matchedValue = matchedValue;
            this.message = message;
        }

        static RegexMatchResult hit(String field, String value, String message) {
            return new RegexMatchResult(true, field, value, message);
        }

        static RegexMatchResult miss(String message) {
            return new RegexMatchResult(false, null, null, message);
        }

        boolean matched() {
            return matched;
        }

        String matchedField() {
            return matchedField;
        }

        String matchedValue() {
            return matchedValue;
        }

        String message() {
            return message;
        }
    }
}
