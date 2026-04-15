package com.yss.subjectmatch.knowledge;

import com.yss.subjectmatch.domain.model.MappingSample;
import com.yss.subjectmatch.extract.support.ExcelParsingSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Apache POI 的映射评估样本解析器，用于从 Excel 文件解析 MappingSample。
 */
@Slf4j
@Component
public class PoiMappingSampleLoader {

    /**
     * 从工作簿解析映射示例。
     */
    public List<MappingSample> parse(Path mappingWorkbookPath) {
        log.info("开始解析映射评估样本，mappingWorkbookPath={}", mappingWorkbookPath);
        try (InputStream inputStream = Files.newInputStream(mappingWorkbookPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            List<MappingSample> samples = new ArrayList<>();
            for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<Object> rowValues = ExcelParsingSupport.readRowValues(row, evaluator, formatter);
                String externalName = ExcelParsingSupport.textAt(rowValues, 3);
                String standardCode = ExcelParsingSupport.textAt(rowValues, 4);
                String standardName = ExcelParsingSupport.textAt(rowValues, 5);
                if (externalName.isBlank() || standardCode.isBlank() || standardName.isBlank()) {
                    continue;
                }
                samples.add(MappingSample.builder()
                        .orgName(ExcelParsingSupport.textAt(rowValues, 0))
                        .orgId(ExcelParsingSupport.textAt(rowValues, 1))
                        .externalCode(ExcelParsingSupport.textAt(rowValues, 2))
                        .externalName(externalName)
                        .standardCode(standardCode)
                        .standardName(standardName)
                        .standardSystem(ExcelParsingSupport.textAt(rowValues, 6))
                        .systemName(ExcelParsingSupport.textAt(rowValues, 7))
                        .build());
            }
            log.info("映射评估样本解析完成，mappingWorkbookPath={}, count={}", mappingWorkbookPath, samples.size());
            return samples;
        } catch (IOException exception) {
            log.error("映射评估样本解析失败，mappingWorkbookPath={}", mappingWorkbookPath, exception);
            throw new IllegalStateException("Failed to load mapping workbook " + mappingWorkbookPath, exception);
        }
    }
}
