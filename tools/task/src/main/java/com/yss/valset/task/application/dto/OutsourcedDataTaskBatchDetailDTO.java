package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 估值表解析任务批次详情。
 */
@Data
public class OutsourcedDataTaskBatchDetailDTO {

    private OutsourcedDataTaskBatchDTO batch;

    private List<OutsourcedDataTaskStepDTO> steps;

    private String currentBlockPoint;

    private String fileResultUrl;

    private String rawDataUrl;

    private String stgDataUrl;

    private String dwdDataUrl;
}
