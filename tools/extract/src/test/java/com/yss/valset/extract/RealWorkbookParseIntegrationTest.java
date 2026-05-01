package com.yss.valset.extract;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.extractor.CsvRawDataExtractor;
import com.yss.valset.extract.extractor.PoiRawDataExtractor;
import com.yss.valset.extract.parser.file.CsvValuationDataParser;
import com.yss.valset.extract.parser.file.OdsValuationDataParser;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

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
        Path source = writeExcelSample("real-sample.xlsx");
        ParsedValuationData parsed = parseRealSource(source, DataSourceType.EXCEL);

        assertThat(parsed.getTitle()).isNotBlank();
        assertThat(parsed.getBasicInfo()).isNotEmpty();
        assertThat(parsed.getHeaders()).isNotEmpty();
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        assertThat(parsed.getSubjects()).isNotEmpty();
    }

    @Test
    void parsesRealCsvWorkbookIntoNonEmptyValuationSections() throws Exception {
        Path source = writeCsvSample("real-sample.csv");
        ParsedValuationData parsed = parseRealSource(source, DataSourceType.CSV);

        assertThat(parsed.getTitle()).isNotBlank();
        assertThat(parsed.getBasicInfo()).isNotEmpty();
        assertThat(parsed.getHeaders()).isNotEmpty();
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        assertThat(parsed.getSubjects()).isNotEmpty();
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

            var parser = sourceType == DataSourceType.CSV
                    ? new CsvValuationDataParser(new ObjectMapper())
                    : new OdsValuationDataParser(new ObjectMapper());
            ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                    .sourceType(sourceType)
                    .sourceUri(source.toString())
                    .build());

            assertThat(parsed.getWorkbookPath()).isEqualTo(source.toString());
            return parsed;
        }
    }

    private Path writeExcelSample(String fileName) throws Exception {
        Path workbookPath = tempDir.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            writeRow(sheet, 0, "DJ0233大家资产厚坤36号集合资产管理产品委托资产估值表20230321");
            writeRow(sheet, 1, "中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表");
            writeRow(sheet, 2, "", "估值日期：2025-03-05", "", "", "", "", "单位净值：", "", "", "1.0102", "", "单位：元", "");
            writeRow(sheet, 3, "", "科目代码", "科目名称", "币种", "数量", "单位成本", "成本", "成本占净值%", "行情", "市值", "市值占净值%", "估值增值", "停牌信息", "债券每百元利息");
            writeRow(sheet, 4, "-", "1002", "银行存款", "", "", "196926.51", "0.0448", "", "196926.51", "0.0448", "", "", "");
            writeRow(sheet, 5, "", "基金资产净值:", "", "", "", "439134701.52", "100", "", "439134701.52", "100", "", "", "");
            writeRow(sheet, 6, "", "基金单位净值:", "1.0102", "", "", "", "", "", "", "", "", "", "");
            writeRow(sheet, 7, "", "制表：", "王雪洁", "复核：", "", "刘娟", "", "", "", "", "", "", "");
            try (OutputStream outputStream = Files.newOutputStream(workbookPath)) {
                workbook.write(outputStream);
            }
        }
        return workbookPath;
    }

    private Path writeCsvSample(String fileName) throws Exception {
        Path csvPath = tempDir.resolve(fileName);
        Files.writeString(csvPath, String.join("\n",
                "DJ0233大家资产厚坤36号集合资产管理产品委托资产估值表20230321",
                "中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表",
                ",估值日期：2025-03-05,,,,,单位净值：,,,1.0102,,单位：元,",
                ",科目代码,科目名称,币种,数量,单位成本,成本,成本占净值%,行情,市值,市值占净值%,估值增值,停牌信息,债券每百元利息",
                "-,1002,银行存款,,,196926.51,0.0448,,196926.51,0.0448,,,,",
                ",基金资产净值:, , , ,439134701.52,100,,439134701.52,100,,,,",
                ",基金单位净值:,1.0102,,,,,,,,,,,",
                ",制表：,王雪洁,复核：,,刘娟,,,,,,,,"
        ), StandardCharsets.UTF_8);
        return csvPath;
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            String value = values[columnIndex];
            if (value == null) {
                continue;
            }
            row.createCell(columnIndex).setCellValue(value);
        }
    }
}
