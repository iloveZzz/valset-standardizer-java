package com.yss.valset.transfer.infrastructure.support;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileFingerprintSupportTest {

    @Test
    void sameContentHasSameFingerprint() throws Exception {
        Path first = Files.createTempFile("fingerprint-first-", ".txt");
        Path second = Files.createTempFile("fingerprint-second-", ".txt");
        Files.writeString(first, "same-content");
        Files.writeString(second, "same-content");

        String firstFingerprint = FileFingerprintSupport.sha256Hex(first);
        String secondFingerprint = FileFingerprintSupport.sha256Hex(second);

        assertThat(firstFingerprint).isEqualTo(secondFingerprint);
    }

    @Test
    void differentContentHasDifferentFingerprint() throws Exception {
        Path first = Files.createTempFile("fingerprint-first-", ".txt");
        Path second = Files.createTempFile("fingerprint-second-", ".txt");
        Files.writeString(first, "content-a");
        Files.writeString(second, "content-b");

        String firstFingerprint = FileFingerprintSupport.sha256Hex(first);
        String secondFingerprint = FileFingerprintSupport.sha256Hex(second);

        assertThat(firstFingerprint).isNotEqualTo(secondFingerprint);
    }
}
