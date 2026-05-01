package com.yss.valset.extract;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
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
        Path source = writeCsvSample("real-sample.csv");
        assertExtractedRows(source, DataSourceType.CSV);
    }

    @Test
    void extractsRealExcelFileIntoOdsTable() throws Exception {
        Path source = writeExcelSample("real-sample.xlsx");
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
            } else {
                ValuationSheetStyleMapper styleMapper = session.getMapper(ValuationSheetStyleMapper.class);
                PoiRawDataExtractor poiExtractor = new PoiRawDataExtractor(mapper, new ObjectMapper(), styleMapper);
                extracted = poiExtractor.extract(
                        DataSourceConfig.builder().sourceType(sourceType).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            }

            List<ValuationFileDataPO> rows = mapper.findByTaskId(9001L);
            assertThat(rows).hasSize(extracted);
            assertThat(extracted).isGreaterThan(0);
            assertThat(rows.get(0).getRowDataJson()).isNotBlank();

            if (sourceType == DataSourceType.EXCEL) {
                List<ValuationSheetStylePO> sheetStyles = session.getMapper(ValuationSheetStyleMapper.class)
                        .findByFileId(9002L);
                assertThat(sheetStyles).isNotEmpty();
                assertThat(sheetStyles.get(0).getSheetStyleJson()).isNotBlank();
                assertThat(containsFontAttributes(sheetStyles.get(0).getSheetStyleJson())).isFalse();
            } else {
                assertThat(session.getMapper(ValuationSheetStyleMapper.class).findByFileId(9002L)).isEmpty();
            }
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
