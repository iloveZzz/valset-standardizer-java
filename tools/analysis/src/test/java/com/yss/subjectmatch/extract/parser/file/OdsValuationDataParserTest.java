package com.yss.subjectmatch.analysis.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.domain.model.HeaderColumnMeta;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OdsValuationDataParserTest {

    @Test
    void parseFromRawRows() throws Exception {
        ValuationFileDataMapper mapper = mock(ValuationFileDataMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        OdsValuationDataParser parser = new OdsValuationDataParser(mapper, objectMapper);

        when(mapper.findByFileId(88L)).thenReturn(List.of(
                row(1, "[\"DJ0233大家资产厚坤36号集合资产管理产品委托资产估值表20230321\"]"),
                row(2, "[\"基金名称：大家资产厚坤36号\"]"),
                row(3, "[\"\",\"\"]"),
                row(4, "[\"\",\"\",\"\",\"\",\"\",\"\",\"成本\",\"\",\"\",\"市值\",\"\",\"\",\"估值增值\",\"\",\"\",\"\",\"\"]"),
                row(5, "[\"\",\"\",\"\",\"\",\"\",\"\",\"本币\",\"\",\"\",\"本币\",\"\",\"\",\"本币\",\"\",\"\",\"\",\"\"]"),
                row(6, "[\"科目代码\",\"科目名称\",\"币种\",\"汇率\",\"数量\",\"单位成本\",\"十亿千百十万千百十元角分\",\"成本占比\",\"行情\",\"十亿千百十万千百十元角分\",\"市值占比\",\"估值增值\",\"\",\"\",\"\",\"停牌信息\",\"权益信息\"]"),
                row(7, "[\"1002\",\"银行存款\",\"人民币\",\"\",\"\",\"900\",\"1000\",\"0.1\",\"\",\"2000\",\"0.2\",\"3000\",\"\",\"\",\"\",\"停牌\",\"权益\"]"),
                row(8, "[\"净值\",\"1.23\"]")
        ));

        ParsedValuationData parsed = parser.parse(DataSourceConfig.builder()
                .sourceType(DataSourceType.EXCEL)
                .sourceUri("/tmp/sample.xlsx")
                .additionalParams("88")
                .build());

        assertThat(parsed.getWorkbookPath()).isEqualTo("/tmp/sample.xlsx");
        assertThat(parsed.getSheetName()).isEqualTo("ODS_RAW_DATA");
        assertThat(parsed.getTitle()).isEqualTo("DJ0233大家资产厚坤36号集合资产管理产品委托资产估值表20230321");
        assertThat(parsed.getHeaderRowNumber()).isEqualTo(4);
        assertThat(parsed.getDataStartRowNumber()).isEqualTo(7);
        assertThat(parsed.getHeaders().subList(0, 6)).containsExactly(
                "科目代码",
                "科目名称",
                "币种",
                "汇率",
                "数量",
                "单位成本"
        );
        assertThat(parsed.getHeaders()).contains(
                "成本|本币|十亿千百十万千百十元角分",
                "成本|本币|成本占比",
                "成本|本币|行情",
                "市值|本币|十亿千百十万千百十元角分",
                "市值|本币|市值占比",
                "市值|本币|估值增值",
                "估值增值|本币|估值增值"
        );
        assertThat(parsed.getHeaderColumns()).hasSize(parsed.getHeaders().size());
        HeaderColumnMeta firstColumn = parsed.getHeaderColumns().get(0);
        assertThat(firstColumn.getColumnIndex()).isEqualTo(0);
        assertThat(firstColumn.getHeaderName()).isEqualTo("科目代码");
        assertThat(firstColumn.getHeaderPath()).isEqualTo("科目代码");
        assertThat(firstColumn.getPathSegments()).containsExactly("科目代码");
        assertThat(firstColumn.getBlankColumn()).isFalse();
        assertThat(parsed.getHeaderColumns().get(6).getHeaderPath()).isEqualTo("成本|本币|十亿千百十万千百十元角分");
        assertThat(parsed.getSubjects()).hasSize(1);
        assertThat(parsed.getMetrics()).hasSize(1);
        assertThat(parsed.getSubjects().get(0).getSubjectCode()).isEqualTo("1002");
        assertThat(parsed.getSubjects().get(0).getCost()).isEqualByComparingTo("1000");
        assertThat(parsed.getSubjects().get(0).getMarketValue()).isEqualByComparingTo("2000");
        assertThat(parsed.getSubjects().get(0).getRawValues()).hasSize(17);
        assertThat(parsed.getSubjects().get(0).getRawValues().get(12)).isEqualTo("");
        assertThat(parsed.getSubjects().get(0).getRawValues().get(15)).isEqualTo("停牌");
        assertThat(parsed.getMetrics().get(0).getMetricName()).isEqualTo("净值");
        assertThat(parsed.getMetrics().get(0).getValue()).isEqualTo("1.23");
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
