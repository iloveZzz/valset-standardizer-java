package com.yss.valset.application.support;

import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务复用服务，用于在同一业务密钥下复用已成功完成的任务。
 */
@Slf4j
@Service
public class WorkflowTaskReuseService {

    private final WorkflowTaskGateway taskGateway;

    public WorkflowTaskReuseService(WorkflowTaskGateway taskGateway) {
        this.taskGateway = taskGateway;
    }

    /**
     * 按任务类型和业务密钥查找可复用的成功任务。
     *
     * @param taskType 任务类型
     * @param businessKey 业务密钥
     * @param forceRebuild 是否强制重新生成
     * @return 可复用任务；不存在则返回 null
     */
    public WorkflowTask findReusableSuccessfulTask(TaskType taskType, String businessKey, boolean forceRebuild) {
        if (forceRebuild || !supportsReuse(taskType)) {
            return null;
        }
        WorkflowTask reusableTask = taskGateway.findLatestSuccessfulTask(taskType, businessKey);
        if (reusableTask != null) {
            log.info("命中可复用任务，taskType={}, businessKey={}, taskId={}",
                    taskType, businessKey, reusableTask.getTaskId());
        }
        return reusableTask;
    }

    private boolean supportsReuse(TaskType taskType) {
        return taskType == TaskType.EXTRACT_DATA
                || taskType == TaskType.PARSE_WORKBOOK
                || taskType == TaskType.MATCH_SUBJECT;
    }
}
