package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Batch 阶段执行结果。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SpringBatchStageExecutionResult {

    private WorkflowStatus status;

    private String rawStatus;

    private String message;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Builder.Default
    private Map<String, Object> input = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Object> output = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
