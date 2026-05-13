package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.config.TransferCryptoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 敏感配置加解密工具。
 */
@Component
@RequiredArgsConstructor
public class TransferSecretCodec {

    private static final String PREFIX = "ENC:v1:";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final List<String> SENSITIVE_KEYS = List.of(
            TransferConfigKeys.PASSWORD,
            TransferConfigKeys.ACCESS_KEY,
            TransferConfigKeys.SECRET_KEY,
            TransferConfigKeys.PASSPHRASE
    );

    private final TransferCryptoProperties transferCryptoProperties;

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank() || isEncrypted(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("敏感配置加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank() || !isEncrypted(cipherText)) {
            return cipherText;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("敏感配置解密失败", e);
        }
    }

    public Map<String, Object> encryptMap(Map<String, Object> source) {
        return transformMap(source, true, false);
    }

    public Map<String, Object> decryptMap(Map<String, Object> source) {
        return transformMap(source, false, false);
    }

    public Map<String, Object> maskMap(Map<String, Object> source) {
        return transformMap(source, false, true);
    }

    private Map<String, Object> transformMap(Map<String, Object> source, boolean encrypt, boolean mask) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                target.put(key, transformMap(castMap(nestedMap), encrypt, mask));
                continue;
            }
            if (value instanceof List<?> list) {
                target.put(key, transformList(list, encrypt, mask));
                continue;
            }
            if (isSensitiveKey(key)) {
                if (mask) {
                    target.put(key, value == null ? null : "");
                } else if (value == null) {
                    target.put(key, null);
                } else if (encrypt) {
                    target.put(key, encrypt(String.valueOf(value)));
                } else {
                    target.put(key, decrypt(String.valueOf(value)));
                }
            } else {
                target.put(key, value);
            }
        }
        return target;
    }

    private List<Object> transformList(List<?> source, boolean encrypt, boolean mask) {
        List<Object> target = new ArrayList<>(source.size());
        for (Object item : source) {
            if (item instanceof Map<?, ?> nestedMap) {
                target.add(transformMap(castMap(nestedMap), encrypt, mask));
            } else if (item instanceof List<?> nestedList) {
                target.add(transformList(nestedList, encrypt, mask));
            } else {
                target.add(item);
            }
        }
        return target;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        for (String candidate : SENSITIVE_KEYS) {
            if (candidate.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private SecretKey secretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(transferCryptoProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, 0, 16, "AES");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }
}
