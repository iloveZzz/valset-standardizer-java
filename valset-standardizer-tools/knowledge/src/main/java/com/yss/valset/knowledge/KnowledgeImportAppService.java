package com.yss.valset.knowledge;

import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库导入服务。
 */
public interface KnowledgeImportAppService {

    /**
     * 导入标准科目落地表。
     */
    KnowledgeImportResponse importStandardSubjects(MultipartFile file, String dataSourceType);

}
