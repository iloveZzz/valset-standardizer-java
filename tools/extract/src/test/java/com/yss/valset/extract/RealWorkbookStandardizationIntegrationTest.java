package com.yss.valset.extract;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.extract.parser.file.OdsValuationDataParser;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.extractor.CsvRawDataExtractor;
import com.yss.valset.extract.extractor.PoiRawDataExtractor;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

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
        Path source = copySample("20230321基金资产估值表DJ0233.xls");
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
        assertThat(standardized.getMetrics()).isNotEmpty();
        assertThat(subjectRows.size()).isGreaterThanOrEqualTo(60);
        assertThat(indexRows.size()).isGreaterThanOrEqualTo(30);
        assertThat(subjectRows.stream().anyMatch(row -> row.getSubjectCd() != null && !row.getSubjectCd().isBlank())).isTrue();
        assertThat(subjectRows.stream().anyMatch(row -> "20230321".equals(row.getBizDate()))).isTrue();
        assertThat(indexRows.stream().anyMatch(row -> row.getIndxNm() != null && !row.getIndxNm().isBlank())).isTrue();
    }

    @Test
    void should_build_non_empty_tr_rows_from_real_csv_sample() throws Exception {
        Path source = copySample("20230321基金资产估值表DJ02www33.csv");
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
        assertThat(standardized.getMetrics()).isNotEmpty();
        assertThat(subjectRows.size()).isGreaterThanOrEqualTo(60);
        assertThat(indexRows.size()).isGreaterThanOrEqualTo(30);
        assertThat(subjectRows.stream().anyMatch(row -> row.getSubjectCd() != null && !row.getSubjectCd().isBlank())).isTrue();
        assertThat(subjectRows.stream().anyMatch(row -> "20230321".equals(row.getBizDate()))).isTrue();
        assertThat(indexRows.stream().anyMatch(row -> row.getIndxNm() != null && !row.getIndxNm().isBlank())).isTrue();
    }

    private ParsedValuationData parseRealSource(Path source, DataSourceType sourceType) throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ValuationFileDataMapper mapper = session.getMapper(ValuationFileDataMapper.class);
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_filedata");
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_sheet_style");

            if (sourceType == DataSourceType.CSV) {
                CsvRawDataExtractor extractor = new CsvRawDataExtractor(mapper, new ObjectMapper());
                extractor.extract(
                        DataSourceConfig.builder().sourceType(DataSourceType.CSV).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            } else {
                ValuationSheetStyleMapper styleMapper = session.getMapper(ValuationSheetStyleMapper.class);
                PoiRawDataExtractor extractor = new PoiRawDataExtractor(mapper, new ObjectMapper(), styleMapper);
                extractor.extract(
                        DataSourceConfig.builder().sourceType(DataSourceType.EXCEL).sourceUri(source.toString()).build(),
                        9001L,
                        9002L
                );
            }

            OdsValuationDataParser parser = new OdsValuationDataParser(mapper, new ObjectMapper());
            return parser.parse(DataSourceConfig.builder()
                    .sourceType(sourceType)
                    .sourceUri(source.toString())
                    .additionalParams("9002")
                    .build());
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
