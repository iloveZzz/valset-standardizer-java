package com.yss.valset.application.command;

import lombok.Data;

/**
 * 解析模板发布请求。
 */
@Data
public class ParseRulePublishCommand {
    /**
     * 发布人。
     */
    private String publisher;
    /**
     * 发布说明。
     */
    private String publishComment;
}
