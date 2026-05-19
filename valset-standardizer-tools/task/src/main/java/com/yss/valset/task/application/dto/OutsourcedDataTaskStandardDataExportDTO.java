package com.yss.valset.task.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 估值标准数据导出结果。
 */
@Data
@AllArgsConstructor
public class OutsourcedDataTaskStandardDataExportDTO implements java.io.Serializable {

    private String fileName;

    private byte[] content;
}
