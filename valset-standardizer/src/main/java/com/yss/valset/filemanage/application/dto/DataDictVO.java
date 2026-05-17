package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 前端字典下拉兼容视图。
 */
@Data
@Builder
public class DataDictVO implements java.io.Serializable {

    private String classCode;

    private String className;

    private String dictValue;

    private String dictName;

    private String systemCode;

    private String dicExt;
}
