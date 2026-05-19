package com.yss.valset.task.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 估值标准数据 Univer sheet 导出命令。
 */
@Data
public class OutsourcedDataTaskStandardDataExportCommand implements java.io.Serializable {

    private String tab;

    private String sheetName;

    private JsonNode workbookData;
}
