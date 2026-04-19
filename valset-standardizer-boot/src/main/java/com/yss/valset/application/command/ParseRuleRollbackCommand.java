package com.yss.valset.application.command;

import lombok.Data;

/**
 * 解析模板回滚请求。
 */
@Data
public class ParseRuleRollbackCommand {
    private String publisher;
    private String publishComment;
    private String rollbackToVersion;
}
