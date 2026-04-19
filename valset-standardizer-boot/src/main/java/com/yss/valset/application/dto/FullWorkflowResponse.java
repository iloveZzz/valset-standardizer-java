package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 全流程执行结果。
 */
@Data
@Builder
public class FullWorkflowResponse {
    private Long fileId;
    private String workbookPath;
    private String dataSourceType;
    private String fileFingerprint;
    private TaskViewDTO extractTask;
    private TaskViewDTO parseTask;
    private TaskViewDTO matchTask;
}
