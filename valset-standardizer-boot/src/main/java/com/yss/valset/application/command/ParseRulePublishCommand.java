package com.yss.valset.application.command;

import lombok.Data;

/**
 * 解析模板发布请求。
 */
@Data
public class ParseRulePublishCommand {
    private String publisher;
    private String publishComment;
}
