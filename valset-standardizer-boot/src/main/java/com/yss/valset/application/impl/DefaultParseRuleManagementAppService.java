package com.yss.valset.application.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseRuleCaseUpsertCommand;
import com.yss.valset.application.command.ParseRuleDefinitionUpsertCommand;
import com.yss.valset.application.command.ParseRuleProfileUpsertCommand;
import com.yss.valset.application.command.ParseRulePublishCommand;
import com.yss.valset.application.command.ParseRuleRollbackCommand;
import com.yss.valset.application.dto.ParseRuleBundleViewDTO;
import com.yss.valset.application.dto.ParseRuleCaseViewDTO;
import com.yss.valset.application.dto.ParseRuleDefinitionViewDTO;
import com.yss.valset.application.dto.ParseRuleMutationResponse;
import com.yss.valset.application.dto.ParseRuleProfileViewDTO;
import com.yss.valset.application.dto.ParseRulePublishLogViewDTO;
import com.yss.valset.application.dto.ParseRuleValidationViewDTO;
import com.yss.valset.application.service.ParseRuleManagementAppService;
import com.yss.valset.extract.repository.entity.ParseRuleCasePO;
import com.yss.valset.extract.repository.entity.ParseRuleDefinitionPO;
import com.yss.valset.extract.repository.entity.ParseRuleProfilePO;
import com.yss.valset.extract.repository.entity.ParseRulePublishLogPO;
import com.yss.valset.extract.repository.mapper.ParseRuleCaseRepository;
import com.yss.valset.extract.repository.mapper.ParseRuleDefinitionRepository;
import com.yss.valset.extract.repository.mapper.ParseRuleProfileRepository;
import com.yss.valset.extract.repository.mapper.ParseRulePublishLogRepository;
import com.yss.valset.extract.standardization.ExternalValuationStandardizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 解析模板管理默认实现。
 */
@Slf4j
@Service
public class DefaultParseRuleManagementAppService implements ParseRuleManagementAppService {

    private static final int DEFAULT_LIMIT = 50;

    private final ParseRuleProfileRepository profileRepository;
    private final ParseRuleDefinitionRepository definitionRepository;
    private final ParseRuleCaseRepository caseRepository;
    private final ParseRulePublishLogRepository publishLogRepository;
    private final ObjectMapper objectMapper;
    private final ExternalValuationStandardizationService standardizationService;

    public DefaultParseRuleManagementAppService(ParseRuleProfileRepository profileRepository,
                                                ParseRuleDefinitionRepository definitionRepository,
                                                ParseRuleCaseRepository caseRepository,
                                                ParseRulePublishLogRepository publishLogRepository,
                                                ObjectMapper objectMapper,
                                                ExternalValuationStandardizationService standardizationService) {
        this.profileRepository = profileRepository;
        this.definitionRepository = definitionRepository;
        this.caseRepository = caseRepository;
        this.publishLogRepository = publishLogRepository;
        this.objectMapper = objectMapper;
        this.standardizationService = standardizationService;
    }

    @Override
    public List<ParseRuleProfileViewDTO> listProfiles(String status, String profileCode, Integer limit) {
        List<ParseRuleProfilePO> profiles = profileRepository.selectList(
                        Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                                .orderByDesc(ParseRuleProfilePO::getModifyTime)
                                .orderByDesc(ParseRuleProfilePO::getId)
                ).stream()
                .filter(profile -> status == null || status.isBlank() || Objects.equals(normalize(status), normalize(profile.getStatus())))
                .filter(profile -> profileCode == null || profileCode.isBlank() || containsIgnoreCase(profile.getProfileCode(), profileCode))
                .limit(limit == null || limit <= 0 ? DEFAULT_LIMIT : limit)
                .toList();
        if (profiles.isEmpty()) {
            return List.of();
        }
        return profiles.stream().map(this::toProfileView).toList();
    }

    @Override
    public ParseRuleBundleViewDTO getProfile(Long profileId) {
        ParseRuleProfilePO profile = getProfilePO(profileId);
        return ParseRuleBundleViewDTO.builder()
                .profile(toProfileView(profile))
                .rules(loadRules(profileId).stream().map(this::toRuleView).toList())
                .cases(loadCases(profileId).stream().map(this::toCaseView).toList())
                .publishLogs(loadPublishLogs(profileId).stream().map(this::toPublishLogView).toList())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseRuleMutationResponse upsertProfile(ParseRuleProfileUpsertCommand command) {
        ParseRuleProfilePO profile = upsertProfilePO(command);
        replaceRules(profile.getId(), command == null ? List.of() : command.getRules());
        replaceCases(profile.getId(), command == null ? List.of() : command.getCases());
        standardizationService.refreshDictionaryCache();
        ParseRuleValidationViewDTO validation = validateProfile(profile.getId());
        return ParseRuleMutationResponse.builder()
                .profile(toProfileView(profile))
                .validation(validation)
                .build();
    }

    @Override
    public ParseRuleValidationViewDTO validateProfile(Long profileId) {
        ParseRuleProfilePO profile = getProfilePO(profileId);
        List<ParseRuleDefinitionPO> rules = loadRules(profileId);
        List<ParseRuleCasePO> cases = loadCases(profileId);
        List<String> issues = new ArrayList<>();

        if (isBlank(profile.getProfileCode())) {
            issues.add("模板编码不能为空");
        }
        if (isBlank(profile.getProfileName())) {
            issues.add("模板名称不能为空");
        }
        if (isBlank(profile.getVersion())) {
            issues.add("模板版本不能为空");
        }
        if (rules.isEmpty()) {
            issues.add("模板至少需要一条规则步骤");
        }
        long enabledRuleCount = rules.stream().filter(rule -> !Boolean.FALSE.equals(rule.getEnabled())).count();
        if (enabledRuleCount == 0) {
            issues.add("模板至少需要一条启用规则");
        }
        if (hasDuplicateStepName(rules)) {
            issues.add("规则步骤名称存在重复");
        }
        for (ParseRuleDefinitionPO rule : rules) {
            validateRule(rule, issues);
        }
        for (ParseRuleCasePO parseCase : cases) {
            validateCase(parseCase, issues);
        }

        return ParseRuleValidationViewDTO.builder()
                .profileId(profile.getId())
                .profileCode(profile.getProfileCode())
                .version(profile.getVersion())
                .publishable(issues.isEmpty())
                .ruleCount(rules.size())
                .enabledRuleCount((int) enabledRuleCount)
                .caseCount(cases.size())
                .issues(issues)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseRuleMutationResponse publishProfile(Long profileId, ParseRulePublishCommand command) {
        ParseRuleProfilePO profile = getProfilePO(profileId);
        ParseRuleValidationViewDTO validation = validateProfile(profileId);
        if (!Boolean.TRUE.equals(validation.getPublishable())) {
            throw new ResponseStatusException(BAD_REQUEST, "模板校验未通过，无法发布: " + String.join("; ", validation.getIssues()));
        }
        String publisher = command == null || isBlank(command.getPublisher()) ? "system" : command.getPublisher().trim();
        String publishComment = command == null ? null : command.getPublishComment();
        LocalDateTime now = LocalDateTime.now();
        archivePublishedProfiles(profile.getProfileCode(), profile.getId(), now);
        profile.setStatus("PUBLISHED");
        profile.setPublishedTime(now);
        profile.setModifyTime(now);
        profileRepository.updateById(profile);

        ParseRulePublishLogPO publishLog = buildPublishLog(profile, "PUBLISHED", publisher, publishComment, validation, null);
        publishLogRepository.insert(publishLog);
        standardizationService.refreshDictionaryCache();
        return ParseRuleMutationResponse.builder()
                .profile(toProfileView(profile))
                .validation(validation)
                .publishLog(toPublishLogView(publishLog))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseRuleMutationResponse rollbackProfile(Long profileId, ParseRuleRollbackCommand command) {
        ParseRuleProfilePO current = getProfilePO(profileId);
        ParseRuleProfilePO target = resolveRollbackTarget(current, command);
        if (target == null) {
            throw new ResponseStatusException(BAD_REQUEST, "未找到可回滚的目标版本");
        }
        String publisher = command == null || isBlank(command.getPublisher()) ? "system" : command.getPublisher().trim();
        String publishComment = command == null ? null : command.getPublishComment();
        LocalDateTime now = LocalDateTime.now();

        current.setStatus("ARCHIVED");
        current.setModifyTime(now);
        profileRepository.updateById(current);

        target.setStatus("PUBLISHED");
        target.setPublishedTime(now);
        target.setModifyTime(now);
        profileRepository.updateById(target);

        ParseRuleValidationViewDTO validation = validateProfile(target.getId());
        ParseRulePublishLogPO publishLog = buildPublishLog(target, "ROLLED_BACK", publisher, publishComment, validation, current.getVersion());
        publishLogRepository.insert(publishLog);
        standardizationService.refreshDictionaryCache();
        return ParseRuleMutationResponse.builder()
                .profile(toProfileView(target))
                .validation(validation)
                .publishLog(toPublishLogView(publishLog))
                .build();
    }

    private ParseRuleProfilePO upsertProfilePO(ParseRuleProfileUpsertCommand command) {
        if (command == null) {
            throw new ResponseStatusException(BAD_REQUEST, "模板请求不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        ParseRuleProfilePO profile = command.getId() == null ? new ParseRuleProfilePO() : getProfilePO(command.getId());
        profile.setProfileCode(command.getProfileCode());
        profile.setProfileName(command.getProfileName());
        profile.setVersion(command.getVersion());
        profile.setFileScene(command.getFileScene());
        profile.setFileTypeName(command.getFileTypeName());
        profile.setSourceChannel(command.getSourceChannel());
        profile.setStatus(isBlank(command.getStatus()) ? "DRAFT" : command.getStatus().trim().toUpperCase(Locale.ROOT));
        profile.setPriority(command.getPriority());
        profile.setMatchExpr(command.getMatchExpr());
        profile.setHeaderExpr(command.getHeaderExpr());
        profile.setRowClassifyExpr(command.getRowClassifyExpr());
        profile.setFieldMapExpr(command.getFieldMapExpr());
        profile.setTransformExpr(command.getTransformExpr());
        profile.setTraceEnabled(command.getTraceEnabled());
        profile.setTimeoutMs(command.getTimeoutMs());
        profile.setChecksum(buildChecksum(command));
        profile.setModifier(firstNonBlank(command.getModifiedBy(), command.getCreatedBy(), "system"));
        profile.setModifyTime(now);
        if (command.getId() == null) {
            profile.setCreater(firstNonBlank(command.getCreatedBy(), command.getModifiedBy(), "system"));
            profile.setCreateTime(now);
            profile.setPublishedTime("PUBLISHED".equalsIgnoreCase(profile.getStatus()) ? now : null);
            profileRepository.insert(profile);
        } else {
            profileRepository.updateById(profile);
        }
        return profile;
    }

    private void replaceRules(Long profileId, List<ParseRuleDefinitionUpsertCommand> commands) {
        definitionRepository.delete(Wrappers.lambdaQuery(ParseRuleDefinitionPO.class)
                .eq(ParseRuleDefinitionPO::getProfileId, profileId));
        if (commands == null || commands.isEmpty()) {
            return;
        }
        List<ParseRuleDefinitionPO> definitions = commands.stream()
                .map(command -> toRulePO(profileId, command))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ParseRuleDefinitionPO::getPriority, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (!definitions.isEmpty()) {
            definitionRepository.insert(definitions);
        }
    }

    private void replaceCases(Long profileId, List<ParseRuleCaseUpsertCommand> commands) {
        caseRepository.delete(Wrappers.lambdaQuery(ParseRuleCasePO.class)
                .eq(ParseRuleCasePO::getProfileId, profileId));
        if (commands == null || commands.isEmpty()) {
            return;
        }
        List<ParseRuleCasePO> cases = commands.stream()
                .map(command -> toCasePO(profileId, command))
                .filter(Objects::nonNull)
                .toList();
        if (!cases.isEmpty()) {
            caseRepository.insert(cases);
        }
    }

    private List<ParseRuleDefinitionPO> loadRules(Long profileId) {
        return definitionRepository.selectList(Wrappers.lambdaQuery(ParseRuleDefinitionPO.class)
                        .eq(ParseRuleDefinitionPO::getProfileId, profileId)
                        .orderByAsc(ParseRuleDefinitionPO::getPriority)
                        .orderByAsc(ParseRuleDefinitionPO::getId))
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ParseRuleCasePO> loadCases(Long profileId) {
        return caseRepository.selectList(Wrappers.lambdaQuery(ParseRuleCasePO.class)
                        .eq(ParseRuleCasePO::getProfileId, profileId)
                        .orderByAsc(ParseRuleCasePO::getId))
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ParseRulePublishLogPO> loadPublishLogs(Long profileId) {
        return publishLogRepository.selectList(Wrappers.lambdaQuery(ParseRulePublishLogPO.class)
                        .eq(ParseRulePublishLogPO::getProfileId, profileId)
                        .orderByDesc(ParseRulePublishLogPO::getPublishTime)
                        .orderByDesc(ParseRulePublishLogPO::getId))
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private ParseRuleProfilePO getProfilePO(Long profileId) {
        if (profileId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "profileId 不能为空");
        }
        ParseRuleProfilePO profile = profileRepository.selectById(profileId);
        if (profile == null) {
            throw new ResponseStatusException(NOT_FOUND, "未找到模板: " + profileId);
        }
        return profile;
    }

    private ParseRuleProfileViewDTO toProfileView(ParseRuleProfilePO profile) {
        List<ParseRuleDefinitionPO> rules = loadRules(profile.getId());
        List<ParseRuleCasePO> cases = loadCases(profile.getId());
        return ParseRuleProfileViewDTO.builder()
                .id(profile.getId())
                .profileCode(profile.getProfileCode())
                .profileName(profile.getProfileName())
                .version(profile.getVersion())
                .fileScene(profile.getFileScene())
                .fileTypeName(profile.getFileTypeName())
                .sourceChannel(profile.getSourceChannel())
                .status(profile.getStatus())
                .priority(profile.getPriority())
                .matchExpr(profile.getMatchExpr())
                .headerExpr(profile.getHeaderExpr())
                .rowClassifyExpr(profile.getRowClassifyExpr())
                .fieldMapExpr(profile.getFieldMapExpr())
                .transformExpr(profile.getTransformExpr())
                .traceEnabled(profile.getTraceEnabled())
                .timeoutMs(profile.getTimeoutMs())
                .checksum(profile.getChecksum())
                .publishedTime(profile.getPublishedTime())
                .ruleCount(rules.size())
                .caseCount(cases.size())
                .build();
    }

    private ParseRuleDefinitionViewDTO toRuleView(ParseRuleDefinitionPO rule) {
        return ParseRuleDefinitionViewDTO.builder()
                .id(rule.getId())
                .profileId(rule.getProfileId())
                .ruleType(rule.getRuleType())
                .stepName(rule.getStepName())
                .priority(rule.getPriority())
                .enabled(rule.getEnabled())
                .exprText(rule.getExprText())
                .exprLang(rule.getExprLang())
                .inputSchemaJson(rule.getInputSchemaJson())
                .outputSchemaJson(rule.getOutputSchemaJson())
                .errorPolicy(rule.getErrorPolicy())
                .timeoutMs(rule.getTimeoutMs())
                .build();
    }

    private ParseRuleCaseViewDTO toCaseView(ParseRuleCasePO parseCase) {
        return ParseRuleCaseViewDTO.builder()
                .id(parseCase.getId())
                .profileId(parseCase.getProfileId())
                .sampleFileId(parseCase.getSampleFileId())
                .sampleFileName(parseCase.getSampleFileName())
                .expectedSheetName(parseCase.getExpectedSheetName())
                .expectedHeaderRow(parseCase.getExpectedHeaderRow())
                .expectedDataStartRow(parseCase.getExpectedDataStartRow())
                .expectedSubjectCount(parseCase.getExpectedSubjectCount())
                .expectedMetricCount(parseCase.getExpectedMetricCount())
                .expectedOutputHash(parseCase.getExpectedOutputHash())
                .status(parseCase.getStatus())
                .build();
    }

    private ParseRulePublishLogViewDTO toPublishLogView(ParseRulePublishLogPO publishLog) {
        return ParseRulePublishLogViewDTO.builder()
                .id(publishLog.getId())
                .profileId(publishLog.getProfileId())
                .version(publishLog.getVersion())
                .publishStatus(publishLog.getPublishStatus())
                .publishTime(publishLog.getPublishTime())
                .publisher(publishLog.getPublisher())
                .publishComment(publishLog.getPublishComment())
                .validationResultJson(publishLog.getValidationResultJson())
                .rollbackFromVersion(publishLog.getRollbackFromVersion())
                .build();
    }

    private ParseRuleDefinitionPO toRulePO(Long profileId, ParseRuleDefinitionUpsertCommand command) {
        if (command == null) {
            return null;
        }
        ParseRuleDefinitionPO rule = new ParseRuleDefinitionPO();
        rule.setProfileId(profileId);
        rule.setRuleType(command.getRuleType());
        rule.setStepName(command.getStepName());
        rule.setPriority(command.getPriority());
        rule.setEnabled(command.getEnabled());
        rule.setExprText(command.getExprText());
        rule.setExprLang(firstNonBlank(command.getExprLang(), "qlexpress4"));
        rule.setInputSchemaJson(command.getInputSchemaJson());
        rule.setOutputSchemaJson(command.getOutputSchemaJson());
        rule.setErrorPolicy(command.getErrorPolicy());
        rule.setTimeoutMs(command.getTimeoutMs());
        rule.setCreater("system");
        rule.setCreateTime(LocalDateTime.now());
        rule.setModifier("system");
        rule.setModifyTime(LocalDateTime.now());
        return rule;
    }

    private ParseRuleCasePO toCasePO(Long profileId, ParseRuleCaseUpsertCommand command) {
        if (command == null) {
            return null;
        }
        ParseRuleCasePO parseCase = new ParseRuleCasePO();
        parseCase.setProfileId(profileId);
        parseCase.setSampleFileId(command.getSampleFileId());
        parseCase.setSampleFileName(command.getSampleFileName());
        parseCase.setExpectedSheetName(command.getExpectedSheetName());
        parseCase.setExpectedHeaderRow(command.getExpectedHeaderRow());
        parseCase.setExpectedDataStartRow(command.getExpectedDataStartRow());
        parseCase.setExpectedSubjectCount(command.getExpectedSubjectCount());
        parseCase.setExpectedMetricCount(command.getExpectedMetricCount());
        parseCase.setExpectedOutputHash(command.getExpectedOutputHash());
        parseCase.setStatus(firstNonBlank(command.getStatus(), "READY"));
        parseCase.setCreater("system");
        parseCase.setCreateTime(LocalDateTime.now());
        parseCase.setModifier("system");
        parseCase.setModifyTime(LocalDateTime.now());
        return parseCase;
    }

    private void validateRule(ParseRuleDefinitionPO rule, List<String> issues) {
        if (rule == null) {
            issues.add("存在空规则步骤");
            return;
        }
        if (rule.getRuleType() == null) {
            issues.add("规则步骤类型不能为空，stepName=" + rule.getStepName());
        }
        if (isBlank(rule.getStepName())) {
            issues.add("规则步骤名称不能为空");
        }
        if (isBlank(rule.getExprText())) {
            issues.add("规则表达式不能为空，stepName=" + rule.getStepName());
        }
    }

    private void validateCase(ParseRuleCasePO parseCase, List<String> issues) {
        if (parseCase == null) {
            issues.add("存在空样例定义");
            return;
        }
        if (parseCase.getSampleFileId() == null && isBlank(parseCase.getSampleFileName())) {
            issues.add("样例必须提供文件标识或文件名");
        }
        if (isBlank(parseCase.getExpectedSheetName())) {
            issues.add("样例预期 sheet 名不能为空");
        }
    }

    private boolean hasDuplicateStepName(List<ParseRuleDefinitionPO> rules) {
        Set<String> names = new LinkedHashSet<>();
        for (ParseRuleDefinitionPO rule : rules) {
            String name = normalize(rule == null ? null : rule.getStepName());
            if (name.isBlank()) {
                continue;
            }
            if (!names.add(name)) {
                return true;
            }
        }
        return false;
    }

    private void archivePublishedProfiles(String profileCode, Long currentProfileId, LocalDateTime now) {
        if (isBlank(profileCode)) {
            return;
        }
        List<ParseRuleProfilePO> profiles = profileRepository.selectList(Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                .eq(ParseRuleProfilePO::getProfileCode, profileCode));
        for (ParseRuleProfilePO candidate : profiles) {
            if (candidate == null || Objects.equals(candidate.getId(), currentProfileId)) {
                continue;
            }
            if (!"PUBLISHED".equalsIgnoreCase(candidate.getStatus())) {
                continue;
            }
            candidate.setStatus("ARCHIVED");
            candidate.setModifyTime(now);
            profileRepository.updateById(candidate);
        }
    }

    private ParseRuleProfilePO resolveRollbackTarget(ParseRuleProfilePO current, ParseRuleRollbackCommand command) {
        if (command != null && !isBlank(command.getRollbackToVersion())) {
            var query = Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                    .eq(ParseRuleProfilePO::getProfileCode, current.getProfileCode())
                    .eq(ParseRuleProfilePO::getVersion, command.getRollbackToVersion())
                    .orderByDesc(ParseRuleProfilePO::getPublishedTime)
                    .orderByDesc(ParseRuleProfilePO::getId);
            return profileRepository.selectList(query)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
        var query = Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                .eq(ParseRuleProfilePO::getProfileCode, current.getProfileCode())
                .ne(ParseRuleProfilePO::getId, current.getId())
                .orderByDesc(ParseRuleProfilePO::getPublishedTime)
                .orderByDesc(ParseRuleProfilePO::getId);
        return profileRepository.selectList(query)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ParseRulePublishLogPO buildPublishLog(ParseRuleProfilePO profile,
                                                  String publishStatus,
                                                  String publisher,
                                                  String publishComment,
                                                  ParseRuleValidationViewDTO validation,
                                                  String rollbackFromVersion) {
        ParseRulePublishLogPO publishLog = new ParseRulePublishLogPO();
        publishLog.setProfileId(profile.getId());
        publishLog.setVersion(profile.getVersion());
        publishLog.setPublishStatus(publishStatus);
        publishLog.setPublishTime(LocalDateTime.now());
        publishLog.setPublisher(publisher);
        publishLog.setPublishComment(publishComment);
        publishLog.setRollbackFromVersion(rollbackFromVersion);
        try {
            publishLog.setValidationResultJson(objectMapper.writeValueAsString(validation));
        } catch (Exception exception) {
            publishLog.setValidationResultJson("{\"error\":\"validation serialization failed\"}");
        }
        return publishLog;
    }

    private String buildChecksum(ParseRuleProfileUpsertCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = objectMapper.writeValueAsString(command);
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception exception) {
            return null;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (isBlank(value) || isBlank(keyword)) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
