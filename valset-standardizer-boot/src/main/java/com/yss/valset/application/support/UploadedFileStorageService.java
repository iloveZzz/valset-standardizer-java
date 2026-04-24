package com.yss.valset.application.support;

import com.yss.filesys.feignsdk.dto.YssFilesysUploadFlowResult;
import com.yss.filesys.feignsdk.service.YssFilesysUploadFlowService;
import com.yss.valset.application.dto.StoredFileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.HexFormat;

/**
 * 上传文件落盘服务。
 */
@Slf4j
@Service
public class UploadedFileStorageService {

    private final String uploadRoot;
    private final YssFilesysUploadFlowService yssFilesysUploadFlowService;
    private final String filesysParentId;
    private final String filesysStorageSettingId;
    private final long filesysChunkSize;

    public UploadedFileStorageService(
            @Value("${subject.match.upload-dir:}") String uploadRoot,
            YssFilesysUploadFlowService yssFilesysUploadFlowService,
            @Value("${subject.match.filesys.parent-id:}") String filesysParentId,
            @Value("${subject.match.filesys.storage-setting-id:}") String filesysStorageSettingId,
            @Value("${subject.match.filesys.chunk-size:5242880}") long filesysChunkSize
    ) {
        this.uploadRoot = resolveUploadRoot(uploadRoot);
        this.yssFilesysUploadFlowService = yssFilesysUploadFlowService;
        this.filesysParentId = filesysParentId;
        this.filesysStorageSettingId = filesysStorageSettingId;
        this.filesysChunkSize = filesysChunkSize;
    }

    /**
     * 将上传文件保存到本地目录。
     */
    public StoredFileDTO store(MultipartFile file, String dataSourceType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename() == null ? "valuation-file" : file.getOriginalFilename();
        String resolvedType = resolveDataSourceType(dataSourceType, originalFilename);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            log.error("读取上传文件失败，originalFilename={}", originalFilename, exception);
            throw new IllegalStateException("读取上传文件失败", exception);
        }

        YssFilesysUploadFlowResult filesysResult = uploadToFilesysIfConfigured(file, bytes, originalFilename);
        String storedFilename = buildStoredFilename(originalFilename, bytes);

        Path directory = Path.of(uploadRoot).toAbsolutePath().resolve(LocalDate.now().toString());
        Path storedPath = directory.resolve(storedFilename);
        try {
            Files.createDirectories(directory);
            Files.write(storedPath, bytes);
            log.info("上传文件临时保存完成，originalFilename={}, storedPath={}, filesysTaskId={}, filesysFileId={}",
                    originalFilename,
                    storedPath,
                    filesysResult == null ? null : filesysResult.getTaskId(),
                    filesysResult == null || filesysResult.getFileRecord() == null ? null : filesysResult.getFileRecord().getFileId());
            return StoredFileDTO.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .absolutePath(storedPath.toString())
                    .dataSourceType(resolvedType)
                    .fileSizeBytes(file.getSize())
                    .fileFingerprint(calculateSha256(bytes))
                    .filesysTaskId(filesysResult == null ? null : filesysResult.getTaskId())
                    .filesysFileId(filesysResult == null || filesysResult.getFileRecord() == null ? null : filesysResult.getFileRecord().getFileId())
                    .filesysObjectKey(filesysResult == null || filesysResult.getFileRecord() == null ? null : filesysResult.getFileRecord().getObjectKey())
                    .filesysOriginalName(filesysResult == null || filesysResult.getFileRecord() == null ? null : filesysResult.getFileRecord().getOriginalName())
                    .filesysMimeType(filesysResult == null || filesysResult.getFileRecord() == null ? null : filesysResult.getFileRecord().getMimeType())
                    .filesysInstantUpload(filesysResult == null ? null : filesysResult.isInstantUpload())
                    .filesysParentId(resolveFilesysParentId())
                    .filesysStorageSettingId(resolveFilesysStorageSettingId())
                    .build();
        } catch (IOException exception) {
            log.error("上传文件临时保存失败，originalFilename={}", originalFilename, exception);
            throw new IllegalStateException("保存上传文件失败", exception);
        }
    }

    /**
     * 删除已落盘的重复文件。
     */
    public void deleteStoredFile(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(absolutePath));
        } catch (IOException exception) {
            log.warn("删除重复落盘文件失败，absolutePath={}", absolutePath, exception);
        }
    }

    private String resolveDataSourceType(String dataSourceType, String filename) {
        if (dataSourceType != null && !dataSourceType.isBlank()) {
            return dataSourceType.trim().toUpperCase(Locale.ROOT);
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".csv")) {
            return "CSV";
        }
        return "EXCEL";
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String buildStoredFilename(String originalFilename, byte[] bytes) {
        String sanitized = sanitizeFilename(originalFilename == null ? "valuation-file" : originalFilename);
        String extension = "";
        int lastDot = sanitized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < sanitized.length() - 1) {
            extension = sanitized.substring(lastDot);
        }
        String fingerprint = calculateSha256(bytes);
        return fingerprint.substring(0, 16) + extension;
    }

    private String resolveUploadRoot(String configuredUploadRoot) {
        if (configuredUploadRoot != null && !configuredUploadRoot.isBlank()) {
            return configuredUploadRoot;
        }
        return Path.of(System.getProperty("user.home"), ".tmp", "valset-standardizer", "uploads").toString();
    }

    /**
     * 计算文件内容指纹，用于识别相同文件。
     */
    private String calculateSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    private YssFilesysUploadFlowResult uploadToFilesysIfConfigured(MultipartFile file, byte[] bytes, String originalFilename) {
        String parentId = resolveFilesysParentId();
        String storageSettingId = resolveFilesysStorageSettingId();
        if (parentId == null || parentId.isBlank() || storageSettingId == null || storageSettingId.isBlank()) {
            log.warn("filesys 上传参数未配置完整，跳过 filesys 上传，仅保留临时抽取副本，originalFilename={}", originalFilename);
            return null;
        }
        try {
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "application/octet-stream";
            }
            YssFilesysUploadFlowResult result = yssFilesysUploadFlowService.upload(
                    bytes,
                    originalFilename,
                    mimeType,
                    parentId,
                    storageSettingId,
                    filesysChunkSize
            );
            log.info("filesys 上传完成，originalFilename={}, taskId={}, fileId={}, objectKey={}, instantUpload={}",
                    originalFilename,
                    result.getTaskId(),
                    result.getFileRecord() == null ? null : result.getFileRecord().getFileId(),
                    result.getFileRecord() == null ? null : result.getFileRecord().getObjectKey(),
                    result.isInstantUpload());
            return result;
        } catch (Exception exception) {
            log.error("filesys 上传失败，originalFilename={}", originalFilename, exception);
            throw new IllegalStateException("上传文件到 filesys 失败", exception);
        }
    }

    private String resolveFilesysParentId() {
        return filesysParentId == null ? null : filesysParentId.trim();
    }

    private String resolveFilesysStorageSettingId() {
        return filesysStorageSettingId == null ? null : filesysStorageSettingId.trim();
    }
}
