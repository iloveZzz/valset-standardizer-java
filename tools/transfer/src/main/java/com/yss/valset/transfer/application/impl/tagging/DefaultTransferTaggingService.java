package com.yss.valset.transfer.application.impl.tagging;

import com.yss.valset.transfer.application.command.TransferTagTestCommand;
import com.yss.valset.transfer.application.dto.TransferTagTestResultDTO;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 默认文件对象标签服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferTaggingService implements TransferTaggingUseCase {

    private final TransferTagGateway transferTagGateway;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final RuleEngine ruleEngine;

    @Override
    public List<TransferObjectTag> tag(TransferObject transferObject, RecognitionContext recognitionContext, ProbeResult probeResult) {
        if (transferObject == null || transferObject.transferId() == null) {
            return List.of();
        }
        List<TransferTagDefinition> tagDefinitions = transferTagGateway.listEnabledTags();
        if (tagDefinitions.isEmpty()) {
            return List.of();
        }
        List<TransferObjectTag> tags = new ArrayList<>();
        for (TransferTagDefinition tagDefinition : tagDefinitions) {
            TagEvaluation evaluation = evaluate(tagDefinition, recognitionContext, probeResult, transferObject);
            if (!evaluation.matched()) {
                continue;
            }
            tags.add(new TransferObjectTag(
                    null,
                    transferObject.transferId(),
                    tagDefinition.tagId(),
                    tagDefinition.tagCode(),
                    tagDefinition.tagName(),
                    tagDefinition.tagValue(),
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
        return tags;
    }

    @Override
    public List<TransferObjectTag> retag(String transferId, boolean overwrite) {
        if (transferId == null || transferId.isBlank()) {
            return List.of();
        }
        if (overwrite) {
            transferObjectTagGateway.deleteByTransferId(transferId);
        }
        return List.of();
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
                command == null || command.getAttributes() == null ? Map.of() : command.getAttributes()
        );
        TagEvaluation evaluation = evaluate(definition, recognitionContext, null, null);
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
                                   TransferObject transferObject) {
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
        String message = "标签未命中";
        if (strategy.contains("SCRIPT")) {
            RuleEvaluationResult result = ruleEngine.evaluate(buildRuleDefinition(definition), new RuleContext(recognitionContext, probeResult, buildVariables(definition, transferObject)));
            scriptMatched = result != null && result.matched();
            message = result == null ? "脚本未返回结果" : result.message();
        }
        if (strategy.contains("REGEX")) {
            RegexMatchResult regexResult = evaluateRegex(definition, recognitionContext, transferObject);
            regexMatched = regexResult.matched();
            matchedField = regexResult.matchedField();
            matchedValue = regexResult.matchedValue();
            if (regexResult.message() != null && !regexResult.message().isBlank()) {
                message = regexResult.message();
            }
        }
        boolean matched = switch (strategy) {
            case "" -> scriptMatched || regexMatched;
            case "SCRIPT_RULE" -> scriptMatched;
            case "REGEX_RULE" -> regexMatched;
            case "SCRIPT_AND_REGEX" -> scriptMatched && regexMatched;
            case "SCRIPT_OR_REGEX" -> scriptMatched || regexMatched;
            default -> scriptMatched || regexMatched;
        };
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("tagCode", definition.tagCode());
        snapshot.put("tagName", definition.tagName());
        snapshot.put("tagValue", definition.tagValue());
        snapshot.put("sourceType", recognitionContext == null ? null : recognitionContext.sourceType());
        snapshot.put("sourceCode", recognitionContext == null ? null : recognitionContext.sourceCode());
        snapshot.put("fileName", recognitionContext == null ? null : recognitionContext.fileName());
        snapshot.put("sender", recognitionContext == null ? null : recognitionContext.sender());
        snapshot.put("subject", recognitionContext == null ? null : recognitionContext.subject());
        snapshot.put("path", recognitionContext == null ? null : recognitionContext.path());
        snapshot.put("probeResult", probeResult);
        snapshot.put("transferId", transferObject == null ? null : transferObject.transferId());
        return new TagEvaluation(matched, scriptMatched, regexMatched, message, matchedField, matchedValue, snapshot);
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
        if (scriptBody == null || scriptBody.isBlank()) {
            return scriptBody;
        }
        String normalized = scriptBody.trim();
        if ((normalized.contains("fn.isValuationTableByMeta(source, tagMeta)")
                || normalized.contains("isValuationTableByMeta(source, tagMeta)"))
                && (normalized.contains("var source =")
                || normalized.contains("String(filePath)")
                || normalized.contains("String(source)")
                || normalized.contains("filePath.trim()")
                || normalized.contains("source.trim()"))) {
            return """
                    String source = hasText(filePath) ? filePath : path;
                    if (!hasText(source)) {
                        return false;
                    }
                    if (!(isExcelFile(source) || isCsvFile(source))) {
                        return false;
                    }
                    return isValuationTableByMeta(source, tagMeta);
                    """;
        }
        return scriptBody;
    }

    private Map<String, Object> buildVariables(TransferTagDefinition definition, TransferObject transferObject) {
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
            variables.putIfAbsent("path", transferObject.localTempPath());
            variables.putIfAbsent("mailFolder", transferObject.mailFolder());
            variables.putIfAbsent("mimeType", transferObject.mimeType());
            variables.putIfAbsent("attributes", transferObject.fileMeta());
            variables.putIfAbsent("tags", List.of());
        }
        return variables;
    }

    private RegexMatchResult evaluateRegex(TransferTagDefinition definition,
                                           RecognitionContext recognitionContext,
                                           TransferObject transferObject) {
        String pattern = normalizeText(definition.regexPattern());
        if (pattern.isBlank()) {
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
        if (candidate.isBlank()) {
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
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private SourceType parseSourceType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            return null;
        }
    }

    private record TagEvaluation(
            boolean matched,
            boolean matchedByScript,
            boolean matchedByRegex,
            String message,
            String matchedField,
            String matchedValue,
            Map<String, Object> snapshot
    ) {
        static TagEvaluation miss(String message) {
            return new TagEvaluation(false, false, false, message, null, null, Map.of());
        }
    }

    private record RegexMatchResult(boolean matched, String matchedField, String matchedValue, String message) {
        static RegexMatchResult hit(String field, String value, String message) {
            return new RegexMatchResult(true, field, value, message);
        }

        static RegexMatchResult miss(String message) {
            return new RegexMatchResult(false, null, null, message);
        }
    }
}
