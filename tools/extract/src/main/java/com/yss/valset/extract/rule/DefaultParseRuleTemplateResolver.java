package com.yss.valset.extract.rule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.extract.repository.entity.ParseRuleProfilePO;
import com.yss.valset.extract.repository.mapper.ParseRuleProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 默认解析模板运行时解析器。
 */
@Slf4j
@Component
public class DefaultParseRuleTemplateResolver implements ParseRuleTemplateResolver {

    private static final List<String> FALLBACK_REQUIRED_HEADERS = List.of("科目代码", "科目名称");
    private static final Pattern FALLBACK_SUBJECT_CODE_PATTERN = Pattern.compile("^\\d{4}[A-Za-z0-9]*$");

    private final ParseRuleProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public DefaultParseRuleTemplateResolver(ParseRuleProfileRepository profileRepository, ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public ParseRuleProfilePO resolvePublishedProfile(String fileScene, String fileTypeName) {
        List<ParseRuleProfilePO> profiles = profileRepository.selectList(Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                .eq(ParseRuleProfilePO::getStatus, "PUBLISHED")
                .eq(fileScene != null && !fileScene.isBlank(), ParseRuleProfilePO::getFileScene, fileScene.trim())
                .eq(fileTypeName != null && !fileTypeName.isBlank(), ParseRuleProfilePO::getFileTypeName, fileTypeName.trim())
                .orderByDesc(ParseRuleProfilePO::getPriority)
                .orderByDesc(ParseRuleProfilePO::getPublishedTime)
                .orderByDesc(ParseRuleProfilePO::getId));
        if (profiles == null || profiles.isEmpty()) {
            profiles = profileRepository.selectList(Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                    .eq(ParseRuleProfilePO::getStatus, "PUBLISHED")
                    .orderByDesc(ParseRuleProfilePO::getPriority)
                    .orderByDesc(ParseRuleProfilePO::getPublishedTime)
                    .orderByDesc(ParseRuleProfilePO::getId));
        }
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        ParseRuleProfilePO profile = profiles.get(0);
        log.debug("解析默认模板命中，fileScene={}, fileTypeName={}, profileCode={}, version={}",
                fileScene,
                fileTypeName,
                profile.getProfileCode(),
                profile.getVersion());
        return profile;
    }

    @Override
    public String resolveHeaderExpr(String fileScene, String fileTypeName) {
        ParseRuleProfilePO profile = resolvePublishedProfile(fileScene, fileTypeName);
        return profile == null || profile.getHeaderExpr() == null || profile.getHeaderExpr().isBlank()
                ? ParseRuleExpressions.HEADER_ROW_EXPR
                : profile.getHeaderExpr().trim();
    }

    @Override
    public String resolveRowClassifyExpr(String fileScene, String fileTypeName) {
        ParseRuleProfilePO profile = resolvePublishedProfile(fileScene, fileTypeName);
        return profile == null || profile.getRowClassifyExpr() == null || profile.getRowClassifyExpr().isBlank()
                ? ParseRuleExpressions.ROW_CLASSIFY_EXPR
                : profile.getRowClassifyExpr().trim();
    }

    @Override
    public String resolveFieldMapExpr(String fileScene, String fileTypeName) {
        ParseRuleProfilePO profile = resolvePublishedProfile(fileScene, fileTypeName);
        return profile == null || profile.getFieldMapExpr() == null || profile.getFieldMapExpr().isBlank()
                ? null
                : profile.getFieldMapExpr().trim();
    }

    @Override
    public String resolveTransformExpr(String fileScene, String fileTypeName) {
        ParseRuleProfilePO profile = resolvePublishedProfile(fileScene, fileTypeName);
        return profile == null || profile.getTransformExpr() == null || profile.getTransformExpr().isBlank()
                ? null
                : profile.getTransformExpr().trim();
    }

    @Override
    public List<String> resolveRequiredHeaders(String fileScene, String fileTypeName) {
        ParseRuleProfilePO profile = resolvePublishedProfile(fileScene, fileTypeName);
        return parseRequiredHeaders(profile);
    }

    @Override
    public Pattern resolveSubjectCodePattern(String fileScene, String fileTypeName) {
        ParseRuleProfilePO profile = resolvePublishedProfile(fileScene, fileTypeName);
        return parseSubjectCodePattern(profile);
    }

    private List<String> parseRequiredHeaders(ParseRuleProfilePO profile) {
        if (profile == null || profile.getRequiredHeadersJson() == null || profile.getRequiredHeadersJson().isBlank()) {
            return FALLBACK_REQUIRED_HEADERS;
        }
        try {
            List<String> requiredHeaders = objectMapper.readValue(profile.getRequiredHeadersJson(), new TypeReference<List<String>>() {
            });
            List<String> normalized = requiredHeaders == null ? List.of() : requiredHeaders.stream()
                    .filter(header -> header != null && !header.isBlank())
                    .map(String::trim)
                    .toList();
            return normalized.isEmpty() ? FALLBACK_REQUIRED_HEADERS : normalized;
        } catch (Exception exception) {
            log.warn("解析模板表头必选字段失败，profileCode={}, version={}, requiredHeadersJson={}",
                    profile == null ? null : profile.getProfileCode(),
                    profile == null ? null : profile.getVersion(),
                    profile == null ? null : profile.getRequiredHeadersJson(),
                    exception);
            return FALLBACK_REQUIRED_HEADERS;
        }
    }

    private Pattern parseSubjectCodePattern(ParseRuleProfilePO profile) {
        if (profile == null || profile.getSubjectCodePattern() == null || profile.getSubjectCodePattern().isBlank()) {
            return FALLBACK_SUBJECT_CODE_PATTERN;
        }
        try {
            return Pattern.compile(profile.getSubjectCodePattern().trim());
        } catch (Exception exception) {
            log.warn("解析模板科目代码正则失败，profileCode={}, version={}, subjectCodePattern={}",
                    profile == null ? null : profile.getProfileCode(),
                    profile == null ? null : profile.getVersion(),
                    profile == null ? null : profile.getSubjectCodePattern(),
                    exception);
            return FALLBACK_SUBJECT_CODE_PATTERN;
        }
    }
}
