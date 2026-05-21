package com.yss.valset.task.application.dto.parseissue;

import lombok.Data;

import java.io.Serializable;

/**
 * 解析标准规则 Sheet 行。
 */
@Data
public class FileParseRuleSheetRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String fileScene;

    private String fileTypeName;

    private String regionName;

    private String columnMap;

    private String columnMapName;

    private String status;

    private String multiIndex;

    private String required;

    private String creater;

    private String createTime;

    private String modifier;

    private String modifyTime;
}
