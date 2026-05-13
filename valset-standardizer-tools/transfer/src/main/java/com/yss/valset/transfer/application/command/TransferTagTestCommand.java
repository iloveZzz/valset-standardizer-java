package com.yss.valset.transfer.application.command;

import lombok.Data;

import java.util.Map;

/**
 * 标签试跑命令。
 */
@Data
public class TransferTagTestCommand {

    private String sourceType;

    private String sourceCode;

    private String fileName;

    private String mimeType;

    private Long fileSize;

    private String sender;

    private String subject;

    private String path;

    private String mailFolder;

    private String body;

    private Map<String, Object> attributes;
}
