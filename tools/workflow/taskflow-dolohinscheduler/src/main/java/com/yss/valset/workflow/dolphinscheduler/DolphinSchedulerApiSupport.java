package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * DolphinScheduler REST 调用辅助器。
 */
public class DolphinSchedulerApiSupport {

    @Getter
    @Setter
    private RestTemplate restTemplate = new RestTemplate();

    @Getter
    @Setter
    private ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    private String baseUrl = "";

    public boolean hasBaseUrl() {
        return StringUtils.hasText(baseUrl);
    }

    public JsonNode getJson(String path, MultiValueMap<String, String> queryParams, Object... uriVariables) {
        return exchangeJson(HttpMethod.GET, path, queryParams, null, null, uriVariables);
    }

    public JsonNode postFormJson(String path, MultiValueMap<String, String> formParams, Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return exchangeJson(HttpMethod.POST, path, null, formParams, headers, uriVariables);
    }

    public JsonNode postJson(String path, Object body, Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return exchangeJson(HttpMethod.POST, path, null, body, headers, uriVariables);
    }

    public JsonNode putFormJson(String path, MultiValueMap<String, String> formParams, Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return exchangeJson(HttpMethod.PUT, path, null, formParams, headers, uriVariables);
    }

    public JsonNode deleteJson(String path, Object... uriVariables) {
        return exchangeJson(HttpMethod.DELETE, path, null, null, null, uriVariables);
    }

    private JsonNode exchangeJson(HttpMethod method,
                                  String path,
                                  MultiValueMap<String, String> queryParams,
                                  Object body,
                                  HttpHeaders headers,
                                  Object... uriVariables) {
        if (!hasBaseUrl()) {
            throw new IllegalStateException("未配置 DolphinScheduler 基础地址");
        }
        URI uri = buildUri(path, queryParams, uriVariables);
        HttpHeaders effectiveHeaders = headers == null ? new HttpHeaders() : headers;
        if (!effectiveHeaders.containsKey(HttpHeaders.CONTENT_TYPE) && body != null) {
            effectiveHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, effectiveHeaders);
        ResponseEntity<String> response = restTemplate.exchange(uri, method, entity, String.class);
        String responseBody = response.getBody();
        if (!StringUtils.hasText(responseBody)) {
            return objectMapper.createObjectNode().put("code", 0).put("msg", "success").set("data", objectMapper.createObjectNode());
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("解析 DolphinScheduler 返回值失败：" + exception.getMessage(), exception);
        }
    }

    private URI buildUri(String path, MultiValueMap<String, String> queryParams, Object... uriVariables) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl())
                .path(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            builder.queryParams(queryParams);
        }
        return builder.buildAndExpand(uriVariables == null ? new Object[0] : uriVariables).encode().toUri();
    }

    private String normalizeBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
