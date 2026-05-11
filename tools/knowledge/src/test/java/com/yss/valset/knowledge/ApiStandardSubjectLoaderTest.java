package com.yss.valset.knowledge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.StandardSubject;
import com.yss.valset.infra.http.FeignJsonFetcher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiStandardSubjectLoaderTest {

    @Test
    void shouldLoadStandardSubjectsFromFeignApi() throws Exception {
        HttpServer server = createServer("{\"data\":[{\"standardCode\":\"1001\",\"standardName\":\"现金\"}]}");
        int port = server.getAddress().getPort();
        try {
            server.start();
            ApiStandardSubjectLoader loader = new ApiStandardSubjectLoader(new FeignJsonFetcher(), new ObjectMapper());
            List<StandardSubject> subjects = loader.load(DataSourceConfig.builder()
                    .sourceType(DataSourceType.API)
                    .sourceUri("http://localhost:" + port + "/subjects")
                    .build());

            assertThat(subjects).hasSize(1);
            assertThat(subjects.get(0).getStandardCode()).isEqualTo("1001");
            assertThat(subjects.get(0).getStandardName()).isEqualTo("现金");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer createServer(String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/subjects", exchange -> writeJson(exchange, body));
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
