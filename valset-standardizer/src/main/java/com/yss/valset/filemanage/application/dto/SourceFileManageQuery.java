package com.yss.valset.filemanage.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 源文件收取管理查询条件，字段保持兼容前端生成 API。
 */
@Data
public class SourceFileManageQuery implements java.io.Serializable {

    private String orgCd;

    private String fileName;

    private String pdCd;

    private String pdType;

    private String fileState;

    private String fileReceiveChannel;

    private String bizDateStart;

    private String bizDateEnd;

    private String fileReceiveDateStart;

    private String fileReceiveDateEnd;

    private String stateGroup;

    private String exceptionCode;

    private String fileAddress;

    private String fileReceiveTime;

    private String fileType;

    private List<String> ids;
}
