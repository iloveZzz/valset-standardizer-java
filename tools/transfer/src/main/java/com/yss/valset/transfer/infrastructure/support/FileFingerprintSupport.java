package com.yss.valset.transfer.infrastructure.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文件内容指纹工具。
 */
public final class FileFingerprintSupport {

    private FileFingerprintSupport() {
    }

    public static String sha256Hex(Path path) {
        if (path == null) {
            return null;
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalStateException("文件不存在或不是普通文件，path=" + path);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return sha256Hex(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("读取文件失败，path=" + path, e);
        }
    }

    public static String sha256Hex(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new IllegalStateException("计算文件指纹失败", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("系统不支持 SHA-256", e);
        }
    }
}
