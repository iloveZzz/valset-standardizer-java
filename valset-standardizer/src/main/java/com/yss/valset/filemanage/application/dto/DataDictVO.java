package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 前端字典下拉兼容视图。
 */
@Data
@Builder
public class DataDictVO implements java.io.Serializable {

    /** 字典分类编码 */
    private String classCode;

    /** 字典分类名称 */
    private String className;

    /** 字典值 */
    private String dictValue;

    /** 字典名称 */
    private String dictName;

    /** 系统编码 */
    private String systemCode;

    /** 字典扩展信息 */
    private String dicExt;
}
