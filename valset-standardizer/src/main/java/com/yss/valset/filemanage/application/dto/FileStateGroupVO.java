package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 源文件收取管理状态分组视图。
 */
@Data
@Builder
public class FileStateGroupVO implements java.io.Serializable {

    private String code;

    private String groupName;

    private Long groupCount;
}
