package com.yss.valset.task.application.dto.parseissue;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析问题处理 Sheet 保存汇总。
 */
@Data
public class ParseIssueHandlingSaveResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int createdCount;

    private int updatedCount;

    private int deletedCount;

    private int skippedCount;

    private int failedCount;

    private List<ParseIssueHandlingSaveErrorDTO> errors = new ArrayList<>();
}
