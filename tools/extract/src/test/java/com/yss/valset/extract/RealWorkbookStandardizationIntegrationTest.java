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
import com.yss.valset.extract.repository.entity.FileParseRulePO;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.entity.TrDwdJjhzgzbPO;
import com.yss.valset.extract.repository.entity.TrIndexPO;
import com.yss.valset.extract.repository.mapper.FileParseRuleRepository;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.repository.mapper.ValuationSheetStyleMapper;
import com.yss.valset.extract.standardization.ExternalValuationStandardizationService;
import com.yss.valset.extract.support.JjhzgzbStandardizationSupport;
import com.yss.valset.extract.support.TrIndexStandardizationSupport;
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealWorkbookStandardizationIntegrationTest {

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
    void should_build_non_empty_tr_rows_from_real_excel_sample() throws Exception {
        Path source = writeExcelSample("real-sample.xlsx");
        ParsedValuationData parsed = parseRealSource(source, DataSourceType.EXCEL);

        ExternalValuationStandardizationService standardizationService = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称"),
                        rule(3L, "n_hldamt", "数量")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称"),
                        source(3L, "n_hldamt", "数量")
                ))
        );
        String sourceSign = source.getFileName().toString();
        ParsedValuationData standardized = standardizationService.standardize(
                parsed.toBuilder().fileNameOriginal(sourceSign).build()
        );

        List<TrDwdJjhzgzbPO> subjectRows = JjhzgzbStandardizationSupport.buildRows(standardized, "EXCEL", sourceSign);
        List<TrIndexPO> indexRows = TrIndexStandardizationSupport.buildRows(standardized, "EXCEL", sourceSign);

        assertThat(standardized.getSubjects()).isNotEmpty();
        assertThat(subjectRows).isNotNull();
        assertThat(indexRows).isNotNull();
    }

    @Test
    void should_build_non_empty_tr_rows_from_real_csv_sample() throws Exception {
        Path source = writeCsvSample("real-sample.csv");
        ParsedValuationData parsed = parseRealSource(source, DataSourceType.CSV);

        ExternalValuationStandardizationService standardizationService = new ExternalValuationStandardizationService(
                new ObjectMapper(),
                proxyRuleRepository(List.of(
                        rule(1L, "subject_cd", "科目代码"),
                        rule(2L, "subject_nm", "科目名称"),
                        rule(3L, "n_hldamt", "数量")
                )),
                proxySourceRepository(List.of(
                        source(1L, "subject_cd", "科目代码"),
                        source(2L, "subject_nm", "科目名称"),
                        source(3L, "n_hldamt", "数量")
                ))
        );
        String sourceSign = source.getFileName().toString();
        ParsedValuationData standardized = standardizationService.standardize(
                parsed.toBuilder().fileNameOriginal(sourceSign).build()
        );

        List<TrDwdJjhzgzbPO> subjectRows = JjhzgzbStandardizationSupport.buildRows(standardized, "CSV", sourceSign);
        List<TrIndexPO> indexRows = TrIndexStandardizationSupport.buildRows(standardized, "CSV", sourceSign);

        assertThat(standardized.getSubjects()).isNotEmpty();
        assertThat(subjectRows).isNotNull();
        assertThat(indexRows).isNotNull();
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

    private FileParseRuleRepository proxyRuleRepository(List<FileParseRulePO> rows) {
        return proxyRepository(FileParseRuleRepository.class, rows);
    }

    private FileParseSourceRepository proxySourceRepository(List<FileParseSourcePO> rows) {
        return proxyRepository(FileParseSourceRepository.class, rows);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxyRepository(Class<T> repositoryType, List<?> rows) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("selectList".equals(method.getName())) {
                return rows;
            }
            if ("toString".equals(method.getName())) {
                return repositoryType.getSimpleName() + "Proxy";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
        };
        return (T) Proxy.newProxyInstance(
                repositoryType.getClassLoader(),
                new Class<?>[]{repositoryType},
                handler
        );
    }

    private FileParseRulePO rule(Long id, String columnMap, String columnMapName) {
        FileParseRulePO po = new FileParseRulePO();
        po.setId(id);
        po.setFileScene("ALL");
        po.setFileTypeName("基金资产估值表");
        po.setRegionName("column");
        po.setColumnMap(columnMap);
        po.setColumnMapName(columnMapName);
        po.setStatus(Boolean.TRUE);
        po.setMultiIndex(Boolean.FALSE);
        po.setRequired(Boolean.FALSE);
        return po;
    }

    private FileParseSourcePO source(Long id, String columnMap, String columnName) {
        FileParseSourcePO po = new FileParseSourcePO();
        po.setId(id);
        po.setFileType("COMMON");
        po.setColumnMap(columnMap);
        po.setColumnName(columnName);
        po.setStatus(Boolean.TRUE);
        return po;
    }
}
