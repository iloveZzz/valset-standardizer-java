package com.yss.valset.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.knowledge.StandardSubjectLoader;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.StandardSubject;
import com.yss.valset.infra.http.FeignJsonFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 API 的标准科目加载器。
 */
@Slf4j
@Component
public class ApiStandardSubjectLoader implements StandardSubjectLoader {

    private final FeignJsonFetcher feignJsonFetcher;
    private final ObjectMapper objectMapper;

    public ApiStandardSubjectLoader(FeignJsonFetcher feignJsonFetcher, ObjectMapper objectMapper) {
        this.feignJsonFetcher = feignJsonFetcher;
        this.objectMapper = objectMapper;
    }

    /**
     * 从 API 数据源加载标准科目。
     */
    @Override
    public List<StandardSubject> load(DataSourceConfig config) {
        String apiUrl = config.getSourceUri();
        List<StandardSubject> subjects = new ArrayList<>();
        log.info("开始从 API 加载标准科目，apiUrl={}", apiUrl);
        try {
            String jsonResponse = feignJsonFetcher.get(apiUrl);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    StandardSubject subject = StandardSubject.builder()
                            .standardCode(node.path("standardCode").asText(""))
                            .standardName(node.path("standardName").asText(""))
                            .build();
                    subjects.add(subject);
                }
            }
            log.info("API 标准科目加载完成，apiUrl={}, count={}", apiUrl, subjects.size());
            return subjects;
        } catch (Exception e) {
            log.error("API 标准科目加载失败，apiUrl={}", apiUrl, e);
            throw new IllegalStateException("Failed to load standard subject from API: " + apiUrl, e);
        }
    }
}
