package com.yss.valset.application.command;

import lombok.Data;

/**
 * 解析模板回滚请求。
 */
@Data
public class ParseRuleRollbackCommand {
    /**
     * 发布人。
     */
    private String publisher;
    /**
     * 回滚说明。
     */
    private String publishComment;
    /**
     * 回滚到的版本号。
     */
    private String rollbackToVersion;
}
