package com.yss.valset.task.application.port;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceDTO;

import java.util.List;
import java.util.Optional;

/**
 * 估值表解析任务持久化网关。
 */
public interface OutsourcedDataTaskGateway {

    PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query);

    OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query);

    List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query);

    Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId);

    OutsourcedDataTaskTraceDTO getTrace(String batchId);

    List<OutsourcedDataTaskStepDTO> listSteps(String batchId);
}
