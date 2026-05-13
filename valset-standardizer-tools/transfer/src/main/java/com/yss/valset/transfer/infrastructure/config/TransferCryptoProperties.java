package com.yss.valset.transfer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件收发分拣模块的加密配置。
 */
@Component
@ConfigurationProperties(prefix = "subject.match.transfer.crypto")
public class TransferCryptoProperties {

    /**
     * 用于本地加解密的密钥种子。
     * 生产环境应通过配置中心或环境变量覆盖。
     */
    private String secretKey = "valset-standardizer-transfer-secret-v1";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
