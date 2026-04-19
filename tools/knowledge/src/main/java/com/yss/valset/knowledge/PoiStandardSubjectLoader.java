package com.yss.valset.knowledge;

import com.yss.valset.domain.knowledge.StandardSubjectLoader;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.StandardSubject;
import com.yss.valset.extract.support.ExcelParsingSupport;
import com.yss.valset.extract.support.MatchTextSupport;
import com.yss.valset.extract.support.SubjectHierarchySupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 基于 Apache POI 的标准科目加载器。
 */
@Slf4j
@Component
public class PoiStandardSubjectLoader implements StandardSubjectLoader {

    /**
     * 从数据源加载标准科目。
     */
    @Override
    public List<StandardSubject> load(DataSourceConfig config) {
        Path standardWorkbookPath = Paths.get(config.getSourceUri());
        log.info("开始从 Excel 加载标准科目，workbookPath={}", standardWorkbookPath);
        try (InputStream inputStream = Files.newInputStream(standardWorkbookPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);

            List<BaseRow> baseRows = new ArrayList<>();
            Set<String> existingCodes = new LinkedHashSet<>();
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<Object> rowValues = ExcelParsingSupport.readRowValues(row, evaluator, formatter);
                String standardCode = ExcelParsingSupport.textAt(rowValues, 0);
                String standardName = ExcelParsingSupport.textAt(rowValues, 1);
                if (standardCode.isBlank() || standardName.isBlank()) {
                    continue;
                }
                List<String> segments = SubjectHierarchySupport.splitSubjectCode(standardCode);
                List<String> pathCodes = SubjectHierarchySupport.buildSubjectPathCodes(standardCode, segments);
                existingCodes.add(standardCode);
                baseRows.add(new BaseRow(standardCode, standardName, segments, pathCodes));
            }

            Map<String, String> nameMap = new LinkedHashMap<>();
            for (BaseRow baseRow : baseRows) {
                nameMap.put(baseRow.standardCode(), baseRow.standardName());
            }

            List<StandardSubject> subjects = new ArrayList<>();
            for (BaseRow baseRow : baseRows) {
                String parentCode = SubjectHierarchySupport.findExistingParentCode(baseRow.pathCodes(), existingCodes);
                List<String> pathNames = new ArrayList<>(baseRow.pathCodes().size());
                for (String pathCode : baseRow.pathCodes()) {
                    pathNames.add(nameMap.getOrDefault(pathCode, ""));
                }
                String pathText = MatchTextSupport.buildPathText(pathNames);
                subjects.add(StandardSubject.builder()
                        .standardCode(baseRow.standardCode())
                        .standardName(baseRow.standardName())
                        .parentCode(parentCode)
                        .parentName(nameMap.get(parentCode))
                        .level(baseRow.pathCodes().size())
                        .rootCode(baseRow.pathCodes().isEmpty() ? baseRow.standardCode() : baseRow.pathCodes().get(0))
                        .segmentCount(baseRow.segments().size())
                        .pathCodes(baseRow.pathCodes())
                        .pathNames(pathNames)
                        .pathText(pathText)
                        .normalizedName(MatchTextSupport.normalizeMatchText(baseRow.standardName()))
                        .normalizedPathText(MatchTextSupport.normalizeMatchText(pathText))
                        .placeholder(MatchTextSupport.isPlaceholderSubject(baseRow.standardCode(), baseRow.standardName()))
                        .build());
            }
            subjects.sort(Comparator
                    .comparing(StandardSubject::getRootCode, Comparator.nullsLast(String::compareTo))
                    .thenComparing(StandardSubject::getLevel, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(StandardSubject::getStandardCode, Comparator.nullsLast(String::compareTo)));
            log.info("Excel 标准科目加载完成，workbookPath={}, count={}", standardWorkbookPath, subjects.size());
            return subjects;
        } catch (IOException exception) {
            log.error("Excel 标准科目加载失败，workbookPath={}", standardWorkbookPath, exception);
            throw new IllegalStateException("Failed to load standard subject workbook " + standardWorkbookPath, exception);
        }
    }

    private record BaseRow(
            String standardCode,
            String standardName,
            List<String> segments,
            List<String> pathCodes
    ) {
    }
}
