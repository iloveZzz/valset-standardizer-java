package com.yss.valset.application.support;

import com.yss.valset.application.dto.StoredFileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 上传文件落盘服务。
 */
@Slf4j
@Service
public class UploadedFileStorageService {

    private final String uploadRoot;

    public UploadedFileStorageService(@Value("${subject.match.upload-dir:uploads}") String uploadRoot) {
        this.uploadRoot = uploadRoot;
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
        String safeFilename = sanitizeFilename(originalFilename);
        String storedFilename = UUID.randomUUID() + "_" + safeFilename;
        Path directory = Path.of(uploadRoot).toAbsolutePath().resolve(LocalDate.now().toString());
        Path storedPath = directory.resolve(storedFilename);
        try {
            Files.createDirectories(directory);
            file.transferTo(storedPath);
            log.info("上传文件保存完成，originalFilename={}, storedPath={}", originalFilename, storedPath);
            return StoredFileDTO.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .absolutePath(storedPath.toString())
                    .dataSourceType(resolvedType)
                    .fileSizeBytes(file.getSize())
                    .fileFingerprint(calculateSha256(storedPath))
                    .build();
        } catch (IOException exception) {
            log.error("上传文件保存失败，originalFilename={}", originalFilename, exception);
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

    /**
     * 计算文件内容指纹，用于识别相同文件。
     */
    private String calculateSha256(Path storedPath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(storedPath);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // 仅用于驱动摘要计算
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("计算文件指纹失败", exception);
        }
    }
}
