package com.yss.valset.filemanage.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 源文件收取管理列表视图，字段保持兼容前端生成 API。
 */
@Data
@Builder
public class SourceFileManageVO implements java.io.Serializable {

    private String id;

    private String fileName;

    private String pdName;

    private String pdCd;

    private String orgName;

    private String orgCd;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime fileReceiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate bizDate;

    private String fileReceiveChannel;

    private String fileType;

    private String pdType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastAnalysisTime;

    private String creater;

    private String fileState;

    private String exceptionMsg;

    private String exceptionCode;

    private String fileAddress;
}
