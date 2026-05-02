package com.yss.valset.task.application.port;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;

import java.util.List;
import java.util.Optional;

/**
 * 估值表解析任务持久化网关。
 */
public interface OutsourcedDataTaskGateway {

    PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query);

    List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query);

    Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId);

    List<OutsourcedDataTaskStepDTO> listSteps(String batchId);

    PageResult<OutsourcedDataTaskLogDTO> pageLogs(String batchId, String stage, Integer pageIndex, Integer pageSize);

    void recordParseLifecycleEvent(ParseLifecycleEvent event);

    void recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent event);
}
