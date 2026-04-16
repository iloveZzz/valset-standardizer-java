package com.yss.subjectmatch.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.yss.subjectmatch.SubjectMatchBootApplication;
import com.yss.subjectmatch.domain.model.TaskStage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 外部估值全流程的 Playwright 场景测试。
 */
@ActiveProfiles("e2e")
@SpringBootTest(
        classes = SubjectMatchBootApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.datasource.primary.url=jdbc:h2:mem:subject_match_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.primary.username=sa",
                "spring.datasource.primary.password=",
                "spring.datasource.primary.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:subject_match_e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema-e2e.sql",
                "spring.quartz.auto-startup=false",
                "subject.match.upload-dir=target/e2e/uploads",
                "subject.match.output-dir=target/e2e/output",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
public class ValuationWorkflowPlaywrightE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path CSV_SAMPLE = Path.of("..", "20230321基金资产估值表DJ02www33.csv").toAbsolutePath().normalize();
    private static final Path XLS_SAMPLE = Path.of("..", "20230321基金资产估值表DJ0233.xls").toAbsolutePath().normalize();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private APIRequestContext requestContext;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of(
                "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"
        )));
        requestContext = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL("http://127.0.0.1:" + port));
    }

    @AfterEach
    void tearDown() {
        if (requestContext != null) {
            requestContext.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @ParameterizedTest(name = "full-flow[{0}]")
    @MethodSource("sampleFiles")
    void should_run_full_flow_for_csv_and_excel(String dataSourceType, Path valuationFile) throws Exception {
        APIResponse uploadResponse = requestContext.post(
                "/api/valuation-workflows/upload",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", valuationFile)
                        .set("dataSourceType", dataSourceType)
                        .set("createdBy", "e2e-user"))
        );
        assertThat(uploadResponse.status()).isEqualTo(200);
        JsonNode uploadJson = readJson(uploadResponse);
        long fileId = uploadJson.get("fileId").asLong();
        String workbookPath = uploadJson.get("workbookPath").asText();
        JsonNode extractTaskJson = uploadJson.get("extractTask");
        assertThat(extractTaskJson.get("taskStage").asText()).isEqualTo(TaskStage.EXTRACT.name());
        assertThat(extractTaskJson.get("taskStartTime").isNull()).isFalse();
        assertThat(extractTaskJson.get("parseTaskTimeMs").asLong()).isGreaterThanOrEqualTo(0L);

        APIResponse analyzeResponse = requestContext.post(
                "/api/valuation-workflows/analyze",
                RequestOptions.create().setData(Map.of(
                        "dataSourceType", dataSourceType,
                        "workbookPath", workbookPath,
                        "fileId", fileId,
                        "createdBy", "e2e-user"
                ))
        );
        assertThat(analyzeResponse.status()).isEqualTo(200);
        JsonNode analyzeJson = readJson(analyzeResponse);
        assertThat(analyzeJson.get("taskStatus").asText()).isEqualTo("SUCCESS");
        assertThat(analyzeJson.get("taskStage").asText()).isEqualTo(TaskStage.PARSE.name());
        assertThat(analyzeJson.get("standardizeTimeMs").asLong()).isGreaterThanOrEqualTo(0L);

        List<Object[]> firstRows = jdbcTemplate.query(
                "select subject_cd, subject_nm, ccy_cd, biz_date " +
                        "from t_tr_jjhzgzb where subject_cd = ? order by id desc limit 1",
                (rs, rowNum) -> new Object[] {
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                },
                "1002"
        );
        assertThat(firstRows).hasSize(1);
        Object[] firstRow = firstRows.get(0);
        assertThat(firstRow[0]).isEqualTo("1002");
        assertThat(firstRow[1]).isEqualTo("银行存款");
        assertThat(firstRow[2]).isEqualTo("CNY");
        assertThat(firstRow[3]).isEqualTo("20230321");

        Integer trIndexCount = jdbcTemplate.queryForObject(
                "select count(*) from t_tr_index where biz_date = ?",
                Integer.class,
                "20230321"
        );
        assertThat(trIndexCount).isGreaterThanOrEqualTo(37);

        List<Object[]> indexRows = jdbcTemplate.query(
                "select indx_nm, indx_valu, source_tp, biz_date from t_tr_index where biz_date = ? and source_tp = ? and indx_valu is not null order by id limit 1",
                (rs, rowNum) -> new Object[] {
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                },
                "20230321",
                dataSourceType
        );
        assertThat(indexRows).hasSize(1);
        Object[] indexRow = indexRows.get(0);
        assertThat((String) indexRow[0]).isNotBlank();
        assertThat(indexRow[1]).isNotNull();
        assertThat(indexRow[2]).isEqualTo(dataSourceType);
        assertThat(indexRow[3]).isEqualTo("20230321");

        Integer dwdSubjectCount = jdbcTemplate.queryForObject(
                "select count(*) from t_dwd_external_valuation_subject where file_id = ?",
                Integer.class,
                fileId
        );
        Integer dwdMetricCount = jdbcTemplate.queryForObject(
                "select count(*) from t_dwd_external_valuation_metric where file_id = ?",
                Integer.class,
                fileId
        );
        assertThat(dwdSubjectCount).isEqualTo(0);
        assertThat(dwdMetricCount).isEqualTo(0);

        APIResponse rawResponse = requestContext.get(
                "/api/valuation-workflows/" + fileId + "/raw-data",
                RequestOptions.create().setQueryParam("limit", 50)
        );
        assertThat(rawResponse.status()).isEqualTo(200);
        JsonNode rawJson = readJson(rawResponse);
        assertThat(rawJson.get("totalRows").asInt()).isGreaterThan(0);

        APIResponse dwdResponse = requestContext.get("/api/valuation-workflows/" + fileId + "/dwd-data");
        assertThat(dwdResponse.status()).isEqualTo(200);
        JsonNode dwdJson = readJson(dwdResponse);
        assertThat(dwdJson.get("subjects").isArray()).isTrue();
        assertThat(dwdJson.get("subjects").size()).isGreaterThan(0);

        Path standardWorkbook = createStandardWorkbookFromDwdSubjects(dwdJson);
        APIResponse standardImportResponse = requestContext.post(
                "/api/knowledge/standard-subjects/import",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", standardWorkbook)
                        .set("dataSourceType", "EXCEL"))
        );
        assertThat(standardImportResponse.status()).isEqualTo(200);
        assertThat(readJson(standardImportResponse).get("importedCount").asLong()).isGreaterThan(0L);

        Path mappingWorkbook = createMappingWorkbookFromDwdSubjects(dwdJson);
        APIResponse mappingImportResponse = requestContext.post(
                "/api/knowledge/mapping-hints/import",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", mappingWorkbook))
        );
        assertThat(mappingImportResponse.status()).isEqualTo(200);
        assertThat(readJson(mappingImportResponse).get("importedCount").asLong()).isGreaterThan(0L);

        APIResponse matchResponse = requestContext.post(
                "/api/valuation-workflows/match",
                RequestOptions.create().setData(Map.of(
                        "dataSourceType", dataSourceType,
                        "workbookPath", workbookPath,
                        "fileId", fileId,
                        "topK", 5,
                        "createdBy", "e2e-user"
                ))
        );
        assertThat(matchResponse.status()).isEqualTo(200);
        JsonNode matchTaskJson = readJson(matchResponse);
        assertThat(matchTaskJson.get("taskStatus").asText()).isEqualTo("SUCCESS");
        assertThat(matchTaskJson.get("taskStage").asText()).isEqualTo(TaskStage.MATCH.name());
        assertThat(matchTaskJson.get("matchStandardSubjectTimeMs").asLong()).isGreaterThanOrEqualTo(0L);

        APIResponse matchResultsResponse = requestContext.get("/api/valuation-workflows/" + fileId + "/match-results");
        assertThat(matchResultsResponse.status()).isEqualTo(200);
        JsonNode matchResultsJson = readJson(matchResultsResponse);
        assertThat(matchResultsJson.get("matchedCount").asInt()).isGreaterThan(0);
        assertThat(matchResultsJson.get("results").isArray()).isTrue();
    }

    @Test
    void should_run_full_process_for_csv() throws Exception {
        JsonNode dwdJson = parseDwdJsonForSample(CSV_SAMPLE);
        Path standardWorkbook = createStandardWorkbookFromDwdSubjects(dwdJson);
        APIResponse standardImportResponse = requestContext.post(
                "/api/knowledge/standard-subjects/import",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", standardWorkbook)
                        .set("dataSourceType", "EXCEL"))
        );
        assertThat(standardImportResponse.status()).isEqualTo(200);
        Path mappingWorkbook = createMappingWorkbookFromDwdSubjects(dwdJson);
        APIResponse mappingImportResponse = requestContext.post(
                "/api/knowledge/mapping-hints/import",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", mappingWorkbook))
        );
        assertThat(mappingImportResponse.status()).isEqualTo(200);
        APIResponse response = requestContext.post(
                "/api/valuation-workflows/full-process",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", CSV_SAMPLE)
                        .set("dataSourceType", "CSV")
                        .set("createdBy", "e2e-user"))
        );
        assertThat(response.status()).isEqualTo(200);
    }

    @Test
    void should_reuse_extract_task_for_same_uploaded_file() throws Exception {
        APIResponse firstUploadResponse = requestContext.post(
                "/api/valuation-workflows/upload",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", CSV_SAMPLE)
                        .set("dataSourceType", "CSV")
                        .set("createdBy", "e2e-user"))
        );
        assertThat(firstUploadResponse.status()).isEqualTo(200);
        JsonNode firstUploadJson = readJson(firstUploadResponse);
        long firstTaskId = firstUploadJson.get("extractTask").get("taskId").asLong();
        long firstFileId = firstUploadJson.get("fileId").asLong();
        String firstFingerprint = firstUploadJson.get("fileFingerprint").asText();
        long firstRowCount = firstUploadJson.get("extractTask").get("resultData").get("rowCount").asLong();

        APIResponse secondUploadResponse = requestContext.post(
                "/api/valuation-workflows/upload",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", CSV_SAMPLE)
                        .set("dataSourceType", "CSV")
                        .set("createdBy", "e2e-user"))
        );
        assertThat(secondUploadResponse.status()).isEqualTo(200);
        JsonNode secondUploadJson = readJson(secondUploadResponse);
        assertThat(secondUploadJson.get("fileFingerprint").asText()).isEqualTo(firstFingerprint);
        assertThat(secondUploadJson.get("reusedExistingExtractTask").asBoolean()).isTrue();
        assertThat(secondUploadJson.get("fileId").asLong()).isEqualTo(firstFileId);
        assertThat(secondUploadJson.get("extractTask").get("taskId").asLong()).isEqualTo(firstTaskId);
        assertThat(secondUploadJson.get("extractTask").get("resultData").get("rowCount").asLong()).isEqualTo(firstRowCount);

        APIResponse rawResponse = requestContext.get(
                "/api/valuation-workflows/" + firstFileId + "/raw-data",
                RequestOptions.create().setQueryParam("limit", 1000)
        );
        assertThat(rawResponse.status()).isEqualTo(200);
        JsonNode rawJson = readJson(rawResponse);
        assertThat(rawJson.get("totalRows").asLong()).isEqualTo(firstRowCount);
    }

    @Test
    void should_force_rebuild_extract_task_when_requested() throws Exception {
        APIResponse firstUploadResponse = requestContext.post(
                "/api/valuation-workflows/upload",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", CSV_SAMPLE)
                        .set("dataSourceType", "CSV")
                        .set("createdBy", "e2e-user"))
        );
        assertThat(firstUploadResponse.status()).isEqualTo(200);
        JsonNode firstUploadJson = readJson(firstUploadResponse);
        long firstTaskId = firstUploadJson.get("extractTask").get("taskId").asLong();

        APIResponse forceUploadResponse = requestContext.post(
                "/api/valuation-workflows/upload",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", CSV_SAMPLE)
                        .set("dataSourceType", "CSV")
                        .set("createdBy", "e2e-user")
                        .set("forceRebuild", "true"))
        );
        assertThat(forceUploadResponse.status()).isEqualTo(200);
        JsonNode forceUploadJson = readJson(forceUploadResponse);
        assertThat(forceUploadJson.get("fileFingerprint").asText()).isEqualTo(firstUploadJson.get("fileFingerprint").asText());
        assertThat(forceUploadJson.get("reusedExistingExtractTask").asBoolean()).isFalse();
        assertThat(forceUploadJson.get("extractTask").get("taskId").asLong()).isNotEqualTo(firstTaskId);
        assertThat(forceUploadJson.get("extractTask").get("taskStage").asText()).isEqualTo(TaskStage.EXTRACT.name());
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> sampleFiles() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("CSV", CSV_SAMPLE),
                org.junit.jupiter.params.provider.Arguments.of("EXCEL", XLS_SAMPLE)
        );
    }

    private JsonNode readJson(APIResponse response) throws Exception {
        return OBJECT_MAPPER.readTree(response.text());
    }

    private Path createStandardWorkbookFromDwdSubjects(JsonNode dwdJson) throws Exception {
        Path workbookPath = Files.createTempFile("subject-match-standard-", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("标准科目");
            int rowIndex = 0;
            for (JsonNode subjectNode : dwdJson.get("subjects")) {
                String subjectCode = subjectNode.path("subjectCode").asText("");
                String subjectName = subjectNode.path("subjectName").asText("");
                if (subjectCode.isBlank() || subjectName.isBlank()) {
                    continue;
                }
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(subjectCode);
                row.createCell(1).setCellValue(subjectName);
            }
            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }
        return workbookPath;
    }

    private Path createMappingWorkbookFromDwdSubjects(JsonNode dwdJson) throws Exception {
        Path workbookPath = Files.createTempFile("subject-match-mapping-", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("映射经验");
            Row headerRow0 = sheet.createRow(0);
            headerRow0.createCell(0).setCellValue("orgName");
            headerRow0.createCell(1).setCellValue("orgId");
            headerRow0.createCell(2).setCellValue("externalCode");
            headerRow0.createCell(3).setCellValue("externalName");
            headerRow0.createCell(4).setCellValue("standardCode");
            headerRow0.createCell(5).setCellValue("standardName");
            headerRow0.createCell(6).setCellValue("standardSystem");
            headerRow0.createCell(7).setCellValue("systemName");
            sheet.createRow(1);
            int rowIndex = 2;
            for (JsonNode subjectNode : dwdJson.get("subjects")) {
                String subjectCode = subjectNode.path("subjectCode").asText("");
                String subjectName = subjectNode.path("subjectName").asText("");
                if (subjectCode.isBlank() || subjectName.isBlank()) {
                    continue;
                }
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue("E2E");
                row.createCell(1).setCellValue("001");
                row.createCell(2).setCellValue(subjectCode);
                row.createCell(3).setCellValue(subjectName);
                row.createCell(4).setCellValue(subjectCode);
                row.createCell(5).setCellValue(subjectName);
                row.createCell(6).setCellValue("E2E");
                row.createCell(7).setCellValue("E2E");
                if (rowIndex >= 100) {
                    break;
                }
            }
            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }
        return workbookPath;
    }

    private Path createStandardWorkbookFromSample(Path sampleFile) throws Exception {
        JsonNode dwdJson = parseDwdJsonForSample(sampleFile);
        return createStandardWorkbookFromDwdSubjects(dwdJson);
    }

    private JsonNode parseDwdJsonForSample(Path sampleFile) throws Exception {
        APIResponse uploadResponse = requestContext.post(
                "/api/valuation-workflows/upload",
                RequestOptions.create().setMultipart(FormData.create()
                        .set("file", sampleFile)
                        .set("createdBy", "e2e-user"))
        );
        assertThat(uploadResponse.status()).isEqualTo(200);
        JsonNode uploadJson = readJson(uploadResponse);
        long fileId = uploadJson.get("fileId").asLong();
        String workbookPath = uploadJson.get("workbookPath").asText();

        APIResponse analyzeResponse = requestContext.post(
                "/api/valuation-workflows/analyze",
                RequestOptions.create().setData(Map.of(
                        "workbookPath", workbookPath,
                        "fileId", fileId,
                        "createdBy", "e2e-user"
                ))
        );
        assertThat(analyzeResponse.status()).isEqualTo(200);
        JsonNode analyzeJson = readJson(analyzeResponse);
        assertThat(analyzeJson.get("taskStatus").asText()).isEqualTo("SUCCESS");
        assertThat(analyzeJson.get("taskStage").asText()).isEqualTo(TaskStage.PARSE.name());
        assertThat(analyzeJson.get("standardizeTimeMs").asLong()).isGreaterThanOrEqualTo(0L);

        APIResponse dwdResponse = requestContext.get("/api/valuation-workflows/" + fileId + "/dwd-data");
        assertThat(dwdResponse.status()).isEqualTo(200);
        return readJson(dwdResponse);
    }
}
