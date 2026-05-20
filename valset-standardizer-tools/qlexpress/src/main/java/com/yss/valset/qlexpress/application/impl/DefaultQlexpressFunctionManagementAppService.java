package com.yss.valset.qlexpress.application.impl;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.qlexpress.application.command.QlexpressFunctionDebugCommand;
import com.yss.valset.qlexpress.application.command.QlexpressFunctionUpsertCommand;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionDebugResultDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionMutationResponse;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionUsageDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionViewDTO;
import com.yss.valset.qlexpress.application.service.QlexpressFunctionManagementAppService;
import com.yss.valset.qlexpress.application.service.QlexpressFunctionUsageAppService;
import com.yss.valset.qlexpress.domain.runtime.QlexpressFunctionScript;
import com.yss.valset.qlexpress.domain.runtime.QlexpressFunctionScriptProvider;
import com.yss.valset.qlexpress.domain.runtime.QlexpressExecutionContextEnhancer;
import com.yss.valset.qlexpress.domain.runtime.QlexpressRunnerRegistry;
import com.yss.valset.qlexpress.infrastructure.entity.QlexpressFunctionPO;
import com.yss.valset.qlexpress.infrastructure.mapper.QlexpressFunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 默认 QLExpress 自定义函数管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultQlexpressFunctionManagementAppService implements QlexpressFunctionManagementAppService, QlexpressFunctionScriptProvider {

    private static final Pattern FUNCTION_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final QlexpressFunctionRepository qlexpressFunctionRepository;
    private final QlexpressRunnerRegistry qlexpressRunnerRegistry;
    private final QlexpressExecutionContextEnhancer contextEnhancer;
    private final ObjectMapper objectMapper;
    private final QlexpressFunctionUsageAppService qlexpressFunctionUsageAppService;

    @Override
    public PageResult<QlexpressFunctionViewDTO> pageFunctions(String functionCnName, String functionName, Boolean enabled, Integer pageIndex, Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        Page<QlexpressFunctionPO> page = qlexpressFunctionRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(QlexpressFunctionPO.class)
                        .like(hasText(functionCnName), QlexpressFunctionPO::getFunctionCnName, functionCnName)
                        .like(hasText(functionName), QlexpressFunctionPO::getFunctionName, functionName)
                        .eq(enabled != null, QlexpressFunctionPO::getEnabled, enabled)
                        .orderByDesc(QlexpressFunctionPO::getUpdatedAt)
                        .orderByDesc(QlexpressFunctionPO::getFunctionId)
        );
        Map<String, QlexpressFunctionUsageDTO> usageCache = qlexpressFunctionUsageAppService.summarizeUsages(page.getRecords());
        List<QlexpressFunctionViewDTO> records = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(po -> toView(po, usageCache)).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), (int) page.getSize(), (int) page.getCurrent() - 1);
    }

    @Override
    public QlexpressFunctionViewDTO getFunction(String functionId) {
        return toView(findById(functionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QlexpressFunctionMutationResponse upsertFunction(QlexpressFunctionUpsertCommand command) {
        validateCommand(command);
        boolean createMode = !hasText(command.getFunctionId());
        QlexpressFunctionPO existing = createMode ? null : findById(command.getFunctionId());
        if (existing != null && Boolean.TRUE.equals(existing.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已激活函数不可修改，请先停用");
        }
        if (existing != null && isSystemSeed(existing) && !existing.getFunctionName().equals(command.getFunctionName().trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "系统内置函数不可修改函数名称");
        }
        assertFunctionNameUnique(command.getFunctionName(), command.getFunctionId());
        qlexpressRunnerRegistry.validateScript(command.getFunctionName().trim(), command.getScriptBody().trim());

        LocalDateTime now = LocalDateTime.now();
        QlexpressFunctionPO po = createMode ? new QlexpressFunctionPO() : existing;
        po.setFunctionCnName(command.getFunctionCnName().trim());
        po.setFunctionName(command.getFunctionName().trim());
        po.setRemark(trimToNull(command.getRemark()));
        po.setScriptBody(command.getScriptBody().trim());
        po.setEnabled(Boolean.TRUE.equals(command.getEnabled()));
        po.setExtInfoJson(toJson(command.getExtInfo()));
        if (createMode) {
            po.setCreatedAt(now);
        }
        po.setUpdatedAt(now);

        if (Boolean.TRUE.equals(po.getEnabled())) {
            validateEnableCandidate(po);
        }
        try {
            if (createMode) {
                qlexpressFunctionRepository.insert(po);
            } else {
                qlexpressFunctionRepository.updateById(po);
            }
        } catch (DataIntegrityViolationException exception) {
            throw duplicateFunctionName(command.getFunctionName(), exception);
        }
        if (Boolean.TRUE.equals(po.getEnabled())) {
            qlexpressRunnerRegistry.refreshAll();
        }
        return mutation(createMode ? "create" : "update", "函数保存成功", po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QlexpressFunctionMutationResponse deleteFunction(String functionId) {
        QlexpressFunctionPO existing = findById(functionId);
        if (Boolean.TRUE.equals(existing.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已激活函数不可删除，请先停用");
        }
        qlexpressFunctionRepository.deleteById(functionId);
        return mutation("delete", "函数删除成功", existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QlexpressFunctionMutationResponse enableFunction(String functionId) {
        QlexpressFunctionPO existing = findById(functionId);
        if (!Boolean.TRUE.equals(existing.getEnabled())) {
            validateEnableCandidate(existing);
            existing.setEnabled(Boolean.TRUE);
            existing.setUpdatedAt(LocalDateTime.now());
            qlexpressFunctionRepository.updateById(existing);
            qlexpressRunnerRegistry.refreshAll();
        }
        return mutation("enable", "函数启用成功", existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QlexpressFunctionMutationResponse disableFunction(String functionId) {
        QlexpressFunctionPO existing = findById(functionId);
        if (Boolean.TRUE.equals(existing.getEnabled())) {
            existing.setEnabled(Boolean.FALSE);
            existing.setUpdatedAt(LocalDateTime.now());
            qlexpressFunctionRepository.updateById(existing);
            qlexpressRunnerRegistry.refreshAll();
        }
        return mutation("disable", "函数停用成功", existing);
    }

    @Override
    public QlexpressFunctionDebugResultDTO debug(QlexpressFunctionDebugCommand command) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "调试参数不能为空");
        }
        String functionName = trimToNull(command.getFunctionName());
        String scriptBody = trimToNull(command.getScriptBody());
        String runnerScope = trimToNull(command.getRunnerScope());
        if (hasText(command.getFunctionId())) {
            QlexpressFunctionPO function = findById(command.getFunctionId());
            functionName = function.getFunctionName();
            scriptBody = function.getScriptBody();
            runnerScope = firstSourceModule(function.getExtInfoJson());
        }
        if (!hasText(functionName) || !hasText(scriptBody)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "调试函数名称和函数脚本不能为空");
        }
        String expression = trimToNull(command.getDebugExpression());
        if (!hasText(expression)) {
            expression = functionName + "()";
        }
        Map<String, Object> context = command.getContext() == null ? Collections.emptyMap() : new LinkedHashMap<>(command.getContext());
        long startedAt = System.currentTimeMillis();
        Set<String> outFunctions = Collections.emptySet();
        Set<String> outVarNames = Collections.emptySet();
        try {
            Express4Runner runner = qlexpressRunnerRegistry.createSandboxRunner(runnerScope,
                    Collections.singletonList(new QlexpressFunctionScript(functionName, scriptBody, runnerScope == null
                            ? Collections.emptyList()
                            : Collections.singletonList(runnerScope))));
            outFunctions = runner.getOutFunctions(expression);
            outVarNames = runner.getOutVarNames(expression);
            Object result = runner.execute(expression, contextEnhancer.enhance(runnerScope, context), QLOptions.DEFAULT_OPTIONS).getResult();
            return QlexpressFunctionDebugResultDTO.builder()
                    .success(Boolean.TRUE)
                    .result(result)
                    .costMs(System.currentTimeMillis() - startedAt)
                    .outFunctions(outFunctions)
                    .outVarNames(outVarNames)
                    .build();
        } catch (Exception exception) {
            return QlexpressFunctionDebugResultDTO.builder()
                    .success(Boolean.FALSE)
                    .errorMessage(exception.getMessage())
                    .costMs(System.currentTimeMillis() - startedAt)
                    .outFunctions(outFunctions)
                    .outVarNames(outVarNames)
                    .build();
        }
    }

    @Override
    public List<QlexpressFunctionScript> listEnabledScripts() {
        return qlexpressFunctionRepository.selectList(
                        Wrappers.lambdaQuery(QlexpressFunctionPO.class)
                                .eq(QlexpressFunctionPO::getEnabled, Boolean.TRUE)
                                .orderByAsc(QlexpressFunctionPO::getFunctionId)
                )
                .stream()
                .map(po -> new QlexpressFunctionScript(po.getFunctionName(), po.getScriptBody(), sourceModules(po.getExtInfoJson())))
                .collect(Collectors.toList());
    }

    private void validateCommand(QlexpressFunctionUpsertCommand command) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数配置不能为空");
        }
        if (!hasText(command.getFunctionCnName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数中文名称不能为空");
        }
        if (!hasText(command.getFunctionName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数名称不能为空");
        }
        if (!FUNCTION_NAME_PATTERN.matcher(command.getFunctionName().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数名称只能包含字母、数字和下划线，且不能以数字开头");
        }
        if (!hasText(command.getScriptBody())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数脚本不能为空");
        }
    }

    private void validateEnableCandidate(QlexpressFunctionPO candidate) {
        List<QlexpressFunctionScript> scripts = qlexpressFunctionRepository.selectList(
                        Wrappers.lambdaQuery(QlexpressFunctionPO.class)
                                .eq(QlexpressFunctionPO::getEnabled, Boolean.TRUE)
                )
                .stream()
                .filter(po -> candidate.getFunctionId() == null || !candidate.getFunctionId().equals(po.getFunctionId()))
                .map(po -> new QlexpressFunctionScript(po.getFunctionName(), po.getScriptBody(), sourceModules(po.getExtInfoJson())))
                .collect(Collectors.toList());
        scripts.add(new QlexpressFunctionScript(candidate.getFunctionName(), candidate.getScriptBody(), sourceModules(candidate.getExtInfoJson())));
        qlexpressRunnerRegistry.validateScripts(scripts);
    }

    private boolean isSystemSeed(QlexpressFunctionPO candidate) {
        if (candidate == null || !hasText(candidate.getExtInfoJson())) {
            return false;
        }
        try {
            Map<String, Object> extInfo = objectMapper.readValue(candidate.getExtInfoJson(), new TypeReference<Map<String, Object>>() {});
            return isSystemSeed(extInfo);
        } catch (Exception exception) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isSystemSeed(Object extInfo) {
        if (extInfo == null) {
            return false;
        }
        if (extInfo instanceof Map<?, ?>) {
            Object sourceType = ((Map<String, Object>) extInfo).get("sourceType");
            return sourceType != null && "SYSTEM_SEED".equalsIgnoreCase(String.valueOf(sourceType));
        }
        return false;
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
                    .collect(Collectors.toList());
        }
        String text = String.valueOf(value).trim();
        return hasText(text) ? Collections.singletonList(text) : Collections.emptyList();
    }

    private String firstSourceModule(String extInfoJson) {
        List<String> modules = sourceModules(extInfoJson);
        return modules.isEmpty() ? null : modules.get(0);
    }

    private void assertFunctionNameUnique(String functionName, String functionId) {
        QlexpressFunctionPO existing = qlexpressFunctionRepository.selectOne(
                Wrappers.lambdaQuery(QlexpressFunctionPO.class)
                        .eq(QlexpressFunctionPO::getFunctionName, functionName.trim())
        );
        if (existing != null && (!hasText(functionId) || !functionId.equals(existing.getFunctionId()))) {
            throw duplicateFunctionName(functionName, null);
        }
    }

    private QlexpressFunctionPO findById(String functionId) {
        if (!hasText(functionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数ID不能为空");
        }
        QlexpressFunctionPO po = qlexpressFunctionRepository.selectById(functionId);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到函数，functionId=" + functionId);
        }
        return po;
    }

    private QlexpressFunctionMutationResponse mutation(String operation, String message, QlexpressFunctionPO po) {
        return QlexpressFunctionMutationResponse.builder()
                .operation(operation)
                .message(message)
                .function(toView(po))
                .build();
    }

    private QlexpressFunctionViewDTO toView(QlexpressFunctionPO po) {
        return toView(po, new LinkedHashMap<>());
    }

    private QlexpressFunctionViewDTO toView(QlexpressFunctionPO po, Map<String, QlexpressFunctionUsageDTO> usageCache) {
        QlexpressFunctionUsageDTO usage = usageSummary(po, usageCache);
        return QlexpressFunctionViewDTO.builder()
                .functionId(po.getFunctionId())
                .functionCnName(po.getFunctionCnName())
                .functionName(po.getFunctionName())
                .remark(po.getRemark())
                .scriptBody(po.getScriptBody())
                .enabled(Boolean.TRUE.equals(po.getEnabled()))
                .extInfo(fromJson(po.getExtInfoJson()))
                .sourceModules(sourceModules(po.getExtInfoJson()))
                .flowLabels(flowLabels(usage))
                .usageStatus(usage == null ? null : usage.getUsageStatus())
                .usageStatusName(usage == null ? null : usage.getUsageStatusName())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private List<String> flowLabels(QlexpressFunctionUsageDTO usage) {
        if (usage == null || usage.getFlowUsages() == null) {
            return Collections.emptyList();
        }
        return usage.getFlowUsages().stream()
                .filter(item -> Boolean.TRUE.equals(item.getMatched()))
                .map(item -> item.getFlowName())
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private QlexpressFunctionUsageDTO usageSummary(QlexpressFunctionPO po) {
        return usageSummary(po, new LinkedHashMap<>());
    }

    private QlexpressFunctionUsageDTO usageSummary(QlexpressFunctionPO po, Map<String, QlexpressFunctionUsageDTO> usageCache) {
        if (po == null || !hasText(po.getFunctionId())) {
            return qlexpressFunctionUsageAppService.summarizeUsage(po);
        }
        return usageCache.computeIfAbsent(po.getFunctionId(), ignored -> qlexpressFunctionUsageAppService.summarizeUsage(po));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "函数扩展信息不是合法 JSON", exception);
        }
    }

    private Object fromJson(String value) {
        if (!hasText(value)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            return value;
        }
    }

    private ResponseStatusException duplicateFunctionName(String functionName, Throwable cause) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "函数名称已存在，functionName=" + functionName, cause);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
