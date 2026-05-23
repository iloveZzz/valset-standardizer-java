package com.yss.valset.task.application.impl.product;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.task.application.command.product.ProductInfoExtractionPreviewCommand;
import com.yss.valset.task.application.command.product.ProductInfoExtractionSaveCommand;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionCandidateDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionPreviewDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionSaveItemDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionSaveResultDTO;
import com.yss.valset.task.application.dto.product.ProductInfoOptionDTO;
import com.yss.valset.task.application.service.product.ProductInfoExtractionAppService;
import com.yss.valset.task.infrastructure.dto.product.ProductInfoExtractionCandidateRow;
import com.yss.valset.task.infrastructure.dto.product.ProductInfoExtractionValuationTitleRow;
import com.yss.valset.task.infrastructure.mapper.product.ProductInfoExtractionQueryMapper;
import com.yss.valset.transfer.infrastructure.entity.ProductMatchRulePO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectTagPO;
import com.yss.valset.transfer.infrastructure.entity.TransferTagPO;
import com.yss.valset.transfer.infrastructure.mapper.ProductMatchRuleRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectTagRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 默认产品信息提取应用服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultProductInfoExtractionAppService implements ProductInfoExtractionAppService {

    static final String DEFAULT_PRODUCT_TYPE = "委外产品";
    static final String DEFAULT_SUBJECT_SYSTEM = "默认科目体系";
    static final String DEFAULT_MANAGER_CODE = "ALL";
    static final String DEFAULT_MANAGER_NAME = "临时机构";
    static final String DEFAULT_HOLDING_STATUS = "存续";
    static final String DEFAULT_EFFECTIVE_FREQUENCY = "日";
    static final int DEFAULT_DELAY_DAYS = 2;
    static final String VALUATION_TABLE_TAG_CODE = "VALUATION_TABLE";
    static final String PRODUCT_MATCH_RULE_TAG_CODE = "PRODUCT_MATCH_RULE";
    static final String MATCH_REASON = "产品信息提取新增规则";

    private final ProductInfoExtractionQueryMapper queryMapper;
    private final TransferObjectRepository transferObjectRepository;
    private final ProductMatchRuleRepository productMatchRuleRepository;
    private final TransferObjectTagRepository transferObjectTagRepository;
    private final TransferTagRepository transferTagRepository;

    @Override
    public PageResult<ProductInfoExtractionCandidateDTO> pageCandidates(String productType,
                                                                        String originalName,
                                                                        Integer pageIndex,
                                                                        Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 0 : pageIndex;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        String normalizedOriginalName = trimToNull(originalName);
        long total = queryMapper.countCandidates(normalizedOriginalName);
        List<ProductInfoExtractionCandidateDTO> data = total <= 0
                ? Collections.emptyList()
                : queryMapper.pageCandidates(normalizedOriginalName, current * size, size)
                .stream()
                .map(this::toCandidate)
                .collect(Collectors.toList());
        return PageResult.of(data, total, size, current);
    }

    @Override
    public PageResult<ProductInfoOptionDTO> pageProductOptions(String keyword,
                                                               Integer pageIndex,
                                                               Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 0 : pageIndex;
        int size = pageSize == null || pageSize <= 0 ? 20 : pageSize;
        String normalizedKeyword = trimToNull(keyword);
        long total = queryMapper.countProductOptions(normalizedKeyword);
        List<ProductInfoOptionDTO> data = total <= 0
                ? Collections.emptyList()
                : queryMapper.pageProductOptions(normalizedKeyword, current * size, size);
        return PageResult.of(data, total, size, current);
    }

    @Override
    public List<ProductInfoExtractionPreviewDTO> preview(ProductInfoExtractionPreviewCommand command) {
        if (command == null || CollectionUtils.isEmpty(command.getTransferIds())) {
            return Collections.emptyList();
        }
        List<String> transferIds = normalizeTransferIds(command.getTransferIds());
        if (transferIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, TransferObjectPO> objectMap = loadTransferObjects(transferIds);
        String productType = firstText(command.getProductType(), DEFAULT_PRODUCT_TYPE);
        LocalDate establishedDate = LocalDate.now().minusYears(2);
        Map<String, String> titleMap = loadValuationTitleMap(objectMap);
        List<ProductInfoExtractionPreviewDTO> previews = new ArrayList<>();
        for (String transferId : transferIds) {
            TransferObjectPO object = objectMap.get(transferId);
            if (object == null) {
                continue;
            }
            previews.add(buildPreview(object, productType, establishedDate, titleMap.get(transferId)));
        }
        return previews;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductInfoExtractionSaveResultDTO saveRules(ProductInfoExtractionSaveCommand command) {
        ProductInfoExtractionSaveResultDTO result = new ProductInfoExtractionSaveResultDTO();
        if (command == null || CollectionUtils.isEmpty(command.getItems())) {
            return result;
        }
        TransferTagPO productTagDefinition = loadProductTagDefinition();
        List<String> transferIds = command.getItems().stream()
                .map(ProductInfoExtractionPreviewDTO::getTransferId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        Map<String, TransferObjectPO> objectMap = loadTransferObjects(transferIds);
        Map<String, Boolean> existingProductTagMap = loadExistingProductTagMap(transferIds);
        for (ProductInfoExtractionPreviewDTO item : command.getItems()) {
            ProductInfoExtractionSaveItemDTO itemResult = saveOne(item, productTagDefinition, objectMap, existingProductTagMap);
            result.getItems().add(itemResult);
            if ("SUCCESS".equals(itemResult.getStatus())) {
                result.setSuccessCount(result.getSuccessCount() + 1);
            } else if ("SKIPPED".equals(itemResult.getStatus())) {
                result.setSkippedCount(result.getSkippedCount() + 1);
            } else {
                result.setFailedCount(result.getFailedCount() + 1);
            }
        }
        return result;
    }

    ProductInfoExtractionPreviewDTO buildPreview(TransferObjectPO object, String productType, LocalDate establishedDate) {
        return buildPreview(object, productType, establishedDate, null);
    }

    ProductInfoExtractionPreviewDTO buildPreview(TransferObjectPO object,
                                                 String productType,
                                                 LocalDate establishedDate,
                                                 String valuationTitle) {
        String originalName = object == null ? "" : firstText(object.getOriginalName(), "");
        FileNameParts parts = parseFileName(originalName);
        FileNameParts titleParts = parseValuationTitle(valuationTitle);
        parts = applyTitleFallback(parts, titleParts);
        ProductInfoExtractionPreviewDTO preview = new ProductInfoExtractionPreviewDTO();
        preview.setTransferId(object == null ? null : object.getTransferId());
        preview.setOriginalName(originalName);
        preview.setProductType(firstText(productType, DEFAULT_PRODUCT_TYPE));
        preview.setSubjectSystem(DEFAULT_SUBJECT_SYSTEM);
        preview.setManagerCode(DEFAULT_MANAGER_CODE);
        preview.setManagerName(DEFAULT_MANAGER_NAME);
        preview.setHoldingStatus(DEFAULT_HOLDING_STATUS);
        preview.setEstablishedDate(establishedDate);
        preview.setProductCode(parts.productCode);
        preview.setProductName(parts.productName);
        preview.setMatchRule(buildMatchRule(originalName));
        preview.setEffectiveFrequency(DEFAULT_EFFECTIVE_FREQUENCY);
        preview.setDelayDays(DEFAULT_DELAY_DAYS);
        preview.setApprovalRequired(Boolean.TRUE);
        return preview;
    }

    FileNameParts parseFileName(String fileName) {
        String baseName = removeExtension(firstText(fileName, ""));
        String normalizedName = removeTrailingBusinessDate(baseName);
        String productCode = extractProductCode(normalizedName);
        String productName = normalizedName;
        if (StringUtils.hasText(productCode) && productName.startsWith(productCode)) {
            productName = productName.substring(productCode.length());
        }
        productName = cleanupProductName(productName);
        return new FileNameParts(productCode, productName);
    }

    FileNameParts parseValuationTitle(String title) {
        String normalizedTitle = normalizeValuationTitle(title);
        if (!StringUtils.hasText(normalizedTitle)) {
            return new FileNameParts(null, null);
        }
        FileNameParts explicitParts = parseExplicitTitleFields(normalizedTitle);
        if (StringUtils.hasText(explicitParts.productCode) || StringUtils.hasText(explicitParts.productName)) {
            return explicitParts;
        }
        return parseFileName(normalizedTitle);
    }

    String buildMatchRule(String fileName) {
        String baseName = removeExtension(firstText(fileName, ""));
        if (!StringUtils.hasText(baseName)) {
            return "";
        }
        FileNameParts fileNameParts = parseFileName(fileName);
        if (StringUtils.hasText(fileNameParts.productCode)) {
            return "(.*)" + Pattern.quote(fileNameParts.productCode) + "(.*)";
        }
        String normalizedRule = buildDateWildcardRule(baseName);
        return StringUtils.hasText(normalizedRule) ? normalizedRule : escapeRegexLiteral(baseName) + "(.*)";
    }

    private FileNameParts applyTitleFallback(FileNameParts fileNameParts, FileNameParts titleParts) {
        if (titleParts == null
                || !StringUtils.hasText(titleParts.productName) && !StringUtils.hasText(titleParts.productCode)) {
            return fileNameParts;
        }
        String productCode = fileNameParts == null ? null : fileNameParts.productCode;
        String productName = fileNameParts == null ? null : fileNameParts.productName;
        if (!StringUtils.hasText(productCode) && StringUtils.hasText(titleParts.productCode)) {
            productCode = titleParts.productCode;
        }
        if (shouldFallbackProductName(productName) && StringUtils.hasText(titleParts.productName)) {
            productName = titleParts.productName;
        }
        return new FileNameParts(productCode, productName);
    }

    private FileNameParts parseExplicitTitleFields(String title) {
        String productCode = extractTitleCode(title);
        String productName = extractTitleName(title);
        return new FileNameParts(trimToNull(productCode), cleanupTitleProductName(productName));
    }

    private String extractTitleCode(String title) {
        java.util.regex.Matcher matcher = Pattern.compile("(产品|基金|组合|资产单元|账套)(代码|编码)\\s*[:：]?\\s*([A-Za-z][A-Za-z0-9_-]*)")
                .matcher(firstText(title, ""));
        return matcher.find() ? matcher.group(3) : null;
    }

    private String extractTitleName(String title) {
        String text = firstText(title, "");
        java.util.regex.Matcher matcher = Pattern.compile("(产品|基金|组合|资产单元|账套)(名称|简称)\\s*[:：]?\\s*")
                .matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int start = matcher.end();
        int end = findNextTitleFieldStart(text, start);
        return text.substring(start, end);
    }

    private int findNextTitleFieldStart(String text, int start) {
        java.util.regex.Matcher matcher = Pattern.compile("\\s+(产品|基金|组合|资产单元|账套)(代码|编码|名称|简称)\\s*[:：]?")
                .matcher(text);
        if (matcher.find(start)) {
            return matcher.start();
        }
        return text.length();
    }

    private String normalizeValuationTitle(String title) {
        String text = firstText(title, "");
        text = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        text = text.replaceAll("\\s+", " ");
        return trimToNull(text);
    }

    private String cleanupTitleProductName(String value) {
        String text = cleanupProductName(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        text = text.replaceFirst("^[\\s:：_\\-—]+", "");
        text = text.replaceAll("(?i)[\\s_\\-—]*((资产)?估值(报表|表)?|净值表)[\\s_\\-—]*\\d{6,8}$", "");
        text = text.replaceAll("(?i)[\\s_\\-—]*((资产)?估值(报表|表)?|净值表)[\\s_\\-—]*$", "");
        text = removeTrailingBusinessDate(text);
        text = text.replaceAll("[\\s:：_\\-—]+$", "");
        return trimToNull(text);
    }

    private ProductInfoExtractionSaveItemDTO saveOne(ProductInfoExtractionPreviewDTO item,
                                                     TransferTagPO productTagDefinition,
                                                     Map<String, TransferObjectPO> objectMap,
                                                     Map<String, Boolean> existingProductTagMap) {
        ProductInfoExtractionSaveItemDTO result = new ProductInfoExtractionSaveItemDTO();
        result.setTransferId(item == null ? null : item.getTransferId());
        result.setOriginalName(item == null ? null : item.getOriginalName());
        result.setProductCode(item == null ? null : item.getProductCode());
        result.setProductName(item == null ? null : item.getProductName());
        String validationMessage = validateSaveItem(item);
        if (validationMessage != null) {
            result.setStatus("FAILED");
            result.setMessage(validationMessage);
            return result;
        }
        String transferId = item.getTransferId().trim();
        TransferObjectPO object = objectMap.get(transferId);
        if (object == null) {
            result.setStatus("FAILED");
            result.setMessage("未找到分拣对象");
            return result;
        }
        if (Boolean.TRUE.equals(existingProductTagMap.get(transferId))) {
            result.setStatus("SKIPPED");
            result.setMessage("分拣对象已存在产品识别规则标签");
            return result;
        }
        Long ruleId = IdWorker.getId();
        ProductMatchRulePO rule = toRulePO(ruleId, item, object);
        try {
            productMatchRuleRepository.insert(rule);
        } catch (DataIntegrityViolationException exception) {
            result.setStatus("FAILED");
            result.setMessage(resolveProductMatchRuleInsertMessage(exception));
            return result;
        }
        transferObjectTagRepository.insert(toProductTagPO(productTagDefinition, object, ruleId, item));
        existingProductTagMap.put(transferId, Boolean.TRUE);
        result.setRuleId(String.valueOf(ruleId));
        result.setStatus("SUCCESS");
        result.setMessage("保存成功");
        return result;
    }

    private String resolveProductMatchRuleInsertMessage(DataIntegrityViolationException exception) {
        String message = exception == null ? null : exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("ORA-01438")) {
            return "产品识别规则保存失败：数据库字段精度不足，请检查 TP_MATCH_RULES.ID 是否为 NUMBER(20)";
        }
        return "产品识别规则保存失败：" + firstText(message, "数据库约束校验失败");
    }

    private ProductMatchRulePO toRulePO(Long ruleId, ProductInfoExtractionPreviewDTO item, TransferObjectPO object) {
        LocalDateTime now = LocalDateTime.now();
        ProductMatchRulePO rule = new ProductMatchRulePO();
        rule.setId(ruleId);
        rule.setFileTypeName(firstText(object.getExtension(), object.getMimeType(), "VALUATION_TABLE"));
        rule.setPdCd(trimToNull(item.getProductCode()));
        rule.setPdNm(trimToNull(item.getProductName()));
        rule.setOrgCd(firstText(item.getManagerCode(), DEFAULT_MANAGER_CODE));
        rule.setOrgNm(firstText(item.getManagerName(), DEFAULT_MANAGER_NAME));
        rule.setPdType(firstText(item.getProductType(), DEFAULT_PRODUCT_TYPE));
        rule.setSubjectSystem(firstText(item.getSubjectSystem(), DEFAULT_SUBJECT_SYSTEM));
        rule.setHoldingStatus(firstText(item.getHoldingStatus(), DEFAULT_HOLDING_STATUS));
        rule.setEstablishedDate(item.getEstablishedDate());
        rule.setEffectiveFrequency(firstText(item.getEffectiveFrequency(), DEFAULT_EFFECTIVE_FREQUENCY));
        rule.setDelayDays(item.getDelayDays() == null ? DEFAULT_DELAY_DAYS : item.getDelayDays());
        rule.setApprovalRequired(item.getApprovalRequired() == null ? Boolean.TRUE : item.getApprovalRequired());
        rule.setFileType(firstText(object.getExtension(), object.getMimeType(), "VALUATION_TABLE"));
        rule.setMatchRules(trimToNull(item.getMatchRule()));
        rule.setCreater("product-info-extraction");
        rule.setCreateTime(now);
        rule.setModifier("product-info-extraction");
        rule.setModifyTime(now);
        rule.setIsValid(1);
        rule.setMemo("产品信息提取自动生成");
        rule.setDebugName(object.getOriginalName());
        rule.setJobName("VALUATION_PRODUCT_INFO_EXTRACTION");
        rule.setJobScene("VALUATION_TABLE");
        return rule;
    }

    private TransferObjectTagPO toProductTagPO(TransferTagPO definition,
                                               TransferObjectPO object,
                                               Long ruleId,
                                               ProductInfoExtractionPreviewDTO item) {
        TransferObjectTagPO tag = new TransferObjectTagPO();
        tag.setId(String.valueOf(IdWorker.getId()));
        tag.setTransferId(object.getTransferId());
        tag.setTagId(definition.getTagId());
        tag.setTagCode(PRODUCT_MATCH_RULE_TAG_CODE);
        tag.setTagName(firstText(definition.getTagName(), "产品识别规则"));
        tag.setTagValue(String.valueOf(ruleId));
        tag.setMatchStrategy(firstText(definition.getMatchStrategy(), "SCRIPT_RULE"));
        tag.setMatchReason(MATCH_REASON);
        tag.setMatchedField("originalName");
        tag.setMatchedValue(object.getOriginalName());
        tag.setMatchSnapshotJson("{\"ruleId\":\"" + ruleId + "\",\"productCode\":\"" + jsonEscape(item.getProductCode()) + "\",\"productName\":\"" + jsonEscape(item.getProductName()) + "\"}");
        tag.setCreatedAt(LocalDateTime.now());
        return tag;
    }

    private ProductInfoExtractionCandidateDTO toCandidate(ProductInfoExtractionCandidateRow row) {
        ProductInfoExtractionCandidateDTO dto = new ProductInfoExtractionCandidateDTO();
        dto.setTransferId(row.getTransferId());
        dto.setOriginalName(row.getOriginalName());
        dto.setSourceType(row.getSourceType());
        dto.setSourceCode(row.getSourceCode());
        dto.setStatus(row.getStatus());
        dto.setDeliveryStatus("DELIVERED");
        dto.setValuationTagName(row.getValuationTagName());
        dto.setReceiveMode(resolveReceiveMode(row.getSourceType()));
        dto.setReceivedAt(row.getReceivedAt());
        return dto;
    }

    private TransferTagPO loadProductTagDefinition() {
        List<TransferTagPO> definitions = transferTagRepository.selectList(
                Wrappers.lambdaQuery(TransferTagPO.class)
                        .eq(TransferTagPO::getTagCode, PRODUCT_MATCH_RULE_TAG_CODE)
                        .orderByAsc(TransferTagPO::getTagId)
        );
        TransferTagPO definition = definitions.isEmpty() ? null : definitions.get(0);
        if (definition == null || !StringUtils.hasText(definition.getTagId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未配置产品识别规则标签定义");
        }
        return definition;
    }

    private Map<String, TransferObjectPO> loadTransferObjects(List<String> transferIds) {
        if (CollectionUtils.isEmpty(transferIds)) {
            return Collections.emptyMap();
        }
        return transferObjectRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectPO.class)
                                .in(TransferObjectPO::getTransferId, transferIds)
                )
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        TransferObjectPO::getTransferId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<String, String> loadValuationTitleMap(Map<String, TransferObjectPO> objectMap) {
        if (objectMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> fileIdTransferIdMap = new LinkedHashMap<>();
        for (TransferObjectPO object : objectMap.values()) {
            Long fileId = parseLong(object.getTransferId());
            if (fileId != null) {
                fileIdTransferIdMap.put(fileId, object.getTransferId());
            }
        }
        if (fileIdTransferIdMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        List<ProductInfoExtractionValuationTitleRow> rows = queryMapper.listValuationTitlesByFileIds(
                new ArrayList<>(fileIdTransferIdMap.keySet())
        );
        for (ProductInfoExtractionValuationTitleRow row : rows) {
            if (row == null || row.getFileId() == null || !StringUtils.hasText(row.getTitle())) {
                continue;
            }
            String transferId = fileIdTransferIdMap.get(row.getFileId());
            if (StringUtils.hasText(transferId)) {
                result.put(transferId, row.getTitle());
            }
        }
        return result;
    }

    private Map<String, Boolean> loadExistingProductTagMap(List<String> transferIds) {
        if (CollectionUtils.isEmpty(transferIds)) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        transferObjectTagRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectTagPO.class)
                                .in(TransferObjectTagPO::getTransferId, transferIds)
                                .eq(TransferObjectTagPO::getTagCode, PRODUCT_MATCH_RULE_TAG_CODE)
                )
                .forEach(tag -> result.put(tag.getTransferId(), Boolean.TRUE));
        return result;
    }

    private List<String> normalizeTransferIds(List<String> transferIds) {
        return transferIds.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String validateSaveItem(ProductInfoExtractionPreviewDTO item) {
        if (item == null || !StringUtils.hasText(item.getTransferId())) {
            return "缺少分拣对象主键";
        }
        if (!StringUtils.hasText(item.getProductCode())) {
            return "产品代码不能为空";
        }
        if (!StringUtils.hasText(item.getProductName())) {
            return "产品名称不能为空";
        }
        if (!StringUtils.hasText(item.getMatchRule())) {
            return "匹配规则不能为空";
        }
        return null;
    }

    private String removeExtension(String fileName) {
        String text = firstText(fileName, "");
        int slashIndex = Math.max(text.lastIndexOf('/'), text.lastIndexOf('\\'));
        if (slashIndex >= 0) {
            text = text.substring(slashIndex + 1);
        }
        int dotIndex = text.lastIndexOf('.');
        return dotIndex > 0 ? text.substring(0, dotIndex) : text;
    }

    private String removeTrailingBusinessDate(String value) {
        String text = firstText(value, "");
        if (text.length() < 7) {
            return text;
        }
        int end = text.length();
        int start = end;
        while (start > 0 && Character.isDigit(text.charAt(start - 1))) {
            start--;
        }
        int digitLength = end - start;
        if (digitLength < 6 || digitLength > 8) {
            return text;
        }
        if (start <= 0) {
            return text;
        }
        char before = text.charAt(start - 1);
        if (Character.isLetterOrDigit(before)) {
            return text;
        }
        return text.substring(0, start);
    }

    private String extractProductCode(String baseName) {
        String text = firstText(baseName, "");
        List<String> candidates = new ArrayList<>();
        java.util.regex.Matcher matcher = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*").matcher(text);
        while (matcher.find()) {
            String candidate = trimToNull(matcher.group());
            if (candidate == null) {
                continue;
            }
            if (looksLikeBusinessDate(candidate)) {
                continue;
            }
            candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort((left, right) -> {
            int leftScore = scoreProductCodeCandidate(left);
            int rightScore = scoreProductCodeCandidate(right);
            if (leftScore != rightScore) {
                return Integer.compare(rightScore, leftScore);
            }
            return Integer.compare(right.length(), left.length());
        });
        return trimToNull(candidates.get(0).replaceAll("[_\\-]+$", ""));
    }

    private String cleanupProductName(String value) {
        String text = firstText(value, "");
        text = text.replaceFirst("^[\\s_\\-—]+", "");
        text = text.replaceAll("(?i)[_\\-—]+估值(报表|表)?[_\\-—]*\\d{6,8}$", "");
        text = text.replaceAll("(?i)[_\\-—]+估值(报表|表)?[_\\-—]*$", "");
        text = text.replaceAll("(?i)[\\s_\\-—]*((资产)?估值(报表|表)?|净值表)[\\s_\\-—]*\\d{6,8}$", "");
        text = text.replaceAll("(?i)[\\s_\\-—]*((资产)?估值(报表|表)?|净值表)[\\s_\\-—]*$", "");
        text = text.replaceAll("[\\s_\\-—]+$", "");
        return trimToNull(text);
    }

    private int scoreProductCodeCandidate(String candidate) {
        int score = 0;
        if (containsLetter(candidate)) {
            score += 10;
        }
        if (containsDigit(candidate)) {
            score += 20;
        }
        if (candidate.contains("_")) {
            score += 3;
        }
        if (candidate.contains("-")) {
            score += 2;
        }
        score += Math.min(candidate.length(), 30);
        return score;
    }

    private boolean containsLetter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isLetter(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isDigit(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeBusinessDate(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String text = value.replaceAll("[_\\-]+", "");
        return text.matches("\\d{6,8}");
    }

    private String buildDateWildcardRule(String baseName) {
        List<String> pieces = new ArrayList<>();
        java.util.regex.Matcher matcher = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}|\\d{8}|\\d{6}").matcher(firstText(baseName, ""));
        int cursor = 0;
        boolean foundDate = false;
        while (matcher.find()) {
            String literal = baseName.substring(cursor, matcher.start());
            if (StringUtils.hasText(literal)) {
                pieces.add(escapeRegexLiteral(removeTrailingRuleSeparator(literal)));
            }
            pieces.add("(.*)");
            cursor = matcher.end();
            foundDate = true;
        }
        if (!foundDate) {
            return "";
        }
        String literal = baseName.substring(cursor);
        if (StringUtils.hasText(literal)) {
            pieces.add(escapeRegexLiteral(removeLeadingRuleSeparator(literal)));
        }
        if (pieces.isEmpty() || !"(.*)".equals(pieces.get(pieces.size() - 1))) {
            pieces.add("(.*)");
        }
        return normalizeWildcardRule(String.join("", pieces));
    }

    private String normalizeWildcardRule(String rule) {
        String text = firstText(rule, "");
        text = text.replaceAll("(\\(\\.\\*\\))+", "(.*)");
        return trimToNull(text);
    }

    private String escapeRegexLiteral(String literal) {
        String text = firstText(literal, "");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if ("\\.[]{}()*+-?^$|".indexOf(value) >= 0) {
                builder.append('\\');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String removeTrailingRuleSeparator(String value) {
        return firstText(value, "").replaceAll("[\\s_\\-—]+$", "");
    }

    private String removeLeadingRuleSeparator(String value) {
        return firstText(value, "").replaceAll("^[\\s_\\-—]+", "");
    }

    private boolean shouldFallbackProductName(String productName) {
        if (!StringUtils.hasText(productName)) {
            return true;
        }
        String normalized = productName.replaceAll("[\\s_\\-—（）()]", "");
        return normalized.isEmpty()
                || normalized.matches("(普通)?(资产)?估值(报表|表)?")
                || normalized.matches("(普通)?净值表");
    }

    private String resolveReceiveMode(String sourceType) {
        String normalized = firstText(sourceType, "").toUpperCase(Locale.ROOT);
        if ("HTTP".equals(normalized)) {
            return "手动上传";
        }
        if ("EMAIL".equals(normalized)) {
            return "邮件收取";
        }
        if ("SFTP".equals(normalized)) {
            return "SFTP收取";
        }
        return StringUtils.hasText(sourceType) ? sourceType : "-";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String jsonEscape(String value) {
        return firstText(value, "").replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static final class FileNameParts {
        private final String productCode;
        private final String productName;

        private FileNameParts(String productCode, String productName) {
            this.productCode = productCode;
            this.productName = productName;
        }
    }
}
