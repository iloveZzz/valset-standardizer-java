package com.yss.subjectmatch.analysis.parser.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 外部 API 估值表数据解析器。
 */
@Component
public class ApiValuationDataParser implements ValuationDataParser {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ApiValuationDataParser(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        String apiUrl = config.getSourceUri();
        List<SubjectRecord> subjects = new ArrayList<>();
        List<MetricRecord> metrics = new ArrayList<>();

        try {
            String jsonResponse = restTemplate.getForObject(apiUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    SubjectRecord subject = new SubjectRecord();
                    subject.setSubjectCode(node.path("subjectCode").asText(""));
                    subject.setSubjectName(node.path("subjectName").asText(""));
                    subjects.add(subject);
                }
            }

            return ParsedValuationData.builder()
                    .workbookPath(apiUrl)
                    .subjects(subjects)
                    .metrics(metrics)
                    .title("API Source: " + apiUrl)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch/parse API source: " + apiUrl, e);
        }
    }
}
