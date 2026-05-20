package com.yss.valset.task.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;

/**
 * 估值解析批次原始工作簿快照。
 */
@Data
public class OutsourcedDataTaskRawWorkbookDTO implements Serializable {

    private String batchId;

    private Long fileId;

    private String fileName;

    private String sourceType;

    private Integer sheetCount;

    private Integer rowCount;

    private JsonNode workbookData;

    private Boolean downloadedFromTarget;

    private String fallbackMessage;
}
