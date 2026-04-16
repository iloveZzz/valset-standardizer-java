package com.yss.subjectmatch.extract;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.analysis.parser.file.OdsValuationDataParser;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.extractor.CsvRawDataExtractor;
import com.yss.subjectmatch.extract.extractor.PoiRawDataExtractor;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.subjectmatch.extract.repository.mapper.ValuationSheetStyleMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealWorkbookParseIntegrationTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    void setupDatabase() throws Exception {
        Reader reader = Resources.getResourceAsReader("mybatis-config-test.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        GlobalConfig globalConfig = GlobalConfigUtils.getGlobalConfig(sqlSessionFactory.getConfiguration());
        globalConfig.setSqlSessionFactory(sqlSessionFactory);
        GlobalConfigUtils.setGlobalConfig(sqlSessionFactory.getConfiguration(), globalConfig);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Connection connection = session.getConnection();
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setLogWriter(null);
            try (Reader schemaReader = Resources.getResourceAsReader("schema-test.sql")) {
                runner.runScript(schemaReader);
            }
        }
    }

    @TempDir
    Path tempDir;

    @Test
    void parsesRealExcelWorkbookIntoNonEmptyValuationSections() throws Exception {
        Path source = copySample("20230321基金资产估值表DJ0233.xls");
        ParsedValuationData parsed = parseRealSource(source, DataSourceType.EXCEL);

        assertThat(parsed.getTitle()).isNotBlank();
        assertThat(parsed.getBasicInfo()).isNotEmpty();
        assertThat(parsed.getHeaders()).isNotEmpty();
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        assertThat(parsed.getSubjects()).isNotEmpty();
        assertThat(parsed.getMetrics()).isNotEmpty();
    }

    @Test
    void parsesRealCsvWorkbookIntoNonEmptyValuationSections() throws Exception {
        Path source = copySample("20230321基金资产估值表DJ02www33.csv");
        ParsedValuationData parsed = parseRealSource(source, DataSourceType.CSV);

        assertThat(parsed.getTitle()).isNotBlank();
        assertThat(parsed.getBasicInfo()).isNotEmpty();
        assertThat(parsed.getHeaders()).isNotEmpty();
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        assertThat(parsed.getSubjects()).isNotEmpty();
        assertThat(parsed.getMetrics()).isNotEmpty();
    }

    private ParsedValuationData parseRealSource(Path source, DataSourceType sourceType) throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ValuationFileDataMapper mapper = session.getMapper(ValuationFileDataMapper.class);
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_filedata");
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_sheet_style");

            if (sourceType == DataSourceType.CSV) {
                CsvRawDataExtractor extractor = new CsvRawDataExtractor(mapper, new ObjectMapper());
                extractor.extract(
                        DataSourceConfig.builder().sourceType(sourceType).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            } else {
                ValuationSheetStyleMapper styleMapper = session.getMapper(ValuationSheetStyleMapper.class);
                PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, new ObjectMapper(), styleMapper);
                extractor.extract(
                        DataSourceConfig.builder().sourceType(sourceType).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            }

            OdsValuationDataParser parser = new OdsValuationDataParser(mapper, new ObjectMapper());
            ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                    .sourceType(sourceType)
                    .sourceUri(source.toString())
                    .additionalParams("9002")
                    .build());

            assertThat(parsed.getWorkbookPath()).isEqualTo(source.toString());
            return parsed;
        }
    }

    private Path copySample(String fileName) throws Exception {
        Path source = resolveSampleFile(fileName);
        Path target = tempDir.resolve(fileName);
        Files.copy(source, target);
        return target;
    }

    private Path resolveSampleFile(String fileName) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && current != null; depth++, current = current.getParent()) {
            Path candidate = current.resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Cannot find sample file " + fileName);
    }
}
