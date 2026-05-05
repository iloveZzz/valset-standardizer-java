package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流运行参数持久化实体。
 */
@Data
@TableName("t_workflow_runtime_param")
public class WorkflowRuntimeParamPO {

    @TableId(value = "runtime_param_id", type = IdType.ASSIGN_ID)
    private String runtimeParamId;

    @TableField("param_namespace")
    private String paramNamespace;

    @TableField("skip_excel_style_parsing")
    private Boolean skipExcelStyleParsing;

    @TableField("enable_match_process")
    private Boolean enableMatchProcess;

    @TableField("persist_standardized_dwd_details")
    private Boolean persistStandardizedDwdDetails;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
