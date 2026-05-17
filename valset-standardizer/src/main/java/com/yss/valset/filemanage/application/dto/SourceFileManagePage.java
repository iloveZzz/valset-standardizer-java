package com.yss.valset.filemanage.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 源文件收取管理分页查询条件，字段保持兼容前端生成 API。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SourceFileManagePage extends SourceFileManageQuery {

    private Integer pageIndex;

    private Integer pageSize;

    private String groupBy;

    private String orderBy;

    private String orderDirection;
}
