package com.yss.valset.knowledge;

import com.yss.valset.domain.knowledge.StandardSubjectLoader;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.StandardSubject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 CSV 的标准科目加载器。
 */
@Slf4j
@Component
public class CsvStandardSubjectLoader implements StandardSubjectLoader {

    /**
     * 从 CSV 数据源加载标准科目。
     */
    @Override
    public List<StandardSubject> load(DataSourceConfig config) {
        Path csvPath = Paths.get(config.getSourceUri());
        List<StandardSubject> subjects = new ArrayList<>();
        log.info("开始从 CSV 加载标准科目，csvPath={}", csvPath);

        try (Reader reader = new InputStreamReader(Files.newInputStream(csvPath));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
            for (CSVRecord record : csvParser) {
                String code = record.isMapped("StandardCode") ? record.get("StandardCode") : "";
                String name = record.isMapped("StandardName") ? record.get("StandardName") : "";
                if (!code.isBlank() && !name.isBlank()) {
                    StandardSubject subject = StandardSubject.builder()
                            .standardCode(code)
                            .standardName(name)
                            .build();
                    subjects.add(subject);
                }
            }
            log.info("CSV 标准科目加载完成，csvPath={}, count={}", csvPath, subjects.size());
            return subjects;
        } catch (Exception e) {
            log.error("CSV 标准科目加载失败，csvPath={}", csvPath, e);
            throw new IllegalStateException("Failed to load standard subject from CSV: " + config.getSourceUri(), e);
        }
    }
}
