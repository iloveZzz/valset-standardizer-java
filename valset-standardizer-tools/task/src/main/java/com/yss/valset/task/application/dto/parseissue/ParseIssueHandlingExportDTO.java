package com.yss.valset.task.application.dto.parseissue;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 解析问题处理导出结果。
 */
@Data
@AllArgsConstructor
public class ParseIssueHandlingExportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;

    private byte[] content;
}
