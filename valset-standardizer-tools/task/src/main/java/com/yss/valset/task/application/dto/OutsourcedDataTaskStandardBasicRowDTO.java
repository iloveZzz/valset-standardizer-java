package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值标准数据基础信息行。
 */
@Data
public class OutsourcedDataTaskStandardBasicRowDTO implements java.io.Serializable {

    private String category;

    private String fieldName;

    private String fieldValue;
}
