package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值标准数据原始列定义。
 */
@Data
public class OutsourcedDataTaskStandardRawColumnDTO implements java.io.Serializable {

    private String fieldKey;

    private String title;

    private Integer columnIndex;
}
