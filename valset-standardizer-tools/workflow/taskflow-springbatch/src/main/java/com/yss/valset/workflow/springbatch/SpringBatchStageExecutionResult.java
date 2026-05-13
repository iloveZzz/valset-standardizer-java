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
 *
 * <p>这个对象是单个阶段的标准输出，既包含执行状态，也包含输入、输出和元数据快照。
 * 上层会把它写入阶段日志，查询接口可以直接拿来做页面展示或故障排查。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SpringBatchStageExecutionResult {

    private WorkflowStatus status;

    /**
     * Spring Batch 或业务阶段对应的原始状态文本，便于排查底层执行结果。
     */
    private String rawStatus;

    /**
     * 阶段完成后的业务提示信息。
     */
    private String message;

    /**
     * 阶段执行开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 阶段执行结束时间。
     */
    private LocalDateTime endTime;

    /**
     * 阶段执行输入快照。
     */
    @Builder.Default
    private Map<String, Object> input = new LinkedHashMap<>();

    /**
     * 阶段执行输出快照。
     */
    @Builder.Default
    private Map<String, Object> output = new LinkedHashMap<>();

    /**
     * 阶段执行元数据，主要用于审计和追踪。
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
