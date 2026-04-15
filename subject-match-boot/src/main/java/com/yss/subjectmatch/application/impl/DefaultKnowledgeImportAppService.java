package com.yss.subjectmatch.application.impl;

import com.yss.subjectmatch.application.dto.KnowledgeImportResponse;
import com.yss.subjectmatch.application.service.KnowledgeImportAppService;
import com.yss.subjectmatch.domain.gateway.MappingHintGateway;
import com.yss.subjectmatch.domain.gateway.MappingSampleGateway;
import com.yss.subjectmatch.domain.gateway.StandardSubjectGateway;
import com.yss.subjectmatch.domain.knowledge.MappingSampleLoader;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.domain.model.MappingHintIndex;
import com.yss.subjectmatch.domain.model.MappingSample;
import com.yss.subjectmatch.domain.model.StandardSubject;
import com.yss.subjectmatch.extract.support.MappingEvaluationSupport;
import com.yss.subjectmatch.knowledge.PoiMappingSampleLoader;
import com.yss.subjectmatch.knowledge.StandardSubjectLoaderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 知识库导入服务默认实现。
 */
@Slf4j
@Service
public class DefaultKnowledgeImportAppService implements KnowledgeImportAppService {

    private final StandardSubjectLoaderRegistry standardSubjectLoaderRegistry;
    private final MappingSampleLoader mappingSampleLoader;
    private final StandardSubjectGateway standardSubjectGateway;
    private final MappingHintGateway mappingHintGateway;
    private final MappingSampleGateway mappingSampleGateway;
    private final PoiMappingSampleLoader poiMappingSampleLoader;

    public DefaultKnowledgeImportAppService(StandardSubjectLoaderRegistry standardSubjectLoaderRegistry,
                                            MappingSampleLoader mappingSampleLoader,
                                            StandardSubjectGateway standardSubjectGateway,
                                            MappingHintGateway mappingHintGateway,
                                            MappingSampleGateway mappingSampleGateway,
                                            PoiMappingSampleLoader poiMappingSampleLoader) {
        this.standardSubjectLoaderRegistry = standardSubjectLoaderRegistry;
        this.mappingSampleLoader = mappingSampleLoader;
        this.standardSubjectGateway = standardSubjectGateway;
        this.mappingHintGateway = mappingHintGateway;
        this.mappingSampleGateway = mappingSampleGateway;
        this.poiMappingSampleLoader = poiMappingSampleLoader;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeImportResponse importStandardSubjects(MultipartFile file, String dataSourceType) {
        Path tempFile = null;
        try {
            tempFile = writeTempFile(file);
            DataSourceType type = resolveDataSourceType(dataSourceType);
            List<StandardSubject> standardSubjects = standardSubjectLoaderRegistry.getLoader(type)
                    .load(DataSourceConfig.builder()
                            .sourceType(type)
                            .sourceUri(tempFile.toString())
                            .build());
            standardSubjectGateway.replaceAll(standardSubjects);
            log.info("标准科目导入完成，sourceType={}, count={}", type, standardSubjects == null ? 0 : standardSubjects.size());
            return KnowledgeImportResponse.builder()
                    .targetTable("t_ods_standard_subject")
                    .sourceType(type.name())
                    .importedCount(standardSubjects == null ? 0L : (long) standardSubjects.size())
                    .build();
        } catch (Exception exception) {
            log.error("标准科目导入失败", exception);
            throw new IllegalStateException("导入标准科目失败", exception);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeImportResponse importMappingHints(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = writeTempFile(file);
            List<MappingSample> samples = poiMappingSampleLoader.parse(tempFile);
            MappingHintIndex mappingHintIndex = MappingEvaluationSupport.buildMappingHintIndex(samples);
            mappingHintGateway.replaceAll(mappingHintIndex.getHints());
            log.info("历史映射经验导入完成，count={}", mappingHintIndex.getHints() == null ? 0 : mappingHintIndex.getHints().size());
            return KnowledgeImportResponse.builder()
                    .targetTable("t_ods_mapping_hint")
                    .sourceType("EXCEL")
                    .importedCount(mappingHintIndex.getHints() == null ? 0L : (long) mappingHintIndex.getHints().size())
                    .build();
        } catch (Exception exception) {
            log.error("历史映射经验导入失败", exception);
            throw new IllegalStateException("导入历史映射经验失败", exception);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeImportResponse importMappingSamples(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = writeTempFile(file);
            List<MappingSample> samples = poiMappingSampleLoader.parse(tempFile);
            mappingSampleGateway.replaceAll(samples);
            log.info("映射样例导入完成，count={}", samples == null ? 0 : samples.size());
            return KnowledgeImportResponse.builder()
                    .targetTable("t_ods_mapping_sample")
                    .sourceType("EXCEL")
                    .importedCount(samples == null ? 0L : (long) samples.size())
                    .build();
        } catch (Exception exception) {
            log.error("映射样例导入失败", exception);
            throw new IllegalStateException("导入映射样例失败", exception);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private DataSourceType resolveDataSourceType(String dataSourceType) {
        if (dataSourceType == null || dataSourceType.isBlank()) {
            return DataSourceType.EXCEL;
        }
        try {
            DataSourceType type = DataSourceType.valueOf(dataSourceType.trim().toUpperCase(Locale.ROOT));
            if (type == DataSourceType.API || type == DataSourceType.DB) {
                throw new IllegalArgumentException("不支持通过文件导入的标准科目数据源类型: " + dataSourceType);
            }
            return type;
        } catch (Exception exception) {
            throw new IllegalArgumentException("不支持的 dataSourceType: " + dataSourceType, exception);
        }
    }

    private Path writeTempFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
        String suffix = ".tmp";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        Path tempFile = Files.createTempFile("subject-match-import-", suffix);
        file.transferTo(tempFile.toFile());
        return tempFile;
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (Exception exception) {
            log.warn("删除临时导入文件失败，tempFile={}", tempFile, exception);
        }
    }
}
