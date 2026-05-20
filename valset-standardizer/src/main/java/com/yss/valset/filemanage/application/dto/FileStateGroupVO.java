package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 源文件收取管理状态分组视图。
 */
@Data
@Builder
public class FileStateGroupVO implements java.io.Serializable {

    /** 状态编码 */
    private String code;

    /** 分组名称 */
    private String groupName;

    /** 分组统计数量 */
    private Long groupCount;
}
