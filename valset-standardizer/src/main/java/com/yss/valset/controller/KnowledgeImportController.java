package com.yss.valset.controller;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.knowledge.KnowledgeImportAppService;
import com.yss.valset.knowledge.KnowledgeImportResponse;
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
@RequestMapping("/knowledge")
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

}
