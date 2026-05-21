package com.yss.valset.task.application.dto.parseissue;

import lombok.Data;

import java.io.Serializable;

/**
 * 解析字段映射 Sheet 行。
 */
@Data
public class FileParseSourceSheetRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String fileType;

    private String columnMap;

    private String columnMapName;

    private String columnName;

    private String fileExtInfo;

    private String status;

    private String creater;

    private String createTime;

    private String modifier;

    private String modifyTime;
}
