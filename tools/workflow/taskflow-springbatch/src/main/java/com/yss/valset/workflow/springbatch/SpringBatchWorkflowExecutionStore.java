package com.yss.valset.workflow.springbatch;

import org.springframework.batch.core.JobExecution;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Batch 执行快照存储。
 */
@Component
public class SpringBatchWorkflowExecutionStore {

    private final Map<Long, JobExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, Long> instanceIndex = new ConcurrentHashMap<>();
    private final Map<Long, List<WorkflowStageLogDTO>> stageLogs = new ConcurrentHashMap<>();

    public void save(String instanceId, JobExecution execution) {
        if (execution == null || execution.getId() == null) {
            return;
        }
        executions.put(execution.getId(), execution);
        if (StringUtils.hasText(instanceId)) {
            instanceIndex.put(instanceId, execution.getId());
        }
    }

    public Optional<JobExecution> findByExecutionId(Long executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(executions.get(executionId));
    }

    public Optional<JobExecution> findByInstanceId(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            return Optional.empty();
        }
        Long executionId = instanceIndex.get(instanceId);
        if (executionId == null) {
            return Optional.empty();
        }
        return findByExecutionId(executionId);
    }

    public Map<Long, JobExecution> snapshot() {
        return new LinkedHashMap<>(executions);
    }

    public void saveStageLog(Long executionId, WorkflowStageLogDTO stageLog) {
        if (executionId == null || stageLog == null) {
            return;
        }
        stageLogs.computeIfAbsent(executionId, ignored -> new ArrayList<>()).add(stageLog);
    }

    public List<WorkflowStageLogDTO> listStageLogs(Long executionId, String stageCode) {
        List<WorkflowStageLogDTO> logs = stageLogs.getOrDefault(executionId, List.of());
        if (!StringUtils.hasText(stageCode)) {
            return new ArrayList<>(logs);
        }
        List<WorkflowStageLogDTO> filtered = new ArrayList<>();
        for (WorkflowStageLogDTO log : logs) {
            if (log != null && stageCode.equals(log.getStageCode())) {
                filtered.add(log);
            }
        }
        return filtered;
    }
}
