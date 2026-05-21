package com.yss.valset.task.application.dto.parseissue;

import lombok.Data;

import java.io.Serializable;

/**
 * 解析问题处理 Sheet 保存行级错误。
 */
@Data
public class ParseIssueHandlingSaveErrorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int rowNumber;

    private String id;

    private String message;
}
