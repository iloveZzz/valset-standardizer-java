package com.yss.valset.task.application.impl.parseissue;

import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import com.yss.valset.task.application.command.parseissue.FileParseSourceSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.ProductMatchRuleSheetSaveCommand;
import com.yss.valset.task.application.dto.parseissue.FileParseSourceSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingSaveResultDTO;
import com.yss.valset.task.application.dto.parseissue.ProductMatchRuleSheetRowDTO;
import com.yss.valset.transfer.infrastructure.entity.ProductMatchRulePO;
import com.yss.valset.transfer.infrastructure.mapper.ProductMatchRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultParseIssueHandlingAppServiceTest {

    private FileParseSourceRepository fileParseSourceRepository;
    private ProductMatchRuleRepository productMatchRuleRepository;
    private DefaultParseIssueHandlingAppService service;

    @BeforeEach
    void setUp() {
        fileParseSourceRepository = mock(FileParseSourceRepository.class);
        productMatchRuleRepository = mock(ProductMatchRuleRepository.class);
        service = new DefaultParseIssueHandlingAppService(fileParseSourceRepository, productMatchRuleRepository);
    }

    @Test
    void listFileParseSourcesMapsAndFiltersRows() {
        FileParseSourcePO po = new FileParseSourcePO();
        po.setId(1L);
        po.setFileType("ALL");
        po.setColumnMap("subject_nm");
        po.setColumnName("科目名称");
        po.setStatus(Boolean.TRUE);
        when(fileParseSourceRepository.selectList(any())).thenReturn(Collections.singletonList(po));

        List<FileParseSourceSheetRowDTO> result = service.listFileParseSources("ALL", "subject", "科目", "启用");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1");
        assertThat(result.get(0).getStatus()).isEqualTo("是");
        verify(fileParseSourceRepository).selectList(any());
    }

    @Test
    void saveFileParseSourcesCreatesUpdatesAndDeletesRows() {
        FileParseSourcePO existing = new FileParseSourcePO();
        existing.setId(10L);
        existing.setFileType("ALL");
        existing.setColumnMap("old_code");
        existing.setColumnName("旧字段");
        existing.setCreater("old-user");
        when(fileParseSourceRepository.selectList(any())).thenReturn(Collections.singletonList(existing));

        FileParseSourceSheetSaveCommand command = new FileParseSourceSheetSaveCommand();
        command.setOriginalIds(Arrays.asList("10", "11"));
        command.setRows(Arrays.asList(
                fileSourceRow("10", "ALL", "subject_nm", "科目名称", "启用"),
                fileSourceRow("", "ALL", "n_valprice", "市价", "0")
        ));

        ParseIssueHandlingSaveResultDTO result = service.saveFileParseSources(command);

        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isZero();
        assertThat(existing.getColumnMap()).isEqualTo("subject_nm");
        assertThat(existing.getStatus()).isTrue();
        assertThat(existing.getCreater()).isEqualTo("old-user");
        verify(fileParseSourceRepository).updateById(existing);
        verify(fileParseSourceRepository).insert(any(FileParseSourcePO.class));
        verify(fileParseSourceRepository).deleteById(eq(11L));
    }

    @Test
    void saveFileParseSourcesReturnsRowErrorsForRequiredFields() {
        FileParseSourcePO existing = new FileParseSourcePO();
        existing.setId(10L);
        when(fileParseSourceRepository.selectList(any())).thenReturn(Collections.singletonList(existing));
        FileParseSourceSheetSaveCommand command = new FileParseSourceSheetSaveCommand();
        command.setOriginalIds(Collections.singletonList("10"));
        command.setRows(Collections.singletonList(fileSourceRow("10", "ALL", "", "科目名称", "启用")));

        ParseIssueHandlingSaveResultDTO result = service.saveFileParseSources(command);

        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors()).extracting("rowNumber", "message")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(2, "标准列编码不能为空"));
        verify(fileParseSourceRepository, never()).insert(any(FileParseSourcePO.class));
        verify(fileParseSourceRepository, never()).deleteById(any(Serializable.class));
    }

    @Test
    void listProductMatchRulesMapsRows() {
        ProductMatchRulePO po = new ProductMatchRulePO();
        po.setId(20L);
        po.setPdCd("P001");
        po.setPdNm("测试产品");
        po.setOrgNm("托管人");
        po.setMatchRules("(.*)P001(.*)");
        po.setIsValid(1);
        po.setApprovalRequired(Boolean.TRUE);
        when(productMatchRuleRepository.selectList(any())).thenReturn(Collections.singletonList(po));

        List<ProductMatchRuleSheetRowDTO> result = service.listProductMatchRules("P", "测试", "托管", "xls", "true");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("20");
        assertThat(result.get(0).getIsValid()).isEqualTo("启用");
        assertThat(result.get(0).getApprovalRequired()).isEqualTo("是");
    }

    @Test
    void saveProductMatchRulesCreatesUpdatesDeletesAndParsesStatusFields() {
        ProductMatchRulePO existing = new ProductMatchRulePO();
        existing.setId(20L);
        existing.setPdCd("OLD");
        existing.setPdNm("旧产品");
        existing.setMatchRules("old");
        existing.setCreater("old-user");
        when(productMatchRuleRepository.selectList(any())).thenReturn(Collections.singletonList(existing));

        ProductMatchRuleSheetSaveCommand command = new ProductMatchRuleSheetSaveCommand();
        command.setOriginalIds(Arrays.asList("20", "21"));
        command.setRows(Arrays.asList(
                productRuleRow("20", "P001", "产品一", "(.*)P001(.*)", "是", "停用"),
                productRuleRow("", "P002", "产品二", "(.*)P002(.*)", "false", "1")
        ));

        ParseIssueHandlingSaveResultDTO result = service.saveProductMatchRules(command);

        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isZero();
        assertThat(existing.getPdCd()).isEqualTo("P001");
        assertThat(existing.getApprovalRequired()).isTrue();
        assertThat(existing.getIsValid()).isZero();
        assertThat(existing.getCreater()).isEqualTo("old-user");

        ArgumentCaptor<ProductMatchRulePO> captor = ArgumentCaptor.forClass(ProductMatchRulePO.class);
        verify(productMatchRuleRepository).insert(captor.capture());
        assertThat(captor.getValue().getApprovalRequired()).isFalse();
        assertThat(captor.getValue().getIsValid()).isEqualTo(1);
        verify(productMatchRuleRepository).deleteById(eq(21L));
    }

    @Test
    void saveProductMatchRulesRejectsInvalidRows() {
        ProductMatchRuleSheetSaveCommand command = new ProductMatchRuleSheetSaveCommand();
        command.setRows(Arrays.asList(
                productRuleRow("", "", "产品一", "(.*)P001(.*)", "是", "启用"),
                productRuleRow("", "P002", "产品二", "", "是", "启用"),
                productRuleRow("", "P003", "产品三", "(.*)P003(.*)", "不确定", "启用")
        ));

        ParseIssueHandlingSaveResultDTO result = service.saveProductMatchRules(command);

        assertThat(result.getFailedCount()).isEqualTo(3);
        assertThat(result.getErrors()).extracting("rowNumber", "message")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2, "产品代码不能为空"),
                        org.assertj.core.groups.Tuple.tuple(3, "匹配规则不能为空"),
                        org.assertj.core.groups.Tuple.tuple(4, "是否审批只能填写是/否、true/false 或 1/0")
                );
        verify(productMatchRuleRepository, times(0)).insert(any(ProductMatchRulePO.class));
    }

    private FileParseSourceSheetRowDTO fileSourceRow(String id, String fileType, String columnMap, String columnName, String status) {
        FileParseSourceSheetRowDTO row = new FileParseSourceSheetRowDTO();
        row.setId(id);
        row.setFileType(fileType);
        row.setColumnMap(columnMap);
        row.setColumnName(columnName);
        row.setStatus(status);
        return row;
    }

    private ProductMatchRuleSheetRowDTO productRuleRow(String id,
                                                       String pdCd,
                                                       String pdNm,
                                                       String matchRules,
                                                       String approvalRequired,
                                                       String isValid) {
        ProductMatchRuleSheetRowDTO row = new ProductMatchRuleSheetRowDTO();
        row.setId(id);
        row.setPdCd(pdCd);
        row.setPdNm(pdNm);
        row.setMatchRules(matchRules);
        row.setApprovalRequired(approvalRequired);
        row.setIsValid(isValid);
        return row;
    }
}
