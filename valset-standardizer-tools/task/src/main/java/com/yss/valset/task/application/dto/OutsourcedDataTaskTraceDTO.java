package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 估值解析任务链路视图。
 */
@Data
public class OutsourcedDataTaskTraceDTO implements java.io.Serializable {

    private OutsourcedDataTaskBatchDTO batch;

    private OutsourcedDataTaskTraceRecordDTO parseQueue;

    private OutsourcedDataTaskTraceRecordDTO transferObject;

    private OutsourcedDataTaskTraceRecordDTO jobExecution;

    private List<OutsourcedDataTaskTraceRecordDTO> stepExecutions = new ArrayList<>();

    private List<OutsourcedDataTaskStepDTO> taskSteps = new ArrayList<>();

    private List<OutsourcedDataTaskTraceLogDTO> logs = new ArrayList<>();

    private OutsourcedDataTaskTraceResultSummaryDTO resultSummary;
}
