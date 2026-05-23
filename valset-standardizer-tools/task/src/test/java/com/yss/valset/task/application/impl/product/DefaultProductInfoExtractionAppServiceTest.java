package com.yss.valset.task.application.impl.product;

import com.yss.valset.task.application.command.product.ProductInfoExtractionSaveCommand;
import com.yss.valset.task.application.command.product.ProductInfoExtractionPreviewCommand;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionPreviewDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionSaveResultDTO;
import com.yss.valset.task.infrastructure.dto.product.ProductInfoExtractionValuationTitleRow;
import com.yss.valset.task.infrastructure.mapper.product.ProductInfoExtractionQueryMapper;
import com.yss.valset.transfer.infrastructure.entity.ProductMatchRulePO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectTagPO;
import com.yss.valset.transfer.infrastructure.entity.TransferTagPO;
import com.yss.valset.transfer.infrastructure.mapper.ProductMatchRuleRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectTagRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProductInfoExtractionAppServiceTest {

    private TransferObjectRepository transferObjectRepository;
    private ProductMatchRuleRepository productMatchRuleRepository;
    private TransferObjectTagRepository transferObjectTagRepository;
    private TransferTagRepository transferTagRepository;
    private ProductInfoExtractionQueryMapper queryMapper;
    private DefaultProductInfoExtractionAppService service;

    @BeforeEach
    void setUp() {
        transferObjectRepository = mock(TransferObjectRepository.class);
        productMatchRuleRepository = mock(ProductMatchRuleRepository.class);
        transferObjectTagRepository = mock(TransferObjectTagRepository.class);
        transferTagRepository = mock(TransferTagRepository.class);
        queryMapper = mock(ProductInfoExtractionQueryMapper.class);
        service = new DefaultProductInfoExtractionAppService(
                queryMapper,
                transferObjectRepository,
                productMatchRuleRepository,
                transferObjectTagRepository,
                transferTagRepository
        );
    }

    @Test
    void buildPreviewExtractsProductFieldsFromFileName() {
        TransferObjectPO object = transferObject(
                "100",
                "NYTXLLYXPZ32_农银理财农银同心灵珑优选配置第32期理财产品（优享）_估值报表_20250228.xls"
        );

        ProductInfoExtractionPreviewDTO preview = service.buildPreview(object, "委外产品", LocalDate.of(2024, 5, 21));

        assertThat(preview.getProductCode()).isEqualTo("NYTXLLYXPZ32");
        assertThat(preview.getProductName()).isEqualTo("农银理财农银同心灵珑优选配置第32期理财产品（优享）");
        assertThat(preview.getMatchRule()).isEqualTo("(.*)\\QNYTXLLYXPZ32\\E(.*)");
        assertThat(preview.getSubjectSystem()).isEqualTo("默认科目体系");
        assertThat(preview.getManagerCode()).isEqualTo("ALL");
        assertThat(preview.getManagerName()).isEqualTo("临时机构");
        assertThat(preview.getHoldingStatus()).isEqualTo("存续");
        assertThat(preview.getDelayDays()).isEqualTo(2);
        assertThat(preview.getApprovalRequired()).isTrue();
    }

    @Test
    void buildPreviewExtractsEmbeddedProductCodeAndNameFromChineseFileName() {
        TransferObjectPO object = transferObject(
                "101",
                "普通估值表(生成)_DL_SDLYHYX2023076外包EWY_20231031.xls"
        );

        ProductInfoExtractionPreviewDTO preview = service.buildPreview(object, "委外产品", LocalDate.of(2024, 5, 21));

        assertThat(preview.getProductCode()).isEqualTo("DL_SDLYHYX2023076");
        assertThat(preview.getProductName()).isEqualTo("普通估值表(生成)_DL_SDLYHYX2023076外包EWY");
        assertThat(preview.getMatchRule()).isEqualTo("(.*)\\QDL_SDLYHYX2023076\\E(.*)");
    }

    @Test
    void buildPreviewKeepsFileNameWithoutTrailingDateIntact() {
        TransferObjectPO object = transferObject(
                "102",
                "普通估值表(生成)_DL_SDLYHYX2023076外包EWY.xls"
        );

        ProductInfoExtractionPreviewDTO preview = service.buildPreview(object, "委外产品", LocalDate.of(2024, 5, 21));

        assertThat(preview.getProductCode()).isEqualTo("DL_SDLYHYX2023076");
        assertThat(preview.getProductName()).isEqualTo("普通估值表(生成)_DL_SDLYHYX2023076外包EWY");
    }

    @Test
    void buildPreviewDoesNotTreatPureDateAsProductCode() {
        TransferObjectPO object = transferObject(
                "103",
                "普通估值表_20231031.xls"
        );

        ProductInfoExtractionPreviewDTO preview = service.buildPreview(object, "委外产品", LocalDate.of(2024, 5, 21));

        assertThat(preview.getProductCode()).isNull();
        assertThat(preview.getProductName()).isEqualTo("普通估值表");
        assertThat(preview.getMatchRule()).isEqualTo("普通估值表(.*)");
    }

    @Test
    void buildPreviewFallsBackToValuationTitleWhenFileNameHasNoProductFields() {
        TransferObjectPO object = transferObject(
                "104",
                "普通估值表_20231031.xls"
        );

        ProductInfoExtractionPreviewDTO preview = service.buildPreview(
                object,
                "委外产品",
                LocalDate.of(2024, 5, 21),
                "产品代码：ABC123 产品名称：农银理财稳享产品 资产估值表 20231031"
        );

        assertThat(preview.getProductCode()).isEqualTo("ABC123");
        assertThat(preview.getProductName()).isEqualTo("农银理财稳享产品");
        assertThat(preview.getMatchRule()).isEqualTo("普通估值表(.*)");
    }

    @Test
    void previewLoadsValuationTitleByTransferIdAsFileId() {
        TransferObjectPO object = transferObject(
                "105",
                "普通估值表_20231031.xls"
        );
        ProductInfoExtractionValuationTitleRow titleRow = new ProductInfoExtractionValuationTitleRow();
        titleRow.setFileId(105L);
        titleRow.setTitle("NYLLA001_农银理财安享产品估值表20231031");
        when(transferObjectRepository.selectList(any())).thenReturn(Collections.singletonList(object));
        when(queryMapper.listValuationTitlesByFileIds(any())).thenReturn(Collections.singletonList(titleRow));
        ProductInfoExtractionPreviewCommand command = new ProductInfoExtractionPreviewCommand();
        command.setTransferIds(Collections.singletonList("105"));

        List<ProductInfoExtractionPreviewDTO> previews = service.preview(command);

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).getProductCode()).isEqualTo("NYLLA001");
        assertThat(previews.get(0).getProductName()).isEqualTo("农银理财安享产品");
        assertThat(previews.get(0).getMatchRule()).isEqualTo("普通估值表(.*)");
    }

    @Test
    void buildMatchRuleUsesFileNameDateWildcardWhenProductCodeMissing() {
        assertThat(service.buildMatchRule("富国基金蓝筹价值1号集合资产管理计划估值表_2024-06-28.xls"))
                .isEqualTo("富国基金蓝筹价值1号集合资产管理计划估值表(.*)");
        assertThat(service.buildMatchRule("东方红稳犇888号20240630估值表.xls"))
                .isEqualTo("东方红稳犇888号(.*)估值表(.*)");
    }

    @Test
    void saveRulesInsertsRuleAndProductTag() {
        ProductInfoExtractionPreviewDTO item = preview("100");
        when(transferObjectRepository.selectList(any())).thenReturn(Collections.singletonList(transferObject("100", "NYTXLLYXPZ32_产品_估值表.xls")));
        when(transferObjectTagRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(transferTagRepository.selectList(any())).thenReturn(Collections.singletonList(productTagDefinition()));

        ProductInfoExtractionSaveResultDTO result = service.saveRules(saveCommand(item));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getRuleId()).isNotBlank();
        ArgumentCaptor<ProductMatchRulePO> ruleCaptor = ArgumentCaptor.forClass(ProductMatchRulePO.class);
        verify(productMatchRuleRepository).insert(ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().getOrgCd()).isEqualTo("ALL");
        assertThat(ruleCaptor.getValue().getOrgNm()).isEqualTo("临时机构");
        verify(transferObjectTagRepository).insert(any(TransferObjectTagPO.class));
    }

    @Test
    void saveRulesSkipsWhenProductTagAlreadyExists() {
        ProductInfoExtractionPreviewDTO item = preview("100");
        TransferObjectTagPO existingTag = new TransferObjectTagPO();
        existingTag.setTransferId("100");
        existingTag.setTagCode("PRODUCT_MATCH_RULE");
        when(transferObjectRepository.selectList(any())).thenReturn(Collections.singletonList(transferObject("100", "NYTXLLYXPZ32_产品_估值表.xls")));
        when(transferObjectTagRepository.selectList(any())).thenReturn(Collections.singletonList(existingTag));
        when(transferTagRepository.selectList(any())).thenReturn(Collections.singletonList(productTagDefinition()));

        ProductInfoExtractionSaveResultDTO result = service.saveRules(saveCommand(item));

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("SKIPPED");
        verify(productMatchRuleRepository, never()).insert(any(ProductMatchRulePO.class));
    }

    @Test
    void saveRulesConvertsProductRulePrecisionViolationToFailedItem() {
        ProductInfoExtractionPreviewDTO item = preview("100");
        when(transferObjectRepository.selectList(any())).thenReturn(Collections.singletonList(transferObject("100", "NYTXLLYXPZ32_产品_估值表.xls")));
        when(transferObjectTagRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(transferTagRepository.selectList(any())).thenReturn(Collections.singletonList(productTagDefinition()));
        doThrow(new DataIntegrityViolationException("ORA-01438", new RuntimeException("ORA-01438")))
                .when(productMatchRuleRepository)
                .insert(any(ProductMatchRulePO.class));

        ProductInfoExtractionSaveResultDTO result = service.saveRules(saveCommand(item));

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(result.getItems().get(0).getMessage()).contains("TP_MATCH_RULES.ID");
        verify(transferObjectTagRepository, never()).insert(any(TransferObjectTagPO.class));
    }

    private ProductInfoExtractionSaveCommand saveCommand(ProductInfoExtractionPreviewDTO item) {
        ProductInfoExtractionSaveCommand command = new ProductInfoExtractionSaveCommand();
        command.setItems(Collections.singletonList(item));
        return command;
    }

    private ProductInfoExtractionPreviewDTO preview(String transferId) {
        ProductInfoExtractionPreviewDTO item = new ProductInfoExtractionPreviewDTO();
        item.setTransferId(transferId);
        item.setOriginalName("NYTXLLYXPZ32_产品_估值表.xls");
        item.setProductType("委外产品");
        item.setSubjectSystem("默认科目体系");
        item.setManagerName("临时机构");
        item.setHoldingStatus("存续");
        item.setEstablishedDate(LocalDate.of(2024, 5, 21));
        item.setProductCode("NYTXLLYXPZ32");
        item.setProductName("产品");
        item.setMatchRule("(.*)NYTXLLYXPZ32(.*)");
        item.setEffectiveFrequency("日");
        item.setDelayDays(2);
        item.setApprovalRequired(Boolean.TRUE);
        return item;
    }

    private TransferObjectPO transferObject(String transferId, String originalName) {
        TransferObjectPO object = new TransferObjectPO();
        object.setTransferId(transferId);
        object.setOriginalName(originalName);
        object.setExtension("xls");
        object.setStatus("IDENTIFIED");
        return object;
    }

    private TransferTagPO productTagDefinition() {
        TransferTagPO definition = new TransferTagPO();
        definition.setTagId("2060000000000000001");
        definition.setTagCode("PRODUCT_MATCH_RULE");
        definition.setTagName("产品识别规则");
        definition.setMatchStrategy("SCRIPT_RULE");
        return definition;
    }
}
