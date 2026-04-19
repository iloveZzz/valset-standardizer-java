package com.yss.valset.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.extract.extractor.CsvRawDataExtractor;
import com.yss.valset.extract.extractor.PoiRawDataExtractor;
import com.yss.valset.extract.repository.entity.ValuationFileDataPO;
import com.yss.valset.extract.repository.entity.ValuationSheetStylePO;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RawDataExtractionIntegrationTest {

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
    void extractsRealCsvFileIntoOdsTable() throws Exception {
        Path source = copySample("20230321基金资产估值表DJ02www33.csv");
        assertExtractedRows(source, DataSourceType.CSV);
    }

    @Test
    void extractsRealExcelFileIntoOdsTable() throws Exception {
        Path source = copySample("20230321基金资产估值表DJ0233.xls");
        assertExtractedRows(source, DataSourceType.EXCEL);
    }

    private void assertExtractedRows(Path source, DataSourceType sourceType) throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ValuationFileDataMapper mapper = session.getMapper(ValuationFileDataMapper.class);
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_filedata");
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_sheet_style");

            int extracted;
            if (sourceType == DataSourceType.CSV) {
                CsvRawDataExtractor csvExtractor = new CsvRawDataExtractor(mapper, new ObjectMapper());
                extracted = csvExtractor.extract(
                        DataSourceConfig.builder().sourceType(sourceType).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            } else if (sourceType == DataSourceType.EXCEL) {
                ValuationSheetStyleMapper styleMapper = session.getMapper(ValuationSheetStyleMapper.class);
                PoiRawDataExtractor poiExtractor = new PoiRawDataExtractor(mapper, new ObjectMapper(), styleMapper);
                extracted = poiExtractor.extract(
                        DataSourceConfig.builder().sourceType(sourceType).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            } else {
                throw new IllegalArgumentException("Unsupported extractor type");
            }

            List<ValuationFileDataPO> rows = mapper.findByTaskId(9001L);
            assertThat(rows).hasSize(extracted);
            assertThat(extracted).isGreaterThan(0);

            List<Object> firstRow = new ObjectMapper().readValue(rows.get(0).getRowDataJson(), List.class);
            assertThat(firstRow.get(0)).isEqualTo("DJ0233大家资产厚坤36号集合资产管理产品委托资产估值表20230321");

            List<Object> headerRow = rows.stream()
                    .map(row -> readRow(row, new ObjectMapper()))
                    .filter(values -> values.contains("科目代码") && values.contains("科目名称"))
                    .findFirst()
                    .orElseThrow();
            assertThat(headerRow).contains("科目代码", "科目名称");

            if (sourceType == DataSourceType.EXCEL) {
                List<ValuationSheetStylePO> sheetStyles = session.getMapper(ValuationSheetStyleMapper.class)
                        .findByFileId(9002L);
                assertThat(sheetStyles).isNotEmpty();
                assertThat(sheetStyles.get(0).getSheetStyleJson()).isNotBlank();
                assertThat(containsFontAttributes(sheetStyles.get(0).getSheetStyleJson())).isFalse();
            } else {
                assertThat(session.getMapper(ValuationSheetStyleMapper.class).findByFileId(9002L)).isEmpty();
            }

            List<Object> dataRow = rows.stream()
                    .map(row -> readRow(row, new ObjectMapper()))
                    .filter(values -> "1002".equals(values.get(0)) && "银行存款".equals(values.get(1)))
                    .findFirst()
                    .orElseThrow();
            assertThat(dataRow.get(0)).isEqualTo("1002");
            assertThat(dataRow.get(1)).isEqualTo("银行存款");
        }
    }

    private boolean containsFontAttributes(String sheetStyleJson) {
        try {
            Map<String, Object> parsed = new ObjectMapper().readValue(sheetStyleJson, Map.class);
            Object cellData = parsed.get("cellData");
            if (!(cellData instanceof Map<?, ?> cellDataMap)) {
                return false;
            }
            for (Object rowValue : cellDataMap.values()) {
                if (!(rowValue instanceof Map<?, ?> rowMap)) {
                    continue;
                }
                for (Object cellValue : rowMap.values()) {
                    if (!(cellValue instanceof Map<?, ?> cellMap)) {
                        continue;
                    }
                    Object styleValue = cellMap.get("s");
                    if (!(styleValue instanceof Map<?, ?> styleMap)) {
                        continue;
                    }
                    for (Object keyObj : styleMap.keySet()) {
                        String key = String.valueOf(keyObj);
                        if (key.equals("ff")
                                || key.equals("fs")
                                || key.equals("it")
                                || key.equals("bl")
                                || key.equals("ul")
                                || key.equals("st")
                                || key.equals("cl")
                                || key.equals("va")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to inspect sheetStyleJson", exception);
        }
    }

    private List<Object> readRow(ValuationFileDataPO row, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(row.getRowDataJson(), List.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read row json", e);
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
