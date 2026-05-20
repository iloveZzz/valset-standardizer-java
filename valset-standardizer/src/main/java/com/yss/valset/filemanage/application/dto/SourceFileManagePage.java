package com.yss.valset.filemanage.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 源文件收取管理分页查询条件，字段保持兼容前端生成 API。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SourceFileManagePage extends SourceFileManageQuery {

    /** 页码 */
    private Integer pageIndex;

    /** 每页条数 */
    private Integer pageSize;

    /** 分组字段 */
    private String groupBy;

    /** 排序字段 */
    private String orderBy;

    /** 排序方向 (ASC/DESC) */
    private String orderDirection;
}
