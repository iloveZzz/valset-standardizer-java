package com.yss.valset.extract.parser.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.infra.http.FeignJsonFetcher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ApiValuationDataParserTest {

    @Test
    void shouldParseValuationDataFromFeignApi() throws Exception {
        HttpServer server = createServer("{\"data\":[{\"subjectCode\":\"A001\",\"subjectName\":\"货币资金\"}]}");
        int port = server.getAddress().getPort();
        try {
            server.start();
            ApiValuationDataParser parser = new ApiValuationDataParser(new FeignJsonFetcher(), new ObjectMapper());
            ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                    .sourceType(DataSourceType.API)
                    .sourceUri("http://localhost:" + port + "/valuation")
                    .build());

            assertThat(parsed.getTitle()).isEqualTo("API Source: http://localhost:" + port + "/valuation");
            assertThat(parsed.getSubjects()).hasSize(1);
            assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("A001");
            assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("货币资金");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer createServer(String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/valuation", exchange -> writeJson(exchange, body));
        return server;
    }

    private void writeJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
