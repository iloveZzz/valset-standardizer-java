package com.yss.valset.extract.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.ValuationFileDataPO;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OdsValuationDataParserTest {

    @Test
    void parseWorkbookLikeRowsWithTitleBasicInfoHeaderSubjectsAndMetrics() throws Exception {
        ValuationFileDataMapper mapper = mapperWithRows(List.of(
                row(1, "[\"证券投资基金估值表\"]"),
                row(2, "[\"中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表\"]"),
                row(3, "[\"\",\"估值日期：2025-03-05\",\"\",\"\",\"\",\"\",\"单位净值：\",\"\",\"\",\"1.0102\",\"\",\"单位：元\",\"\"]"),
                row(4, "[\"\",\"科目代码\",\"科目名称\",\"币种\",\"数量\",\"单位成本\",\"成本\",\"成本占净值%\",\"行情\",\"市值\",\"市值占净值%\",\"估值增值\",\"停牌信息\",\"债券每百元利息\"]"),
                row(5, "[\"-\",\"1002\",\"银行存款\",\"\",\"\",\"196926.51\",\"0.0448\",\"\",\"196926.51\",\"0.0448\",\"\",\"\",\"\"]"),
                row(6, "[\"\",\"基金资产净值:\",\"\",\"\",\"\",\"439134701.52\",\"100\",\"\",\"439134701.52\",\"100\",\"\",\"\",\"\"]"),
                row(7, "[\"\",\"基金单位净值:\",\"1.0102\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"]"),
                row(8, "[\"\",\"制表：\",\"王雪洁\",\"复核：\",\"\",\"刘娟\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"]")
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getWorkbookPath()).isEqualTo("/tmp/sample.xlsx");
        assertThat(parsed.getSheetName()).isEqualTo("ODS_RAW_DATA");
        assertThat(parsed.getTitle()).isEqualTo("中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表");
        assertThat(parsed.getHeaderRowNumber()).isEqualTo(4);
        assertThat(parsed.getDataStartRowNumber()).isEqualTo(5);
        assertThat(parsed.getBasicInfo()).containsEntry("估值日期", "2025-03-05");
        assertThat(parsed.getBasicInfo()).containsEntry("单位净值", "1.0102");
        assertThat(parsed.getBasicInfo()).containsEntry("单位", "元");
        assertThat(parsed.getHeaders()).containsExactly(
                "",
                "科目代码",
                "科目名称",
                "数量",
                "单位成本",
                "成本",
                "成本占净值%",
                "行情",
                "市值",
                "市值占净值%",
                "估值增值",
                "停牌信息",
                "债券每百元利息"
        );
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        HeaderColumnMeta firstColumn = parsed.getHeaderColumns().get(0);
        assertThat(firstColumn.getColumnIndex()).isEqualTo(0);
        assertThat(firstColumn.getHeaderName()).isEqualTo("");
        assertThat(firstColumn.getHeaderPath()).isEqualTo("");
        assertThat(firstColumn.getPathSegments()).isEmpty();
        assertThat(firstColumn.getBlankColumn()).isTrue();
        assertThat(parsed.getHeaderColumns().get(5).getHeaderPath()).isEqualTo("成本");
        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getMetrics()).hasSize(2);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("1002");
        assertThat(parsed.getSubjects().get(0).getRawValues()).hasSize(13);
        assertThat(parsed.getMetrics().get(0).getMetricType()).isEqualTo("metric_row");
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("基金资产净值");
        assertThat(parsed.getMetrics().get(0).getValue()).isEqualTo("439134701.52");
        assertThat(parsed.getMetrics().get(1).getMetricType()).isEqualTo("metric_data");
        assertThat(parsed.getMetrics().get(1).getMetricName()).isEqualTo("基金单位净值");
        assertThat(parsed.getMetrics().get(1).getValue()).isEqualTo("1.0102");
    }

    @Test
    void parseFromRowsWithLeadingPlaceholdersBeforeSubjectCode() throws Exception {
        ValuationFileDataMapper mapper = mapperWithRows(List.of(
                row(1, "[\"XX001测试组合估值表\"]"),
                row(2, "[\"估值日期：2025-03-05\"]"),
                row(3, "[\"\",\"\"]"),
                row(4, "[\"\",\"\",\"科目代码\",\"科目名称\",\"币种\",\"数量\"]"),
                row(5, "[\"\",\"-\",\"1002.01\",\"银行存款\",\"人民币\",\"100.00\"]"),
                row(6, "[\"单位净值\",\"1.0102\"]")
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample-leading-placeholders.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("100201");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("银行存款");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("单位净值");
        assertThat(parsed.getMetrics().get(0).getValue()).isEqualTo("1.0102");
    }

    @Test
    void parseFromRowsWithSubjectCodeInLaterColumn() throws Exception {
        ValuationFileDataMapper mapper = mapperWithRows(List.of(
                row(1, "[\"XX002延后科目代码测试\"]"),
                row(2, "[\"\",\"\"]"),
                row(3, "[\"\",\"\",\"\",\"科目代码\",\"科目名称\",\"币种\",\"数量\"]"),
                row(4, "[\"\",\"-\",\"\",\"1101.02.14\",\"上交所企业债\",\"200000\"]"),
                row(5, "[\"单位净值\",\"0.9988\"]")
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample-later-column.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("11010214");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("上交所企业债");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("单位净值");
        assertThat(parsed.getMetrics().get(0).getValue()).isEqualTo("0.9988");
    }

    @Test
    void parseMetricLikeRowsWithAmountValuesShouldNotEnterSubjects() throws Exception {
        ValuationFileDataMapper mapper = mapperWithRows(List.of(
                row(1, "[\"证券投资基金估值表\"]"),
                row(2, "[\"实收资本相关测试表\"]"),
                row(3, "[\"\",\"估值日期：2025-03-05\",\"\",\"\",\"\",\"\",\"单位净值：\",\"\",\"\",\"1.0102\",\"\",\"单位：元\",\"\"]"),
                row(4, "[\"\",\"科目代码\",\"科目名称\",\"币种\",\"数量\",\"单位成本\",\"成本\",\"成本占净值%\",\"行情\",\"市值\",\"市值占净值%\",\"估值增值\",\"停牌信息\",\"债券每百元利息\"]"),
                row(5, "[\"\",\"证券投资合计:\",\"\",\"434689297.91\",\"1\",\"434689297.91\",\"98.99\",\"\",\"434689297.91\",\"98.99\",\"\",\"\",\"\"]"),
                row(6, "[\"\",\"实收资本金额\",\"\",\"434689297.91\",\"1\",\"434689297.91\",\"98.99\",\"\",\"434689297.91\",\"98.99\",\"\",\"\",\"\"]"),
                row(7, "[\"\",\"实收资本\",\"\",\"434689297.91\",\"\",\"434689297.91\",\"98.99\",\"\",\"434689297.91\",\"98.99\",\"\",\"\",\"\"]")
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample-metric-like.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getSubjects()).isEmpty();
        assertThat(parsed.getMetrics()).hasSize(3);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("证券投资合计");
        assertThat(parsed.getMetrics().get(1).getMetricName()).isEqualTo("实收资本金额");
        assertThat(parsed.getMetrics().get(2).getMetricName()).isEqualTo("实收资本");
    }

    @Test
    void parseSubjectNameContainingDigitsShouldStillEnterSubjects() throws Exception {
        ValuationFileDataMapper mapper = mapperWithRows(List.of(
                row(1, "[\"证券投资基金估值表\"]"),
                row(2, "[\"中粮信托有限责任公司__242218_中粮信托盼盈1号集合资金信托计划__专用表\"]"),
                row(3, "[\"\",\"估值日期：2025-03-05\",\"\",\"\",\"\",\"\",\"单位净值：\",\"\",\"\",\"1.0102\",\"\",\"单位：元\",\"\"]"),
                row(4, "[\"\",\"科目代码\",\"科目名称\",\"币种\",\"数量\",\"单位成本\",\"成本\",\"成本占净值%\",\"行情\",\"市值\",\"市值占净值%\",\"估值增值\",\"停牌信息\",\"债券每百元利息\"]"),
                row(5, "[\"\",\"1101.02.14.01.241068\",\"24鄂旅03\",\"\",\"\",\"131788.22\",\"0.03\",\"\",\"131788.22\",\"0.03\",\"\",\"\",\"\"]"),
                row(6, "[\"\",\"实收资本金额\",\"\",\"434689297.91\",\"1\",\"434689297.91\",\"98.99\",\"\",\"434689297.91\",\"98.99\",\"\",\"\",\"\"]")
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample-subject-digits.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("1101021401241068");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("24鄂旅03");
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("实收资本金额");
    }

    @Test
    void parseSubjectCodeShouldNormalizeSpacedAndMixedAlphanumericFormats() throws Exception {
        ValuationFileDataMapper mapper = mapperWithRows(List.of(
                row(1, "[\"证券投资基金估值表\"]"),
                row(2, "[\"科目代码归一化测试\"]"),
                row(3, "[\"\",\"估值日期：2025-03-05\"]"),
                row(4, "[\"\",\"\",\"科目代码\",\"科目名称\",\"币种\",\"数量\"]"),
                row(5, "[\"\",\"\",\"2221 06 01\",\"应付申购款\",\"100.00\"]"),
                row(6, "[\"\",\"\",\"1103.B9.01.2020016 IB\",\"20江苏银行永续债\",\"200.00\"]")
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample-subject-code-normalize.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getSubjects()).hasSize(2);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("22210601");
        assertThat(parsed.getSubjects().get(0).getSubjectName()).isEqualTo("应付申购款");
        assertThat(parsed.getSubjects().get(1).getSubjectCode()).isEqualTo("1103B9012020016IB");
        assertThat(parsed.getSubjects().get(1).getSubjectName()).isEqualTo("20江苏银行永续债");
    }

    private ValuationFileDataMapper mapperWithRows(List<ValuationFileDataPO> rows) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("findByFileId".equals(method.getName())) {
                return rows;
            }
            if ("toString".equals(method.getName())) {
                return "ValuationFileDataMapperTestProxy";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected mapper method: " + method.getName());
        };
        return (ValuationFileDataMapper) Proxy.newProxyInstance(
                ValuationFileDataMapper.class.getClassLoader(),
                new Class<?>[]{ValuationFileDataMapper.class},
                handler
        );
    }

    private ValuationFileDataPO row(int rowDataNumber, String rowDataJson) {
        ValuationFileDataPO po = new ValuationFileDataPO();
        po.setTaskId(1L);
        po.setFileId(88L);
        po.setRowDataNumber(rowDataNumber);
        po.setRowDataJson(rowDataJson);
        return po;
    }
}
