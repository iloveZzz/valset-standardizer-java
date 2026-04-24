package com.yss.valset.transfer.domain.model.config;

/**
 * 文件路由分组策略。
 */
public enum TransferRouteGroupStrategy {
    NONE,
    FILE_TYPE,
    FILE_NAME,
    MAIL_FROM,
    MAIL_TO,
    REG_RULE,
    CUSTOM
}
