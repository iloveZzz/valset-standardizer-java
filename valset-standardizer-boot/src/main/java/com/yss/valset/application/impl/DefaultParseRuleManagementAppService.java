package com.yss.valset.application.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseRuleCaseUpsertCommand;
import com.yss.valset.application.command.ParseRuleDefinitionUpsertCommand;
import com.yss.valset.application.command.ParseRuleProfileUpsertCommand;
import com.yss.valset.application.command.ParseRulePublishCommand;
import com.yss.valset.application.command.ParseRuleRollbackCommand;
import com.yss.valset.application.dto.ParseRuleBundleExportDTO;
import com.yss.valset.application.dto.ParseRuleBundleViewDTO;
import com.yss.valset.application.dto.ParseRuleCaseViewDTO;
import com.yss.valset.application.dto.ParseRuleDefinitionViewDTO;
import com.yss.valset.application.dto.ParseRuleMutationResponse;
import com.yss.valset.application.dto.ParseRuleProfileViewDTO;
import com.yss.valset.application.dto.ParseRuleRegressionCaseViewDTO;
import com.yss.valset.application.dto.ParseRuleRegressionViewDTO;
import com.yss.valset.application.dto.ParseRulePublishLogViewDTO;
import com.yss.valset.application.dto.ParseRuleTraceViewDTO;
import com.yss.valset.application.dto.ParseRuleValidationViewDTO;
import com.yss.valset.application.service.ParseRuleManagementAppService;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.domain.parser.ValuationDataParserProvider;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.rule.ParseRuleEngine;
import com.yss.valset.domain.rule.ParseRuleTraceContext;
import com.yss.valset.domain.rule.ParseRuleTraceContextHolder;
import com.yss.valset.extract.repository.entity.ParseRuleCasePO;
import com.yss.valset.extract.repository.entity.ParseRuleDefinitionPO;
import com.yss.valset.extract.repository.entity.ParseRuleProfilePO;
import com.yss.valset.extract.repository.entity.ParseRulePublishLogPO;
import com.yss.valset.extract.repository.entity.ParseRuleTracePO;
import com.yss.valset.extract.repository.mapper.ParseRuleCaseRepository;
import com.yss.valset.extract.repository.mapper.ParseRuleDefinitionRepository;
import com.yss.valset.extract.repository.mapper.ParseRuleProfileRepository;
import com.yss.valset.extract.repository.mapper.ParseRulePublishLogRepository;
import com.yss.valset.extract.repository.mapper.ParseRuleTraceRepository;
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
import java.util.HexFormat;
import java.util.regex.Pattern;
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
    private static final List<String> DEFAULT_REQUIRED_HEADERS = List.of("科目代码", "科目名称");
    private static final String DEFAULT_SUBJECT_CODE_PATTERN = "^\\d{4}[A-Za-z0-9]*$";

    private final ParseRuleProfileRepository profileRepository;
    private final ParseRuleDefinitionRepository definitionRepository;
    private final ParseRuleCaseRepository caseRepository;
    private final ParseRulePublishLogRepository publishLogRepository;
    private final ParseRuleTraceRepository traceRepository;
    private final ObjectMapper objectMapper;
    private final ParseRuleEngine parseRuleEngine;
    private final ExternalValuationStandardizationService standardizationService;
    private final ValsetFileInfoGateway fileInfoGateway;
    private final ValuationDataParserProvider parserProvider;

    public DefaultParseRuleManagementAppService(ParseRuleProfileRepository profileRepository,
                                                ParseRuleDefinitionRepository definitionRepository,
                                                ParseRuleCaseRepository caseRepository,
                                                ParseRulePublishLogRepository publishLogRepository,
                                                ParseRuleTraceRepository traceRepository,
                                                ObjectMapper objectMapper,
                                                ParseRuleEngine parseRuleEngine,
                                                ExternalValuationStandardizationService standardizationService,
                                                ValsetFileInfoGateway fileInfoGateway,
                                                ValuationDataParserProvider parserProvider) {
        this.profileRepository = profileRepository;
        this.definitionRepository = definitionRepository;
        this.caseRepository = caseRepository;
        this.publishLogRepository = publishLogRepository;
        this.traceRepository = traceRepository;
        this.objectMapper = objectMapper;
        this.parseRuleEngine = parseRuleEngine;
        this.standardizationService = standardizationService;
        this.fileInfoGateway = fileInfoGateway;
        this.parserProvider = parserProvider;
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
    public ParseRuleBundleExportDTO exportProfile(Long profileId) {
        ParseRuleProfilePO profile = getProfilePO(profileId);
        List<ParseRuleDefinitionPO> rules = loadRules(profileId);
        List<ParseRuleCasePO> cases = loadCases(profileId);
        List<ParseRulePublishLogPO> publishLogs = loadPublishLogs(profileId);
        return ParseRuleBundleExportDTO.builder()
                .bundleType("parse-rule-bundle")
                .bundleVersion("1")
                .exportedAt(LocalDateTime.now())
                .profile(toProfileUpsertCommand(profile))
                .rules(rules.stream().map(this::toRuleUpsertCommand).toList())
                .cases(cases.stream().map(this::toCaseUpsertCommand).toList())
                .publishLogs(publishLogs.stream().map(this::toPublishLogView).toList())
                .build();
    }

    @Override
    public ParseRuleMutationResponse importProfile(String bundleJson, Long overwriteProfileId) {
        if (isBlank(bundleJson)) {
            throw new ResponseStatusException(BAD_REQUEST, "模板包内容不能为空");
        }
        try {
            ParseRuleBundleExportDTO bundle = objectMapper.readValue(bundleJson, ParseRuleBundleExportDTO.class);
            if (bundle == null || bundle.getProfile() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "模板包缺少模板主数据");
            }
            List<String> bundleIssues = new ArrayList<>();
            validateImportedBundle(bundle, bundleIssues);
            if (!bundleIssues.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "模板包校验未通过: " + String.join("; ", bundleIssues));
            }
            ParseRuleProfileUpsertCommand command = bundle.getProfile();
            command.setId(overwriteProfileId);
            command.setRules(bundle.getRules() == null ? List.of() : bundle.getRules());
            command.setCases(bundle.getCases() == null ? List.of() : bundle.getCases());
            command.setCreatedBy(firstNonBlank(command.getCreatedBy(), "system"));
            command.setModifiedBy(firstNonBlank(command.getModifiedBy(), command.getCreatedBy(), "system"));
            if (isBlank(command.getStatus())) {
                command.setStatus("DRAFT");
            }
            normalizeBundleIds(command);
            return upsertProfile(command);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "解析模板包失败: " + exception.getMessage(), exception);
        }
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
        if (hasDuplicateProfileVersion(profile)) {
            issues.add("模板编码和版本号已存在，请修改版本号或覆盖导入");
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
    public ParseRuleRegressionViewDTO runRegression(Long profileId) {
        ParseRuleProfilePO profile = getProfilePO(profileId);
        List<ParseRuleCasePO> cases = loadCases(profileId);
        List<String> issues = new ArrayList<>();
        if (cases.isEmpty()) {
            issues.add("模板未配置样例，无法执行回归");
            return ParseRuleRegressionViewDTO.builder()
                    .profileId(profile.getId())
                    .profileCode(profile.getProfileCode())
                    .version(profile.getVersion())
                    .passed(Boolean.FALSE)
                    .totalCases(0)
                    .passedCases(0)
                    .issues(issues)
                    .caseResults(List.of())
                    .build();
        }

        List<ParseRuleRegressionCaseViewDTO> caseResults = new ArrayList<>();
        int passedCases = 0;
        for (ParseRuleCasePO parseCase : cases) {
            ParseRuleTraceContext traceContext = ParseRuleTraceContext.builder()
                    .profileId(profile.getId())
                    .profileCode(profile.getProfileCode())
                    .version(profile.getVersion())
                    .fileId(parseCase == null ? null : parseCase.getSampleFileId())
                    .taskId(null)
                    .traceEnabled(Boolean.TRUE)
                    .traceScope("REGRESSION")
                    .build();
            try (ParseRuleTraceContextHolder.TraceScope ignored = ParseRuleTraceContextHolder.withContext(traceContext)) {
                ParseRuleRegressionCaseViewDTO caseResult = runRegressionCase(parseCase, issues);
                caseResults.add(caseResult);
                if (Boolean.TRUE.equals(caseResult.getPassed())) {
                    passedCases++;
                }
            }
        }

        return ParseRuleRegressionViewDTO.builder()
                .profileId(profile.getId())
                .profileCode(profile.getProfileCode())
                .version(profile.getVersion())
                .passed(issues.isEmpty() && passedCases == cases.size())
                .totalCases(cases.size())
                .passedCases(passedCases)
                .issues(issues)
                .caseResults(caseResults)
                .build();
    }

    @Override
    public List<ParseRuleTraceViewDTO> listTraces(Long profileId, Long fileId, Long taskId, String traceType, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
        List<ParseRuleTracePO> traces = traceRepository.selectList(Wrappers.lambdaQuery(ParseRuleTracePO.class)
                        .eq(profileId != null, ParseRuleTracePO::getProfileId, profileId)
                        .eq(fileId != null, ParseRuleTracePO::getFileId, fileId)
                        .eq(taskId != null, ParseRuleTracePO::getTaskId, taskId)
                        .eq(traceType != null && !traceType.isBlank(), ParseRuleTracePO::getTraceType, traceType.trim())
                        .orderByDesc(ParseRuleTracePO::getTraceTime)
                        .orderByDesc(ParseRuleTracePO::getId)
                        .last("limit " + safeLimit))
                .stream()
                .filter(Objects::nonNull)
                .toList();
        return traces.stream().map(this::toTraceView).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseRuleMutationResponse publishProfile(Long profileId, ParseRulePublishCommand command) {
        ParseRuleProfilePO profile = getProfilePO(profileId);
        ParseRuleValidationViewDTO validation = validateProfile(profileId);
        if (!Boolean.TRUE.equals(validation.getPublishable())) {
            throw new ResponseStatusException(BAD_REQUEST, "模板校验未通过，无法发布: " + String.join("; ", validation.getIssues()));
        }
        ParseRuleRegressionViewDTO regression = runRegression(profileId);
        if (!Boolean.TRUE.equals(regression.getPassed())) {
            throw new ResponseStatusException(BAD_REQUEST, "模板样例回归未通过，无法发布: " + String.join("; ", regression.getIssues()));
        }
        String publisher = command == null || isBlank(command.getPublisher()) ? "system" : command.getPublisher().trim();
        String publishComment = command == null ? null : command.getPublishComment();
        LocalDateTime now = LocalDateTime.now();
        archivePublishedProfiles(profile.getProfileCode(), profile.getId(), now);
        profile.setStatus("PUBLISHED");
        profile.setPublishedTime(now);
        profile.setModifyTime(now);
        profileRepository.updateById(profile);

        ParseRulePublishLogPO publishLog = buildPublishLog(profile, "PUBLISHED", publisher, publishComment, validation, null, regression);
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
        ParseRulePublishLogPO publishLog = buildPublishLog(target, "ROLLED_BACK", publisher, publishComment, validation, current.getVersion(), null);
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
        profile.setRequiredHeadersJson(writeRequiredHeaders(command.getRequiredHeaders()));
        profile.setSubjectCodePattern(normalizeSubjectCodePattern(command.getSubjectCodePattern()));
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
                .requiredHeaders(normalizeRequiredHeaders(profile.getRequiredHeadersJson()))
                .subjectCodePattern(profile.getSubjectCodePattern())
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

    private ParseRuleTraceViewDTO toTraceView(ParseRuleTracePO tracePO) {
        return ParseRuleTraceViewDTO.builder()
                .id(tracePO.getId())
                .traceScope(tracePO.getTraceScope())
                .traceType(tracePO.getTraceType())
                .profileId(tracePO.getProfileId())
                .profileCode(tracePO.getProfileCode())
                .version(tracePO.getVersion())
                .fileId(tracePO.getFileId())
                .taskId(tracePO.getTaskId())
                .stepName(tracePO.getStepName())
                .expression(tracePO.getExpression())
                .inputJson(tracePO.getInputJson())
                .outputJson(tracePO.getOutputJson())
                .success(tracePO.getSuccess())
                .costMs(tracePO.getCostMs())
                .errorMessage(tracePO.getErrorMessage())
                .traceTime(tracePO.getTraceTime())
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

    private ParseRuleDefinitionUpsertCommand toRuleUpsertCommand(ParseRuleDefinitionPO rule) {
        if (rule == null) {
            return null;
        }
        ParseRuleDefinitionUpsertCommand command = new ParseRuleDefinitionUpsertCommand();
        command.setId(rule.getId());
        command.setRuleType(rule.getRuleType());
        command.setStepName(rule.getStepName());
        command.setPriority(rule.getPriority());
        command.setEnabled(rule.getEnabled());
        command.setExprText(rule.getExprText());
        command.setExprLang(rule.getExprLang());
        command.setInputSchemaJson(rule.getInputSchemaJson());
        command.setOutputSchemaJson(rule.getOutputSchemaJson());
        command.setErrorPolicy(rule.getErrorPolicy());
        command.setTimeoutMs(rule.getTimeoutMs());
        return command;
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

    private ParseRuleCaseUpsertCommand toCaseUpsertCommand(ParseRuleCasePO parseCase) {
        if (parseCase == null) {
            return null;
        }
        ParseRuleCaseUpsertCommand command = new ParseRuleCaseUpsertCommand();
        command.setId(parseCase.getId());
        command.setSampleFileId(parseCase.getSampleFileId());
        command.setSampleFileName(parseCase.getSampleFileName());
        command.setExpectedSheetName(parseCase.getExpectedSheetName());
        command.setExpectedHeaderRow(parseCase.getExpectedHeaderRow());
        command.setExpectedDataStartRow(parseCase.getExpectedDataStartRow());
        command.setExpectedSubjectCount(parseCase.getExpectedSubjectCount());
        command.setExpectedMetricCount(parseCase.getExpectedMetricCount());
        command.setExpectedOutputHash(parseCase.getExpectedOutputHash());
        command.setStatus(parseCase.getStatus());
        return command;
    }

    private ParseRuleRegressionCaseViewDTO runRegressionCase(ParseRuleCasePO parseCase, List<String> issues) {
        if (parseCase == null) {
            issues.add("存在空样例定义");
            return ParseRuleRegressionCaseViewDTO.builder()
                    .passed(Boolean.FALSE)
                    .reason("样例定义为空")
                    .build();
        }
        try {
            if (parseCase.getSampleFileId() == null) {
                issues.add("样例缺少 sampleFileId，caseId=" + parseCase.getId());
                return regressionFailure(parseCase, "缺少 sampleFileId", null, null, null, null);
            }
            ValsetFileInfo sampleFileInfo = fileInfoGateway.findById(parseCase.getSampleFileId());
            if (sampleFileInfo == null) {
                issues.add("未找到样例文件，sampleFileId=" + parseCase.getSampleFileId());
                return regressionFailure(parseCase, "未找到样例文件", null, null, null, null);
            }
            DataSourceType dataSourceType = resolveDataSourceType(sampleFileInfo);
            DataSourceConfig config = DataSourceConfig.builder()
                    .sourceType(dataSourceType)
                    .sourceUri(sampleFileInfo.getStorageUri())
                    .additionalParams(String.valueOf(sampleFileInfo.getFileId()))
                    .build();
            ValuationDataParser parser = parserProvider.getParser(dataSourceType);
            ParsedValuationData parsedValuationData = parser.parse(config);
            ParsedValuationData standardizedValuationData = standardizationService.standardize(parsedValuationData);
            String actualHash = buildRegressionHash(standardizedValuationData);
            String expectedHash = normalize(parseCase.getExpectedOutputHash());
            boolean passed = !expectedHash.isBlank() && expectedHash.equalsIgnoreCase(actualHash);
            String reason = passed ? "回归通过" : (expectedHash.isBlank() ? "缺少预期输出哈希" : "输出摘要哈希不一致");
            if (!passed) {
                issues.add("样例回归未通过，caseId=" + parseCase.getId() + ", reason=" + reason);
            }
            return ParseRuleRegressionCaseViewDTO.builder()
                    .caseId(parseCase.getId())
                    .sampleFileId(parseCase.getSampleFileId())
                    .sampleFileName(parseCase.getSampleFileName())
                    .expectedOutputHash(parseCase.getExpectedOutputHash())
                    .actualOutputHash(actualHash)
                    .passed(passed)
                    .reason(reason)
                    .expectedHeaderRow(parseCase.getExpectedHeaderRow())
                    .actualHeaderRow(standardizedValuationData.getHeaderRowNumber())
                    .expectedDataStartRow(parseCase.getExpectedDataStartRow())
                    .actualDataStartRow(standardizedValuationData.getDataStartRowNumber())
                    .expectedSubjectCount(parseCase.getExpectedSubjectCount())
                    .actualSubjectCount(standardizedValuationData.getSubjects() == null ? 0 : standardizedValuationData.getSubjects().size())
                    .expectedMetricCount(parseCase.getExpectedMetricCount())
                    .actualMetricCount(standardizedValuationData.getMetrics() == null ? 0 : standardizedValuationData.getMetrics().size())
                    .build();
        } catch (Exception exception) {
            String reason = "样例回归执行失败: " + exception.getMessage();
            issues.add(reason);
            log.warn("解析模板样例回归失败，caseId={}", parseCase.getId(), exception);
            return regressionFailure(parseCase, reason, null, null, null, null);
        }
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
            return;
        }
        if (rule.getExprLang() != null && !rule.getExprLang().isBlank() && !"qlexpress4".equalsIgnoreCase(rule.getExprLang().trim())) {
            issues.add("规则表达式语言不支持，stepName=" + rule.getStepName() + ", exprLang=" + rule.getExprLang());
        }
        if (parseRuleEngine != null) {
            try {
                parseRuleEngine.evaluate(rule.getExprText(), buildRuleValidationContext(rule));
            } catch (Exception exception) {
                issues.add("规则表达式校验失败，stepName=" + rule.getStepName() + ", reason=" + exception.getMessage());
            }
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
        if (isBlank(parseCase.getExpectedOutputHash())) {
            issues.add("样例预期输出哈希不能为空，sampleFileName=" + firstNonBlank(parseCase.getSampleFileName(), String.valueOf(parseCase.getSampleFileId())));
        }
    }

    private void validateImportedBundle(ParseRuleBundleExportDTO bundle, List<String> issues) {
        if (bundle == null) {
            issues.add("模板包不能为空");
            return;
        }
        if (!"parse-rule-bundle".equalsIgnoreCase(normalize(bundle.getBundleType()))) {
            issues.add("模板包类型不正确");
        }
        if (!"1".equalsIgnoreCase(normalize(bundle.getBundleVersion()))) {
            issues.add("模板包版本不支持");
        }
        if (bundle.getProfile() == null) {
            issues.add("模板包缺少模板主数据");
            return;
        }
        if (isBlank(bundle.getProfile().getProfileCode())) {
            issues.add("模板编码不能为空");
        }
        if (isBlank(bundle.getProfile().getProfileName())) {
            issues.add("模板名称不能为空");
        }
        if (isBlank(bundle.getProfile().getVersion())) {
            issues.add("模板版本不能为空");
        }
        List<ParseRuleDefinitionUpsertCommand> rules = bundle.getRules() == null ? List.of() : bundle.getRules();
        if (rules.isEmpty()) {
            issues.add("模板包至少需要一条规则步骤");
        }
        for (ParseRuleDefinitionUpsertCommand rule : rules) {
            if (rule == null) {
                issues.add("模板包中存在空规则步骤");
                continue;
            }
            if (rule.getRuleType() == null) {
                issues.add("模板包规则步骤类型不能为空，stepName=" + rule.getStepName());
            }
            if (isBlank(rule.getStepName())) {
                issues.add("模板包规则步骤名称不能为空");
            }
            if (isBlank(rule.getExprText())) {
                issues.add("模板包规则表达式不能为空，stepName=" + rule.getStepName());
            }
            if (rule.getExprLang() != null && !rule.getExprLang().isBlank() && !"qlexpress4".equalsIgnoreCase(rule.getExprLang().trim())) {
                issues.add("模板包规则表达式语言不支持，stepName=" + rule.getStepName() + ", exprLang=" + rule.getExprLang());
            }
        }
        List<ParseRuleCaseUpsertCommand> cases = bundle.getCases() == null ? List.of() : bundle.getCases();
        for (ParseRuleCaseUpsertCommand parseCase : cases) {
            if (parseCase == null) {
                issues.add("模板包中存在空样例定义");
                continue;
            }
            if (parseCase.getSampleFileId() == null && isBlank(parseCase.getSampleFileName())) {
                issues.add("模板包样例必须提供文件标识或文件名");
            }
            if (isBlank(parseCase.getExpectedSheetName())) {
                issues.add("模板包样例预期 sheet 名不能为空");
            }
            if (isBlank(parseCase.getExpectedOutputHash())) {
                issues.add("模板包样例预期输出哈希不能为空");
            }
        }
    }

    private Map<String, Object> buildRuleValidationContext(ParseRuleDefinitionPO rule) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("row", List.of());
        context.put("requiredHeaders", DEFAULT_REQUIRED_HEADERS);
        context.put("footerKeywords", List.of());
        context.put("subjectCodePattern", DEFAULT_SUBJECT_CODE_PATTERN);
        context.put("headerText", "");
        context.put("segments", List.of());
        context.put("exactCandidate", null);
        context.put("segmentCandidate", null);
        context.put("aliasCandidate", null);
        context.put("ruleType", rule == null || rule.getRuleType() == null ? null : rule.getRuleType().name());
        context.put("stepName", rule == null ? null : rule.getStepName());
        context.put("fileName", "");
        context.put("sheetName", "");
        return context;
    }

    private boolean hasDuplicateProfileVersion(ParseRuleProfilePO profile) {
        if (profile == null || isBlank(profile.getProfileCode()) || isBlank(profile.getVersion())) {
            return false;
        }
        List<ParseRuleProfilePO> profiles = profileRepository.selectList(Wrappers.lambdaQuery(ParseRuleProfilePO.class)
                .eq(ParseRuleProfilePO::getProfileCode, profile.getProfileCode())
                .eq(ParseRuleProfilePO::getVersion, profile.getVersion()));
        for (ParseRuleProfilePO candidate : profiles) {
            if (candidate == null) {
                continue;
            }
            if (!Objects.equals(candidate.getId(), profile.getId())) {
                return true;
            }
        }
        return false;
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
                                                  String rollbackFromVersion,
                                                  ParseRuleRegressionViewDTO regression) {
        ParseRulePublishLogPO publishLog = new ParseRulePublishLogPO();
        publishLog.setProfileId(profile.getId());
        publishLog.setVersion(profile.getVersion());
        publishLog.setPublishStatus(publishStatus);
        publishLog.setPublishTime(LocalDateTime.now());
        publishLog.setPublisher(publisher);
        publishLog.setPublishComment(publishComment);
        publishLog.setRollbackFromVersion(rollbackFromVersion);
        try {
            Map<String, Object> validationAndRegression = new LinkedHashMap<>();
            validationAndRegression.put("validation", validation);
            validationAndRegression.put("regression", regression);
            publishLog.setValidationResultJson(objectMapper.writeValueAsString(validationAndRegression));
        } catch (Exception exception) {
            publishLog.setValidationResultJson("{\"error\":\"validation serialization failed\"}");
        }
        return publishLog;
    }

    private ParseRuleProfileUpsertCommand toProfileUpsertCommand(ParseRuleProfilePO profile) {
        ParseRuleProfileUpsertCommand command = new ParseRuleProfileUpsertCommand();
        command.setId(profile.getId());
        command.setProfileCode(profile.getProfileCode());
        command.setProfileName(profile.getProfileName());
        command.setVersion(profile.getVersion());
        command.setFileScene(profile.getFileScene());
        command.setFileTypeName(profile.getFileTypeName());
        command.setSourceChannel(profile.getSourceChannel());
        command.setStatus(profile.getStatus());
        command.setPriority(profile.getPriority());
        command.setMatchExpr(profile.getMatchExpr());
        command.setHeaderExpr(profile.getHeaderExpr());
        command.setRowClassifyExpr(profile.getRowClassifyExpr());
        command.setFieldMapExpr(profile.getFieldMapExpr());
        command.setTransformExpr(profile.getTransformExpr());
        command.setRequiredHeaders(normalizeRequiredHeaders(profile.getRequiredHeadersJson()));
        command.setSubjectCodePattern(profile.getSubjectCodePattern());
        command.setTraceEnabled(profile.getTraceEnabled());
        command.setTimeoutMs(profile.getTimeoutMs());
        command.setCreatedBy(profile.getCreater());
        command.setModifiedBy(profile.getModifier());
        command.setRules(loadRules(profile.getId()).stream().map(this::toRuleUpsertCommand).toList());
        command.setCases(loadCases(profile.getId()).stream().map(this::toCaseUpsertCommand).toList());
        return command;
    }

    private void normalizeBundleIds(ParseRuleProfileUpsertCommand command) {
        if (command == null) {
            return;
        }
        command.setId(command.getId());
        if (command.getRules() != null) {
            command.getRules().forEach(rule -> {
                if (rule != null) {
                    rule.setId(null);
                }
            });
        }
        if (command.getCases() != null) {
            command.getCases().forEach(parseCase -> {
                if (parseCase != null) {
                    parseCase.setId(null);
                }
            });
        }
    }

    private ParseRuleRegressionCaseViewDTO regressionFailure(ParseRuleCasePO parseCase,
                                                             String reason,
                                                             String actualHash,
                                                             Integer actualHeaderRow,
                                                             Integer actualDataStartRow,
                                                             Integer actualCount) {
        return ParseRuleRegressionCaseViewDTO.builder()
                .caseId(parseCase.getId())
                .sampleFileId(parseCase.getSampleFileId())
                .sampleFileName(parseCase.getSampleFileName())
                .expectedOutputHash(parseCase.getExpectedOutputHash())
                .actualOutputHash(actualHash)
                .passed(Boolean.FALSE)
                .reason(reason)
                .expectedHeaderRow(parseCase.getExpectedHeaderRow())
                .actualHeaderRow(actualHeaderRow)
                .expectedDataStartRow(parseCase.getExpectedDataStartRow())
                .actualDataStartRow(actualDataStartRow)
                .expectedSubjectCount(parseCase.getExpectedSubjectCount())
                .actualSubjectCount(actualCount)
                .expectedMetricCount(parseCase.getExpectedMetricCount())
                .actualMetricCount(actualCount)
                .build();
    }

    private DataSourceType resolveDataSourceType(ValsetFileInfo fileInfo) {
        String fileExtension = fileInfo == null ? null : fileInfo.getFileExtension();
        if (fileExtension != null) {
            String normalized = fileExtension.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("csv")) {
                return DataSourceType.CSV;
            }
        }
        String fileFormat = fileInfo == null ? null : fileInfo.getFileFormat();
        if (fileFormat != null && fileFormat.trim().equalsIgnoreCase("CSV")) {
            return DataSourceType.CSV;
        }
        return DataSourceType.EXCEL;
    }

    private String buildRegressionHash(ParsedValuationData parsedValuationData) {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("sheetName", parsedValuationData.getSheetName());
            summary.put("headerRowNumber", parsedValuationData.getHeaderRowNumber());
            summary.put("dataStartRowNumber", parsedValuationData.getDataStartRowNumber());
            summary.put("title", parsedValuationData.getTitle());
            summary.put("subjectCount", parsedValuationData.getSubjects() == null ? 0 : parsedValuationData.getSubjects().size());
            summary.put("metricCount", parsedValuationData.getMetrics() == null ? 0 : parsedValuationData.getMetrics().size());
            summary.put("headers", parsedValuationData.getHeaders() == null ? List.of() : parsedValuationData.getHeaders());
            summary.put("basicInfo", parsedValuationData.getBasicInfo() == null ? Map.of() : parsedValuationData.getBasicInfo());
            String payload = objectMapper.writeValueAsString(summary);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("构建样例回归摘要失败", exception);
        }
    }

    private String buildChecksum(ParseRuleProfileUpsertCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = objectMapper.writeValueAsString(normalizeChecksumCommand(command));
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception exception) {
            return null;
        }
    }

    private ParseRuleProfileUpsertCommand normalizeChecksumCommand(ParseRuleProfileUpsertCommand command) {
        if (command == null) {
            return null;
        }
        ParseRuleProfileUpsertCommand normalized = new ParseRuleProfileUpsertCommand();
        normalized.setId(null);
        normalized.setProfileCode(command.getProfileCode());
        normalized.setProfileName(command.getProfileName());
        normalized.setVersion(command.getVersion());
        normalized.setFileScene(command.getFileScene());
        normalized.setFileTypeName(command.getFileTypeName());
        normalized.setSourceChannel(command.getSourceChannel());
        normalized.setStatus(command.getStatus());
        normalized.setPriority(command.getPriority());
        normalized.setMatchExpr(command.getMatchExpr());
        normalized.setHeaderExpr(command.getHeaderExpr());
        normalized.setRowClassifyExpr(command.getRowClassifyExpr());
        normalized.setFieldMapExpr(command.getFieldMapExpr());
        normalized.setTransformExpr(command.getTransformExpr());
        normalized.setRequiredHeaders(normalizeRequiredHeaders(command.getRequiredHeaders()));
        normalized.setSubjectCodePattern(normalizeSubjectCodePattern(command.getSubjectCodePattern()));
        normalized.setTraceEnabled(command.getTraceEnabled());
        normalized.setTimeoutMs(command.getTimeoutMs());
        normalized.setCreatedBy(command.getCreatedBy());
        normalized.setModifiedBy(command.getModifiedBy());
        if (command.getRules() != null) {
            normalized.setRules(command.getRules().stream().map(rule -> {
                if (rule == null) {
                    return null;
                }
                ParseRuleDefinitionUpsertCommand copy = new ParseRuleDefinitionUpsertCommand();
                copy.setId(null);
                copy.setRuleType(rule.getRuleType());
                copy.setStepName(rule.getStepName());
                copy.setPriority(rule.getPriority());
                copy.setEnabled(rule.getEnabled());
                copy.setExprText(rule.getExprText());
                copy.setExprLang(rule.getExprLang());
                copy.setInputSchemaJson(rule.getInputSchemaJson());
                copy.setOutputSchemaJson(rule.getOutputSchemaJson());
                copy.setErrorPolicy(rule.getErrorPolicy());
                copy.setTimeoutMs(rule.getTimeoutMs());
                return copy;
            }).toList());
        }
        if (command.getCases() != null) {
            normalized.setCases(command.getCases().stream().map(parseCase -> {
                if (parseCase == null) {
                    return null;
                }
                ParseRuleCaseUpsertCommand copy = new ParseRuleCaseUpsertCommand();
                copy.setId(null);
                copy.setSampleFileId(parseCase.getSampleFileId());
                copy.setSampleFileName(parseCase.getSampleFileName());
                copy.setExpectedSheetName(parseCase.getExpectedSheetName());
                copy.setExpectedHeaderRow(parseCase.getExpectedHeaderRow());
                copy.setExpectedDataStartRow(parseCase.getExpectedDataStartRow());
                copy.setExpectedSubjectCount(parseCase.getExpectedSubjectCount());
                copy.setExpectedMetricCount(parseCase.getExpectedMetricCount());
                copy.setExpectedOutputHash(parseCase.getExpectedOutputHash());
                copy.setStatus(parseCase.getStatus());
                return copy;
            }).toList());
        }
        return normalized;
    }

    private String writeRequiredHeaders(List<String> requiredHeaders) {
        try {
            return objectMapper.writeValueAsString(normalizeRequiredHeaders(requiredHeaders));
        } catch (Exception exception) {
            return objectMapperValue(DEFAULT_REQUIRED_HEADERS);
        }
    }

    private List<String> normalizeRequiredHeaders(String requiredHeadersJson) {
        if (isBlank(requiredHeadersJson)) {
            return DEFAULT_REQUIRED_HEADERS;
        }
        try {
            List<String> requiredHeaders = objectMapper.readValue(requiredHeadersJson, new TypeReference<List<String>>() {
            });
            return normalizeRequiredHeaders(requiredHeaders);
        } catch (Exception exception) {
            return DEFAULT_REQUIRED_HEADERS;
        }
    }

    private List<String> normalizeRequiredHeaders(List<String> requiredHeaders) {
        if (requiredHeaders == null || requiredHeaders.isEmpty()) {
            return DEFAULT_REQUIRED_HEADERS;
        }
        List<String> normalized = requiredHeaders.stream()
                .filter(header -> header != null && !header.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? DEFAULT_REQUIRED_HEADERS : normalized;
    }

    private String normalizeSubjectCodePattern(String subjectCodePattern) {
        if (isBlank(subjectCodePattern)) {
            return DEFAULT_SUBJECT_CODE_PATTERN;
        }
        try {
            Pattern.compile(subjectCodePattern.trim());
            return subjectCodePattern.trim();
        } catch (Exception exception) {
            return DEFAULT_SUBJECT_CODE_PATTERN;
        }
    }

    private String objectMapperValue(List<String> requiredHeaders) {
        try {
            return objectMapper.writeValueAsString(requiredHeaders);
        } catch (Exception exception) {
            return "[]";
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
