package com.yss.valset.task.application.impl.parseissue;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.entity.FileParseRulePO;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import com.yss.valset.extract.repository.mapper.FileParseRuleRepository;
import com.yss.valset.task.application.command.parseissue.FileParseSourceSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.FileParseRuleSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.ProductMatchRuleSheetSaveCommand;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingExportDTO;
import com.yss.valset.task.application.dto.parseissue.FileParseSourceSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.FileParseRuleSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingSaveErrorDTO;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingSaveResultDTO;
import com.yss.valset.task.application.dto.parseissue.ProductMatchRuleSheetRowDTO;
import com.yss.valset.task.application.service.parseissue.ParseIssueHandlingAppService;
import com.yss.valset.task.application.support.ParseIssueHandlingWorkbookExportSupport;
import com.yss.valset.transfer.infrastructure.entity.ProductMatchRulePO;
import com.yss.valset.transfer.infrastructure.mapper.ProductMatchRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认解析问题处理配置维护服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultParseIssueHandlingAppService implements ParseIssueHandlingAppService {

    private static final String AUDIT_USER = "parse-issue-handling";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FileParseSourceRepository fileParseSourceRepository;
    private final FileParseRuleRepository fileParseRuleRepository;
    private final ProductMatchRuleRepository productMatchRuleRepository;
    private final ParseIssueHandlingWorkbookExportSupport workbookExportSupport;

    @Override
    public List<FileParseSourceSheetRowDTO> listFileParseSources(String fileType,
                                                                 String columnMap,
                                                                 String columnName,
                                                                 String status) {
        Boolean enabled = parseOptionalBoolean(status);
        Map<String, String> columnMapNameMap = loadFileParseRuleNameMap();
        return fileParseSourceRepository.selectList(
                Wrappers.lambdaQuery(FileParseSourcePO.class)
                                .like(hasText(fileType), FileParseSourcePO::getFileType, trimToNull(fileType))
                                .like(hasText(columnMap), FileParseSourcePO::getColumnMap, trimToNull(columnMap))
                                .like(hasText(columnName), FileParseSourcePO::getColumnName, trimToNull(columnName))
                                .eq(enabled != null, FileParseSourcePO::getStatus, enabled)
                                .orderByDesc(FileParseSourcePO::getModifyTime)
                                .orderByDesc(FileParseSourcePO::getId)
                ).stream()
                .map(po -> toFileParseSourceRow(po, columnMapNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public ParseIssueHandlingExportDTO exportFileParseSources(String fileType,
                                                              String columnMap,
                                                              String columnName,
                                                              String status) {
        List<FileParseSourceSheetRowDTO> rows = listFileParseSources(fileType, columnMap, columnName, status);
        return new ParseIssueHandlingExportDTO("外部列指标映射.xlsx", workbookExportSupport.exportFileParseSources(rows));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseIssueHandlingSaveResultDTO saveFileParseSources(FileParseSourceSheetSaveCommand command) {
        ParseIssueHandlingSaveResultDTO result = new ParseIssueHandlingSaveResultDTO();
        List<FileParseSourceSheetRowDTO> rows = command == null || command.getRows() == null
                ? Collections.emptyList()
                : command.getRows();
        Set<Long> originalIds = parseIdSet(command == null ? null : command.getOriginalIds());
        Map<Long, FileParseSourcePO> existingMap = loadFileParseSourceMap(originalIds);
        Set<Long> submittedIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < rows.size(); index += 1) {
            FileParseSourceSheetRowDTO row = rows.get(index);
            int rowNumber = index + 2;
            if (isBlankFileParseSourceRow(row)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            String validationMessage = validateFileParseSourceRow(row);
            if (validationMessage != null) {
                Long invalidExistingId = parseLong(row == null ? null : row.getId());
                if (invalidExistingId != null) {
                    submittedIds.add(invalidExistingId);
                }
                addError(result, rowNumber, row == null ? null : row.getId(), validationMessage);
                continue;
            }
            Long id = parseLong(row.getId());
            if (id != null) {
                if (!submittedIds.add(id)) {
                    addError(result, rowNumber, row.getId(), "ID 重复");
                    continue;
                }
                FileParseSourcePO existing = existingMap.get(id);
                if (existing == null) {
                    addError(result, rowNumber, row.getId(), "未找到原始记录");
                    continue;
                }
                applyFileParseSourceRow(existing, row);
                existing.setModifier(AUDIT_USER);
                existing.setModifyTime(now);
                fileParseSourceRepository.updateById(existing);
                result.setUpdatedCount(result.getUpdatedCount() + 1);
            } else {
                FileParseSourcePO po = new FileParseSourcePO();
                po.setId(IdWorker.getId());
                applyFileParseSourceRow(po, row);
                po.setCreater(AUDIT_USER);
                po.setCreateTime(now);
                po.setModifier(AUDIT_USER);
                po.setModifyTime(now);
                fileParseSourceRepository.insert(po);
                submittedIds.add(po.getId());
                result.setCreatedCount(result.getCreatedCount() + 1);
            }
        }

        for (Long originalId : originalIds) {
            if (!submittedIds.contains(originalId)) {
                fileParseSourceRepository.deleteById(originalId);
                result.setDeletedCount(result.getDeletedCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<FileParseRuleSheetRowDTO> listFileParseRules(String fileScene,
                                                             String fileTypeName,
                                                             String regionName,
                                                             String columnMap,
                                                             String columnMapName,
                                                             String status) {
        Boolean enabled = parseOptionalBoolean(status);
        return fileParseRuleRepository.selectList(
                Wrappers.lambdaQuery(FileParseRulePO.class)
                                .like(hasText(fileScene), FileParseRulePO::getFileScene, trimToNull(fileScene))
                                .like(hasText(fileTypeName), FileParseRulePO::getFileTypeName, trimToNull(fileTypeName))
                                .like(hasText(regionName), FileParseRulePO::getRegionName, trimToNull(regionName))
                                .like(hasText(columnMap), FileParseRulePO::getColumnMap, trimToNull(columnMap))
                                .like(hasText(columnMapName), FileParseRulePO::getColumnMapName, trimToNull(columnMapName))
                                .eq(enabled != null, FileParseRulePO::getStatus, enabled)
                                .orderByDesc(FileParseRulePO::getModifyTime)
                                .orderByDesc(FileParseRulePO::getId)
                ).stream()
                .map(this::toFileParseRuleRow)
                .collect(Collectors.toList());
    }

    @Override
    public ParseIssueHandlingExportDTO exportFileParseRules(String fileScene,
                                                            String fileTypeName,
                                                            String regionName,
                                                            String columnMap,
                                                            String columnMapName,
                                                            String status) {
        List<FileParseRuleSheetRowDTO> rows = listFileParseRules(fileScene, fileTypeName, regionName, columnMap, columnMapName, status);
        return new ParseIssueHandlingExportDTO("标准列指标映射.xlsx", workbookExportSupport.exportFileParseRules(rows));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseIssueHandlingSaveResultDTO saveFileParseRules(FileParseRuleSheetSaveCommand command) {
        ParseIssueHandlingSaveResultDTO result = new ParseIssueHandlingSaveResultDTO();
        List<FileParseRuleSheetRowDTO> rows = command == null || command.getRows() == null
                ? Collections.emptyList()
                : command.getRows();
        Set<Long> originalIds = parseIdSet(command == null ? null : command.getOriginalIds());
        Map<Long, FileParseRulePO> existingMap = loadFileParseRuleMap(originalIds);
        Set<Long> submittedIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < rows.size(); index += 1) {
            FileParseRuleSheetRowDTO row = rows.get(index);
            int rowNumber = index + 2;
            if (isBlankFileParseRuleRow(row)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            String validationMessage = validateFileParseRuleRow(row);
            if (validationMessage != null) {
                Long invalidExistingId = parseLong(row == null ? null : row.getId());
                if (invalidExistingId != null) {
                    submittedIds.add(invalidExistingId);
                }
                addError(result, rowNumber, row == null ? null : row.getId(), validationMessage);
                continue;
            }
            Long id = parseLong(row.getId());
            if (id != null) {
                if (!submittedIds.add(id)) {
                    addError(result, rowNumber, row.getId(), "ID 重复");
                    continue;
                }
                FileParseRulePO existing = existingMap.get(id);
                if (existing == null) {
                    addError(result, rowNumber, row.getId(), "未找到原始记录");
                    continue;
                }
                applyFileParseRuleRow(existing, row);
                existing.setModifier(AUDIT_USER);
                existing.setModifyTime(now);
                fileParseRuleRepository.updateById(existing);
                result.setUpdatedCount(result.getUpdatedCount() + 1);
            } else {
                FileParseRulePO po = new FileParseRulePO();
                po.setId(IdWorker.getId());
                applyFileParseRuleRow(po, row);
                po.setCreater(AUDIT_USER);
                po.setCreateTime(now);
                po.setModifier(AUDIT_USER);
                po.setModifyTime(now);
                fileParseRuleRepository.insert(po);
                submittedIds.add(po.getId());
                result.setCreatedCount(result.getCreatedCount() + 1);
            }
        }

        for (Long originalId : originalIds) {
            if (!submittedIds.contains(originalId)) {
                fileParseRuleRepository.deleteById(originalId);
                result.setDeletedCount(result.getDeletedCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<ProductMatchRuleSheetRowDTO> listProductMatchRules(String pdCd,
                                                                   String pdNm,
                                                                   String orgNm,
                                                                   String fileType,
                                                                   String isValid) {
        Integer enabled = parseOptionalEnabledInteger(isValid);
        return productMatchRuleRepository.selectList(
                Wrappers.lambdaQuery(ProductMatchRulePO.class)
                                .like(hasText(pdCd), ProductMatchRulePO::getPdCd, trimToNull(pdCd))
                                .like(hasText(pdNm), ProductMatchRulePO::getPdNm, trimToNull(pdNm))
                                .like(hasText(orgNm), ProductMatchRulePO::getOrgNm, trimToNull(orgNm))
                                .like(hasText(fileType), ProductMatchRulePO::getFileType, trimToNull(fileType))
                                .eq(enabled != null, ProductMatchRulePO::getIsValid, enabled)
                                .orderByDesc(ProductMatchRulePO::getModifyTime)
                                .orderByDesc(ProductMatchRulePO::getId)
                ).stream()
                .map(this::toProductMatchRuleRow)
                .collect(Collectors.toList());
    }

    @Override
    public ParseIssueHandlingExportDTO exportProductMatchRules(String pdCd,
                                                               String pdNm,
                                                               String orgNm,
                                                               String fileType,
                                                               String isValid) {
        List<ProductMatchRuleSheetRowDTO> rows = listProductMatchRules(pdCd, pdNm, orgNm, fileType, isValid);
        return new ParseIssueHandlingExportDTO("产品识别规则配置表.xlsx", workbookExportSupport.exportProductMatchRules(rows));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseIssueHandlingSaveResultDTO saveProductMatchRules(ProductMatchRuleSheetSaveCommand command) {
        ParseIssueHandlingSaveResultDTO result = new ParseIssueHandlingSaveResultDTO();
        List<ProductMatchRuleSheetRowDTO> rows = command == null || command.getRows() == null
                ? Collections.emptyList()
                : command.getRows();
        Set<Long> originalIds = parseIdSet(command == null ? null : command.getOriginalIds());
        Map<Long, ProductMatchRulePO> existingMap = loadProductMatchRuleMap(originalIds);
        Set<Long> submittedIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (int index = 0; index < rows.size(); index += 1) {
            ProductMatchRuleSheetRowDTO row = rows.get(index);
            int rowNumber = index + 2;
            if (isBlankProductMatchRuleRow(row)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }
            String validationMessage = validateProductMatchRuleRow(row);
            if (validationMessage != null) {
                Long invalidExistingId = parseLong(row == null ? null : row.getId());
                if (invalidExistingId != null) {
                    submittedIds.add(invalidExistingId);
                }
                addError(result, rowNumber, row == null ? null : row.getId(), validationMessage);
                continue;
            }
            Long id = parseLong(row.getId());
            if (id != null) {
                if (!submittedIds.add(id)) {
                    addError(result, rowNumber, row.getId(), "ID 重复");
                    continue;
                }
                ProductMatchRulePO existing = existingMap.get(id);
                if (existing == null) {
                    addError(result, rowNumber, row.getId(), "未找到原始记录");
                    continue;
                }
                applyProductMatchRuleRow(existing, row);
                existing.setModifier(AUDIT_USER);
                existing.setModifyTime(now);
                productMatchRuleRepository.updateById(existing);
                result.setUpdatedCount(result.getUpdatedCount() + 1);
            } else {
                ProductMatchRulePO po = new ProductMatchRulePO();
                po.setId(IdWorker.getId());
                applyProductMatchRuleRow(po, row);
                po.setCreater(AUDIT_USER);
                po.setCreateTime(now);
                po.setModifier(AUDIT_USER);
                po.setModifyTime(now);
                productMatchRuleRepository.insert(po);
                submittedIds.add(po.getId());
                result.setCreatedCount(result.getCreatedCount() + 1);
            }
        }

        for (Long originalId : originalIds) {
            if (!submittedIds.contains(originalId)) {
                productMatchRuleRepository.deleteById(originalId);
                result.setDeletedCount(result.getDeletedCount() + 1);
            }
        }
        return result;
    }

    private Map<Long, FileParseSourcePO> loadFileParseSourceMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return fileParseSourceRepository.selectList(
                        Wrappers.lambdaQuery(FileParseSourcePO.class).in(FileParseSourcePO::getId, ids)
                ).stream()
                .collect(Collectors.toMap(FileParseSourcePO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, ProductMatchRulePO> loadProductMatchRuleMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return productMatchRuleRepository.selectList(
                        Wrappers.lambdaQuery(ProductMatchRulePO.class).in(ProductMatchRulePO::getId, ids)
                ).stream()
                .collect(Collectors.toMap(ProductMatchRulePO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, FileParseRulePO> loadFileParseRuleMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return fileParseRuleRepository.selectList(
                        Wrappers.lambdaQuery(FileParseRulePO.class).in(FileParseRulePO::getId, ids)
                ).stream()
                .collect(Collectors.toMap(FileParseRulePO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, String> loadFileParseRuleNameMap() {
        return fileParseRuleRepository.selectList(
                        Wrappers.lambdaQuery(FileParseRulePO.class)
                                .orderByDesc(FileParseRulePO::getModifyTime)
                                .orderByDesc(FileParseRulePO::getId)
                ).stream()
                .collect(Collectors.toMap(
                        item -> trimToNull(item.getColumnMap()),
                        item -> trimToNull(item.getColumnMapName()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private FileParseSourceSheetRowDTO toFileParseSourceRow(FileParseSourcePO po, Map<String, String> columnMapNameMap) {
        FileParseSourceSheetRowDTO row = new FileParseSourceSheetRowDTO();
        row.setId(toText(po.getId()));
        row.setFileType(po.getFileType());
        row.setColumnMap(po.getColumnMap());
        row.setColumnMapName(columnMapNameMap.get(trimToNull(po.getColumnMap())));
        row.setColumnName(po.getColumnName());
        row.setFileExtInfo(po.getFileExtInfo());
        row.setStatus(formatBoolean(po.getStatus()));
        row.setCreater(po.getCreater());
        row.setCreateTime(formatDateTime(po.getCreateTime()));
        row.setModifier(po.getModifier());
        row.setModifyTime(formatDateTime(po.getModifyTime()));
        return row;
    }

    private FileParseRuleSheetRowDTO toFileParseRuleRow(FileParseRulePO po) {
        FileParseRuleSheetRowDTO row = new FileParseRuleSheetRowDTO();
        row.setId(toText(po.getId()));
        row.setFileScene(po.getFileScene());
        row.setFileTypeName(po.getFileTypeName());
        row.setRegionName(po.getRegionName());
        row.setColumnMap(po.getColumnMap());
        row.setColumnMapName(po.getColumnMapName());
        row.setStatus(formatBoolean(po.getStatus()));
        row.setMultiIndex(formatBoolean(po.getMultiIndex()));
        row.setRequired(formatBoolean(po.getRequired()));
        row.setCreater(po.getCreater());
        row.setCreateTime(formatDateTime(po.getCreateTime()));
        row.setModifier(po.getModifier());
        row.setModifyTime(formatDateTime(po.getModifyTime()));
        return row;
    }

    private ProductMatchRuleSheetRowDTO toProductMatchRuleRow(ProductMatchRulePO po) {
        ProductMatchRuleSheetRowDTO row = new ProductMatchRuleSheetRowDTO();
        row.setId(toText(po.getId()));
        row.setFileTypeName(po.getFileTypeName());
        row.setPdCd(po.getPdCd());
        row.setPdNm(po.getPdNm());
        row.setOrgCd(po.getOrgCd());
        row.setOrgNm(po.getOrgNm());
        row.setPdType(po.getPdType());
        row.setSubjectSystem(po.getSubjectSystem());
        row.setHoldingStatus(po.getHoldingStatus());
        row.setEstablishedDate(formatDate(po.getEstablishedDate()));
        row.setEffectiveFrequency(po.getEffectiveFrequency());
        row.setDelayDays(toText(po.getDelayDays()));
        row.setApprovalRequired(formatBoolean(po.getApprovalRequired()));
        row.setFileType(po.getFileType());
        row.setMatchRules(po.getMatchRules());
        row.setIsValid(formatEnabledInteger(po.getIsValid()));
        row.setMemo(po.getMemo());
        row.setDebugName(po.getDebugName());
        row.setJobName(po.getJobName());
        row.setJobScene(po.getJobScene());
        row.setCreater(po.getCreater());
        row.setCreateTime(formatDateTime(po.getCreateTime()));
        row.setModifier(po.getModifier());
        row.setModifyTime(formatDateTime(po.getModifyTime()));
        return row;
    }

    private void applyFileParseSourceRow(FileParseSourcePO po, FileParseSourceSheetRowDTO row) {
        po.setFileType(trimToNull(row.getFileType()));
        po.setColumnMap(trimToNull(row.getColumnMap()));
        po.setColumnName(trimToNull(row.getColumnName()));
        po.setFileExtInfo(trimToNull(row.getFileExtInfo()));
        po.setStatus(parseOptionalBoolean(row.getStatus()));
    }

    private void applyFileParseRuleRow(FileParseRulePO po, FileParseRuleSheetRowDTO row) {
        po.setFileScene(trimToNull(row.getFileScene()));
        po.setFileTypeName(trimToNull(row.getFileTypeName()));
        po.setRegionName(trimToNull(row.getRegionName()));
        po.setColumnMap(trimToNull(row.getColumnMap()));
        po.setColumnMapName(trimToNull(row.getColumnMapName()));
        po.setStatus(parseOptionalBoolean(row.getStatus()));
        po.setMultiIndex(parseOptionalBoolean(row.getMultiIndex()));
        po.setRequired(parseOptionalBoolean(row.getRequired()));
    }

    private void applyProductMatchRuleRow(ProductMatchRulePO po, ProductMatchRuleSheetRowDTO row) {
        po.setFileTypeName(trimToNull(row.getFileTypeName()));
        po.setPdCd(trimToNull(row.getPdCd()));
        po.setPdNm(trimToNull(row.getPdNm()));
        po.setOrgCd(trimToNull(row.getOrgCd()));
        po.setOrgNm(trimToNull(row.getOrgNm()));
        po.setPdType(trimToNull(row.getPdType()));
        po.setSubjectSystem(trimToNull(row.getSubjectSystem()));
        po.setHoldingStatus(trimToNull(row.getHoldingStatus()));
        po.setEstablishedDate(parseLocalDate(row.getEstablishedDate()));
        po.setEffectiveFrequency(trimToNull(row.getEffectiveFrequency()));
        po.setDelayDays(parseInteger(row.getDelayDays()));
        po.setApprovalRequired(parseOptionalBoolean(row.getApprovalRequired()));
        po.setFileType(trimToNull(row.getFileType()));
        po.setMatchRules(trimToNull(row.getMatchRules()));
        po.setIsValid(parseOptionalEnabledInteger(row.getIsValid()));
        po.setMemo(trimToNull(row.getMemo()));
        po.setDebugName(trimToNull(row.getDebugName()));
        po.setJobName(trimToNull(row.getJobName()));
        po.setJobScene(trimToNull(row.getJobScene()));
    }

    private String validateFileParseSourceRow(FileParseSourceSheetRowDTO row) {
        if (row == null) {
            return "空行";
        }
        if (hasText(row.getId()) && parseLong(row.getId()) == null) {
            return "ID 必须是数字";
        }
        if (!hasText(row.getFileType())) {
            return "文件类型不能为空";
        }
        if (!hasText(row.getColumnMap())) {
            return "标准列编码不能为空";
        }
        if (!hasText(row.getColumnName())) {
            return "来源列名称不能为空";
        }
        if (hasText(row.getStatus()) && parseOptionalBoolean(row.getStatus()) == null) {
            return "启用状态只能填写启用/停用、是/否、true/false 或 1/0";
        }
        return null;
    }

    private String validateFileParseRuleRow(FileParseRuleSheetRowDTO row) {
        if (row == null) {
            return "空行";
        }
        if (hasText(row.getId()) && parseLong(row.getId()) == null) {
            return "ID 必须是数字";
        }
        if (!hasText(row.getFileScene())) {
            return "文件场景不能为空";
        }
        if (!hasText(row.getFileTypeName())) {
            return "文件类型名称不能为空";
        }
        if (!hasText(row.getRegionName())) {
            return "标准区域名称不能为空";
        }
        if (!hasText(row.getColumnMap())) {
            return "标准列编码不能为空";
        }
        if (!hasText(row.getColumnMapName())) {
            return "标准列名称不能为空";
        }
        if (hasText(row.getStatus()) && parseOptionalBoolean(row.getStatus()) == null) {
            return "启用状态只能填写启用/停用、是/否、true/false 或 1/0";
        }
        if (hasText(row.getMultiIndex()) && parseOptionalBoolean(row.getMultiIndex()) == null) {
            return "是否多实例指标只能填写是/否、true/false 或 1/0";
        }
        if (hasText(row.getRequired()) && parseOptionalBoolean(row.getRequired()) == null) {
            return "是否必需只能填写是/否、true/false 或 1/0";
        }
        return null;
    }

    private String validateProductMatchRuleRow(ProductMatchRuleSheetRowDTO row) {
        if (row == null) {
            return "空行";
        }
        if (hasText(row.getId()) && parseLong(row.getId()) == null) {
            return "ID 必须是数字";
        }
        if (!hasText(row.getPdCd())) {
            return "产品代码不能为空";
        }
        if (!hasText(row.getPdNm())) {
            return "产品名称不能为空";
        }
        if (!hasText(row.getMatchRules())) {
            return "匹配规则不能为空";
        }
        if (hasText(row.getApprovalRequired()) && parseOptionalBoolean(row.getApprovalRequired()) == null) {
            return "是否审批只能填写是/否、true/false 或 1/0";
        }
        if (hasText(row.getIsValid()) && parseOptionalEnabledInteger(row.getIsValid()) == null) {
            return "启用状态只能填写启用/停用、是/否、true/false 或 1/0";
        }
        if (hasText(row.getDelayDays()) && parseInteger(row.getDelayDays()) == null) {
            return "延迟天数必须是整数";
        }
        if (hasText(row.getEstablishedDate()) && parseLocalDate(row.getEstablishedDate()) == null) {
            return "成立日格式必须是 yyyy-MM-dd";
        }
        return null;
    }

    private boolean isBlankFileParseSourceRow(FileParseSourceSheetRowDTO row) {
        if (row == null) {
            return true;
        }
        return allBlank(row.getId(), row.getFileType(), row.getColumnMap(), row.getColumnMapName(), row.getColumnName(),
                row.getFileExtInfo(), row.getStatus(), row.getCreater(), row.getCreateTime(), row.getModifier(),
                row.getModifyTime());
    }

    private boolean isBlankFileParseRuleRow(FileParseRuleSheetRowDTO row) {
        if (row == null) {
            return true;
        }
        return allBlank(row.getId(), row.getFileScene(), row.getFileTypeName(), row.getRegionName(), row.getColumnMap(),
                row.getColumnMapName(), row.getStatus(), row.getMultiIndex(), row.getRequired(), row.getCreater(),
                row.getCreateTime(), row.getModifier(), row.getModifyTime());
    }

    private boolean isBlankProductMatchRuleRow(ProductMatchRuleSheetRowDTO row) {
        if (row == null) {
            return true;
        }
        return allBlank(row.getId(), row.getFileTypeName(), row.getPdCd(), row.getPdNm(), row.getOrgCd(), row.getOrgNm(),
                row.getPdType(), row.getSubjectSystem(), row.getHoldingStatus(), row.getEstablishedDate(),
                row.getEffectiveFrequency(), row.getDelayDays(), row.getApprovalRequired(), row.getFileType(),
                row.getMatchRules(), row.getIsValid(), row.getMemo(), row.getDebugName(), row.getJobName(),
                row.getJobScene(), row.getCreater(), row.getCreateTime(), row.getModifier(), row.getModifyTime());
    }

    private void addError(ParseIssueHandlingSaveResultDTO result, int rowNumber, String id, String message) {
        ParseIssueHandlingSaveErrorDTO error = new ParseIssueHandlingSaveErrorDTO();
        error.setRowNumber(rowNumber);
        error.setId(trimToNull(id));
        error.setMessage(message);
        result.getErrors().add(error);
        result.setFailedCount(result.getFailedCount() + 1);
    }

    private Set<Long> parseIdSet(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>();
        for (String id : ids) {
            Long value = parseLong(id);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private static Boolean parseOptionalBoolean(String value) {
        String normalized = normalizeBooleanText(value);
        if (!hasText(normalized)) {
            return null;
        }
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized)
                || "y".equals(normalized) || "是".equals(normalized) || "启用".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized)
                || "n".equals(normalized) || "否".equals(normalized) || "停用".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Integer parseOptionalEnabledInteger(String value) {
        Boolean enabled = parseOptionalBoolean(value);
        return enabled == null ? null : enabled ? 1 : 0;
    }

    private static String normalizeBooleanText(String value) {
        return trimToNull(value) == null ? null : trimToNull(value).toLowerCase();
    }

    private static Long parseLong(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static LocalDate parseLocalDate(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private static String formatDate(LocalDate value) {
        return value == null ? "" : DATE_FORMATTER.format(value);
    }

    private static String formatBoolean(Boolean value) {
        if (value == null) {
            return "";
        }
        return value ? "是" : "否";
    }

    private static String formatEnabledInteger(Integer value) {
        if (value == null) {
            return "";
        }
        return value == 1 ? "启用" : "停用";
    }

    private static String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private static boolean allBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return false;
            }
        }
        return true;
    }
}
