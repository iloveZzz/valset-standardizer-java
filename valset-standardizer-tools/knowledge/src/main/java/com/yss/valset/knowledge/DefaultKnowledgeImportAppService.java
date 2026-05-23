package com.yss.valset.knowledge;

import com.yss.valset.domain.gateway.StandardSubjectGateway;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.StandardSubject;
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
    private final StandardSubjectGateway standardSubjectGateway;

    public DefaultKnowledgeImportAppService(StandardSubjectLoaderRegistry standardSubjectLoaderRegistry,
                                            StandardSubjectGateway standardSubjectGateway) {
        this.standardSubjectLoaderRegistry = standardSubjectLoaderRegistry;
        this.standardSubjectGateway = standardSubjectGateway;
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

    private DataSourceType resolveDataSourceType(String dataSourceType) {
        if (dataSourceType == null || dataSourceType.trim().isEmpty()) {
            return DataSourceType.EXCEL;
        }
        try {
            DataSourceType type = DataSourceType.valueOf(dataSourceType.trim().toUpperCase(Locale.ROOT));
            if (type == DataSourceType.API ) {
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
        Path tempFile = Files.createTempFile("valset-standardizer-import-", suffix);
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
