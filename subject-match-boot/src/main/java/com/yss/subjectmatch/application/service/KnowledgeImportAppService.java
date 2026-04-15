package com.yss.subjectmatch.application.service;

import com.yss.subjectmatch.application.dto.KnowledgeImportResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库导入服务。
 */
public interface KnowledgeImportAppService {

    /**
     * 导入标准科目落地表。
     */
    KnowledgeImportResponse importStandardSubjects(MultipartFile file, String dataSourceType);

    /**
     * 导入历史映射经验落地表。
     */
    KnowledgeImportResponse importMappingHints(MultipartFile file);

    /**
     * 导入映射样例落地表。
     */
    KnowledgeImportResponse importMappingSamples(MultipartFile file);
}
