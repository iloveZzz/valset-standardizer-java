package com.yss.valset.transfer.application.impl.tagging;

import com.yss.valset.transfer.application.service.TransferObjectBusinessFieldProjectionUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.rule.TransferRuleFunctions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认文件主对象业务字段投影服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTransferObjectBusinessFieldProjectionService implements TransferObjectBusinessFieldProjectionUseCase {

    private static final int BUSINESS_DATE_FILE_SCAN_LIMIT = 10;
    private static final LocalDate MIN_BUSINESS_DATE = LocalDate.of(2016, 1, 1);
    private static final String BUSINESS_DATE_TAG_CODE = "BUSINESS_DATE";
    private static final String BUSINESS_ID_TAG_CODE = "BUSINESS_ID";
    private static final Pattern BASIC_ISO_DATE_PATTERN = Pattern.compile("(\\d{8})");
    private static final Pattern DASH_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2})");
    private static final Pattern SLASH_DATE_PATTERN = Pattern.compile("(\\d{4}/\\d{1,2}/\\d{1,2})");
    private static final Pattern CN_DATE_PATTERN = Pattern.compile("(\\d{4}年\\d{1,2}月\\d{1,2}日)");

    private final TransferRuleFunctions transferRuleFunctions = new TransferRuleFunctions();
    private final TransferObjectGateway transferObjectGateway;

    @Override
    public TransferObject project(TransferObject transferObject, List<TransferObjectTag> tags) {
        if (transferObject == null || !StringUtils.hasText(transferObject.transferId())) {
            return transferObject;
        }

        LocalDate businessDate = resolveBusinessDate(transferObject, tags);
        String businessId = resolveBusinessId(transferObject, tags);
        LocalDate receiveDate = resolveReceiveDate(transferObject);

        if (sameBusinessFields(transferObject, businessDate, businessId, receiveDate)) {
            return transferObject;
        }

        TransferObject updated = transferObject.withBusinessFields(businessDate, businessId, receiveDate);
        return transferObjectGateway.save(updated);
    }

    private boolean sameBusinessFields(TransferObject transferObject, LocalDate businessDate, String businessId, LocalDate receiveDate) {
        return equalsDate(transferObject.businessDate(), businessDate)
                && equalsText(transferObject.businessId(), businessId)
                && equalsDate(transferObject.receiveDate(), receiveDate);
    }

    private boolean equalsText(String left, String right) {
        String leftText = normalizeText(left);
        String rightText = normalizeText(right);
        return leftText.equals(rightText);
    }

    private boolean equalsDate(LocalDate left, LocalDate right) {
        return left == null ? right == null : left.equals(right);
    }

    private LocalDate resolveBusinessDate(TransferObject transferObject, List<TransferObjectTag> tags) {
        String candidate = firstNonBlank(
                findTagValue(tags, BUSINESS_DATE_TAG_CODE),
                findTagValue(tags, "BUSINESS_DATE_FILE_NAME"),
                transferObject.originalName()
        );
        LocalDate parsed = parseBusinessDate(candidate);
        if (parsed != null) {
            return parsed;
        }
        return resolveBusinessDateFromFileContent(transferObject);
    }

    private String resolveBusinessId(TransferObject transferObject, List<TransferObjectTag> tags) {
        String candidate = firstNonBlank(
                findTagValue(tags, BUSINESS_ID_TAG_CODE),
                transferObject.sourceRef(),
                transferObject.mailSubject(),
                transferObject.originalName()
        );
        return normalizeText(candidate).isBlank() ? transferObject.businessId() : normalizeText(candidate);
    }

    private LocalDate resolveReceiveDate(TransferObject transferObject) {
        if (transferObject.receiveDate() != null) {
            return transferObject.receiveDate();
        }
        Instant receivedAt = transferObject.receivedAt();
        return receivedAt == null ? null : receivedAt.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate resolveBusinessDateFromFileContent(TransferObject transferObject) {
        Path filePath = resolveBusinessDateFilePath(transferObject);
        if (filePath == null || !Files.exists(filePath) || !Files.isReadable(filePath)) {
            return null;
        }
        String fileName = filePath.getFileName() == null ? null : filePath.getFileName().toString();
        if (!isExcelFile(fileName) && !isCsvFile(fileName)) {
            return null;
        }
        try {
            List<List<String>> rows = isCsvFile(fileName)
                    ? transferRuleFunctions.readCsvDataWithin(filePath, BUSINESS_DATE_FILE_SCAN_LIMIT)
                    : transferRuleFunctions.readExcelDataWithin(filePath, BUSINESS_DATE_FILE_SCAN_LIMIT);
            for (List<String> row : rows) {
                if (row == null || row.isEmpty()) {
                    continue;
                }
                for (String cellValue : row) {
                    LocalDate parsed = parseBusinessDate(cellValue);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
            return null;
        } catch (Exception exception) {
            log.warn("文件内容提取业务日期失败，transferId={}，filePath={}，error={}",
                    transferObject == null ? null : transferObject.transferId(),
                    filePath,
                    exception.getMessage());
            return null;
        }
    }

    private Path resolveBusinessDateFilePath(TransferObject transferObject) {
        if (transferObject == null) {
            return null;
        }
        if (StringUtils.hasText(transferObject.localTempPath())) {
            return Path.of(transferObject.localTempPath());
        }
        if (StringUtils.hasText(transferObject.realStoragePath())) {
            return Path.of(transferObject.realStoragePath());
        }
        return null;
    }

    private boolean isExcelFile(String fileName) {
        String normalized = normalizeText(fileName).toLowerCase(Locale.ROOT);
        return normalized.endsWith(".xlsx") || normalized.endsWith(".xls");
    }

    private boolean isCsvFile(String fileName) {
        return normalizeText(fileName).toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private String findTagValue(List<TransferObjectTag> tags, String tagCode) {
        if (tags == null || tags.isEmpty() || !StringUtils.hasText(tagCode)) {
            return null;
        }
        String normalizedTagCode = tagCode.trim().toUpperCase(Locale.ROOT);
        for (TransferObjectTag tag : tags) {
            if (tag == null || !StringUtils.hasText(tag.tagCode())) {
                continue;
            }
            if (!normalizedTagCode.equals(tag.tagCode().trim().toUpperCase(Locale.ROOT))) {
                continue;
            }
            String candidate = firstNonBlank(tag.tagValue(), tag.matchedValue(), matchedValueFromSnapshot(tag.matchSnapshot()));
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String matchedValueFromSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        Object value = snapshot.get("matchedValue");
        return value == null ? null : String.valueOf(value);
    }

    private LocalDate parseBusinessDate(String candidate) {
        String text = normalizeText(candidate);
        if (text.isBlank()) {
            return null;
        }
        LocalDate direct = tryParseExact(text);
        if (isValidBusinessDate(direct)) {
            return direct;
        }
        for (Pattern pattern : List.of(BASIC_ISO_DATE_PATTERN, DASH_DATE_PATTERN, SLASH_DATE_PATTERN, CN_DATE_PATTERN)) {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            LocalDate parsed = tryParseExact(matcher.group(1));
            if (isValidBusinessDate(parsed)) {
                return parsed;
            }
        }
        return null;
    }

    private boolean isValidBusinessDate(LocalDate value) {
        return value != null && !value.isBefore(MIN_BUSINESS_DATE);
    }

    private LocalDate tryParseExact(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String candidate = text.trim();
        try {
            if (candidate.matches("\\d{8}")) {
                return LocalDate.parse(candidate, DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (candidate.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                return LocalDate.parse(candidate, DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
            if (candidate.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
                return LocalDate.parse(candidate, DateTimeFormatter.ofPattern("yyyy/M/d"));
            }
            if (candidate.matches("\\d{4}年\\d{1,2}月\\d{1,2}日")) {
                String normalized = candidate.replace("年", "-").replace("月", "-").replace("日", "");
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
        } catch (DateTimeParseException exception) {
            return null;
        }
        return null;
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
}
