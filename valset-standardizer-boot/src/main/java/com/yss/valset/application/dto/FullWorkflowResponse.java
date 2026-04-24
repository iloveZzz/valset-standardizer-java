package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 全流程执行结果。
 */
@Data
@Builder
public class FullWorkflowResponse {
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 工作簿路径。
     */
    private String workbookPath;
    /**
     * 数据源类型。
     */
    private String dataSourceType;
    /**
     * 文件指纹。
     */
    private String fileFingerprint;
    /**
     * 文件服务任务标识。
     */
    private String filesysTaskId;
    /**
     * 文件服务文件标识。
     */
    private String filesysFileId;
    /**
     * 文件服务对象键。
     */
    private String filesysObjectKey;
    /**
     * 是否立即完成文件服务上传。
     */
    private Boolean filesysInstantUpload;
    /**
     * 提取任务详情。
     */
    private TaskViewDTO extractTask;
    /**
     * 解析任务详情。
     */
    private TaskViewDTO parseTask;
    /**
     * 匹配任务详情。
     */
    private TaskViewDTO matchTask;
}
