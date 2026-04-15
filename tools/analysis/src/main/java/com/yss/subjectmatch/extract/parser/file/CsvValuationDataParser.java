package com.yss.subjectmatch.analysis.file;

import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV 格式估值表数据解析器。
 */
@Component
public class CsvValuationDataParser implements ValuationDataParser {

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        Path csvPath = Paths.get(config.getSourceUri());
        List<SubjectRecord> subjects = new ArrayList<>();
        List<MetricRecord> metrics = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        Map<String, String> basicInfo = new HashMap<>();

        try (Reader reader = new InputStreamReader(Files.newInputStream(csvPath));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            headers = new ArrayList<>(csvParser.getHeaderNames());

            for (CSVRecord csvRecord : csvParser) {
                String code = csvRecord.isMapped("SubjectCode") ? csvRecord.get("SubjectCode") : "";
                String name = csvRecord.isMapped("SubjectName") ? csvRecord.get("SubjectName") : "";

                if (!code.isEmpty() || !name.isEmpty()) {
                    SubjectRecord subject = new SubjectRecord();
                    subject.setSubjectCode(code);
                    subject.setSubjectName(name);
                    subjects.add(subject);
                }
            }

            return ParsedValuationData.builder()
                    .workbookPath(csvPath.toAbsolutePath().toString())
                    .headers(headers)
                    .subjects(subjects)
                    .metrics(metrics)
                    .basicInfo(basicInfo)
                    .title(csvPath.getFileName().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CSV source: " + config.getSourceUri(), e);
        }
    }
}
