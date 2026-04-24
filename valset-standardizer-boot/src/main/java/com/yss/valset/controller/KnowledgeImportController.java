package com.yss.valset.controller;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.dto.KnowledgeImportResponse;
import com.yss.valset.application.service.KnowledgeImportAppService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库导入接口。
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeImportController {

    private final KnowledgeImportAppService knowledgeImportAppService;

    public KnowledgeImportController(KnowledgeImportAppService knowledgeImportAppService) {
        this.knowledgeImportAppService = knowledgeImportAppService;
    }

    /**
     * 导入标准科目落地表。
     *
     * @param file 上传的标准科目文件
     * @param dataSourceType 数据源类型
     * @return 导入结果
     */
    @PostMapping(value = "/standard-subjects/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入标准科目落地表", description = "上传标准科目文件并覆盖落地表，核心匹配流程后续只读取该表。")
    public SingleResult<KnowledgeImportResponse> importStandardSubjects(@RequestPart("file") MultipartFile file,
                                                                        @RequestParam(value = "dataSourceType", required = false) String dataSourceType) {
        return SingleResult.of(knowledgeImportAppService.importStandardSubjects(file, dataSourceType));
    }

    /**
     * 导入历史映射经验落地表。
     *
     * @param file 上传的历史映射样本文件
     * @return 导入结果
     */
    @PostMapping(value = "/mapping-hints/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入历史映射经验落地表", description = "上传历史映射样本文件并覆盖落地表，核心匹配流程后续只读取该表。")
    public SingleResult<KnowledgeImportResponse> importMappingHints(@RequestPart("file") MultipartFile file) {
        return SingleResult.of(knowledgeImportAppService.importMappingHints(file));
    }

    /**
     * 导入映射样例落地表。
     *
     * @param file 上传的映射样例文件
     * @return 导入结果
     */
    @PostMapping(value = "/mapping-samples/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入映射样例落地表", description = "上传映射样例文件并覆盖落地表，评估流程后续从该表读取样例数据。")
    public SingleResult<KnowledgeImportResponse> importMappingSamples(@RequestPart("file") MultipartFile file) {
        return SingleResult.of(knowledgeImportAppService.importMappingSamples(file));
    }
}
