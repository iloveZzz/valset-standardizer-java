package com.yss.valset.task.application.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskBatchCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskActionResultDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDetailDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;

import java.util.List;

/**
 * 持仓穿透任务应用服务。
 */
public interface HoldingPenetrationTaskService {

    OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query);

    PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query);

    OutsourcedDataTaskBatchDetailDTO getTask(String batchId);

    List<OutsourcedDataTaskStepDTO> listSteps(String batchId);

    PageResult<OutsourcedDataTaskLogDTO> pageLogs(String batchId, String stage, Integer pageIndex, Integer pageSize);

    OutsourcedDataTaskActionResultDTO execute(String batchId, OutsourcedDataTaskActionCommand command);

    OutsourcedDataTaskActionResultDTO retry(String batchId, OutsourcedDataTaskActionCommand command);

    OutsourcedDataTaskActionResultDTO stop(String batchId, OutsourcedDataTaskActionCommand command);

    OutsourcedDataTaskActionResultDTO retryStep(String batchId, String stepId, OutsourcedDataTaskActionCommand command);

    List<OutsourcedDataTaskActionResultDTO> batchExecute(OutsourcedDataTaskBatchCommand command);

    List<OutsourcedDataTaskActionResultDTO> batchRetry(OutsourcedDataTaskBatchCommand command);

    List<OutsourcedDataTaskActionResultDTO> batchStop(OutsourcedDataTaskBatchCommand command);
}
