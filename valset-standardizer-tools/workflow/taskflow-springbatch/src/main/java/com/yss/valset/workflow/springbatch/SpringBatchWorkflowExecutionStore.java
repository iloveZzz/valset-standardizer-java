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
 *
 * <p>这里保存的是当前工作流运行态的内存快照，用来支撑以下场景：
 * <ul>
 *     <li>按 executionId / instanceId 回查最近一次作业</li>
 *     <li>回放作业级别的执行结果</li>
 *     <li>按阶段码过滤阶段日志</li>
 * </ul>
 *
 * <p>这个类当前是内存实现，后续如果切换为数据库或外部存储，
 * 对上层客户端的查询语义不需要改动。
 */
@Component
public class SpringBatchWorkflowExecutionStore {

    private final Map<Long, JobExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, Long> instanceIndex = new ConcurrentHashMap<>();
    private final Map<Long, List<WorkflowStageLogDTO>> stageLogs = new ConcurrentHashMap<>();

    public void save(String instanceId, JobExecution execution) {
        // 只保存已分配 executionId 的作业，避免未初始化对象污染快照。
        if (execution == null || execution.getId() == null) {
            return;
        }
        executions.put(execution.getId(), execution);
        if (StringUtils.hasText(instanceId)) {
            instanceIndex.put(instanceId, execution.getId());
        }
    }

    public Optional<JobExecution> findByExecutionId(Long executionId) {
        // 通过作业执行 ID 精确回查最近一次执行态。
        if (executionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(executions.get(executionId));
    }

    public Optional<JobExecution> findByInstanceId(String instanceId) {
        // 外部实例 ID 更适合用于查询接口和回调回写，这里提供一层反查索引。
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
        // 返回浅拷贝，避免调用方直接修改内部快照容器。
        return new LinkedHashMap<>(executions);
    }

    public void saveStageLog(Long executionId, WorkflowStageLogDTO stageLog) {
        // 阶段日志以 executionId 为主键聚合，保证同一次作业下的步骤顺序可回放。
        if (executionId == null || stageLog == null) {
            return;
        }
        stageLogs.computeIfAbsent(executionId, ignored -> new ArrayList<>()).add(stageLog);
    }

    public List<WorkflowStageLogDTO> listStageLogs(Long executionId, String stageCode) {
        // 支持按阶段码过滤，方便前端只查看某个步骤的日志。
        List<WorkflowStageLogDTO> logs = stageLogs.getOrDefault(executionId, java.util.Arrays.asList());
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
